# ReplayMod
A Minecraft mod to record game sessions and replay them afterwards from any perspective.

## Building
Make sure your sub-projects are up-to-date: `git submodule update --init --recursive`

After the initial clone, running `./gradlew setupDecompWorkspace` is necessary. This may take quite some time.
This will also be necessary if the `replaymod_at.cfg` file has been changed (getting errors about code that is trying 
to access private fields is a good indication that this has happened).

**Note**: ForgeGradle 1.2 (used for 1.7.10) is not compatible with Gradle 4.
As such, you need to use an older version (Gradle 3.x) to build the ReplayMod for 1.7.10.
An easy way to do that is to pass `--old-gradle` as the first argument to the Gradle wrapper shell script: `./gradlew --old-gradle build`

### No IDE
You can build the mod by running `./gradlew build`. You can then find the final jar files in `versions/$MCVERSION/build/libs/`.
You can also build single versions by running `./gradlew :1.8:build` (builds the MC 1.8 version).

### IntelliJ
For the initial setup run `./gradlew idea genIntellijRuns`.
You also need to enable the Mixin annotation processor:
1. Go to File -> Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors
2. Tick "Enable annotation processing"
3. Add a new entry to the "Annotation Processor options"
4. Set the name to "reobfSrgFile" and the value to "$path/build/mcp-srg.srg" where you replace $path with the full 
path to the folder containing the gradlew file

Whenever you switch to a different core version (see `:setCoreVersion` below), you can either just run `./gradlew idea` or instead run
`./gradlew copySrg` and then refresh the gradle project from within IntelliJ.

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
Version-dependent import statements shall be placed separately from and after all other imports but before the `static` and `java.*` imports.
Common version dependent code (including the fml and forge event bus) are available as static methods/fields in the `MCVer` class.

The source code resides in `src/main` (gradle project `:core`) and is automatically passed through the
preprocessor when any of the concrete versions are built (gradle projects `:1.8`, `:1.8.9`, etc.).
Do **NOT** edit any of the code in `versions/$MCVERSION/build/` as it is automatically generated and will be overwritten without warning.

You can pass the original source code through the preprocessor if you wish to develop/debug with another version of Minecraft:
```bash
./gradle :1.9.4:setCoreVersion # switches all sources in src/main to 1.9.4
```
If you do so, you'll also have to run `./gradlew :core:copySrg :core:setupDecompWorkspace :jGui:core:setupDecompWorkspace`,
followed by a refresh of the project in your IDE.

Make sure to switch back to the most recent branch before committing!
Care should also be taken that switching to a different branch and back doesn't introduce any uncommitted changes (e.g. due to different indention, especially in case of nested conditions).

The `replaymod_at.cfg` file uses the same preprocessor but with different keywords (see already existent examples in that file).
If required, more file extensions and keywords can be added to the upstream implementation or the respective tasks.

### Versioning
The ReplayMod uses the versioning scheme outlined [here](http://mcforge.readthedocs.io/en/latest/conventions/versioning/)
with three changes:
- No `MAJORAPI`, the ReplayMod does not provide any external API
- "Updating to a new Minecraft version" should not increment `MAJORMOD`, we maintain one version of the ReplayMod
for each version of Minecraft and all these versions share the same feature set (and most bugs). We therefore try to
keep the version name the same for all of them (with the exception of `MCVERSION` of course). This also means that the
"Multiple Minecraft Version" section does not apply.
- For pre-releases the shorter `-bX` is used instead of `-betaX`

When a new version is (pre-)release, a new annotated tag should be added with the name of the version and the
`versions.json` file in the `master` branch should be updated. To simplify this process the gradle task `doRelease` can
be used: `./gradlew -PreleaseVersion=2.0.0-rc1 doRelease`. It will create the tag and update the version.json
accordingly.
(**Dev Note**: The gradle task still needs to be updated, now that all versions are on a single branch!
We're also considering to change the naming of versions to be commit-based, rather than tag-based.
So you don't get a different version if you haven't pulled all the tags.)

Care should be taken that the updated `version.json` is not pushed before a jar file is available on the
download page (or Jenkins) as it will inform the users of the update.

### Bugs
Bugs in the mod are tracked via [Bugzilla](https://bugs.replaymod.com/).
GitHub should only be used for issues that are generally not likely to affect any end users.

Bug numbers in commits such as `(fixes #42)` refer to Bugzilla unless noted otherwise.

## License
The ReplayMod is provided under the terms of the GNU General Public License Version 3 or (at your option) any later version.
See `LICENSE.md` for the full license text.