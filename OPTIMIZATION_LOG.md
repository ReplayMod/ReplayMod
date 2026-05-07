# Optimization Log

| ID          | Status      | Description                           | Before | After | Delta | Commit |
|-------------|-------------|---------------------------------------|--------|-------|-------|--------|
| SEEK-01     | DONE        | Keyframe index seek                   | O(t) full replay scan for long jumps | auto RandomAccessReplay/QuickMode seek | benchmark unavailable | pending |
| SEEK-02     | SKIP        | QuickMode parallel index scan         | Stateful ReplayInputStream scan | Benchmark task missing | n/a | - |
| PARALLEL-01 | DONE        | Shared ForkJoinPool + IO_POOL         | ad-hoc quick init thread | shared REPLAY_POOL/IO_POOL | n/a | 637d4b1f |
| PARALLEL-02 | DONE        | Async chunk deserialization           | sender-thread packet conversion | REPLAY_POOL one-packet prefetch | benchmark unavailable | pending |
| PARALLEL-03 | SKIP        | Dirty-flag chunk rebuild              | vanilla dirty chunk set | existing `chunksToRebuild` gate | n/a | - |
| GPU-01      | DONE        | Multi-threaded mesh build             | render-blocking rebuild wait | vanilla ChunkBuilder workers + blocking upload drain | benchmark unavailable | pending |
| GPU-02      | DONE        | FFmpeg VAAPI/NVENC export             | libx264-only default presets | hardware H.264 selector incl. NVENC/VAAPI | benchmark unavailable | pending |
| MEM-01      | DONE        | Location object pooling               | retained `Location` per entity sample | primitive position arrays | benchmark unavailable | pending |
| MEM-02      | DONE        | Primitive long map                    | `TreeMap<Long, Location>` | primitive sorted timestamp arrays | benchmark unavailable | pending |
| MEM-03      | SKIP        | LRU chunk cache                       | client world owns loaded chunks | unsafe without ClientChunkManager rewrite | n/a | - |

## Session Notes

- 2026-05-07: Session checklist started. Created Nix flake dev shell and added Java 8/16/17/21 toolchain paths.
- 2026-05-07: Root `build` is blocked by `:26.1` requiring Java 25, which is not available in the current Nix input.
- 2026-05-07: `:1.19.4:build` and `:1.19.4:test` are blocked by existing 1.19.4 compile errors before SEEK-01 code can be verified.
- 2026-05-07: `runSeekBenchmark` cannot be reached because root Gradle configuration fails on `:26.1` Java 25.
- 2026-05-07: Added JDK 25 to the Nix dev shell and exported JDK8/16/17/21/25 homes. Gradle detects all toolchains when `$REPLAYMOD_GRADLE_TOOLCHAINS` is passed on the command line.
- 2026-05-07: SEEK-02 full byte-range parallelization skipped for now: `RandomAccessReplay` depends on ordered `ReplayInputStream`/ViaVersion state, and `runQuickIndexBenchmark` is not defined in this repo.
- 2026-05-07: PARALLEL-01 implemented shared `ReplayExecutors`; QuickMode initialization now runs on `REPLAY_POOL` behind `optimizedQuickModeInitialization=true`.
- 2026-05-07: Verification: `./gradlew --no-daemon $REPLAYMOD_GRADLE_TOOLCHAINS :1.19.4:compileJava` passed; `./gradlew --no-daemon $REPLAYMOD_GRADLE_TOOLCHAINS :1.19.4:test` passed.
- 2026-05-07: Root `./gradlew --no-daemon build` now passes toolchain configuration but fails on existing `:1.12.2:compileJava` `GuiButton.func_214457_x` errors.
- 2026-05-07: SEEK-01 implemented as safe auto QuickMode seek for large interactive jumps. Direct raw-offset FullReplaySender resume is not safe because `ReplayInputStream` and ViaVersion maintain ordered protocol state.
- 2026-05-07: PARALLEL-02 implemented one-packet `REPLAY_POOL` prefetch in `FullReplaySender`, overlapping packet conversion with current packet handling.
- 2026-05-07: GPU-02 hardware selector extended with VAAPI and documented in `README_DEV.md`.
- 2026-05-07: MEM-01/MEM-02 implemented in ReplayStudio `EntityPositionTracker` by replacing `TreeMap<Long, Location>` with primitive sorted arrays.
- 2026-05-07: PARALLEL-03 skipped: this 1.19.4 path already consumes Minecraft's dirty `chunksToRebuild` set rather than rebuilding all chunks every frame.
- 2026-05-07: MEM-03 skipped: loaded chunk lifetime is owned by Minecraft `ClientChunkManager`; evicting chunks behind it would corrupt render/world state without a larger manager rewrite.
- 2026-05-07: Fixed cross-version GUI preprocessing regressions found by root `build`: 1.12.2 `GuiButton.x` access and 1.17.1-1.19.1 replay button render override.
- 2026-05-07: Fixed old Forge unit-test bootstrap: jGui event bridge now registers only under LaunchClassLoader, so 1.8.9/1.9.4 `SPTimelineTest` can run under Gradle.
- 2026-05-07: Root verification passed: `./gradlew --no-daemon $REPLAYMOD_GRADLE_TOOLCHAINS build` and `./gradlew --no-daemon $REPLAYMOD_GRADLE_TOOLCHAINS test`.
- 2026-05-07: Benchmark tasks requested by AGENT.md are not defined in this Gradle build (`tasks --all` has no `run*Benchmark` entries).
- 2026-05-07: Client launch verified for 1.19.4 under Xvfb + llvmpipe + null OpenAL. Minecraft reached resource reload/atlas creation and ran until intentional timeout; log scan found 0 `ERROR`, `Exception`, `Crash`, `FAILED`, or `GLFW error` entries.
- 2026-05-07: Ported the current render/NVENC optimization set through the 1.21.4 preprocessing chain. `:1.21.4:build` passed, producing `versions/1.21.4/build/libs/replaymod-1.21.4-2.6.26-1-g50ef322-dirty.jar`.
- 2026-05-07: Native OpenGL/NVENC is now optional by default for the optimized render path. Missing or failed `replaymod_native_encoder` falls back to the FFmpeg rawvideo path; `replaymod.nativeEncoder.required=true` / `REPLAYMOD_NATIVE_ENCODER_REQUIRED=true` keeps the previous fail-fast behavior.
- 2026-05-07: Reduced simple pathing entity tracker stalls for 1.19.4 and 1.21.4. Normal time/position keyframe add/edit/drag no longer blocks on loading entity positions; the tracker is loaded only when spectator keyframes need it, with progress updates throttled to 1% increments.
- 2026-05-07: Added a start-time index for path segments so timeline property lookup during playback/render is O(log n) instead of scanning every segment. Also reduced path preview work by sampling normal path segments according to duration and removing temporary vector allocation from distance checks. `:ReplayStudio:test`, `:1.19.4:build`, and `:1.21.4:build` passed.
- 2026-05-07: Added `hideDisplayEntities` replay setting, default ON, to skip Block/Item/Text Display Entity rendering while a replay is active. This replaces the need for an external client tweak for that specific replay-view/render optimization and can be toggled in Replay Mod settings.
- 2026-05-07: Verification: `:1.19.4:build` and `:1.21.4:build` passed. Client launch checks for both versions reached resource reload and atlas creation under WSLg/llvmpipe after adding Mesa/libglvnd to `LD_LIBRARY_PATH`; no mixin apply failure, injection error, GLFW error, or crash was found. Remaining startup errors are environment-only: offline auth 401 on 1.19.4 and missing `libflite.so` narrator library on both versions.
