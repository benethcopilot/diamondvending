plugins {
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// NeoForge-only classes never compile into the Fabric jar.
sourceSets.main {
    java.exclude("diamondvending/platform/neoforge/**")
    // JSON that differs between Minecraft versions (recipes, item models)
    resources.srcDir(rootProject.file("src/main/resources-" + if (sc.current.parsed >= "26.1") "26.1" else "1.21.1"))
}

// Game tests live in their own source set and test mod, so no test code ships in the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
    java.exclude("diamondvending/gametest/neoforge/**")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    // Use `mod{dependency type}` even on 26.1+ — loom-back-compat converts them
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        // One run folder per node: worlds from 26.1 must never be opened by 1.21.1
        runDirectory = rootProject.file("run/${sc.current.project}")
    }

    mods {
        register("diamondvending") {
            sourceSet(sourceSets.main.get())
        }
        register("diamondvending_gametest") {
            sourceSet(gametest)
        }
    }

    runs {
        register("gametest") {
            server()
            displayName = "Game Test"
            sourceSet = gametest.name
            systemProperties.put("fabric-api.gametest", "true")
            systemProperties.put("fabric-api.gametest.report-file", file("build/gametest/report.xml").absolutePath)
            runDirectory = file("build/gametest")
            generateRunConfig = false
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
            register("fabric_loader", "deps.fabric_loader")
            val fabricApiVersion: String = sc.properties["deps.fabric_api"]
            val fabricApi = fabricApiVersion.substringBefore('+')
            inputs.property("fabric_api", fabricApi)
            put("fabric_api", fabricApi)
            inputs.property("java", requiredJava.majorVersion)
            put("java", requiredJava.majorVersion)
        }

        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/neoforge.mods.toml")
    }

    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:deprecation")
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
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }
}
