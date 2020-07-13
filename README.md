# ReplayMod
A Minecraft mod to record game sessions and replay them afterwards from any perspective.

## Building
Make sure your sub-projects are up-to-date: `git submodule update --init --recursive`

For compiling 1.7.10, you must run `./gradlew :jGui:1.7.10:setupDecompWorkspace :1.7.10:setupDecompWorkspace` once after the initial clone. This may take quite some time.

### No IDE
You can build the mod by running `./gradlew build` (or just `./gradlew shadowJar`). You can then find the final jar files in `versions/$MCVERSION/build/libs/`.
You can also build single versions by running `./gradlew :1.8:build` (or just `./gradlew :1.8:shadowJar`) (builds the MC 1.8 version).

### IntelliJ
Ensure you have at least IDEA 2020.1.
Build the mod via Gradle as explained above at least once (`./gradlew compileJava` should be sufficient). This will ensure that the sources for all MC versions are generated.
Then import the Gradle project from within IDEA: File -> Open -> build.gradle -> Open as Project
Finally configure IDEA to build everything by itself instead of delegating it to Gradle (cause that is slow): File -> Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Build and run using: IntelliJ IDEA

The generation of run configurations for 1.14+ currently depends on https://github.com/FabricMC/fabric-loom/pull/236

### Eclipse

## Development
### Branches
Loosely based on [this branching model](http://nvie.com/posts/a-successful-git-branching-model/) with `stable` instead of `master`.

TL;DR:
Main development happens on the `develop` branch, snapshots are built from this branch.
The `stable` branch contains the most recent release.

The `master` branch is solely to be used for the `version.json` file that contains a list of all versions
used by the clients to check for updates of this mod.

### The Preprocessor
To support multiple Minecraft versions with the ReplayMod, a [JCP](https://github.com/raydac/java-comment-preprocessor)-inspired preprocessor is used:
```java
        //#if MC>=11200
        // This is the block for MC >= 1.12.0
        category.addDetail(name, callable::call);
        //#else
        //$$ // This is the block for MC < 1.12.0
        //$$ category.setDetail(name, callable::call);
        //#endif
```
Any comments starting with `//$$` will automatically be introduced / removed based on the surrounding condition(s).
Normal comments are left untouched. The `//#else` branch is optional.

Conditions can be nested arbitrarily but their indention shall always be equal to the indention of the code at the `//#if` line.
The `//$$` shall be aligned with the inner-most `//#if`.
```java
    //#if MC>=10904
    public CPacketResourcePackStatus makeStatusPacket(String hash, Action action) {
        //#if MC>=11002
        return new CPacketResourcePackStatus(action);
        //#else
        //$$ return new CPacketResourcePackStatus(hash, action);
        //#endif
    }
    //#else
    //$$ public C19PacketResourcePackStatus makeStatusPacket(String hash, Action action) {
    //$$     return new C19PacketResourcePackStatus(hash, action);
    //$$ }
    //#endif
```
Code for the more recent MC version shall be placed in the first branch of the if-else-construct.
Version-dependent import statements shall be placed separately from and after all other imports.
Common version dependent code (including the fml and forge event bus) are available as static methods/fields in the `MCVer` class.

The source code in `src/main` is generally for the most recent Minecraft version and is automatically passed through the
preprocessor when any of the other versions are built (gradle projects `:1.8`, `:1.8.9`, etc.).
Do **NOT** edit any of the code in `versions/$MCVERSION/build/` as it is automatically generated and will be overwritten without warning.

You can change the version of the code in `src/main` if you wish to develop/debug with another version of Minecraft:
```bash
./gradle :1.9.4:setCoreVersion # switches all sources in src/main to 1.9.4
```
If you do so, you'll also have to refresh the project in your IDE.

Make sure to switch back to the most recent branch before committing!
Care should also be taken that switching to a different branch and back doesn't introduce any uncommitted changes (e.g. due to different indention, especially in case of nested conditions).

Some files may use the same preprocessor with different keywords.
If required, more file extensions and keywords can be added in the `preprocess` block of the `versions/common.gradle` script.

### Versioning
The ReplayMod uses the versioning scheme outlined [here](http://mcforge.readthedocs.io/en/latest/conventions/versioning/)
with three changes:
- No `MAJORAPI`, the ReplayMod does not provide any external API
- "Updating to a new Minecraft version" should not increment `MAJORMOD`, we maintain one version of the ReplayMod
for each version of Minecraft and all these versions share the same feature set (and most bugs). We therefore try to
keep the version name the same for all of them (with the exception of `MCVERSION` of course). This also means that the
"Multiple Minecraft Version" section does not apply.
- For pre-releases the shorter `-bX` is used instead of `-betaX`

When a new version is (pre-)release, a new commit modifying the `version.txt` file should be added and the
`versions.json` file in the `master` branch should be updated. To simplify this process the gradle task `doRelease` can
be used: `./gradlew -PreleaseVersion=2.0.0-rc1 doRelease`. It will create the commit and update the version.json
accordingly.

Care should be taken that the updated `version.json` is not pushed before a jar file is available on the
download page (or Jenkins) as it will inform the users of the update.

### Bugs
GitHub should generally be used to report bugs.

In the past, bugs were tracked via [Bugzilla](https://bugs.replaymod.com/), so bug numbers in commits prior to 2020 such as `(fixes #42)` generally referred to Bugzilla unless noted otherwise.

## License
The ReplayMod is provided under the terms of the GNU General Public License Version 3 or (at your option) any later version.
See `LICENSE.md` for the full license text.
