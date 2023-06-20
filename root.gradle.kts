import groovy.json.JsonOutput
import java.io.ByteArrayOutputStream

plugins {
    id("fabric-loom") version "0.11-SNAPSHOT" apply false
    id("com.replaymod.preprocess") version "48e02ad"
    id("com.github.hierynomus.license") version "0.15.0"
}

val latestVersion = file("version.txt").readLines().first()
var releaseCommit = command("git", "blame", "-p", "-l", "version.txt").first().split(" ").first()
if (latestVersion == "2.1.0") { // First version since change from tag-based
    releaseCommit = "35ac26e91689ac9bdf12dbb9902c452464a75108" // git rev-parse 1.12.2-2.1.0
}
val currentCommit = command("git", "rev-parse", "HEAD").first()
if (releaseCommit == currentCommit) {
    version = latestVersion
} else {
    val diff = command("git", "log", "--format=oneline", "$releaseCommit..$currentCommit").size
    version = "$latestVersion-$diff-g${currentCommit.substring(0, 7)}"
}
if (gitDescribe().endsWith("*")) {
    version = "$version-dirty"
}

group = "com.replaymod"

val bundleJar by tasks.creating(Copy::class) {
    into("$buildDir/libs")
}

subprojects {
    buildscript {
        repositories {
            maven("https://jitpack.io")
        }
    }

    afterEvaluate {
        val projectBundleJar = project.tasks.findByName("bundleJar")
        if (projectBundleJar != null && projectBundleJar.hasProperty("archivePath") && project.name != "core") {
            bundleJar.dependsOn(projectBundleJar)
            bundleJar.from(projectBundleJar.withGroovyBuilder { getProperty("archivePath") })
        }
    }
}

fun gitDescribe(): String {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--always", "--dirty=*")
            standardOutput = stdout
        }
        return stdout.toString().trim()
    } catch (e: Throwable) {
        return "unknown"
    }
}

fun command(vararg cmd: Any): List<String> {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine(*cmd)
        standardOutput = stdout
    }
    return stdout.toString().trim().split("\n")
}

fun generateVersionsJson(): Map<String, Any> {
    val versionComparator = compareBy<String>(
        { (it.split(".").getOrNull(0) ?: "0").toInt() },
        { (it.split(".").getOrNull(1) ?: "0").toInt() },
        { (it.split(".").getOrNull(2) ?: "0").toInt() }
    )

    // Find all tag-style releases by listing all tags
    val tagVersions = command("git", "for-each-ref", "--sort=taggerdate", "--format=%(refname:short)", "refs/tags")

    // Find all commit-style releases
    // List all commits
    val releaseCommits =
            command("git", "log", "--format=%H", "--date-order", "-E",
                    "--grep", "Pre-release [0-9]+ of [0-9]+\\.[0-9]+\\.[0-9]+",
                    "--grep", "Release [0-9]+\\.[0-9]+\\.[0-9]+")
    // Find version string and MC versions for each commit hash
    val commitVersions = releaseCommits.map { commit ->
        val version = command("git", "show", "$commit:version.txt").first()
        val mcVersions = command("git", "ls-tree", "-d", "--name-only", "$commit:versions")
                // In the past (early days of preprocessor) we used to have an internal "core" project
                .filter { it != "core" }
                // Internal project used to automatically remap from Forge 1.12.2 to Fabric 1.14.4
                .filter { it != "1.14.4-forge" }
                // We dropped 1.7.10 with the Gradle 7 update but still kept its source in case someone
                // volunteers to update FG 1.2 to Gradle 7.
                .filterNot { it == "1.7.10" && versionComparator.compare(version, "2.6.0") >= 0 }
        val versions = mcVersions.map { "$it-$version" }.toMutableList()
        when (version) {
            "2.6.7" -> versions.add("1.19.1-2.6.7") // forgot to add the .gitkeep file before merging
        }
        versions
    }.flatten()

    val versions = commitVersions + tagVersions.reversed()
    val mcVersions = versions
            .map {it.substring(0, it.indexOf("-"))}
            .distinct()
            .sortedWith(versionComparator)

    val promos = mutableMapOf<String, String>()
    val root = mutableMapOf<String, Any>(
            "homepage" to "https://www.replaymod.com/download/",
            "promos" to promos
    )
    mcVersions.forEach { mcVersion ->
        var mcVersionRoot = mutableMapOf<String, String>()
        var latest: String? = null
        var recommended: String? = null
        versions.forEach {
            val (thisMcVersion, modVersion, preVersion) = it.split("-") + listOf(null, null)
            if (thisMcVersion == mcVersion) {
                mcVersionRoot[it] = "See https://www.replaymod.com/changelog.txt"
                if (latest == null) latest = it
                if (preVersion == null) {
                    if (recommended == null) recommended = it
                }
            }
        }
        root[mcVersion] = mcVersionRoot
        promos[mcVersion + "-latest"] = latest!!
        if (recommended != null) {
            promos[mcVersion + "-recommended"] = recommended!!
        }
    }
    return root
}

val writeVersionsJson by tasks.registering {
    doLast {
        val versionsRoot = generateVersionsJson()
        val versionsJson = JsonOutput.prettyPrint(JsonOutput.toJson(versionsRoot))
        File("versions.json").writeText(versionsJson)
    }
}

val doRelease by tasks.registering {
    doLast {
        // Parse version
        val version = project.extra["releaseVersion"] as String
        if (gitDescribe().endsWith("*")) {
            throw InvalidUserDataException("Git working tree is dirty. Make sure to commit all changes.")
        }
        val (modVersion, preVersion) = if ("-b" in version) {
            version.split("-b")
        } else {
            listOf(version, null)
        }

        // Merge release branch into stable but do not yet commit (we need to update the version.txt first)
        command("git", "checkout", "stable")
        command("git", "merge", "--no-ff", "--no-commit", "release-$version")

        // Update version.txt
        file("version.txt").writeText("$version\n")
        command("git", "add", "version.txt")

        // Finallize the merge. The message is what is later used to identify releses for building the version.json tree
        val commitMessage = if (preVersion != null)
            "Pre-release $preVersion of $modVersion"
        else
            "Release $modVersion"
        command("git", "commit", "-m", commitMessage)

        // Generate versions.json content
        val versionsRoot = generateVersionsJson()
        val versionsJson = JsonOutput.prettyPrint(JsonOutput.toJson(versionsRoot))

        // Switch to master branch to update versions.json
        command("git", "checkout", "master")

        // Write versions.json
        File("versions.json").writeText(versionsJson)

        // Commit changes
        project.exec { commandLine("git", "add", "versions.json") }
        project.exec { commandLine("git", "commit", "-m", "Update versions.json for $version") }

        // Return to previous branch
        project.exec { commandLine("git", "checkout", "-") }
    }
}

defaultTasks("bundleJar")

preprocess {
    val mc12001 = createNode("1.20.1", 12001, "yarn")
    val mc11904 = createNode("1.19.4", 11904, "yarn")
    val mc11903 = createNode("1.19.3", 11903, "yarn")
    val mc11902 = createNode("1.19.2", 11902, "yarn")
    val mc11901 = createNode("1.19.1", 11901, "yarn")
    val mc11900 = createNode("1.19", 11900, "yarn")
    val mc11802 = createNode("1.18.2", 11802, "yarn")
    val mc11801 = createNode("1.18.1", 11801, "yarn")
    val mc11701 = createNode("1.17.1", 11701, "yarn")
    val mc11604 = createNode("1.16.4", 11604, "yarn")
    val mc11601 = createNode("1.16.1", 11601, "yarn")
    val mc11502 = createNode("1.15.2", 11502, "yarn")
    val mc11404 = createNode("1.14.4", 11404, "yarn")
    val mc11404Forge = createNode("1.14.4-forge", 11404, "srg")
    val mc11202 = createNode("1.12.2", 11202, "srg")
    val mc11201 = createNode("1.12.1", 11201, "srg")
    val mc11200 = createNode("1.12", 11200, "srg")
    val mc11102 = createNode("1.11.2", 11102, "srg")
    val mc11100 = createNode("1.11", 11100, "srg")
    val mc11002 = createNode("1.10.2", 11002, "srg")
    val mc10904 = createNode("1.9.4", 10904, "srg")
    val mc10809 = createNode("1.8.9", 10809, "srg")
    val mc10800 = createNode("1.8", 10800, "srg")
    val mc10710 = createNode("1.7.10", 10710, "srg")

    mc12001.link(mc11904)
    mc11904.link(mc11903)
    mc11903.link(mc11902, file("versions/mapping-fabric-1.19.3-1.19.2.txt"))
    mc11902.link(mc11901)
    mc11901.link(mc11900)
    mc11900.link(mc11802, file("versions/mapping-fabric-1.19-1.18.2.txt"))
    mc11802.link(mc11801)
    mc11801.link(mc11701, file("versions/mapping-fabric-1.18.1-1.17.1.txt"))
    mc11701.link(mc11604, file("versions/mapping-fabric-1.17.1-1.16.4.txt"))
    mc11604.link(mc11601)
    mc11601.link(mc11502, file("versions/mapping-fabric-1.16.1-1.15.2.txt"))
    mc11502.link(mc11404, file("versions/mapping-fabric-1.15.2-1.14.4.txt"))
    mc11404.link(mc11404Forge, file("versions/mapping-1.14.4-fabric-forge.txt"))
    mc11404Forge.link(mc11202, file("versions/1.14.4-forge/mapping.txt"))
    mc11202.link(mc11201)
    mc11201.link(mc11200)
    mc11200.link(mc11102, file("versions/1.12/mapping.txt"))
    mc11102.link(mc11100, file("versions/1.11.2/mapping.txt"))
    mc11100.link(mc11002, file("versions/1.11/mapping.txt"))
    mc11002.link(mc10904)
    mc10904.link(mc10809, file("versions/1.9.4/mapping.txt"))
    mc10809.link(mc10800, file("versions/1.8.9/mapping.txt"))
    mc10800.link(mc10710, file("versions/1.8/mapping.txt"))
}
