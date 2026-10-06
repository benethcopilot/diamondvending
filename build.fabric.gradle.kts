plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

// DO NOT set group = ...!
version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "${property("mod.id") as String}-fabric"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    else -> JavaVersion.VERSION_21
}

// Only Fabric's own classes compile into the Fabric jar.
sourceSets.main {
    java.exclude("diamondvending/platform/neoforge/**", "diamondvending/platform/forge/**")
    // JSON that differs between Minecraft versions (recipes, item models)
    resources.srcDir(rootProject.file("src/main/resources-" + if (sc.current.parsed >= "26.1") "26.1" else "1.21.1"))
}

// Game tests live in their own source set and test mod, so no test code ships in the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
    java.exclude("diamondvending/gametest/neoforge/**", "diamondvending/gametest/forge/**")
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
        // Release QA (docs/qa-checklist.md): the dev player is "Dev" as on NeoForge, and
        // -Pdiamondvending.join=127.0.0.1 joins that server at once
        named("client") {
            programArgs("--username", "Dev")
            providers.gradleProperty("diamondvending.join").orNull?.let { programArgs("--quickPlayMultiplayer", it) }
        }
        // Its own folder, so a client and a server can run at the same time without sharing logs
        named("server") {
            runDirectory = rootProject.file("run/${sc.current.project}-server")
            vmArg("-Xmx1G")
        }
        register("gametest") {
            server()
            displayName = "Game Test"
            sourceSet = gametest.name
            systemProperties.put("fabric-api.gametest", "true")
            systemProperties.put("fabric-api.gametest.report-file", file("build/gametest/report.xml").absolutePath)
            runDirectory = file("build/gametest")
            generateRunConfig = false
        }
        // A real game window that takes screenshots (26.1 only: older Fabric API has no client tests). Local only —
        // CI runners have no display. See docs/dev-setup.md.
        if (sc.current.parsed >= "26.1") {
            register("clientGametest") {
                client()
                displayName = "Client Game Test"
                sourceSet = gametest.name
                systemProperties.put("fabric.client.gametest", "true")
                runDirectory = file("build/clientgametest")
                generateRunConfig = false
            }
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
            // Players need the floors, not the versions we build against
            register("fabric_loader", "deps.fabric_loader_min")
            register("fabric_api", "deps.fabric_api_min")
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
        // MetadataFloorsTest checks this node's processed metadata
        systemProperty("diamondvending.node", sc.current.project)
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

// Uploads this jar to Modrinth and CurseForge (.github/workflows/release.yml, "Releasing" in docs/dev-setup.md). A site
// is used only when its project id is in the environment, and nothing is uploaded unless PUBLISH=true.
publishMods {
    // This version's section of CHANGELOG.md, from "## [<version>]" to the next "## ["
    val releaseNotes = rootProject.file("CHANGELOG.md").readText()
        .substringAfter("## [${property("mod.version")}]", "")
        .substringAfter("\n")
        .substringBefore("\n## [")
        .trim()
    val minecraftReleases = sc.properties.rawOrNull("mod", "mc_releases")?.to<List<String>>() ?: listOf(sc.current.version)

    file = loomx.modJar.flatMap { it.archiveFile }
    version = project.version.toString()
    displayName = "${property("mod.name")} ${property("mod.version")} for Fabric ${sc.current.version}"
    changelog = releaseNotes
    type = STABLE
    modLoaders.add("fabric")
    dryRun = providers.environmentVariable("PUBLISH").orNull != "true"

    providers.environmentVariable("MODRINTH_PROJECT_ID").orNull?.let { id ->
        modrinth {
            projectId = id
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            minecraftVersions.addAll(minecraftReleases)
            requires("fabric-api")
        }
    }
    providers.environmentVariable("CURSEFORGE_PROJECT_ID").orNull?.let { id ->
        curseforge {
            projectId = id
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            client = true // the mod is needed on both sides
            server = true
            minecraftVersions.addAll(minecraftReleases)
            requires("fabric-api")
        }
    }
}
