plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.141"
    id("neoforge-mutex")
    id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-forge"

// Forge 1.20.1 runs on Java 17, so every file this node compiles must be Java 17 (docs/dev-setup.md).
val requiredJava = JavaVersion.VERSION_17

// Only Forge's own classes compile into the Forge jar.
sourceSets.main {
    java.exclude("diamondvending/platform/fabric/**", "diamondvending/platform/neoforge/**")
    // What differs on 1.20.1: the mod metadata, pack.mcmeta, recipes and the example catalog
    resources.srcDir(rootProject.file("src/main/resources-1.20.1"))
}

// Game tests live in their own source set and test mod, so no test code ships in the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
    java.exclude("diamondvending/gametest/fabric/**", "diamondvending/gametest/neoforge/**")
    resources.srcDir(rootProject.file("src/gametest/resources-1.20.1"))
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

legacyForge {
    version = "${sc.current.version}-${property("deps.forge_loader")}"
    addModdingDependenciesTo(gametest)

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
        register("diamondvending_gametest") {
            sourceSet(gametest)
        }
    }

    runs {
        // One run folder per node: worlds from newer versions must never be opened by 1.20.1
        register("client") {
            gameDirectory = file("../../run/${sc.current.project}")
            client()
            // Release QA (docs/qa-checklist.md): -Pdiamondvending.join=127.0.0.1 joins that server at once
            providers.gradleProperty("diamondvending.join").orNull?.let { programArguments.addAll("--quickPlayMultiplayer", it) }
        }
        register("server") {
            // Its own folder, so a client and a server can run at the same time without sharing logs
            gameDirectory = file("../../run/${sc.current.project}-server")
            server()
            programArgument("--nogui")
            jvmArgument("-Xmx1G")
        }
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            gameDirectory = file("build/gametest")
            systemProperty("forge.enabledGameTestNamespaces", property("mod.id") as String)
        }
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("description", "mod.description")
            register("authors", "mod.authors")
            register("minecraft", "mod.mc_compat")
            // Players need the floor, not the version we build against
            register("forge", "deps.forge_loader_min")
        }

        filesMatching("META-INF/mods.toml") { expand(props) }
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
        // 1.20.1 reads 1.21.1's item model format (custom_model_data overrides)
        from(rootProject.file("src/main/resources-1.21.1/assets")) { into("assets") }
        eachFile { path = DataFolders.for1201(path) }
    }

    named<ProcessResources>("processGametestResources") {
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
        eachFile { path = DataFolders.for1201(path) }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
    }

    test {
        useJUnitPlatform()
        // GeneratedFilesTest compares against the files in the repository
        systemProperty("diamondvending.root", rootProject.projectDir.absolutePath)
        // MetadataFloorsTest checks this node's processed metadata
        systemProperty("diamondvending.node", sc.current.project)
    }

    // Includes the license file in the built mod
    withType<Jar> {
        val name = project.property("mod.id")
        inputs.property("mod_id", name)
        from("../../LICENSE") { rename { "$it-$name" } }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        // The reobfuscated jar: the one that runs in a real Forge install (the plain jar goes to build/devlibs)
        from(named<net.neoforged.moddevgradle.legacyforge.tasks.RemapJar>("reobfJar").flatMap { it.archiveFile },
            named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
