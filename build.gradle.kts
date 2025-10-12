import com.replaymod.gradle.preprocess.PreprocessTask
import gg.essential.gradle.util.*

plugins {
    java
    id("io.github.goooler.shadow") apply false
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
}

val mcVersion = platform.mcVersion

var jGuiVersion = project.name
if (jGuiVersion in listOf("1.10.2", "1.11", "1.11.2")) jGuiVersion = "1.9.4"
if (jGuiVersion in listOf("1.12.1", "1.12.2")) jGuiVersion = "1.12"
val jGui = project.evaluationDependsOn(":jGui:$jGuiVersion")

version = "${project.name}-${rootProject.version}"
base.archivesName.set("replaymod")
java.withSourcesJar()

loom {
    mixin.defaultRefmapName.set("mixins.replaymod.refmap.json")
    noServerRunConfigs()
}

if (platform.isLegacyForge) {
    loom.runs.named("client") {
        property("fml.coreMods.load", "com.replaymod.core.LoadingPlugin")
    }
}

repositories {
    mavenLocal()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.terraformersmc.com/releases/") {
        content {
            includeGroup("com.terraformersmc")
        }
    }
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

// Include dep in fat jar without relocation and, when forge supports it, without exploding (TODO)
val shade by configurations.creating
// Include dep in fat jar with relocation and minimization
val shadow by configurations.creating {
    exclude(group = "net.fabricmc", module = "fabric-loader")
    exclude(group = "com.google.guava", module = "guava-jdk5")
    exclude(group = "com.google.guava", module = "guava") // provided by MC
    exclude(group = "com.google.code.gson", module = "gson") // provided by MC (or manually bundled for 1.11.2 and below)
}

dependencies {
    if (platform.isFabric) {
        val fabricApiVersion = when (mcVersion) {
            11404 -> "0.4.3+build.247-1.14"
            11502 -> "0.5.1+build.294-1.15"
            11601 -> "0.14.0+build.371-1.16"
            11603 -> "0.17.1+build.394-1.16"
            11604 -> "0.42.0+1.16"
            11701 -> "0.46.1+1.17"
            11800 -> "0.43.1+1.18"
            11801 -> "0.43.1+1.18"
            11802 -> "0.47.9+1.18.2"
            11900 -> "0.55.3+1.19"
            11901 -> "0.58.5+1.19.1"
            11902 -> "0.68.0+1.19.2"
            11903 -> "0.68.1+1.19.3"
            11904 -> "0.76.0+1.19.4"
            12001 -> "0.83.1+1.20.1"
            12002 -> "0.91.2+1.20.2"
            12004 -> "0.91.2+1.20.4"
            12006 -> "0.98.0+1.20.6"
            12100 -> "0.100.3+1.21"
            12102 -> "0.106.1+1.21.2"
            12104 -> "0.111.0+1.21.4"
            12105 -> "0.119.9+1.21.5"
            12107 -> "0.128.1+1.21.7"
            12110 -> "0.135.0+1.21.10"
            else -> throw UnsupportedOperationException()
        }
        val fabricApiModules = mutableListOf(
            "api-base",
            "networking-v0",
            "keybindings-v0",
            "resource-loader-v0",
        )
        if (mcVersion >= 11600) {
            fabricApiModules.remove("keybindings-v0")
            fabricApiModules.add("key-binding-api-v1")
        }
        if (mcVersion >= 11604) {
            fabricApiModules.add("screen-api-v1")
            fabricApiModules.add("networking-api-v1")
        }
        if (mcVersion >= 11700) {
            fabricApiModules.remove("networking-v0")
        }
        for (module in fabricApiModules) {
            val dep = fabricApi.module("fabric-$module", fabricApiVersion)
            modImplementation(dep)
            "include"(dep)
        }
    }

    if (!platform.isFabric) {
        // Mixin 0.8 is no longer compatible with MC 1.11.2 or older
        val mixinVersion = if (mcVersion >= 11200) "0.8.2" else "0.7.11-SNAPSHOT"
        compileOnly("org.spongepowered:mixin:$mixinVersion")
        implementation(shade("org.spongepowered:mixin:$mixinVersion") {
            isTransitive = false // deps should all be bundled with MC
        })
    }

    if (platform.isFabric) {
        "include"(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.3.6")!!)!!)
    }

    implementation(shadow("com.googlecode.mp4parser:isoparser:1.1.7")!!)
    implementation(shadow("org.apache.commons:commons-exec:1.3")!!)
    implementation(shadow("com.google.apis:google-api-services-youtube:v3-rev178-1.22.0")!!)
    implementation(shadow("com.google.api-client:google-api-client-gson:1.20.0")!!)
    implementation(shadow("com.google.api-client:google-api-client-java6:1.20.0")!!)
    implementation(shadow("com.google.oauth-client:google-oauth-client-jetty:1.20.0")!!)

    val lwjgl by configurations.creating
    for (suffix in listOf("", ":natives-linux", ":natives-windows", ":natives-macos", ":natives-macos-arm64")) {
        lwjgl("org.lwjgl:lwjgl:3.3.1$suffix")
        lwjgl("org.lwjgl:lwjgl-tinyexr:3.3.1$suffix")
    }
    compileOnly("org.lwjgl:lwjgl-tinyexr:3.3.1")
    shadow(prebundle(lwjgl, "com/replaymod/render/utils/lwjgl.jar"))

    if (mcVersion < 11200) {
        // The version which MC ships is too old, we'll need to ship our own
        implementation(shadow("com.google.code.gson:gson:2.8.7")!!)
    }

    implementation(shadow("com.github.javagl.JglTF:jgltf-model:3af6de4")!!)

    if (platform.isFabric) {
        implementation(shadow("org.apache.maven:maven-artifact:3.6.1")!!)
    }

    implementation(shadow("org.aspectj:aspectjrt:1.8.2")!!)

    implementation(shadow("com.github.ReplayMod.JavaBlend:2.79.0:a0696f8")!!)

    implementation(shadow("com.github.ReplayMod:ReplayStudio")!!)
    // FIXME hack because I don't know how to get this to be inherited properly
    implementation(rootProject.files("libs/ReplayStudio/.gradle/prebundled-jars/viaVersion.jar"))

    implementation(project(path = jGui.path, configuration = "namedElements"))
    implementation(shadow("com.github.ReplayMod:lwjgl-utils:27dcd66")!!)

    if (platform.isFabric) {
        val modMenuVersion = when {
            mcVersion >= 12110 -> "16.0.0-rc.1"
            mcVersion >= 12107 -> "15.0.0-beta.3"
            mcVersion >= 12105 -> "14.0.0-rc.2"
            mcVersion >= 12104 -> "13.0.0-beta.1"
            mcVersion >= 12102 -> "12.0.0-beta.1"
            mcVersion >= 12100 -> "11.0.0-rc.4"
            mcVersion >= 12006 -> "10.0.0-beta.1"
            mcVersion >= 12003 -> "9.0.0-pre.1"
            mcVersion >= 12002 -> "8.0.0"
            mcVersion >= 12000 -> "7.0.1"
            mcVersion >= 11904 -> "6.1.0-rc.4"
            mcVersion >= 11903 -> "5.0.0-alpha.4"
            mcVersion >= 11901 -> "4.0.5"
            mcVersion >= 11900 -> "4.0.4"
            mcVersion >= 11802 -> "3.1.0"
            mcVersion >= 11800 -> "3.0.0"
            mcVersion >= 11700 -> "2.0.0-beta.7"
            mcVersion >= 11602 -> "1.16.8"
            mcVersion >= 11600 -> null // maven doesn't have one for this version (only 1.16.5)
            mcVersion >= 11500 -> "1.10.6"
            else -> null
        }
        if (modMenuVersion != null) {
            modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")
        } else {
            // Oldest modmenu on their maven is 1.10.5 for MC 1.15.2; for older versions we won't run it in dev
            modCompileOnly("com.terraformersmc:modmenu:1.10.6")
        }
        // Lacks maven dependencies
        if (mcVersion == 12006) {
            //modRuntimeOnly("net.fabricmc.fabric-api:fabric-api:0.98.0+1.20.6")
        }
    }

    val irisVersion = when {
        mcVersion >= 12000 -> "1.7.2+1.20.1"
        mcVersion >= 11600 -> "1.18.x-v1.2.0"
        else -> null
    }
    if (irisVersion != null) {
        modCompileOnly("maven.modrinth:iris:$irisVersion")
    }

    testImplementation("junit:junit:4.11")
}

preprocess {
    keywords.set(mapOf(
        ".java" to PreprocessTask.DEFAULT_KEYWORDS,
        ".kt" to PreprocessTask.DEFAULT_KEYWORDS,
        ".json" to PreprocessTask.DEFAULT_KEYWORDS,
        ".mcmeta" to PreprocessTask.DEFAULT_KEYWORDS,
        ".cfg" to PreprocessTask.CFG_KEYWORDS,
        ".vert" to PreprocessTask.DEFAULT_KEYWORDS,
        ".frag" to PreprocessTask.DEFAULT_KEYWORDS,
    ))

    patternAnnotation.set("com.replaymod.gradle.remap.Pattern")
}

tasks.jar {
    archiveClassifier.set("raw")

    if (!platform.isFabric) {
        manifest {
            attributes(
                "TweakClass" to "com.replaymod.core.tweaker.ReplayModTweaker",
                "TweakOrder" to "0",
                "FMLCorePluginContainsFMLMod" to "true",
                "FMLCorePlugin" to "com.replaymod.core.LoadingPlugin",
                "FMLAT" to "replaymod_at.cfg",
            )
        }
    }
}

tasks.remapJar {
    if (platform.isFabric) {
        addNestedDependencies.set(true)
    }
    archiveClassifier.set("obf")
}

val configureRelocationOutput = project.layout.buildDirectory.file("configureRelocation")
val configureRelocation by tasks.registering {
    dependsOn(tasks.jar)
    dependsOn(shadow)
    outputs.file(configureRelocationOutput)
    doLast {
        val pkgs = files(shadow).filter { it.exists() }.map {
            val tree = if (it.isDirectory) fileTree(it) else zipTree(it)
            val pkgs = mutableSetOf<String>()
            tree.visit {
                val file = this
                if (!file.isDirectory && file.name.endsWith(".class") && file.path.contains("/")) {
                    val pkg = file.path.substring(0, file.path.lastIndexOf("/")) + "/"
                    if (pkg.startsWith("com/")) {
                        if (pkg.startsWith("com/google/")) {
                            if (!pkg.startsWith("com/google/common")) {
                                pkgs += pkg.substring(0, pkg.indexOf("/", "com/google/".length))
                            }
                        } else if (!pkg.startsWith("com/replaymod")) {
                            pkgs += pkg.substring(0, pkg.indexOf("/", 4))
                        }
                    } else if (pkg.startsWith("net/")) {
                        if (!pkg.startsWith("net/minecraft")
                            && !pkg.startsWith("net/fabric")) {
                            pkgs += pkg.substring(0, pkg.indexOf("/", "net/".length))
                        }
                    } else if (pkg.startsWith("org/")) {
                        if (pkg.startsWith("org/apache/")) {
                            if (pkg.startsWith("org/apache/commons/")) {
                                if (!pkg.startsWith("org/apache/commons/io")) {
                                    pkgs += pkg.substring(0, pkg.indexOf("/", "org/apache/commons/".length))
                                }
                            } else if (!pkg.startsWith("org/apache/logging")) {
                                pkgs += pkg.substring(0, pkg.indexOf("/", "org/apache/".length))
                            }
                        } else if (pkg.startsWith("org/lwjgl")) {
                            return@visit // either bundled with MC or uses natives which we can't relocate
                        } else if (!pkg.startsWith("org/spongepowered")) {
                            pkgs += pkg.substring(0, pkg.indexOf("/", 4))
                        }
                    } else if (pkg.startsWith("it/unimi/dsi/fastutil") && mcVersion >= 11400) {
                        return@visit // MC uses this as well
                    } else if (!pkg.startsWith("javax/")) {
                        // Note: we cannot just use top level packages as those will be too generic and we'll run
                        // into this long standing bug: https://github.com/johnrengelman/shadow/issues/232
                        val i = pkg.indexOf("/")
                        val i2 = pkg.indexOf("/", i + 1)
                        if (i2 > 0) {
                            pkgs += pkg.substring(0, i2)
                        }
                    }
                }
            }
            pkgs
        }.flatten().toSortedSet()
        configureRelocationOutput.get().asFile.writeText(pkgs.joinToString("\n"))
    }
}

val bundleJar by tasks.registering(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    from(tasks.remapJar.flatMap { it.archiveFile })

    from(jGui.tasks.remapJar.flatMap { it.archiveFile }.map { zipTree(it) }) {
        filesMatching("mixins.jgui.json") {
            filter { it.replace("de.johni0702", "com.replaymod.lib.de.johni0702") }
        }
        filesMatching("mixins.jgui.refmap.json") {
            filter { it.replace("de/johni0702", "com/replaymod/lib/de/johni0702") }
        }
    }
    relocate("de.johni0702", "com.replaymod.lib.de.johni0702")

    manifest.inheritFrom(tasks.jar.get().manifest)
    from(shade)
    configurations = listOf(shadow)
    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")

    dependsOn(configureRelocation)
    inputs.file(configureRelocationOutput)
    doFirst {
        configureRelocationOutput.get().asFile.forEachLine { pkg ->
            val pkgName = pkg.replace("/", ".")
            relocate(pkgName, "com.replaymod.lib.$pkgName")
        }
    }

    // No need to shadow netty, MC provides it
    // (actually, pre-1.12 ships a netty which is too old, so we need to shade it there anyway)
    if (mcVersion >= 11200) {
        relocate("com.github.steveice10.netty", "io.netty")
        exclude("com/github/steveice10/netty/**")
    }

    if (mcVersion >= 11400) {
        // MC ships this
        exclude("it/unimi/dsi/fastutil/**")
    }

    minimize {
        exclude(dependency(".*spongepowered:mixin:.*"))
    }
}
tasks.assemble { dependsOn(bundleJar) }
