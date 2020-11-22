import groovy.json.JsonOutput
import java.io.ByteArrayOutputStream

plugins {
    id("fabric-loom") version "0.4-SNAPSHOT" apply false
    id("com.replaymod.preprocess") version "24ac087"
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

// Loom tries to find the active mixin version by recursing up to the root project and checking each project's
// compileClasspath and build script classpath (in that order). Since we've loom in our root project's classpath,
// loom will only find it after checking the root project's compileClasspath (which doesn't exist by default).
configurations.register("compileClasspath")

val shadowJar by tasks.creating(Copy::class) {
    into("$buildDir/libs")
}

subprojects {
    buildscript {
        repositories {
            maven("https://jitpack.io")
        }
    }

    afterEvaluate {
        val projectShadowJar = project.tasks.findByName("shadowJar")
        if (projectShadowJar != null && projectShadowJar.hasProperty("archivePath") && project.name != "core") {
            shadowJar.dependsOn(projectShadowJar)
            shadowJar.from(projectShadowJar.withGroovyBuilder { getProperty("archivePath") })
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
        mcVersions.map { "$it-$version" }
    }.flatten()

    val versions = commitVersions + tagVersions.reversed()
    val mcVersions = versions
            .map {it.substring(0, it.indexOf("-"))}
            .distinct()
            .sortedWith(compareBy(
                    { (it.split(".").getOrNull(0) ?: "0").toInt() },
                    { (it.split(".").getOrNull(1) ?: "0").toInt() },
                    { (it.split(".").getOrNull(2) ?: "0").toInt() }
            ))

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
                mcVersionRoot[it] = if (tagVersions.contains(it))
                    "See https://github.com/ReplayMod/ReplayMod/releases/$it"
                else
                    "See https://www.replaymod.com/forum/thread/100"
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

        // Create new commit
        val commitMessage = if (preVersion != null)
            "Pre-release $preVersion of $modVersion"
        else
            "Release $modVersion"
        file("version.txt").writeText("$version\n")
        command("git", "add", "version.txt")
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

defaultTasks("shadowJar")

preprocess {
    "1.16.4"(11604, "yarn") {
        "1.16.1"(11601, "yarn") {
            "1.15.2"(11502, "yarn") {
                "1.14.4"(11404, "yarn", file("versions/mapping-fabric-1.15.2-1.14.4.txt")) {
                    "1.14.4-forge"(11404, "srg", file("versions/mapping-1.14.4-fabric-forge.txt")) {
                        "1.12.2"(11202, "srg", file("versions/1.14.4-forge/mapping.txt")) {
                            "1.12.1"(11201, "srg") {
                                "1.12"(11200, "srg") {
                                    "1.11.2"(11102, "srg", file("versions/1.12/mapping.txt")) {
                                        "1.11"(11100, "srg", file("versions/1.11.2/mapping.txt")) {
                                            "1.10.2"(11002, "srg", file("versions/1.11/mapping.txt")) {
                                                "1.9.4"(10904, "srg") {
                                                    "1.8.9"(10809, "srg", file("versions/1.9.4/mapping.txt")) {
                                                        "1.8"(10800, "srg", file("versions/1.8.9/mapping.txt")) {
                                                            "1.7.10"(10710, "srg", file("versions/1.8/mapping.txt"))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
