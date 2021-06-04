pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
        maven("https://maven.fabricmc.net")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.replaymod.preprocess" -> {
                    useModule("com.github.replaymod:preprocessor:${requested.version}")
                }
            }
        }
    }
}

val jGuiVersions = listOf(
        // "1.7.10",
        "1.8",
        "1.8.9",
        "1.9.4",
        "1.12",
        "1.14.4-forge",
        "1.14.4",
        "1.15.2",
        "1.16.1",
        "1.16.4"
)
val replayModVersions = listOf(
        // "1.7.10",
        "1.8",
        "1.8.9",
        "1.9.4",
        "1.10.2",
        "1.11",
        "1.11.2",
        "1.12",
        "1.12.1",
        "1.12.2",
        "1.14.4-forge",
        "1.14.4",
        "1.15.2",
        "1.16.1",
        "1.16.4"
)

rootProject.buildFileName = "root.gradle.kts"

include(":jGui")
project(":jGui").apply {
    projectDir = file("jGui")
    buildFileName = "preprocess.gradle.kts"
}
jGuiVersions.forEach { version ->
    include(":jGui:$version")
    project(":jGui:$version").apply {
        projectDir = file("jGui/versions/$version")
        buildFileName = "../../build.gradle"
    }
}

replayModVersions.forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle"
    }
}
