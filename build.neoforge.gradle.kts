plugins {
    id("net.neoforged.moddev") version "2.0.141"
    id("neoforge-mutex")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-neoforge"

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Fabric-only classes never compile into the NeoForge jar.
sourceSets.main {
    java.exclude("diamondvending/platform/fabric/**")
}

// Game tests live in their own source set and test mod, so no test code ships in the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
    java.exclude("diamondvending/gametest/fabric/**")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

neoForge {
    version = property("deps.neo_loader") as String
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
        // One run folder per node: worlds from 26.1 must never be opened by 1.21.1
        register("client") {
            gameDirectory = file("../../run/${sc.current.project}")
            client()
        }
        register("server") {
            gameDirectory = file("../../run/${sc.current.project}")
            server()
        }
        register("gameTestServer") {
            type = "gameTestServer"
            sourceSet = gametest
            gameDirectory = file("build/gametest")
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod.id") as String)
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
        }

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        exclude("fabric.mod.json")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    test {
        useJUnitPlatform()
        // GeneratedFilesTest compares against the files in the repository
        systemProperty("diamondvending.root", rootProject.projectDir.absolutePath)
    }

    register<JavaExec>("generateArt") {
        group = "diamondvending"
        description = "Regenerates textures, models and test structures from MachineLayout"
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass = "diamondvending.art.ArtGenerator"
        args(rootProject.projectDir.absolutePath)
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
        from(jar.flatMap { it.archiveFile }, named<Jar>("sourcesJar").flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
