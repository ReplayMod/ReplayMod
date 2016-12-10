# ReplayMod
A Minecraft mod to record game sessions and replay them afterwards from any perspective.

## Building
Make sure your sub-projects are up-to-date: `git submodule update --init --recursive`

For each branch you visit the first time, running `./gradlew setupDecompWorkspace` is necessary. 
This will also be necessary if the `replaymod_at.cfg` file has been changed (getting errors about code that is trying 
to access private fields is a good indication that this has happened).

### No IDE
You can build the mod by running `./gradlew :build`. You can then find the final jar files in `build/libs/`.

### IntelliJ
For the initial setup run `./gradlew preshadowJar idea genIntellijRuns`.
You also need to enable the Mixin annotation processor:
1. Go to File -> Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors
2. Tick "Enable annotation processing"
3. Add a new entry to the "Annotation Processor options"
4. Set the name to "reobfSrgFile" and the value to "$path/build/mcp-srg.srg" where you replace $path with the full 
path to the folder containing the gradlew file

Whenever you switch to another branch, you can either just run `./gradlew preshadowJar idea` or instead run 
`./gradlew preshadowJar copySrg` and then refresh the gradle project from within IntelliJ.

### Eclipse

## Development
### Branches
New features are developed on the `1.8` branch and merged upwards for release. Larger features are developed on their
own branch that is based on `1.8` and then merged back once it's finished.

Features or bug fixes that apply only to a specific Minecraft version are fixed on the branch corresponding to that 
version and reverted when merging upwards.

Both subprojects follow a similar branching model.

The user documentation (`docs` folder) is only committed to on the current development branch. Any changes committed on
a branch for a more recent Minecraft version are not kept up to date on the website and should only be
of importance once support for the previous Minecraft version is dropped.

The `master` branch is solely to be used for the `version.json` file that contains a list of all versions
used by the clients to check for updates of this mod.

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
be used: `./gradlew -PreleaseVersion=1.8-2.0.0-rc1 doRelease`. It will create the tag and update the version.json
accordingly.

Care should be taken that the updated `version.json` is not pushed before a jar file is available on the
download page (or Jenkins) as it will inform the users of the update.

### Bugs
Bugs in the mod are tracked via [Bugzilla](https://bugs.replaymod.com/).
GitHub should only be used for issues that are generally not likely to affect any end users.

Bug numbers in commits such as `(fixes #42)` refer to Bugzilla unless noted otherwise.

## License
The ReplayMod is provided under the terms of the GNU General Public License Version 3 or (at your option) any later version.
See `LICENSE.md` for the full license text.