# Minecraft 26.3 Fabric build and limited compatibility test

Status: compiles; short Windows and Linux X11/OpenGL smoke tests passed. This is an experimental
port, not a certification of the complete companion-mod compatibility suite.
Linux results below cover one AMD/Mesa machine and are not a general Linux
compatibility certification.

Checked on 2026-10-08 using the installed Elric 26.3 companion mods. Tests ran in
an isolated game directory with a generated creative world; the actual instance's
worlds and configuration were not used or modified. All ten companion JARs were
copied byte for byte from the instance. Only ReplayMod was rebuilt and installed.

## Tested mod set

| Mod | Version | Result in the tested workflow |
| --- | --- | --- |
| Emma Bridge | 0.2.0 | Passed with ReplayMod's optional shutdown workaround |
| Emma SMP Client | 0.1.0+26.3 | Loaded; no observed conflict |
| Emma EndInv | 1.4.6 | Loaded in the client and integrated server; no observed conflict |
| Entity Model Features | 3.3.8 | Loaded and rendered entities; no observed conflict |
| Entity Texture Features | 7.2.4 | Loaded and rendered entities; no observed conflict |
| Fabric API | 0.161.0+26.3 | Passed |
| Iris | 1.11.7+mc26.3 | Passed normal video export with the active shader pack |
| JourneyMap | 26.3-6.0.10 | Loaded through world recording and replay; no observed conflict |
| Sodium | 0.9.3-alpha.1+mc26.3 | Passed; its Flawless Frames integration was detected |
| Simple Voice Chat | 2.6.24+26.3 | Live connection authenticated; replay reconnect error fixed |

## Verification

Runtime: Minecraft 26.3, Fabric Loader 0.19.5, Prism's Java 25.0.1, Windows,
OpenGL on NVIDIA RTX 5080. The active shader pack was
`ComplementaryReimagined_r5.9.zip`, using a copy of the instance's Iris settings.

The test generated a world, recorded approximately nine seconds with several
spawned entity types, saved the recording, opened it, sought through playback,
captured and released the mouse, and pressed the native T key to open replay
controls. Native left clicks toggled Play/Pause twice. It exported 20 H.264 frames
at 640x360 and 20 fps, then closed the replay and Minecraft. FFprobe verified the
video, an extracted frame was visually inspected, and the final client exited
with code 0. Runs passed both without shaders and with Complementary enabled.

The selected ReplayStudio and ReplayMod automated test tasks passed 60 tests.
These checks are separate from the short in-game smoke test and do not cover
the complete feature or compatibility suites of the installed companion mods.
Build output: `build/logs/build-26.3-final-compat.log`.
Final runtime output: `build/compat-check/integration-installed-mods-final.log`.
Test source and evidence remain under `build/compat-check/` locally.

## Linux smoke test on emmabrain

Tested on 2026-10-08: Ubuntu 25.10, kernel 6.17.0-41-generic, Java 25.0.2,
Fabric Loader 0.19.5, Radeon 8060S / Ryzen AI Max+ 395, Mesa 25.2.8,
SDL X11 and OpenGL. Used a new isolated game directory and offline test identity;
no existing launcher accounts, instances or worlds were changed.

The same ten companion JARs and the Windows-tested ReplayMod JAR were copied
byte for byte and verified with SHA-256. Two short runs passed: shaders disabled,
then ComplementaryReimagined_r5.9.zip enabled. Each generated a creative world,
recorded about nine seconds, opened the recording, checked native T and left-click
controls, sought through playback, exported 20 H.264 frames at 640x360 / 20 fps,
and closed with process exit code 0. FFprobe confirmed both videos and their
first frames were visually inspected. Export sizes: 593025 bytes without shaders,
619128 bytes with shaders.

The first isolated launch needed Prism's separate Linux LWJGL component added
to the test classpath; that was test-launcher setup, not a ReplayMod source fix.
The shader compiler logged warnings but completed both rendering and shutdown.
Emma Bridge logged an Emmatone default-settings write warning on first launch;
custom AI actions and configuration persistence were not tested. Its shutdown
worker workaround completed in both runs.

No Wayland, Vulkan, other GPUs/drivers, multiplayer servers, long recordings or
complete companion-mod feature suites were tested. Selected log markers are in
[`test-evidence/linux-2026-10-08.txt`](test-evidence/linux-2026-10-08.txt).

## Fixes included in ReplayMod

1. **SDL unbound keys:** Minecraft 26.3 expects keyboard code 0 for an unbound
   key. Earlier port builds used -1, causing `InputConstants.isKeyDown` failures
   when the mouse was captured. Registration now uses 0, and an input mixin
   migrates ReplayMod's saved negative keyboard bindings when options load.
2. **Voice Chat replay authentication:** Recorded voice authentication packets
   caused Simple Voice Chat to interpret ReplayMod's embedded connection as a
   live server. An optional mixin skips that authentication during replay only.
   Live-world authentication still passed the test.
3. **Emma Bridge shutdown:** Emmatone cache workers in the installed 0.2.0 JAR
   remained alive after Minecraft closed, triggering its shutdown watchdog. An
   optional client-close hook stops their executor, waits for workers to finish,
   and flushes cached worlds. The installed Emma Bridge JAR is unchanged.

## Limits

These results cover startup, integrated-world recording, replay controls,
seeking, normal video export, and shutdown with this exact mod set. They do not
certify every custom AI action, multiplayer server protocol, cosmetic feature,
resource pack, panoramic export mode, or Linux configurations beyond the short
X11/OpenGL test described above.

Simple Voice Chat audio is not recorded by ReplayMod alone. The
[Replay Voice Chat addon](https://github.com/henkelmax/replay-voice-chat)
provides that feature; it is not installed in this instance. Its published source
currently targets 26.2, so a matching 26.3 port would need separate work.

## Artifact and installation

Distributable: `versions/26.3/build/libs/replaymod-26.3-2.6.27-dirty.jar`.
SHA-256: `aa577e771a463c510c71b4c63ad5e38337e24fe30406217322184769586e7ffc`.

The tested JAR replaced ReplayMod in the Elric 26.3 instance. The previous JAR
was backed up and hash verified under
`build/compat-check/prism-backup-2026-10-08/`; `installation.json` records the
original and installed hashes. Restart the instance to load the fixes. Press T
inside a replay to release the cursor and use its controls.

Installed Emma Bridge SHA-256:
`da1114e89c85e4b8459212ec090f06887f63df00bca1d918ce0cebfd5ae332b1`.
Its contents match the current master build in `EmmaMinecraft261` except for
the newer uncommitted `hero_supplies` change in `SetModeHandler.class`. The
older `EmmaMinecraft261-26.3` checkout was not used to replace the installed mod.
