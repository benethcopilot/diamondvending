# Diamond Vending — Plan 1: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Stonecutter project that builds four mod jars ({1.21.1, 26.1.2} × {Fabric, NeoForge}) in CI, plus the machine's pure-Java core logic (click regions, purchase decisions, problem detection) fully unit-tested.

**Architecture:** One source tree under `src/`, processed by Stonecutter into four Gradle subprojects (`versions/<node>`). Loader-specific classes live in `diamondvending/platform/<loader>/` and are excluded from the other loader's build by Gradle. Game-independent rules live in `diamondvending/core/` with **no Minecraft imports**, so they are unit-tested with plain JUnit.

**Tech Stack:** Java 21/25, Gradle 9.7.1 (wrapper), Stonecutter 0.9.8, Fabric Loom via loom-back-compat 0.4.2, NeoForge ModDevGradle 2.0.141, Fabric Loader 0.19.5, JUnit 6.1.3, GitHub Actions.

**Spec:** [`docs/superpowers/specs/2026-09-23-diamond-vending-design.md`](../specs/2026-09-23-diamond-vending-design.md) · **Roadmap:** [`2026-09-23-roadmap.md`](2026-09-23-roadmap.md)

## Global Constraints

- Mod ID `diamondvending`; display name **Diamond Vending**; Java package and Gradle group `diamondvending`; mod-list author **"Diamond Vending Team"**; license **MIT**.
- **No personal username** anywhere in identifiers, metadata, or URLs shipped in the jar (so no GitHub URLs in mod metadata yet).
- Stonecutter nodes (exact names): `1.21.1-fabric`, `1.21.1-neoforge`, `26.1-fabric`, `26.1-neoforge` (the `26.1` nodes build against Minecraft **26.1.2**). **vcsVersion = `26.1-neoforge`.**
- Versions: Fabric Loader `0.19.5`; Fabric API `0.116.17+1.21.1` / `0.155.3+26.1.2`; NeoForge `21.1.250` / `26.1.2.107`; ModDevGradle `2.0.141`.
- **No runtime dependencies** besides the loader (and Fabric API on Fabric). No mixins, no access wideners in Plan 1.
- Gradle runs on **JDK 25**; bytecode targets Java 21 for 1.21.1 and Java 25 for 26.1.
- `diamondvending/core/` **must not import `net.minecraft`** (or any loader package).
- Local builds: Gradle heap `-Xmx3G`, `org.gradle.parallel=false`, build **one target at a time** (the dev machine has had memory pressure).
- Before every commit, Stonecutter's active version must be the vcsVersion (`stonecutter.gradle.kts` says `stonecutter active "26.1-neoforge"`).
- Shell rule for agents on this machine: **one command per Bash call — never chain with `&&`, `||`, or `;`.**
- Git workflow: work on branch `plan-1/foundation`; open a PR at the end; squash-merge after CI is green.
- Front-face canvas values (spec §2.2), in pixels, `u` left→right and `v` top→bottom as seen from the front, rects are `[min, max)`:
  - Window `u 1.5–22.5, v 2–24`; Lamp `u 26–28.5, v 0.5–2`; Display `u 24.75–29.75, v 3–5.5`
  - Buttons 1–12: 2 × 2 px, 2 columns × 6 rows starting at `u 25, v 7`, pitch 2.75 px; hit cells padded 0.375 px on every side
  - Coin slot `u 24–27.5, v 24.5–29`; Coin return `u 28–30.5, v 25–27.5`; Tray `u 3–21, v 26–30.5`
- Selections are numbered in reading order: shelves 3 per row (4 rows), buttons 2 per row (6 rows). Index `i` is 0-based in code, shown to players as `i + 1`.
- Purchase check order (spec §3.3): catalog missing → empty → sold out → tray full → cash box full → not enough money; pay **credit first**, then inventory.
- Problem order (spec §3.5 b): catalog missing, not set up, cash box full, sold out, tray full. Cash box full and sold out never apply to infinite machines.

## Review Focus

1. **Clicks landing exactly on edges or a hair outside the block** (Minecraft hit vectors like `1.0000001`) — must clamp and resolve to a region or `NONE`, never throw. → Task 2, `slightlyOutOfRangeHitsAreClamped`, `seamBetweenPartsIsStillTheWindow`.
2. **Huge credit/inventory counts** (e.g. `Integer.MAX_VALUE` credit plus inventory) — funds math must not overflow into a false "not enough money". → Task 3, `hugeFundsDoNotOverflow`.
3. **Committing with Stonecutter switched to another version** — code in git would be in the wrong comment state. → Task 5, CI `guard` job.
4. **A jar shipping the other loader's classes** (e.g. NeoForge classes inside the Fabric jar → crash on load). → Task 1 Step 10 manual check and Task 5 CI jar-contents step.
5. **Impossible machine facts from callers** (more selections in stock than set up, or more than 12) — must be rejected loudly, not produce a wrong warning. → Task 4, `impossibleFactsAreRejected`.

---

## File Structure

```
settings.gradle.kts               Stonecutter nodes + plugin versions
stonecutter.gradle.kts            active version, per-node properties, swaps, replacements
stonecutter.properties.toml       mod metadata + per-version dependency versions
build.fabric.gradle.kts           Fabric node build (Loom via loom-back-compat)
build.neoforge.gradle.kts         NeoForge node build (ModDevGradle)
gradle.properties                 Gradle JVM/memory flags
buildSrc/                         neoforge-mutex plugin (verbatim from template)
gradlew, gradlew.bat, gradle/wrapper/   Gradle wrapper (verbatim from template)
src/main/resources/fabric.mod.json
src/main/resources/META-INF/neoforge.mods.toml
src/main/java/diamondvending/
  DiamondVending.java             MOD_ID, LOGGER, id(), init() — shared by both loaders
  platform/fabric/DiamondVendingFabric.java       Fabric entrypoint (excluded from NeoForge builds)
  platform/neoforge/DiamondVendingNeoForge.java   NeoForge entrypoint (excluded from Fabric builds)
  core/                           pure Java — no Minecraft imports
    Rect.java                     rectangle on the 32×32 front canvas
    Region.java                   what kind of thing a click hit
    Hit.java                      region + button index
    Facing.java                   horizontal facing without Minecraft types
    MachineLayout.java            all front-face geometry: regions, buttons, shelves, hit → canvas
    PurchaseInput.java            measured facts for one purchase attempt
    DenyReason.java               why a purchase was refused
    PurchaseDecision.java         Approved(fromCredit, fromInventory) | Denied(reason)
    PurchaseRules.java            the purchase decision
    Problem.java                  persistent machine problems, in priority order
    MachineFacts.java             measured facts about a machine
    MachineProblems.java          facts → active problems
src/test/java/diamondvending/core/
  MachineLayoutTest.java, PurchaseRulesTest.java, MachineProblemsTest.java
.github/workflows/build.yml       CI: guard + 4-target build/test matrix
.github/dependabot.yml            weekly grouped updates for Actions + Gradle
docs/dev-setup.md                 how to build, run, switch versions
CLAUDE.md                         project rules for AI agents
CHANGELOG.md                      Keep-a-Changelog format
```

---

### Task 1: Toolchain and Stonecutter project skeleton

Builds four empty-but-loadable mod jars. Folds in JDK install, template import, metadata, entrypoints, and dev docs.

**Files:**
- Create: `settings.gradle.kts`, `stonecutter.gradle.kts`, `stonecutter.properties.toml`, `build.fabric.gradle.kts`, `build.neoforge.gradle.kts`, `gradle.properties`
- Copy verbatim from template: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `buildSrc/build.gradle.kts`, `buildSrc/src/main/kotlin/neoforge-mutex.gradle.kts`
- Create: `src/main/resources/fabric.mod.json`, `src/main/resources/META-INF/neoforge.mods.toml`
- Create: `src/main/java/diamondvending/DiamondVending.java`, `src/main/java/diamondvending/platform/fabric/DiamondVendingFabric.java`, `src/main/java/diamondvending/platform/neoforge/DiamondVendingNeoForge.java`
- Create: `docs/dev-setup.md`, `CLAUDE.md`, `CHANGELOG.md`
- Modify: `.gitignore`, `README.md`

**Interfaces:**
- Produces: `diamondvending.DiamondVending` with `public static final String MOD_ID = "diamondvending"`, `public static final org.slf4j.Logger LOGGER`, `public static Identifier id(String path)` (written as `Identifier`; Stonecutter rewrites it to `ResourceLocation` for 1.21.1), `public static void init()`.
- Produces: Gradle tasks `:<node>:build`, `:<node>:test`, `:<node>:runClient`, `:<node>:runServer` for the four nodes.

- [ ] **Step 1: Create the working branch**

```bash
git -C /c/Users/benet/mcvending checkout -b plan-1/foundation
```

- [ ] **Step 2: Install JDK 25 (Temurin)** — run in PowerShell:

```powershell
winget install --id EclipseAdoptium.Temurin.25.JDK -e --accept-package-agreements --accept-source-agreements
```

Then find the install folder (Bash):

```bash
ls "/c/Program Files/Eclipse Adoptium"
```

Expected: a folder like `jdk-25.0.4.101-hotspot`. Use its exact name below as `<JDK_DIR>`.

- [ ] **Step 3: Make the JDK visible to new shells**

Bash tool sessions start from the user profile and don't see PATH changes made after Claude Code started. Append to `~/.bashrc` (create it if missing) — use the Edit/Write tool, not a heredoc:

```bash
# Diamond Vending dev: JDK 25
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/<JDK_DIR>"
export PATH="$JAVA_HOME/bin:$PATH"
```

Also set it for Windows apps/IDEs (PowerShell):

```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Eclipse Adoptium\<JDK_DIR>', 'User')
```

Verify in a new Bash call:

```bash
java -version
```

Expected: `openjdk version "25.0.4"` (Temurin).

- [ ] **Step 4: Copy the template's wrapper and buildSrc verbatim**

Template: `https://github.com/stonecutter-versioning/stonecutter-template-multiloader`, commit `fb821b0496133d1ae806b16d6ec89748c5d3492c` (CC0). Clone it to the scratchpad (skip if the scratchpad clone already exists), then check out the pinned commit:

```bash
git clone https://github.com/stonecutter-versioning/stonecutter-template-multiloader.git "$SCRATCH/sc-template"
```
```bash
git -C "$SCRATCH/sc-template" checkout fb821b0496133d1ae806b16d6ec89748c5d3492c
```

(`$SCRATCH` = the session scratchpad directory.) Copy, one command each:

```bash
cp "$SCRATCH/sc-template/gradlew" /c/Users/benet/mcvending/gradlew
```
```bash
cp "$SCRATCH/sc-template/gradlew.bat" /c/Users/benet/mcvending/gradlew.bat
```
```bash
mkdir -p /c/Users/benet/mcvending/gradle/wrapper /c/Users/benet/mcvending/buildSrc/src/main/kotlin
```
```bash
cp "$SCRATCH/sc-template/gradle/wrapper/gradle-wrapper.jar" "$SCRATCH/sc-template/gradle/wrapper/gradle-wrapper.properties" /c/Users/benet/mcvending/gradle/wrapper/
```
```bash
cp "$SCRATCH/sc-template/buildSrc/build.gradle.kts" /c/Users/benet/mcvending/buildSrc/build.gradle.kts
```
```bash
cp "$SCRATCH/sc-template/buildSrc/src/main/kotlin/neoforge-mutex.gradle.kts" /c/Users/benet/mcvending/buildSrc/src/main/kotlin/neoforge-mutex.gradle.kts
```

Verify `gradle/wrapper/gradle-wrapper.properties` contains `gradle-9.7.1-bin.zip`.

- [ ] **Step 5: Write the Gradle settings and Stonecutter controller**

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the right Loom variant for obfuscated (1.21.1) and unobfuscated (26.1+) Minecraft
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        /** Creates `versions/{project}-{loader}` nodes, each built by `build.{loader}.gradle.kts`. */
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("1.21.1", "fabric", "neoforge")
        match("26.1", "fabric", "neoforge", version = "26.1.2")
        vcsVersion = "26.1-neoforge"
    }
}

rootProject.name = "diamondvending"
```

`stonecutter.gradle.kts`:

```kotlin
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    // Applies version- and loader-specific tables from stonecutter.properties.toml
    properties {
        tags(version, loader)
    }

    // Enables `//? if fabric` / `//? if neoforge` in source comments
    constants {
        match(loader, "fabric", "neoforge")
    }

    swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    replacements {
        // Mojang renamed ResourceLocation to Identifier in 1.21.11; write `Identifier` in source.
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
```

`stonecutter.properties.toml`:

```toml
mod.id = "diamondvending"
mod.name = "Diamond Vending"
mod.version = "0.1.0"
mod.description = "A real-looking 2x2 vending machine that sells items for diamonds."
mod.authors = "Diamond Vending Team"

deps.fabric_loader = "0.19.5"
deps.junit = "6.1.3"
loomx.loom_version = "1.17-SNAPSHOT"

["1.21.1"]
# Releases to mark compatible when publishing (later)
mod.mc_releases = ["1.21", "1.21.1"]

[fabric."1.21.1"]
mod.mc_compat = ">=1.21 <=1.21.1"
deps.fabric_api = "0.116.17+1.21.1"

[neoforge."1.21.1"]
mod.mc_compat = "[1.21, 1.21.1]"
deps.neo_loader = "21.1.250"

["26.1"]
mod.mc_releases = ["26.1", "26.1.1", "26.1.2"]

[fabric."26.1"]
mod.mc_compat = "~26.1"
deps.fabric_api = "0.155.3+26.1.2"

[neoforge."26.1"]
mod.mc_compat = "[26.1, 26.2)"
deps.neo_loader = "26.1.2.107"
```

`gradle.properties`:

```properties
# Memory-capped: the dev machine has hit memory pressure. Build one target at a time locally.
org.gradle.jvmargs=-Xmx3G -XX:+UseParallelGC
org.gradle.parallel=false
org.gradle.configuration-cache=true
```

- [ ] **Step 6: Write the two loader build scripts**

`build.fabric.gradle.kts`:

```kotlin
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
            inputs.property("java", requiredJava.majorVersion)
            put("java", requiredJava.majorVersion)
        }

        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/neoforge.mods.toml")
    }

    test {
        useJUnitPlatform()
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
```

`build.neoforge.gradle.kts`:

```kotlin
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

dependencies {
    testImplementation(platform("org.junit:junit-bom:${property("deps.junit")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

neoForge {
    version = property("deps.neo_loader") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
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
```

- [ ] **Step 7: Write mod metadata**

`src/main/resources/fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "${id}",
  "version": "${version}",
  "name": "${name}",
  "description": "${description}",
  "authors": ["${authors}"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["diamondvending.platform.fabric.DiamondVendingFabric"]
  },
  "depends": {
    "fabricloader": ">=${fabric_loader}",
    "fabric-api": "*",
    "minecraft": "${minecraft}",
    "java": ">=${java}"
  }
}
```

`src/main/resources/META-INF/neoforge.mods.toml`:

```toml
modLoader = "javafml"
loaderVersion = "[1,)"
license = "MIT"

[[mods]]
modId = "${id}"
displayName = "${name}"
version = "${version}"
authors = "${authors}"
description = '''${description}'''

[[dependencies.${id}]]
modId = "minecraft"
type = "required"
versionRange = "${minecraft}"
ordering = "NONE"
side = "BOTH"
```

- [ ] **Step 8: Write the entrypoints**

`src/main/java/diamondvending/DiamondVending.java`:

```java
package diamondvending;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants and setup used by both loaders. */
public final class DiamondVending {
    public static final String MOD_ID = "diamondvending";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.1.2";

    private DiamondVending() {}

    /** An id in this mod's namespace, e.g. {@code id("vending_machine")}. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Common setup, called once by each loader's entrypoint. */
    public static void init() {
        LOGGER.info("Diamond Vending {} loaded for Minecraft {}", VERSION, MINECRAFT);
    }
}
```

`src/main/java/diamondvending/platform/fabric/DiamondVendingFabric.java`:

```java
package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint (declared in fabric.mod.json). Excluded from NeoForge builds. */
public final class DiamondVendingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DiamondVending.init();
    }
}
```

`src/main/java/diamondvending/platform/neoforge/DiamondVendingNeoForge.java`:

```java
package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint. Excluded from Fabric builds. */
@Mod(DiamondVending.MOD_ID)
public final class DiamondVendingNeoForge {
    public DiamondVendingNeoForge(IEventBus modBus, ModContainer container) {
        DiamondVending.init();
    }
}
```

- [ ] **Step 9: Extend `.gitignore`** — append (keep existing lines):

```gitignore
# Gradle / Java extras (from the Stonecutter template)
classes/
*.launch
hs_err_*.log
replay_*.log
*.hprof
*.jfr
```

- [ ] **Step 10: Build each target, one at a time, and inspect the jars**

The first build downloads Minecraft and decompiles it; allow up to ~10 minutes per target.

```bash
./gradlew :1.21.1-neoforge:build --stacktrace
```
Expected: `BUILD SUCCESSFUL`. Then, one call each:
```bash
./gradlew :1.21.1-fabric:build --stacktrace
```
```bash
./gradlew :26.1-neoforge:build --stacktrace
```
```bash
./gradlew :26.1-fabric:build --stacktrace
```

Check each jar's contents (one call per jar):

```bash
jar tf versions/1.21.1-fabric/build/libs/diamondvending-fabric-0.1.0+1.21.1.jar
```
Expected: lists `fabric.mod.json`, `diamondvending/DiamondVending.class`, `diamondvending/platform/fabric/DiamondVendingFabric.class`, `LICENSE-diamondvending`; **no** `diamondvending/platform/neoforge/` and **no** `META-INF/neoforge.mods.toml`.

```bash
jar tf versions/1.21.1-neoforge/build/libs/diamondvending-neoforge-0.1.0+1.21.1.jar
```
Expected: `META-INF/neoforge.mods.toml`, `diamondvending/platform/neoforge/DiamondVendingNeoForge.class`; **no** `platform/fabric/`, **no** `fabric.mod.json`. Repeat for the two `26.1` jars (`...+26.1.2.jar`).

Check placeholders were expanded:
```bash
unzip -p versions/1.21.1-fabric/build/libs/diamondvending-fabric-0.1.0+1.21.1.jar fabric.mod.json
```
Expected: real values (`"id": "diamondvending"`, `"minecraft": ">=1.21 <=1.21.1"`, `"java": ">=21"`), no `${`.

If `java.exclude` did **not** keep the other loader's package out (it appears in the jar or causes a compile error), replace the `sourceSets.main { java.exclude(...) }` block in the affected build script with a source-set filter on the generated sources and re-run:
```kotlin
tasks.withType<JavaCompile>().configureEach {
    exclude("diamondvending/platform/neoforge/**") // in build.fabric.gradle.kts; use platform/fabric/** in the NeoForge script
}
```

- [ ] **Step 11: Record Stonecutter's switch/reset task names**

```bash
./gradlew tasks --group stonecutter
```
Expected: tasks named `Set active project to 1.21.1-fabric` (one per node) and `Reset active project`. `docs/dev-setup.md` (next step) uses these names; if the listed names differ, use the listed ones there instead. Then confirm nothing was switched:
```bash
git -C /c/Users/benet/mcvending diff --stat stonecutter.gradle.kts
```
Expected: no output.

- [ ] **Step 12: Write the dev docs**

`docs/dev-setup.md`:

````markdown
# Developer Setup

## Requirements
- **JDK 25** (Eclipse Temurin). Gradle downloads JDK 21 automatically for the 1.21.1 builds.
- Git. Any IDE with Gradle support (IntelliJ IDEA recommended).

## Build targets
Stonecutter turns the single `src/` tree into four Gradle projects:

| Node | Minecraft | Loader |
|---|---|---|
| `1.21.1-fabric` | 1.21.1 | Fabric |
| `1.21.1-neoforge` | 1.21.1 | NeoForge |
| `26.1-fabric` | 26.1.2 | Fabric |
| `26.1-neoforge` | 26.1.2 | NeoForge |

Build or test **one target at a time** (memory):

```bash
./gradlew :26.1-neoforge:build      # jar + unit tests
./gradlew :1.21.1-fabric:test       # unit tests only
./gradlew :1.21.1-neoforge:runClient
./gradlew :26.1-fabric:runServer
```

Jars land in `versions/<node>/build/libs/`. `./gradlew build` builds all four (slow, memory-heavy).
Each node has its own game folder under `run/<node>/`.

## Stonecutter in 60 seconds
- Code is written once in `src/`. Differences use comments:
  `//? if neoforge {` … `//?}` and `//? if >=26.1 {` … `//?} else {` … `//?}`.
- Write `Identifier` (26.1 name). Stonecutter rewrites it to `ResourceLocation` for 1.21.1.
- Loader-only classes go in `diamondvending/platform/fabric/` or `.../neoforge/`; each loader's
  build excludes the other folder, so these files need no loader comments.
- `diamondvending/core/` is plain Java with **no Minecraft imports** — test it with JUnit.
- To edit code for another version in the IDE, run e.g.
  `./gradlew "Set active project to 1.21.1-fabric"`. **Before committing, run
  `./gradlew "Reset active project"`** — git must always hold the `26.1-neoforge` state. CI
  rejects anything else.

## Workflow
Branch → PR (fill in the checklist) → CI green → squash-merge.
````

`CLAUDE.md`:

```markdown
# Diamond Vending — rules for AI agents

- Spec: `docs/superpowers/specs/2026-09-23-diamond-vending-design.md`. Roadmap and plans: `docs/superpowers/plans/`.
- Build/run details: `docs/dev-setup.md`. Build **one Stonecutter node at a time**; never run all four in parallel on this machine.
- **Stonecutter:** git holds the `26.1-neoforge` state. If you switch the active version, reset it before committing. Write `Identifier`, not `ResourceLocation`.
- Loader-only code → `diamondvending/platform/<loader>/`. Version differences → `//? if >=26.1` comments, kept as small as possible.
- `diamondvending/core/` must never import `net.minecraft` or loader packages.
- Player-facing text: every failure names the reason and who can fix it. Any rule a player could trip on must be explained in the manual (spec §6.2) in the same change.
- No personal usernames in identifiers, metadata, or shipped URLs.
- Git: branch → PR → CI green → squash-merge. Don't push to `main` directly.
```

`CHANGELOG.md`:

```markdown
# Changelog

All notable changes to Diamond Vending are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Project skeleton building for Minecraft 1.21.1 and 26.1.2 on NeoForge and Fabric.
```

In `README.md`, replace the status line `> Status: design phase — nothing to download yet.` with:

```markdown
> Status: in development — see the [roadmap](docs/superpowers/plans/2026-09-23-roadmap.md). Building from source: [dev setup](docs/dev-setup.md).
```

- [ ] **Step 13: Commit** (Stonecutter must be on `26.1-neoforge` — Step 11 verified)

```bash
git -C /c/Users/benet/mcvending add settings.gradle.kts stonecutter.gradle.kts stonecutter.properties.toml build.fabric.gradle.kts build.neoforge.gradle.kts gradle.properties gradlew gradlew.bat gradle buildSrc src docs/dev-setup.md CLAUDE.md CHANGELOG.md README.md .gitignore
```
```bash
git -C /c/Users/benet/mcvending status --short
```
Expected: only the files above staged; no `build/`, `run/`, or `.gradle/`.
```bash
git -C /c/Users/benet/mcvending commit -m "build: Stonecutter project for 1.21.1 and 26.1.2 on Fabric and NeoForge"
```

---

### Task 2: `MachineLayout` — front-face geometry (TDD)

**Files:**
- Create: `src/main/java/diamondvending/core/Rect.java`, `Region.java`, `Hit.java`, `Facing.java`, `MachineLayout.java`
- Test: `src/test/java/diamondvending/core/MachineLayoutTest.java`

**Interfaces:**
- Consumes: nothing (pure Java).
- Produces (used by Plans 2–4: click routing, renderer, art generator):
  - `record Rect(double u0, double v0, double u1, double v1)` with `boolean contains(double u, double v)` (`[min,max)`), `double centerU()`, `double centerV()`.
  - `enum Region { NONE, WINDOW, LAMP, DISPLAY, BUTTON, COIN_SLOT, COIN_RETURN, TRAY }`
  - `record Hit(Region region, int button)`; `Hit.NONE`; `Hit.of(Region)`; `Hit.button(int)`.
  - `enum Facing { NORTH, SOUTH, WEST, EAST }` with `int dx()`, `int dz()` (front normal), `int rightDx()`, `int rightDz()` (offset from left column to right column).
  - `final class MachineLayout`: constants `SELECTIONS = 12`, `CANVAS = 32`, `Rect WINDOW, LAMP, DISPLAY, COIN_SLOT, COIN_RETURN, TRAY`; methods `Rect button(int i)`, `Rect buttonCell(int i)`, `Rect shelfSlot(int i)`, `Hit hitAt(double u, double v)`, `double[] toCanvas(Facing f, boolean right, boolean upper, double fx, double fy, double fz)` → `{u, v}`, `Hit hitOnFront(Facing f, boolean right, boolean upper, double fx, double fy, double fz)`.

- [ ] **Step 1: Write the failing test**

`src/test/java/diamondvending/core/MachineLayoutTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineLayoutTest {

    private static Hit centerOf(Rect r) {
        return MachineLayout.hitAt(r.centerU(), r.centerV());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void buttonCenterHitsThatButton(int i) {
        assertEquals(Hit.button(i), centerOf(MachineLayout.button(i)));
    }

    @Test
    void buttonsAreNumberedInReadingOrderTwoPerRow() {
        assertEquals(new Rect(25, 7, 27, 9), MachineLayout.button(0));
        assertEquals(new Rect(27.75, 7, 29.75, 9), MachineLayout.button(1));
        assertEquals(new Rect(25, 9.75, 27, 11.75), MachineLayout.button(2));
        assertEquals(new Rect(27.75, 20.75, 29.75, 22.75), MachineLayout.button(11));
    }

    @Test
    void gapsBetweenButtonsStillHitAButton() {
        // Horizontal gap between buttons 0 and 1 is u 27..27.75: left half → 0, right half → 1
        assertEquals(Hit.button(0), MachineLayout.hitAt(27.2, 8));
        assertEquals(Hit.button(1), MachineLayout.hitAt(27.5, 8));
        // Vertical gap between buttons 0 and 2 is v 9..9.75
        assertEquals(Hit.button(0), MachineLayout.hitAt(26, 9.2));
        assertEquals(Hit.button(2), MachineLayout.hitAt(26, 9.5));
    }

    @Test
    void panelAndBodyPartsResolve() {
        assertEquals(Hit.of(Region.COIN_SLOT), centerOf(MachineLayout.COIN_SLOT));
        assertEquals(Hit.of(Region.COIN_RETURN), centerOf(MachineLayout.COIN_RETURN));
        assertEquals(Hit.of(Region.TRAY), centerOf(MachineLayout.TRAY));
        assertEquals(Hit.of(Region.DISPLAY), centerOf(MachineLayout.DISPLAY));
        assertEquals(Hit.of(Region.LAMP), centerOf(MachineLayout.LAMP));
        assertEquals(Hit.of(Region.WINDOW), centerOf(MachineLayout.WINDOW));
    }

    @Test
    void plainBodyHitsNothing() {
        assertEquals(Hit.NONE, MachineLayout.hitAt(0.5, 0.5));   // top-left corner
        assertEquals(Hit.NONE, MachineLayout.hitAt(23.2, 12));   // strip between window and panel
        assertEquals(Hit.NONE, MachineLayout.hitAt(31.5, 31.5)); // bottom-right corner
    }

    @Test
    void seamBetweenPartsIsStillTheWindow() {
        assertEquals(Hit.of(Region.WINDOW), MachineLayout.hitAt(16, 12));
        assertEquals(Hit.of(Region.WINDOW), MachineLayout.hitAt(15.999, 16));
    }

    @Test
    void regionsNeverOverlap() {
        List<Rect> all = new ArrayList<>(List.of(
                MachineLayout.WINDOW, MachineLayout.LAMP, MachineLayout.DISPLAY,
                MachineLayout.COIN_SLOT, MachineLayout.COIN_RETURN, MachineLayout.TRAY));
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) all.add(MachineLayout.buttonCell(i));

        for (double u = 0; u < MachineLayout.CANVAS; u += 0.125) {
            for (double v = 0; v < MachineLayout.CANVAS; v += 0.125) {
                int owners = 0;
                for (Rect r : all) if (r.contains(u, v)) owners++;
                assertTrue(owners <= 1, "regions overlap at u=" + u + " v=" + v);
            }
        }
    }

    @Test
    void everyRegionFitsOnTheCanvas() {
        List<Rect> all = new ArrayList<>(List.of(
                MachineLayout.WINDOW, MachineLayout.LAMP, MachineLayout.DISPLAY,
                MachineLayout.COIN_SLOT, MachineLayout.COIN_RETURN, MachineLayout.TRAY));
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) all.add(MachineLayout.buttonCell(i));
        for (Rect r : all) {
            assertTrue(r.u0() >= 0 && r.u1() <= MachineLayout.CANVAS && r.u0() < r.u1(), r.toString());
            assertTrue(r.v0() >= 0 && r.v1() <= MachineLayout.CANVAS && r.v0() < r.v1(), r.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void shelfSlotsSitInsideTheWindow(int i) {
        Rect s = MachineLayout.shelfSlot(i);
        Rect w = MachineLayout.WINDOW;
        assertTrue(s.u0() >= w.u0() && s.u1() <= w.u1() && s.v0() >= w.v0() && s.v1() <= w.v1(), s.toString());
    }

    @Test
    void shelvesHoldThreeItemsPerRowInReadingOrder() {
        assertEquals(new Rect(3, 3.5, 6.5, 7), MachineLayout.shelfSlot(0));
        assertEquals(new Rect(17.5, 3.5, 21, 7), MachineLayout.shelfSlot(2));
        assertEquals(new Rect(3, 9, 6.5, 12.5), MachineLayout.shelfSlot(3));
        assertEquals(new Rect(17.5, 20, 21, 23.5), MachineLayout.shelfSlot(11));
    }

    @Test
    void selectionIndexOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MachineLayout.button(12));
        assertThrows(IllegalArgumentException.class, () -> MachineLayout.buttonCell(-1));
        assertThrows(IllegalArgumentException.class, () -> MachineLayout.shelfSlot(12));
    }

    @Test
    void viewersRightFollowsFacing() {
        assertEquals(1, Facing.SOUTH.rightDx());
        assertEquals(0, Facing.SOUTH.rightDz());
        assertEquals(-1, Facing.NORTH.rightDx());
        assertEquals(0, Facing.NORTH.rightDz());
        assertEquals(0, Facing.EAST.rightDx());
        assertEquals(-1, Facing.EAST.rightDz());
        assertEquals(0, Facing.WEST.rightDx());
        assertEquals(1, Facing.WEST.rightDz());
    }

    @ParameterizedTest
    @EnumSource(Facing.class)
    void canvasUGrowsSmoothlyAcrossBothColumns(Facing facing) {
        boolean alongX = facing.rightDx() != 0;
        int dir = alongX ? facing.rightDx() : facing.rightDz(); // +1 or -1
        for (int step = 0; step < 20; step++) {
            double t = step / 10.0 + 0.05;            // blocks from the machine's left edge
            boolean right = t >= 1;
            double world = dir > 0 ? t : 1 - t;       // coordinate relative to the master block origin
            double partOrigin = right ? dir : 0;
            double frac = world - partOrigin;         // position inside the part's own block
            double fx = alongX ? frac : 0.5;
            double fz = alongX ? 0.5 : frac;
            double u = MachineLayout.toCanvas(facing, right, false, fx, 0.5, fz)[0];
            assertEquals(16 * t, u, 1e-9, facing + " t=" + t);
        }
    }

    @Test
    void canvasVIsMeasuredFromTheTop() {
        assertEquals(0, MachineLayout.toCanvas(Facing.SOUTH, false, true, 0.5, 1, 0.5)[1], 1e-9);
        assertEquals(16, MachineLayout.toCanvas(Facing.SOUTH, false, true, 0.5, 0, 0.5)[1], 1e-9);
        assertEquals(16, MachineLayout.toCanvas(Facing.SOUTH, false, false, 0.5, 1, 0.5)[1], 1e-9);
        assertEquals(32, MachineLayout.toCanvas(Facing.SOUTH, false, false, 0.5, 0, 0.5)[1], 1e-9);
    }

    @Test
    void slightlyOutOfRangeHitsAreClamped() {
        double[] uv = MachineLayout.toCanvas(Facing.SOUTH, true, false, 1.0000001, -0.0000001, 0.5);
        assertEquals(32, uv[0], 1e-9);
        assertEquals(32, uv[1], 1e-9);
        assertEquals(Hit.NONE, MachineLayout.hitOnFront(Facing.SOUTH, true, false, 1.0000001, -0.0000001, 0.5));
        assertEquals(Hit.NONE, MachineLayout.hitOnFront(Facing.WEST, false, true, 0.5, 1.0000001, -0.0000001));
    }

    @Test
    void hitOnFrontFindsButtonsThroughTheRightPart() {
        // Button 0's center is canvas (26, 8): right column (face u = 10/16 = 0.625), upper row (fy = 0.5)
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.SOUTH, true, true, 0.625, 0.5, 0.5));
        // Facing north mirrors x
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.NORTH, true, true, 0.375, 0.5, 0.5));
        // Facing east uses z, mirrored; facing west uses z directly
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.EAST, true, true, 0.5, 0.5, 0.375));
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.WEST, true, true, 0.5, 0.5, 0.625));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: FAIL — compilation errors (`cannot find symbol: class MachineLayout`, `Rect`, `Hit`, `Region`, `Facing`).

- [ ] **Step 3: Write the implementation**

`src/main/java/diamondvending/core/Rect.java`:

```java
package diamondvending.core;

/** A rectangle on the machine's 32×32 front canvas, in pixels. Min edges inclusive, max edges exclusive. */
public record Rect(double u0, double v0, double u1, double v1) {
    public boolean contains(double u, double v) {
        return u >= u0 && u < u1 && v >= v0 && v < v1;
    }

    public double centerU() {
        return (u0 + u1) / 2;
    }

    public double centerV() {
        return (v0 + v1) / 2;
    }
}
```

`src/main/java/diamondvending/core/Region.java`:

```java
package diamondvending.core;

/** The kinds of things on the machine's front face. */
public enum Region {
    NONE, WINDOW, LAMP, DISPLAY, BUTTON, COIN_SLOT, COIN_RETURN, TRAY
}
```

`src/main/java/diamondvending/core/Hit.java`:

```java
package diamondvending.core;

/** What a click on the front canvas hit. {@code button} is 0–11 for {@link Region#BUTTON}, otherwise -1. */
public record Hit(Region region, int button) {
    public static final Hit NONE = new Hit(Region.NONE, -1);

    public static Hit of(Region region) {
        return new Hit(region, -1);
    }

    public static Hit button(int index) {
        return new Hit(Region.BUTTON, index);
    }
}
```

`src/main/java/diamondvending/core/Facing.java`:

```java
package diamondvending.core;

/** The horizontal direction the machine's front faces, mirroring Minecraft's Direction without depending on it. */
public enum Facing {
    NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0);

    private final int dx;
    private final int dz;

    Facing(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** X component of the front face's outward normal. */
    public int dx() {
        return dx;
    }

    /** Z component of the front face's outward normal. */
    public int dz() {
        return dz;
    }

    /** X offset from the left column to the right column, as seen by someone facing the front. */
    public int rightDx() {
        return dz;
    }

    /** Z offset from the left column to the right column, as seen by someone facing the front. */
    public int rightDz() {
        return -dx;
    }
}
```

`src/main/java/diamondvending/core/MachineLayout.java`:

```java
package diamondvending.core;

/**
 * Geometry of the machine's front face: a 32×32 pixel canvas (16 px per block), {@code u} left→right and
 * {@code v} top→bottom as seen from the front. The textures must match these numbers (spec §2.2).
 */
public final class MachineLayout {
    public static final int SELECTIONS = 12;
    public static final int CANVAS = 32;

    public static final Rect WINDOW = new Rect(1.5, 2, 22.5, 24);
    public static final Rect LAMP = new Rect(26, 0.5, 28.5, 2);
    public static final Rect DISPLAY = new Rect(24.75, 3, 29.75, 5.5);
    public static final Rect COIN_SLOT = new Rect(24, 24.5, 27.5, 29);
    public static final Rect COIN_RETURN = new Rect(28, 25, 30.5, 27.5);
    public static final Rect TRAY = new Rect(3, 26, 21, 30.5);

    private static final double BUTTON_U = 25;
    private static final double BUTTON_V = 7;
    private static final double BUTTON_SIZE = 2;
    private static final double BUTTON_PITCH = 2.75;
    private static final double BUTTON_PAD = (BUTTON_PITCH - BUTTON_SIZE) / 2;

    private static final double SHELF_U = 3;
    private static final double SHELF_V = 3.5;
    private static final double SHELF_SIZE = 3.5;
    private static final double SHELF_PITCH_U = 7.25;
    private static final double SHELF_PITCH_V = 5.5;

    private MachineLayout() {}

    /** The visible button for selection {@code i} (0–11): 2 columns × 6 rows in reading order. */
    public static Rect button(int i) {
        checkSelection(i);
        double u = BUTTON_U + (i % 2) * BUTTON_PITCH;
        double v = BUTTON_V + (i / 2) * BUTTON_PITCH;
        return new Rect(u, v, u + BUTTON_SIZE, v + BUTTON_SIZE);
    }

    /** The clickable cell for button {@code i}: the button padded by half the gap on every side, so gaps have no dead spots. */
    public static Rect buttonCell(int i) {
        Rect b = button(i);
        return new Rect(b.u0() - BUTTON_PAD, b.v0() - BUTTON_PAD, b.u1() + BUTTON_PAD, b.v1() + BUTTON_PAD);
    }

    /** Where selection {@code i}'s item sits behind the glass: 4 shelves × 3 items in reading order. */
    public static Rect shelfSlot(int i) {
        checkSelection(i);
        double u = SHELF_U + (i % 3) * SHELF_PITCH_U;
        double v = SHELF_V + (i / 3) * SHELF_PITCH_V;
        return new Rect(u, v, u + SHELF_SIZE, v + SHELF_SIZE);
    }

    /** What is at canvas point (u, v). */
    public static Hit hitAt(double u, double v) {
        for (int i = 0; i < SELECTIONS; i++) {
            if (buttonCell(i).contains(u, v)) return Hit.button(i);
        }
        if (COIN_SLOT.contains(u, v)) return Hit.of(Region.COIN_SLOT);
        if (COIN_RETURN.contains(u, v)) return Hit.of(Region.COIN_RETURN);
        if (TRAY.contains(u, v)) return Hit.of(Region.TRAY);
        if (DISPLAY.contains(u, v)) return Hit.of(Region.DISPLAY);
        if (LAMP.contains(u, v)) return Hit.of(Region.LAMP);
        if (WINDOW.contains(u, v)) return Hit.of(Region.WINDOW);
        return Hit.NONE;
    }

    /**
     * Converts a hit on one part's front face to canvas coordinates.
     *
     * @param right whether the part is in the right-hand column (as seen from the front)
     * @param upper whether the part is in the upper row
     * @param fx    hit x inside the part's block (hit location minus block position), nominally 0–1
     * @param fy    hit y inside the part's block, nominally 0–1
     * @param fz    hit z inside the part's block, nominally 0–1
     * @return {@code {u, v}} on the 32×32 canvas
     */
    public static double[] toCanvas(Facing facing, boolean right, boolean upper, double fx, double fy, double fz) {
        fx = clamp01(fx);
        fy = clamp01(fy);
        fz = clamp01(fz);
        double faceU = switch (facing) {
            case SOUTH -> fx;
            case NORTH -> 1 - fx;
            case EAST -> 1 - fz;
            case WEST -> fz;
        };
        double faceV = 1 - fy;
        return new double[] {(right ? 16 : 0) + faceU * 16, (upper ? 0 : 16) + faceV * 16};
    }

    /** {@link #toCanvas} followed by {@link #hitAt}. */
    public static Hit hitOnFront(Facing facing, boolean right, boolean upper, double fx, double fy, double fz) {
        double[] uv = toCanvas(facing, right, upper, fx, fy, fz);
        return hitAt(uv[0], uv[1]);
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static void checkSelection(int i) {
        if (i < 0 || i >= SELECTIONS) {
            throw new IllegalArgumentException("selection index must be 0-" + (SELECTIONS - 1) + ", was " + i);
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: `BUILD SUCCESSFUL`. Confirm the tests actually ran (not skipped):
```bash
ls versions/26.1-neoforge/build/test-results/test/
```
Expected: `TEST-diamondvending.core.MachineLayoutTest.xml`. If the folder is missing or has no XML (Stonecutter did not wire `src/test`), add to **both** build scripts, above `dependencies {`:
```kotlin
sourceSets.test {
    java.srcDir(rootProject.file("src/test/java"))
}
```
and re-run. Then confirm on a Fabric/1.21.1 node too:
```bash
./gradlew :1.21.1-fabric:test --stacktrace
```
Expected: `BUILD SUCCESSFUL` with the same XML under `versions/1.21.1-fabric/build/test-results/test/`.

- [ ] **Step 5: Commit**

```bash
git -C /c/Users/benet/mcvending add src/main/java/diamondvending/core src/test/java/diamondvending/core/MachineLayoutTest.java
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat(core): MachineLayout front-face geometry and click regions"
```
(If Step 4 required the `sourceSets.test` fallback, also add `build.fabric.gradle.kts build.neoforge.gradle.kts`.)

---

### Task 3: `PurchaseRules` — the purchase decision (TDD)

**Files:**
- Create: `src/main/java/diamondvending/core/PurchaseInput.java`, `DenyReason.java`, `PurchaseDecision.java`, `PurchaseRules.java`
- Test: `src/test/java/diamondvending/core/PurchaseRulesTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces (used by Plan 3's transaction code):
  - `record PurchaseInput(boolean catalogMissing, boolean selectionSetUp, boolean infinite, int quantity, int stockCount, boolean trayHasRoom, boolean cashBoxHasRoom, int price, int credit, int inventoryCurrency)` — throws `IllegalArgumentException` if any count is negative. `cashBoxHasRoom` means "room for `price` currency items".
  - `enum DenyReason { CATALOG_MISSING, EMPTY, SOLD_OUT, TRAY_FULL, CASH_BOX_FULL, NOT_ENOUGH_MONEY }`
  - `sealed interface PurchaseDecision` with `record Approved(int fromCredit, int fromInventory)` and `record Denied(DenyReason reason)`.
  - `PurchaseRules.decide(PurchaseInput) → PurchaseDecision`.

- [ ] **Step 1: Write the failing test**

`src/test/java/diamondvending/core/PurchaseRulesTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseRulesTest {

    /** A healthy owned machine selling 1 item for 3 diamonds to a buyer with 10 diamonds and no credit. */
    private static final class In {
        boolean catalogMissing = false;
        boolean setUp = true;
        boolean infinite = false;
        int quantity = 1;
        int stock = 10;
        boolean trayRoom = true;
        boolean cashRoom = true;
        int price = 3;
        int credit = 0;
        int inventory = 10;

        PurchaseDecision decide() {
            return PurchaseRules.decide(new PurchaseInput(
                    catalogMissing, setUp, infinite, quantity, stock, trayRoom, cashRoom, price, credit, inventory));
        }
    }

    private static PurchaseDecision denied(DenyReason reason) {
        return new PurchaseDecision.Denied(reason);
    }

    @Test
    void paysFromInventoryWhenThereIsNoCredit() {
        assertEquals(new PurchaseDecision.Approved(0, 3), new In().decide());
    }

    @Test
    void spendsCreditFirst() {
        In in = new In();
        in.credit = 2;
        assertEquals(new PurchaseDecision.Approved(2, 1), in.decide());
    }

    @Test
    void creditCanCoverTheWholePrice() {
        In in = new In();
        in.credit = 5;
        assertEquals(new PurchaseDecision.Approved(3, 0), in.decide());
    }

    @Test
    void exactChangeIsEnough() {
        In in = new In();
        in.credit = 1;
        in.inventory = 2;
        assertEquals(new PurchaseDecision.Approved(1, 2), in.decide());
    }

    @Test
    void shortByOneIsRefused() {
        In in = new In();
        in.credit = 1;
        in.inventory = 1;
        assertEquals(denied(DenyReason.NOT_ENOUGH_MONEY), in.decide());
    }

    @Test
    void freeItemsNeedNoMoney() {
        In in = new In();
        in.price = 0;
        in.inventory = 0;
        assertEquals(new PurchaseDecision.Approved(0, 0), in.decide());
    }

    @Test
    void soldOutWhenStockIsBelowTheQuantity() {
        In in = new In();
        in.quantity = 16;
        in.stock = 15;
        assertEquals(denied(DenyReason.SOLD_OUT), in.decide());
        in.stock = 16;
        assertEquals(new PurchaseDecision.Approved(0, 3), in.decide());
    }

    @Test
    void emptySelectionIsRefused() {
        In in = new In();
        in.setUp = false;
        assertEquals(denied(DenyReason.EMPTY), in.decide());
    }

    @Test
    void fullTrayIsRefused() {
        In in = new In();
        in.trayRoom = false;
        assertEquals(denied(DenyReason.TRAY_FULL), in.decide());
    }

    @Test
    void fullCashBoxIsRefused() {
        In in = new In();
        in.cashRoom = false;
        assertEquals(denied(DenyReason.CASH_BOX_FULL), in.decide());
    }

    @Test
    void missingCatalogIsRefused() {
        In in = new In();
        in.catalogMissing = true;
        assertEquals(denied(DenyReason.CATALOG_MISSING), in.decide());
    }

    @Test
    void infiniteMachinesIgnoreStockAndCashBox() {
        In in = new In();
        in.infinite = true;
        in.stock = 0;
        in.cashRoom = false;
        assertEquals(new PurchaseDecision.Approved(0, 3), in.decide());
    }

    @Test
    void infiniteMachinesStillNeedTrayRoomAndMoney() {
        In in = new In();
        in.infinite = true;
        in.trayRoom = false;
        assertEquals(denied(DenyReason.TRAY_FULL), in.decide());
        in.trayRoom = true;
        in.inventory = 0;
        assertEquals(denied(DenyReason.NOT_ENOUGH_MONEY), in.decide());
    }

    @Test
    void checksHappenInSpecOrder() {
        In in = new In();
        in.catalogMissing = true;
        in.setUp = false;
        in.stock = 0;
        in.trayRoom = false;
        in.cashRoom = false;
        in.inventory = 0;
        assertEquals(denied(DenyReason.CATALOG_MISSING), in.decide());
        in.catalogMissing = false;
        assertEquals(denied(DenyReason.EMPTY), in.decide());
        in.setUp = true;
        assertEquals(denied(DenyReason.SOLD_OUT), in.decide());
        in.stock = 10;
        assertEquals(denied(DenyReason.TRAY_FULL), in.decide());
        in.trayRoom = true;
        assertEquals(denied(DenyReason.CASH_BOX_FULL), in.decide());
        in.cashRoom = true;
        assertEquals(denied(DenyReason.NOT_ENOUGH_MONEY), in.decide());
    }

    @Test
    void hugeFundsDoNotOverflow() {
        In in = new In();
        in.price = 999;
        in.credit = Integer.MAX_VALUE;
        in.inventory = Integer.MAX_VALUE;
        assertEquals(new PurchaseDecision.Approved(999, 0), in.decide());
    }

    @Test
    void negativeCountsAreRejected() {
        In in = new In();
        in.price = -1;
        assertThrows(IllegalArgumentException.class, in::decide);
        in.price = 3;
        in.credit = -5;
        assertThrows(IllegalArgumentException.class, in::decide);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: FAIL — `cannot find symbol: class PurchaseRules` (and `PurchaseInput`, `PurchaseDecision`, `DenyReason`).

- [ ] **Step 3: Write the implementation**

`src/main/java/diamondvending/core/PurchaseInput.java`:

```java
package diamondvending.core;

/**
 * Everything the purchase decision needs, already measured by the caller.
 *
 * @param quantity          items handed out per purchase
 * @param stockCount        matching items in stock (ignored for infinite machines)
 * @param cashBoxHasRoom    whether the cash box can take {@code price} currency items (ignored for infinite machines)
 * @param credit            the buyer's credit on this machine, in currency items
 * @param inventoryCurrency currency items in the buyer's inventory, hotbar and offhand
 */
public record PurchaseInput(
        boolean catalogMissing,
        boolean selectionSetUp,
        boolean infinite,
        int quantity,
        int stockCount,
        boolean trayHasRoom,
        boolean cashBoxHasRoom,
        int price,
        int credit,
        int inventoryCurrency) {

    public PurchaseInput {
        if (quantity < 0 || stockCount < 0 || price < 0 || credit < 0 || inventoryCurrency < 0) {
            throw new IllegalArgumentException("counts must not be negative (quantity=" + quantity + ", stock=" + stockCount
                    + ", price=" + price + ", credit=" + credit + ", inventory=" + inventoryCurrency + ")");
        }
    }
}
```

`src/main/java/diamondvending/core/DenyReason.java`:

```java
package diamondvending.core;

/** Why a purchase was refused, in the order the checks run (spec §3.3). */
public enum DenyReason {
    CATALOG_MISSING, EMPTY, SOLD_OUT, TRAY_FULL, CASH_BOX_FULL, NOT_ENOUGH_MONEY
}
```

`src/main/java/diamondvending/core/PurchaseDecision.java`:

```java
package diamondvending.core;

/** The outcome of a purchase attempt. */
public sealed interface PurchaseDecision {
    /** The purchase goes ahead: take {@code fromCredit} from the buyer's credit, then {@code fromInventory} from their inventory. */
    record Approved(int fromCredit, int fromInventory) implements PurchaseDecision {}

    /** The purchase is refused and nothing changes. */
    record Denied(DenyReason reason) implements PurchaseDecision {}
}
```

`src/main/java/diamondvending/core/PurchaseRules.java`:

```java
package diamondvending.core;

/** Decides whether a purchase can happen and how it is paid for. All checks run before anything changes (spec §3.3). */
public final class PurchaseRules {
    private PurchaseRules() {}

    public static PurchaseDecision decide(PurchaseInput in) {
        if (in.catalogMissing()) return deny(DenyReason.CATALOG_MISSING);
        if (!in.selectionSetUp()) return deny(DenyReason.EMPTY);
        if (!in.infinite() && in.stockCount() < in.quantity()) return deny(DenyReason.SOLD_OUT);
        if (!in.trayHasRoom()) return deny(DenyReason.TRAY_FULL);
        if (!in.infinite() && !in.cashBoxHasRoom()) return deny(DenyReason.CASH_BOX_FULL);
        long funds = (long) in.credit() + in.inventoryCurrency();
        if (funds < in.price()) return deny(DenyReason.NOT_ENOUGH_MONEY);

        int fromCredit = Math.min(in.credit(), in.price());
        return new PurchaseDecision.Approved(fromCredit, in.price() - fromCredit);
    }

    private static PurchaseDecision deny(DenyReason reason) {
        return new PurchaseDecision.Denied(reason);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: `BUILD SUCCESSFUL`; `versions/26.1-neoforge/build/test-results/test/` now also has `TEST-diamondvending.core.PurchaseRulesTest.xml`.

- [ ] **Step 5: Commit**

```bash
git -C /c/Users/benet/mcvending add src/main/java/diamondvending/core src/test/java/diamondvending/core/PurchaseRulesTest.java
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat(core): PurchaseRules decision with credit-first payment"
```

---

### Task 4: `MachineProblems` — persistent problem detection (TDD)

**Files:**
- Create: `src/main/java/diamondvending/core/Problem.java`, `MachineFacts.java`, `MachineProblems.java`
- Test: `src/test/java/diamondvending/core/MachineProblemsTest.java`

**Interfaces:**
- Consumes: `MachineLayout.SELECTIONS` (Task 2).
- Produces (used by Plan 3's display/HUD and Plan 4's setup screen):
  - `enum Problem { CATALOG_MISSING, NOT_SET_UP, CASH_BOX_FULL, SOLD_OUT, TRAY_FULL }` in priority order, each with `String key()` → `"catalog_missing"`, `"not_set_up"`, `"cash_box_full"`, `"sold_out"`, `"tray_full"` (becomes lang keys like `diamondvending.problem.cash_box_full.display`).
  - `record MachineFacts(boolean infinite, boolean catalogMissing, int selectionsSetUp, int selectionsInStock, boolean cashBoxHasEmptySlot, boolean trayHasEmptySlot)` — throws `IllegalArgumentException` unless `0 <= selectionsInStock <= selectionsSetUp <= 12`.
  - `MachineProblems.of(MachineFacts) → List<Problem>` (immutable, priority order).

- [ ] **Step 1: Write the failing test**

`src/test/java/diamondvending/core/MachineProblemsTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineProblemsTest {

    /** A healthy owned machine: 5 selections set up, all in stock, room everywhere. */
    private static final class Facts {
        boolean infinite = false;
        boolean catalogMissing = false;
        int setUp = 5;
        int inStock = 5;
        boolean cashSlot = true;
        boolean traySlot = true;

        List<Problem> problems() {
            return MachineProblems.of(new MachineFacts(infinite, catalogMissing, setUp, inStock, cashSlot, traySlot));
        }
    }

    @Test
    void healthyMachineHasNoProblems() {
        assertEquals(List.of(), new Facts().problems());
    }

    @Test
    void missingCatalog() {
        Facts f = new Facts();
        f.catalogMissing = true;
        assertEquals(List.of(Problem.CATALOG_MISSING), f.problems());
    }

    @Test
    void notSetUp() {
        Facts f = new Facts();
        f.setUp = 0;
        f.inStock = 0;
        assertEquals(List.of(Problem.NOT_SET_UP), f.problems());
    }

    @Test
    void cashBoxFull() {
        Facts f = new Facts();
        f.cashSlot = false;
        assertEquals(List.of(Problem.CASH_BOX_FULL), f.problems());
    }

    @Test
    void soldOutOnlyWhenEverySelectionIsOut() {
        Facts f = new Facts();
        f.inStock = 1;
        assertEquals(List.of(), f.problems());
        f.inStock = 0;
        assertEquals(List.of(Problem.SOLD_OUT), f.problems());
    }

    @Test
    void trayFull() {
        Facts f = new Facts();
        f.traySlot = false;
        assertEquals(List.of(Problem.TRAY_FULL), f.problems());
    }

    @Test
    void infiniteMachinesNeverReportCashBoxOrSoldOut() {
        Facts f = new Facts();
        f.infinite = true;
        f.cashSlot = false;
        f.inStock = 0;
        assertEquals(List.of(), f.problems());
    }

    @Test
    void infiniteMachinesStillReportTheRest() {
        Facts f = new Facts();
        f.infinite = true;
        f.traySlot = false;
        assertEquals(List.of(Problem.TRAY_FULL), f.problems());
        f.setUp = 0;
        f.inStock = 0;
        assertEquals(List.of(Problem.NOT_SET_UP, Problem.TRAY_FULL), f.problems());
    }

    @Test
    void missingCatalogHidesNotSetUpAndSoldOut() {
        Facts f = new Facts();
        f.catalogMissing = true;
        f.setUp = 0;
        f.inStock = 0;
        assertEquals(List.of(Problem.CATALOG_MISSING), f.problems());
    }

    @Test
    void severalProblemsComeInPriorityOrder() {
        Facts f = new Facts();
        f.cashSlot = false;
        f.inStock = 0;
        f.traySlot = false;
        assertEquals(List.of(Problem.CASH_BOX_FULL, Problem.SOLD_OUT, Problem.TRAY_FULL), f.problems());
    }

    @Test
    void keysAreUniqueSnakeCase() {
        Set<String> seen = new HashSet<>();
        for (Problem p : Problem.values()) {
            assertTrue(p.key().matches("[a-z]+(_[a-z]+)*"), p.key());
            assertTrue(seen.add(p.key()), "duplicate key " + p.key());
        }
    }

    @Test
    void impossibleFactsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MachineFacts(false, false, 2, 3, true, true));
        assertThrows(IllegalArgumentException.class, () -> new MachineFacts(false, false, 13, 0, true, true));
        assertThrows(IllegalArgumentException.class, () -> new MachineFacts(false, false, -1, 0, true, true));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: FAIL — `cannot find symbol: class MachineProblems` (and `Problem`, `MachineFacts`).

- [ ] **Step 3: Write the implementation**

`src/main/java/diamondvending/core/Problem.java`:

```java
package diamondvending.core;

/** Persistent machine problems, in display priority order (spec §3.5 b). */
public enum Problem {
    CATALOG_MISSING("catalog_missing"),
    NOT_SET_UP("not_set_up"),
    CASH_BOX_FULL("cash_box_full"),
    SOLD_OUT("sold_out"),
    TRAY_FULL("tray_full");

    private final String key;

    Problem(String key) {
        this.key = key;
    }

    /** Stable snake_case id, used to build translation keys. */
    public String key() {
        return key;
    }
}
```

`src/main/java/diamondvending/core/MachineFacts.java`:

```java
package diamondvending.core;

/**
 * Measured facts about a machine, used to work out its problems.
 *
 * @param selectionsSetUp     selections that have an item (0–12)
 * @param selectionsInStock   set-up selections with enough stock for one purchase (owned machines)
 * @param cashBoxHasEmptySlot whether the cash box has at least one empty slot (owned machines)
 * @param trayHasEmptySlot    whether the tray has at least one empty slot
 */
public record MachineFacts(
        boolean infinite,
        boolean catalogMissing,
        int selectionsSetUp,
        int selectionsInStock,
        boolean cashBoxHasEmptySlot,
        boolean trayHasEmptySlot) {

    public MachineFacts {
        if (selectionsSetUp < 0 || selectionsSetUp > MachineLayout.SELECTIONS
                || selectionsInStock < 0 || selectionsInStock > selectionsSetUp) {
            throw new IllegalArgumentException("impossible selection counts: setUp=" + selectionsSetUp
                    + ", inStock=" + selectionsInStock);
        }
    }
}
```

`src/main/java/diamondvending/core/MachineProblems.java`:

```java
package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/** Works out which persistent problems a machine has (spec §3.5 b). */
public final class MachineProblems {
    private MachineProblems() {}

    /** Active problems in priority order; empty when the machine is healthy. */
    public static List<Problem> of(MachineFacts f) {
        List<Problem> problems = new ArrayList<>();
        if (f.catalogMissing()) {
            problems.add(Problem.CATALOG_MISSING);
        } else if (f.selectionsSetUp() == 0) {
            problems.add(Problem.NOT_SET_UP);
        }
        if (!f.infinite() && !f.cashBoxHasEmptySlot()) {
            problems.add(Problem.CASH_BOX_FULL);
        }
        if (!f.infinite() && !f.catalogMissing() && f.selectionsSetUp() > 0 && f.selectionsInStock() == 0) {
            problems.add(Problem.SOLD_OUT);
        }
        if (!f.trayHasEmptySlot()) {
            problems.add(Problem.TRAY_FULL);
        }
        return List.copyOf(problems);
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: `BUILD SUCCESSFUL` with `TEST-diamondvending.core.MachineProblemsTest.xml` present. Then run the other three nodes, one call each, to prove the core compiles and passes everywhere:
```bash
./gradlew :26.1-fabric:test --stacktrace
```
```bash
./gradlew :1.21.1-neoforge:test --stacktrace
```
```bash
./gradlew :1.21.1-fabric:test --stacktrace
```
Expected: `BUILD SUCCESSFUL` for each.

- [ ] **Step 5: Commit**

```bash
git -C /c/Users/benet/mcvending add src/main/java/diamondvending/core src/test/java/diamondvending/core/MachineProblemsTest.java
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat(core): MachineProblems persistent problem detection"
```

---

### Task 5: CI, Dependabot, and the PR

**Files:**
- Create: `.github/workflows/build.yml`, `.github/dependabot.yml`
- Modify: `CHANGELOG.md`, `docs/superpowers/plans/2026-09-23-roadmap.md` (Plan 1 status)

**Interfaces:**
- Consumes: the four node names and jar names from Task 1.
- Produces: required-in-practice checks `guard` and `Build <node>` on every PR; downloadable jar artifacts per node.

- [ ] **Step 1: Write the workflow**

`.github/workflows/build.yml`:

```yaml
name: Build

on:
  push:
    branches: [main]
  pull_request:
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: build-${{ github.ref }}
  cancel-in-progress: true

jobs:
  guard:
    name: Stonecutter is on the VCS version
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - name: Active version must be 26.1-neoforge
        run: grep -q 'stonecutter active "26.1-neoforge"' stonecutter.gradle.kts

  build:
    name: Build ${{ matrix.target }}
    needs: guard
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        target: [1.21.1-fabric, 1.21.1-neoforge, 26.1-fabric, 26.1-neoforge]
    steps:
      - uses: actions/checkout@v7

      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: |
            21
            25

      - uses: gradle/actions/setup-gradle@v6

      - name: Build and test
        run: ./gradlew :${{ matrix.target }}:build --stacktrace

      - name: Jar contains only this loader's code
        run: |
          jar=$(ls versions/${{ matrix.target }}/build/libs/diamondvending-*.jar | grep -v -- '-sources' | head -n 1)
          echo "Checking $jar"
          case "${{ matrix.target }}" in
            *-fabric)   other=neoforge ;;
            *-neoforge) other=fabric ;;
          esac
          if jar tf "$jar" | grep -q "diamondvending/platform/$other/"; then
            echo "::error::$jar contains diamondvending/platform/$other classes"
            exit 1
          fi

      - uses: actions/upload-artifact@v7
        with:
          name: diamondvending-${{ matrix.target }}
          path: versions/${{ matrix.target }}/build/libs/*.jar
          if-no-files-found: error

      - uses: actions/upload-artifact@v7
        if: failure()
        with:
          name: test-report-${{ matrix.target }}
          path: versions/${{ matrix.target }}/build/reports/tests/
          if-no-files-found: ignore
```

- [ ] **Step 2: Write the Dependabot config**

`.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
    groups:
      actions:
        patterns: ["*"]

  - package-ecosystem: gradle
    directory: /
    schedule:
      interval: weekly
    groups:
      gradle:
        patterns: ["*"]
```

(Minecraft, loader, and Fabric API versions live in `stonecutter.properties.toml`, which Dependabot does not read; those stay manual on purpose.)

- [ ] **Step 3: Update the changelog and roadmap**

In `CHANGELOG.md` under `### Added`, add:

```markdown
- Core rules (not yet wired into the game): front-face click regions, purchase decisions with credit-first payment, and machine problem detection — all unit-tested.
- CI that builds and tests all four jars on every pull request.
```

In `docs/superpowers/plans/2026-09-23-roadmap.md`, change Plan 1's status cell from `Planned` to `Done`.

- [ ] **Step 4: Commit and push**

```bash
git -C /c/Users/benet/mcvending add .github/workflows/build.yml .github/dependabot.yml CHANGELOG.md docs/superpowers/plans/2026-09-23-roadmap.md
```
```bash
git -C /c/Users/benet/mcvending commit -m "ci: build and test all four targets; weekly Dependabot updates"
```
```bash
git -C /c/Users/benet/mcvending push -u origin plan-1/foundation
```

- [ ] **Step 5: Open the PR and watch CI**

```bash
gh pr create -R benethcopilot/diamondvending --base main --head plan-1/foundation --milestone v1.0 --label build --title "Plan 1: Foundation — Stonecutter build, CI, and core rules" --body-file "$SCRATCH/plan1-pr.md"
```

Write `$SCRATCH/plan1-pr.md` first with the PR template's sections filled in (What changed / Why / Checklist — tick the build box only after CI passes; the manual and player-message boxes are "n/a: no player-facing behavior yet"), ending with the PR attribution lines required by the session.

Then:
```bash
gh pr checks -R benethcopilot/diamondvending --watch
```
Expected: `Stonecutter is on the VCS version` and all four `Build …` jobs pass. If a job fails, download its `test-report-<node>` artifact or read the log (`gh run view --log-failed`), fix on the branch, commit, push, and re-watch.

- [ ] **Step 6: Merge**

```bash
gh pr merge -R benethcopilot/diamondvending --squash --delete-branch
```
```bash
git -C /c/Users/benet/mcvending checkout main
```
```bash
git -C /c/Users/benet/mcvending pull --ff-only
```
```bash
git -C /c/Users/benet/mcvending branch -D plan-1/foundation
```

Plan 1 is done when `main` has a green `Build` run with four jar artifacts.
