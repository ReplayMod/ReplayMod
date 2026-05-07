# Optimization Log

| ID          | Status      | Description                           | Before | After | Delta | Commit |
|-------------|-------------|---------------------------------------|--------|-------|-------|--------|
| SEEK-01     | IN_PROGRESS | Keyframe index seek                   | -      | -     | -     | -      |
| SEEK-02     | SKIP        | QuickMode parallel index scan         | Stateful ReplayInputStream scan | Benchmark task missing | n/a | - |
| PARALLEL-01 | DONE        | Shared ForkJoinPool + IO_POOL         | ad-hoc quick init thread | shared REPLAY_POOL/IO_POOL | n/a | 637d4b1f |
| PARALLEL-02 | PENDING     | Async chunk deserialization           | -      | -     | -     | -      |
| PARALLEL-03 | PENDING     | Dirty-flag chunk rebuild              | -      | -     | -     | -      |
| GPU-01      | PENDING     | Multi-threaded mesh build             | -      | -     | -     | -      |
| GPU-02      | PENDING     | FFmpeg VAAPI/NVENC export             | -      | -     | -     | -      |
| MEM-01      | PENDING     | Location object pooling               | -      | -     | -     | -      |
| MEM-02      | PENDING     | Primitive long map                    | -      | -     | -     | -      |
| MEM-03      | PENDING     | LRU chunk cache                       | -      | -     | -     | -      |

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
