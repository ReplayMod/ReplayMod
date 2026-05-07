# AGENTS.md — ReplayMod 1.19.4 Optimization Agent

## 🎯 Mission

You are an autonomous senior Minecraft mod engineer working on a Fabric 1.19.4 fork
of ReplayMod. Your goals, in priority order:

1. **Keyframe-based seek** — replace O(t) full-scan seek with O(1) keyframe seek
2. **Memory reduction** — eliminate heap waste in replay data structures
3. **Multi-core utilization** — parallelize all CPU-bound work
4. **GPU utilization** — hardware-accelerated rendering and MP4 export

Never stop between the steps of one optimization cycle.
Persist until every target in `OPTIMIZATION_LOG.md` is `DONE` or `SKIP`.

---

## 🖥️ Environment (Nix)

This repo uses a **Nix flake** dev environment. Always work inside it.

### Entering the dev shell
```bash
nix develop          # enter devShell (Java 17, Gradle, FFmpeg, Mesa, etc.)
# OR if direnv is configured:
direnv allow
```

### If `flake.nix` does not exist — CREATE IT FIRST
```bash
ls flake.nix 2>/dev/null || echo "MISSING"
```
If missing, create `flake.nix` at the repo root:

```nix
{
  description = "ReplayMod 1.19.4 dev environment";

  inputs.nixpkgs.url     = "github:NixOS/nixpkgs/nixos-23.11";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let pkgs = nixpkgs.legacyPackages.${system}; in {
        devShells.default = pkgs.mkShell {
          buildInputs = with pkgs; [
            jdk17 gradle
            ffmpeg-full          # includes VAAPI, libx264, libx265
            vulkan-tools mesa libGL libGLU
            pkg-config git
          ];
          shellHook = ''
            export JAVA_HOME=${pkgs.jdk17}
            export PATH=$JAVA_HOME/bin:$PATH
            echo "ReplayMod dev shell — Java: $(java -version 2>&1 | head -1)"
            echo "CPUs: $(nproc)  |  DRI: $(ls /dev/dri/ 2>/dev/null | tr '\n' ' ')"
          '';
        };
      });
}
```
Commit `flake.nix` + `flake.lock` before any other change.

---

## 📁 Repository Structure

```
replaymod/
├── flake.nix                               # Nix dev env (create if missing)
├── flake.lock
├── build.gradle
├── OPTIMIZATION_LOG.md                     # You maintain this file
└── src/main/java/com/replaymod/
    ├── replay/
    │   ├── ReplayHandler.java              # Replay session lifecycle
    │   ├── FullReplaySender.java           # ⚠️ SEEK-01  O(t) seek
    │   ├── QuickReplaySender.java          # ⚠️ SEEK-02  slow index build
    │   └── renderer/                       # ⚠️ GPU-01   mesh upload
    ├── replaystudio/
    │   └── util/Location.java              # ⚠️ MEM-01   allocation hotspot
    ├── render/
    │   └── VideoRenderer.java              # ⚠️ GPU-02   MP4 export
    └── mixin/                              # Chunk force-load mixins
```

---

## 🔄 Autonomous Optimization Loop

```
SESSION START → run Session Start Checklist (bottom of file)

LOOP:
  1. READ OPTIMIZATION_LOG.md → pick first PENDING target
  2. ANALYZE source files (grep + read; never guess)
  3. DESIGN  → write a // PLAN: comment block before any code changes
  4. IMPLEMENT
  5. BUILD   → ./gradlew build → fix errors, rebuild
  6. TEST    → ./gradlew test  → fix failures, retest
  7. BENCHMARK → run the target's specific command (see each target below)
  8. EVALUATE:
       improvement > 2%  → keep changes + commit
       improvement ≤ 2%  → git checkout -- . (revert) + mark REVERTED
  9. UPDATE OPTIMIZATION_LOG.md (status, before/after metric, commit hash)
  10. COMMIT → format: "perf(SEEK-01): keyframe seek — time -94%"
  11. GOTO 1

STOP only when all targets are DONE | REVERTED | SKIP
```

---

## 🎯 Optimization Targets

---

### ★ SEEK — Keyframe-Based Seek  ← Start here

#### SEEK-01: Keyframe index + snapshot-based backward seek

**Problem (current behavior)**
`FullReplaySender.seekTo(long targetMs)` processes every packet from timestamp 0
to reach `targetMs`. Seeking to 1 hour = processing ~72,000 packets sequentially.
This is O(total_time) and blocks the game thread the entire time.

**Target behavior**
- On replay open, build a *keyframe index* in the background.
- Each keyframe = world snapshot at a fixed interval (default every 30 s).
- `seekTo(T)`:
  1. Binary-search index for largest keyframe ≤ T  → O(log n)
  2. Restore that snapshot into the live world
  3. Fast-forward only the packets between that keyframe and T  → O(30 s of data)
- Any seek in any length replay now takes ≤ 500 ms.

**Files to create**

`KeyframeIndex.java`
```java
// Scans .mcpr zip once on a background thread (IO_POOL, see PARALLEL-01).
// Every KEYFRAME_INTERVAL_MS (config, default 30_000):
//   record { long timestampMs, long fileByteOffset, byte[] snapshotLz4 }
// Persists as sidecar: <name>.mcpr.idx  (skip re-scan if mtime matches)
// Exposes: Entry floorEntry(long ms)  — binary search, O(log n)
```

`WorldSnapshot.java`
```java
// Serializes / restores:
//   - All loaded chunk NBT data (ClientLevel.getChunk)
//   - Entity tracker positions (EntityTrackerEntry list)
// Compression: LZ4 block (add to build.gradle: lz4-java 1.8.0)
```

**Modify `FullReplaySender.seekTo()`**
```java
// OLD: restart fileStream at offset 0, process all packets until targetMs
// NEW:
KeyframeIndex.Entry kf = index.floorEntry(targetMs);
worldSnapshot.restore(kf.snapshotLz4);          // restore world state
fileStream.seek(kf.fileByteOffset);             // jump file cursor
processPacketsUntil(targetMs);                  // ≤ KEYFRAME_INTERVAL_MS worth
```

**Dependencies to add in `build.gradle`**
```groovy
implementation 'org.lz4:lz4-java:1.8.0'
```

**Benchmark**
```bash
./gradlew runSeekBenchmark --args="src/test/resources/sample.mcpr 3600000"
# Reports: seek latency (ms) to 1h mark
# Target:  < 500 ms regardless of replay length
```

---

#### SEEK-02: QuickReplaySender — parallel index build

**Problem**: Single-threaded full-file scan at QuickMode init. 1 GB ≈ 12 s wait.

**Fix**
```java
// Partition file into N = availableProcessors() byte-ranges.
// Scan each range on REPLAY_POOL (ForkJoinPool, see PARALLEL-01).
// Merge N sorted partial indexes with a k-way merge.
// Wrap result in CompletableFuture<QuickIndex> — do not block the UI thread.
// Show existing ProgressPopup during build.
```

**Benchmark**
```bash
./gradlew runQuickIndexBenchmark --args="src/test/resources/sample.mcpr"
# Reports: index build time (ms) — single-core vs N-core
```

---

### PARALLEL — Multi-Core Utilization

#### PARALLEL-01: Shared thread pools (`ReplayExecutors.java`)

**Create** `src/main/java/com/replaymod/replay/ReplayExecutors.java`:
```java
public final class ReplayExecutors {
    // CPU-bound work (mesh build, index merge, keyframe scan)
    public static final ForkJoinPool REPLAY_POOL =
        new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));

    // IO-bound work (packet read, file seek, chunk eviction)
    public static final ExecutorService IO_POOL =
        Executors.newFixedThreadPool(
            Math.min(8, Runtime.getRuntime().availableProcessors()),
            r -> { Thread t = new Thread(r, "replaymod-io"); t.setDaemon(true); return t; }
        );

    // Shutdown: call from ReplayHandler.onReplayClose()
    public static void shutdown() {
        REPLAY_POOL.shutdown();
        IO_POOL.shutdown();
    }
}
```
Migrate: KeyframeIndex scan, QuickMode index build, chunk deserialization.

#### PARALLEL-02: Async chunk packet deserialization

**Problem**: `FullReplaySender` deserializes `ClientboundLevelChunkWithLightPacket`
on the game thread, causing frame stalls during heavy packet bursts.

**Fix**
- Pre-read next 256 raw packet bytes on `IO_POOL`
- Deserialize on `REPLAY_POOL` workers
- Hand off to game thread via `LinkedBlockingDeque<Packet<?>>` (capacity 128)
- Game thread calls `Minecraft.execute(packet::handle)` only

**Benchmark**
```bash
./gradlew runThroughputBenchmark --args="src/test/resources/sample.mcpr 60000"
# Reports: packets/sec, 99p frame stall time (ms)
```

#### PARALLEL-03: Dirty-flag chunk rebuild

**Problem**: Every rendered frame rebuilds ALL chunk meshes, even static ones.

**Fix**
```java
// THREADING: populated by IO_POOL packet handler; consumed by render thread
private final Set<ChunkPos> dirtyChunks = ConcurrentHashMap.newKeySet();

// On chunk packet arrival: dirtyChunks.add(pos)
// In WorldRenderer mixin force-rebuild hook:
//   only call chunk.rebuildMesh() if dirtyChunks.remove(pos) returns true
```

**Benchmark**
```bash
./gradlew runStaticSceneBenchmark --args="src/test/resources/sample.mcpr 300"
# Reports: ms/frame on a 5 s completely static scene
# Target:  < 2 ms/frame (essentially no rebuild work)
```

---

### GPU — Hardware Acceleration

#### GPU-01: Multi-threaded chunk mesh building

**Problem**: Chunk geometry (vertex data) is built on the render thread.
On scenes with many chunks this dominates frame time.

**Fix**
- Move vertex data *construction* to `REPLAY_POOL` workers (pure CPU, no GL calls)
- Keep VBO *upload* (`glBufferData`) on the render thread (OpenGL requirement)
- Hand off via `ConcurrentLinkedQueue<BuiltMesh>`
- Budget: max 4 ms of VBO uploads per frame; defer remainder to next frame

**Sodium check**: If `SodiumWorldRenderer` is on the classpath at runtime,
mark GPU-01 as `SKIP` — Sodium handles this already. Log the skip reason.

**Benchmark**
```bash
./gradlew runMeshBenchmark --args="src/test/resources/sample.mcpr 600"
# Reports: avg frame time (ms), VBO uploads/frame
```

#### GPU-02: Hardware-accelerated MP4 export (VAAPI / NVENC)

**Problem**: `VideoRenderer` always uses software `libx264`. On Linux with
Intel/AMD, VAAPI encodes 10–30× faster. NVIDIA NVENC is similarly fast.

**Fix — encoder detection**
```java
// Run at export start (once):
//   ffmpeg -encoders 2>&1 | grep -E "h264_nvenc|h264_vaapi|h264_videotoolbox"
// Return best available HwEncoder enum value; fall back to SOFTWARE silently.
public enum HwEncoder { NVENC, VAAPI, VIDEOTOOLBOX, SOFTWARE }
```

**FFmpeg argument sets**
```java
// VAAPI (Linux — available via ffmpeg-full in Nix devShell + mesa)
List.of("-vaapi_device", "/dev/dri/renderD128",
        "-vf", "format=nv12,hwupload",
        "-c:v", "h264_vaapi", "-qp", "23")

// NVENC (requires nvidia driver outside the flake; document in README_DEV.md)
List.of("-c:v", "h264_nvenc", "-preset", "p4", "-rc", "vbr", "-cq", "23")

// Software fallback
List.of("-c:v", "libx264", "-preset", "fast", "-crf", "23")
```

Show active encoder name in the render settings screen (small label).
Document NVENC requirements in `README_DEV.md`.

**Benchmark**
```bash
./gradlew runExportBenchmark --args="src/test/resources/sample.mcpr 0 60000"
# Reports: export time for 1-min clip (seconds), encoder name used
```

---

### MEMORY — Heap Reduction

#### MEM-01: Location object pooling

**File**: `replaystudio/util/Location.java`
**Problem**: New `Location` per packet. Top heap consumer per JProfiler
(`Location`, `TreeMap$Entry`, boxed `Long`).
**Fix**: `ArrayDeque<Location>` pool with `acquire()` / `release()`, or flatten
to `long[]` (pack xyz as fixed-point millimetres, yaw/pitch as shorts).
**Benchmark**: `./gradlew runMemoryBenchmark` — heap MB after 10-min replay load + GC.

#### MEM-02: Replace `TreeMap<Long, ?>` with primitive long map

**Files**: `FullReplaySender.java`, `QuickReplaySender.java`
**Fix**: Eclipse Collections `LongObjectHashMap` or sorted `long[]` + binary search.
Add: `implementation 'org.eclipse.collections:eclipse-collections:11.1.0'`
**Benchmark**: heap delta during `QuickReplaySender` init.

#### MEM-03: LRU chunk cache with configurable heap budget

**Problem**: All chunks in JVM heap. 64+ render distance → OOM.
**Fix**: Track chunk data size. When > `replaymod.chunkCacheBudgetMB` (default 512),
evict LRU chunks to `java.io.tmpdir` async on `IO_POOL`. Re-read on demand.
**Benchmark**: peak heap (MB) during 128-chunk-distance playback.

---

## 📊 OPTIMIZATION_LOG.md — Maintenance

Create if missing. Update after every target.

```markdown
# Optimization Log

| ID          | Status  | Description                           | Before | After | Delta | Commit |
|-------------|---------|---------------------------------------|--------|-------|-------|--------|
| SEEK-01     | PENDING | Keyframe index seek                   | -      | -     | -     | -      |
| SEEK-02     | PENDING | QuickMode parallel index scan         | -      | -     | -     | -      |
| PARALLEL-01 | PENDING | Shared ForkJoinPool + IO_POOL         | -      | -     | -     | -      |
| PARALLEL-02 | PENDING | Async chunk deserialization           | -      | -     | -     | -      |
| PARALLEL-03 | PENDING | Dirty-flag chunk rebuild              | -      | -     | -     | -      |
| GPU-01      | PENDING | Multi-threaded mesh build             | -      | -     | -     | -      |
| GPU-02      | PENDING | FFmpeg VAAPI/NVENC export             | -      | -     | -     | -      |
| MEM-01      | PENDING | Location object pooling               | -      | -     | -     | -      |
| MEM-02      | PENDING | Primitive long map                    | -      | -     | -     | -      |
| MEM-03      | PENDING | LRU chunk cache                       | -      | -     | -     | -      |
```

Status flow: `PENDING` → `IN_PROGRESS` → `DONE` | `REVERTED` | `SKIP`

---

## ✅ Code Quality Rules

- **No regressions**: `./gradlew test` passes before every commit.
- **Thread safety**: Every new thread has a `// THREADING: start/stop/interrupt`
  comment documenting its lifecycle.
- **Config-gated**: Behavior changes are behind a boolean flag defaulting `true`.
- **Revert threshold**: ≤ 2% improvement → revert → `REVERTED`.
- **Java 17**: No preview features.
- **Headless only**: No full Minecraft client launch. Use Gradle benchmark tasks.
- **Nix shell**: Every `./gradlew` command runs inside `nix develop`.

---

## 🚫 Out of Scope

- `.mcpr` wire format compatibility (no serialization breaks)
- 1.20+ code paths
- Forge / NeoForge (Fabric only)
- GUI / camera path editor

---

## 📝 Session Start Checklist

```bash
# 1. Enter Nix dev shell
nix develop

# 2. Verify flake.nix exists (create if not — see Environment section)
ls flake.nix || echo "CREATE flake.nix FIRST"

# 3. Clean working tree check
git branch --show-current && git status

# 4. Probe hardware (informs GPU and PARALLEL targets)
nproc
lspci | grep -iE "vga|3d|nvidia|amd|intel" || true
ls /dev/dri/ 2>/dev/null || echo "no DRI device detected"
ffmpeg -encoders 2>/dev/null | grep -E "h264_nvenc|h264_vaapi" || echo "no HW encoder"

# 5. Baseline build + tests
./gradlew build 2>&1 | tail -5
./gradlew test  2>&1 | tail -10

# 6. Read optimization log
cat OPTIMIZATION_LOG.md 2>/dev/null || echo "MISSING — will create at loop start"

# 7. Begin loop from first PENDING target in OPTIMIZATION_LOG.md
```
