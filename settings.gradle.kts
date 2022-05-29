pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://jitpack.io")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.essential.gg/repository/maven-public")
    }
    plugins {
        id("gg.essential.multi-version.root") version "0.6.0"
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}

val jGuiVersions = listOf(
        // "1.7.10",
        // "1.8",
        "1.8.9",
        "1.9.4",
        "1.12",
        "1.14.4-forge",
        "1.14.4",
        "1.15.2",
        "1.16.1",
        "1.16.4",
        "1.17.1",
        "1.18.1",
        "1.18.2",
        "1.19",
        "1.19.1",
        "1.19.2",
        "1.19.3",
        "1.19.4",
        "1.20.1",
        "1.20.2",
        "1.20.4",
)
val replayModVersions = listOf(
        // "1.7.10",
        // "1.8",
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
        "1.16.4",
        "1.17.1",
        "1.18.1",
        "1.18.2",
        "1.19",
        "1.19.1",
        "1.19.2",
        "1.19.3",
        "1.19.4",
        "1.20.1",
        "1.20.2",
        "1.20.4",
)

rootProject.buildFileName = "root.gradle.kts"

include(":jGui")
project(":jGui").apply {
    projectDir = file("jGui")
    buildFileName = "root.gradle.kts"
}
jGuiVersions.forEach { version ->
    include(":jGui:$version")
    project(":jGui:$version").apply {
        projectDir = file("jGui/versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}

replayModVersions.forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}
