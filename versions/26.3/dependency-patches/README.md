# Historical 26.3 dependency patches

The ported dependencies are committed to the DragonbornElric jGui and ReplayStudio
forks and pinned by this repository's submodules. A fresh recursive clone already
contains the changes; do not apply these patches to its current submodules.
The patches below are retained for reconstructing the port from the original
upstream base commits.

Base commits:

- jGui: `688febf61dd4629491b303037d72da38f92392cf`
- ReplayStudio: `06978a304fc023ed9cfe48ccc7c5f32d8d499460`

After initializing clean submodules at those commits, from the repository root:

```powershell
git -C jGui apply ../versions/26.3/dependency-patches/jgui.patch
git -C libs/ReplayStudio apply ../../versions/26.3/dependency-patches/replaystudio.patch
./build-26.3.ps1
```

The jGui patch adds SDL key/modifier handling, mouse-button conversion, 26.3 input
mixins, and RenderPearl mappings. ReplayStudio uses ViaVersion 5.12.1 and adapts
26.3 movement, position-path, join, and respawn packets with protocol tests.
