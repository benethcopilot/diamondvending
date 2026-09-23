# Diamond Vending — Plan 2: The Machine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A placeable, dyeable, owner-protected 2×2 vending machine block with generated textures and a crafting recipe, on all four targets, verified by in-game GameTests in CI.

**Architecture:** Loader differences sit behind a tiny `Registrar` interface (one implementation per loader). Version differences use Stonecutter comments in a few small helpers (`RegistryCompat`, `MachineAccess`, `Messages`, `MachineItems`) and inside the block/block entity. Textures, block/item models, blockstates and the GameTest structure are **generated** from `core/MachineLayout` by a Java generator in the test source set, and a JUnit test fails whenever the committed files drift from it. GameTests live in a separate `gametest` source set that builds a test-only mod, so no test code ships.

**Tech Stack:** as Plan 1, plus vanilla GameTest framework (NeoForge `gameTestServer` run, Fabric API `fabric-gametest`), `java.awt`/`ImageIO` for textures.

**Spec:** [`docs/superpowers/specs/2026-09-23-diamond-vending-design.md`](../specs/2026-09-23-diamond-vending-design.md) · **Roadmap:** [`2026-09-23-roadmap.md`](2026-09-23-roadmap.md) · **Previous:** [Plan 1](2026-09-23-plan-1-foundation.md)

## Global Constraints

- Everything in Plan 1's Global Constraints still holds (mod id, package `diamondvending`, nodes, vcsVersion `26.1-neoforge`, no runtime deps, `core/` has no Minecraft imports, one Gradle node at a time, one shell command per Bash call, branch → PR → squash).
- Branch: `plan-2/the-machine`.
- If `java` isn't on PATH in the Bash tool, prefix Gradle with `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"`.
- `$SCRATCH` = the session's scratchpad directory. When a compile error needs checking against real Minecraft sources, extract them there first (one command each): `unzip -q -o versions/1.21.1-neoforge/build/moddev/artifacts/neoforge-21.1.250-sources.jar -d "$SCRATCH/src-1211"`, `unzip -q -o versions/26.1-neoforge/build/moddev/artifacts/minecraft-patched-26.1.2.107-sources.jar -d "$SCRATCH/src-261"`, and the NeoForge 26.1 API sources jar from `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.107/*/neoforge-26.1.2.107-sources.jar` into `$SCRATCH/src-261-neo`. Every API in this plan was checked against these on 2026-09-23.
- Block id **`diamondvending:vending_machine`**; item id the same; block entity type id the same.
- Block-state properties (exact names/values): `facing` (north/south/west/east), `half` (lower/upper), `side` (left/right), `color` (16 dye names). Default color **red**.
- The **lower-left part (as seen from the front) is the master** and the only part with a block entity. Right-hand column offset = `core/Facing.rightDx/rightDz`; the front faces the placing player.
- Block settings (spec §2.1): light level **6**, push reaction **BLOCK**, explosion resistance **1200**, tagged `minecraft:wither_immune`, `minecraft:dragon_immune`, `minecraft:mineable/pickaxe`, no loot table (drops are handled in code).
- **Protection:** hardness is **−1** (automation can't break it). Owners and admins mine it like an iron block (hardness **5**). Everyone else gets **zero** progress and the action-bar message **"Only the owner can do that."** (lang key `message.diamondvending.owner_only`).
- **Admin** = creative mode **or** permission level 2 (1.21.1: `hasPermissions(2)`; 26.1: `permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`). Machines with no owner are admin-only.
- Breaking any part removes all four; survival breaks drop **one** machine item carrying the machine's color (`minecraft:base_color`); creative breaks drop nothing.
- Dye: owner/admin right-clicking with a dye repaints all four parts and uses one dye (none in creative); same color = no dye used; non-owners get the owner-only message and nothing changes.
- Item color: `minecraft:base_color` component (missing = red). On 1.21.1 the item also carries `minecraft:custom_model_data` = dye id + 1 (1.21.1 item models can only switch on that).
- Recipe (spec §6.1): shaped `BGB / BRB / BDB` — B iron block, G glass pane, R redstone, D diamond → 1 vending machine; unlocked by obtaining a diamond.
- Generated files must match the generator exactly (`GeneratedFilesTest`); regenerate with `./gradlew :26.1-neoforge:generateArt`.
- Version-specific resources: `src/main/resources-1.21.1/` and `src/main/resources-26.1/` (added per node by the build scripts); everything else stays in `src/main/resources/`.

## Review Focus

1. **Machines facing east, south or west** — the right-hand column must go to the viewer's right in every facing, or parts get orphaned. → Task 2, `worksInEveryFacing`.
2. **Machines with no owner** (placed by `/setblock` or structures) — only admins may mine them. → Task 2, `ownerlessMachinesAreAdminOnly`.
3. **One part removed by a world edit** (not a player) — the remaining parts must disappear, never leave ghost halves. → Task 2, `aBrokenMachineCleansItselfUp`.
4. **Two machines side by side** — breaking one must not touch the other. → Task 2, `neighbouringMachinesStaySeparate`.
5. **Dyeing in creative** — must not use up the dye (and survival must). → Task 3, `creativeDyeingKeepsTheDye`.

---

## File Structure

```
build.fabric.gradle.kts / build.neoforge.gradle.kts   + resources-<mc> dir, gametest source set + test mod + run, generateArt task
src/main/java/diamondvending/
  Messages.java                         action-bar messages (version-specific call)
  registry/Registrar.java               loader-neutral registration interface
  registry/RegistryCompat.java          version-specific property ids + block entity type construction
  registry/ModContent.java              our block, item, block entity type
  block/MachineSide.java                LEFT/RIGHT block-state enum
  block/MachinePart.java                the four parts: state <-> part, positions from master
  block/MachineAccess.java              owner/admin checks
  block/MachineItems.java               colored machine items, dye color lookup
  block/VendingMachineBlock.java        placement, breaking, protection, integrity, dyeing
  block/VendingMachineBlockEntity.java  owner (saved + synced)
  platform/fabric/FabricRegistrar.java, platform/neoforge/NeoForgeRegistrar.java
src/main/resources/                     lang, tags, advancement, generated textures/models/blockstate, icon
src/main/resources-1.21.1/              recipe (1.21.1 format), item model with custom_model_data overrides
src/main/resources-26.1/                recipe (26.1 format), item model definition (select on base_color)
src/test/java/diamondvending/art/       NbtWriter, Json, Dye, Structures, Textures, Models, ArtGenerator + tests
src/gametest/java/diamondvending/gametest/
  MachineTests.java                     shared test bodies (+ ALL map)
  fabric/FabricGameTests.java           Fabric entrypoint (annotations differ per version)
  neoforge/NeoForgeGameTests.java       NeoForge 1.21.1 @GameTestHolder
  neoforge/NeoForgeGameTestMod.java     test mod; registers 26.1 test instances
src/gametest/resources/                 test mod metadata (both loaders) + generated test structure
docs/images/machine-colors.png          generated preview of all 16 colors
```

---

### Task 1: GameTest harness (and the generator it needs)

Deliverable: `runGameTestServer` (NeoForge) and `runGametest` (Fabric) run a smoke test on a generated test platform, on all four nodes, locally and in CI.

**Files:**
- Create: `src/test/java/diamondvending/art/NbtWriter.java`, `Structures.java`, `ArtGenerator.java`, `NbtWriterTest.java`, `StructuresTest.java`, `GeneratedFilesTest.java`
- Create: `src/gametest/java/diamondvending/gametest/MachineTests.java`, `fabric/FabricGameTests.java`, `neoforge/NeoForgeGameTests.java`, `neoforge/NeoForgeGameTestMod.java`
- Create: `src/gametest/resources/fabric.mod.json`, `src/gametest/resources/META-INF/neoforge.mods.toml`
- Generate: `src/gametest/resources/data/diamondvending/structure/gametest_platform.nbt`
- Modify: `build.fabric.gradle.kts`, `build.neoforge.gradle.kts`, `.github/workflows/build.yml`

**Interfaces:**
- Produces: `ArtGenerator.generateAll(): List<ArtGenerator.Output>` with `record Output(String path, Kind kind, byte[] bytes)` and `enum Kind { PNG, JSON, NBT }` — paths are relative to the repo root. `ArtGenerator.main(String[] args)` writes them under `args[0]`.
- Produces: Gradle tasks `:<node>:generateArt`, `:<node>:runGameTestServer` (NeoForge nodes), `:<node>:runGametest` (Fabric nodes).
- Produces: `MachineTests.STRUCTURE_NAME = "gametest_platform"`, `STRUCTURE = "diamondvending:gametest_platform"`, `MAX_TICKS = 100`, `ALL: Map<String, Consumer<GameTestHelper>>`. Every later test is a `public static void name(GameTestHelper)` in `MachineTests`, an entry in `ALL`, a method in `FabricGameTests`, and a 1.21.1 method in `NeoForgeGameTests`.

- [ ] **Step 1: Create the branch**

```bash
git -C /c/Users/benet/mcvending checkout -b plan-2/the-machine
```

- [ ] **Step 2: Write the failing generator tests**

`src/test/java/diamondvending/art/NbtWriterTest.java`:

```java
package diamondvending.art;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NbtWriterTest {

    @Test
    void writesACompoundWithAnInt() {
        Map<String, Object> root = NbtWriter.compound();
        root.put("a", 1);
        assertArrayEquals(new byte[] {10, 0, 0, 3, 0, 1, 'a', 0, 0, 0, 1, 0}, NbtWriter.toBytes(root));
    }

    @Test
    void writesListsOfIntsAndEmptyLists() {
        Map<String, Object> root = NbtWriter.compound();
        root.put("l", List.of(7));
        root.put("e", List.of());
        assertArrayEquals(new byte[] {
                10, 0, 0,
                9, 0, 1, 'l', 3, 0, 0, 0, 1, 0, 0, 0, 7,
                9, 0, 1, 'e', 0, 0, 0, 0, 0,
                0}, NbtWriter.toBytes(root));
    }

    @Test
    void writesStringsAndNestedCompounds() {
        Map<String, Object> inner = NbtWriter.compound();
        inner.put("s", "hi");
        Map<String, Object> root = NbtWriter.compound();
        root.put("c", inner);
        assertArrayEquals(new byte[] {10, 0, 0, 10, 0, 1, 'c', 8, 0, 1, 's', 0, 2, 'h', 'i', 0, 0}, NbtWriter.toBytes(root));
    }

    @Test
    void rejectsUnsupportedValues() {
        Map<String, Object> root = NbtWriter.compound();
        root.put("d", 1.5);
        assertThrows(IllegalArgumentException.class, () -> NbtWriter.toBytes(root));
    }
}
```

`src/test/java/diamondvending/art/StructuresTest.java`:

```java
package diamondvending.art;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuresTest {

    @Test
    @SuppressWarnings("unchecked")
    void platformIsA6x4x6BoxWithAFloor() {
        Map<String, Object> platform = Structures.gametestPlatform();
        assertEquals(List.of(6, 4, 6), platform.get("size"));
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) platform.get("blocks");
        assertEquals(6 * 4 * 6, blocks.size());
        for (Map<String, Object> block : blocks) {
            List<Integer> pos = (List<Integer>) block.get("pos");
            assertEquals(pos.get(1) == 0 ? 0 : 1, block.get("state"), "floor at y=0, air above: " + pos);
        }
        List<Map<String, Object>> palette = (List<Map<String, Object>>) platform.get("palette");
        assertEquals("minecraft:polished_andesite", palette.get(0).get("Name"));
        assertEquals("minecraft:air", palette.get(1).get("Name"));
    }
}
```

`src/test/java/diamondvending/art/GeneratedFilesTest.java`:

```java
package diamondvending.art;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fails when a generated file is missing or no longer matches the generator (i.e. someone edited it by hand or changed MachineLayout). */
class GeneratedFilesTest {
    private static final Path ROOT = Path.of(System.getProperty("diamondvending.root", "../.."));

    @Test
    void committedFilesMatchTheGenerator() throws IOException {
        List<String> stale = new ArrayList<>();
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) {
            Path file = ROOT.resolve(output.path());
            if (!Files.exists(file) || !sameContent(output, Files.readAllBytes(file))) {
                stale.add(output.path());
            }
        }
        assertTrue(stale.isEmpty(), "Generated files are missing or stale. Run `./gradlew :26.1-neoforge:generateArt` and commit:\n"
                + String.join("\n", stale));
    }

    private static boolean sameContent(ArtGenerator.Output output, byte[] actual) throws IOException {
        return switch (output.kind()) {
            case JSON -> new String(output.bytes(), UTF_8).equals(new String(actual, UTF_8).replace("\r\n", "\n"));
            case NBT -> Arrays.equals(gunzip(output.bytes()), gunzip(actual));
            case PNG -> samePixels(output.bytes(), actual);
        };
    }

    private static byte[] gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return in.readAllBytes();
        }
    }

    private static boolean samePixels(byte[] expected, byte[] actual) throws IOException {
        BufferedImage a = ImageIO.read(new ByteArrayInputStream(expected));
        BufferedImage b = ImageIO.read(new ByteArrayInputStream(actual));
        if (b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
            }
        }
        return true;
    }
}
```

- [ ] **Step 3: Run them to verify they fail**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: FAIL — compilation errors: `cannot find symbol: class NbtWriter`, `Structures`, `ArtGenerator`.

- [ ] **Step 4: Implement the generator pieces**

`src/test/java/diamondvending/art/NbtWriter.java`:

```java
package diamondvending.art;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/** Just enough of Minecraft's NBT format to write structure files: compounds, lists, ints and strings. */
final class NbtWriter {
    private static final byte TAG_END = 0;
    private static final byte TAG_INT = 3;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;

    private NbtWriter() {}

    /** An insertion-ordered compound, so output bytes are deterministic. */
    static Map<String, Object> compound() {
        return new LinkedHashMap<>();
    }

    /** Uncompressed NBT for a root compound with an empty name. */
    static byte[] toBytes(Map<String, Object> root) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");
            writeCompoundBody(out, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Gzip-compressed NBT — the format of {@code .nbt} structure files. */
    static byte[] toGzip(Map<String, Object> root) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(toBytes(root));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void writeCompoundBody(DataOutputStream out, Map<String, Object> compound) throws IOException {
        for (Map.Entry<String, Object> entry : compound.entrySet()) {
            out.writeByte(tagOf(entry.getValue()));
            out.writeUTF(entry.getKey());
            writePayload(out, entry.getValue());
        }
        out.writeByte(TAG_END);
    }

    @SuppressWarnings("unchecked")
    private static void writePayload(DataOutputStream out, Object value) throws IOException {
        switch (value) {
            case Integer i -> out.writeInt(i);
            case String s -> out.writeUTF(s);
            case List<?> list -> {
                out.writeByte(list.isEmpty() ? TAG_END : tagOf(list.getFirst()));
                out.writeInt(list.size());
                for (Object element : list) writePayload(out, element);
            }
            case Map<?, ?> map -> writeCompoundBody(out, (Map<String, Object>) map);
            default -> throw new IllegalArgumentException("unsupported NBT value: " + value);
        }
    }

    private static byte tagOf(Object value) {
        return switch (value) {
            case Integer i -> TAG_INT;
            case String s -> TAG_STRING;
            case List<?> l -> TAG_LIST;
            case Map<?, ?> m -> TAG_COMPOUND;
            default -> throw new IllegalArgumentException("unsupported NBT value: " + value);
        };
    }
}
```

`src/test/java/diamondvending/art/Structures.java`:

```java
package diamondvending.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Structure templates used by the GameTests. */
final class Structures {
    /** Minecraft 1.21.1's data version; newer versions upgrade the structure when loading it. */
    private static final int DATA_VERSION_1_21_1 = 3955;

    private Structures() {}

    /** The GameTest stage: a 6×4×6 box with a polished andesite floor at y = 0 and air above. */
    static Map<String, Object> gametestPlatform() {
        int sizeX = 6;
        int sizeY = 4;
        int sizeZ = 6;
        List<Object> blocks = new ArrayList<>();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    Map<String, Object> block = NbtWriter.compound();
                    block.put("pos", List.of(x, y, z));
                    block.put("state", y == 0 ? 0 : 1);
                    blocks.add(block);
                }
            }
        }
        Map<String, Object> floor = NbtWriter.compound();
        floor.put("Name", "minecraft:polished_andesite");
        Map<String, Object> air = NbtWriter.compound();
        air.put("Name", "minecraft:air");

        Map<String, Object> root = NbtWriter.compound();
        root.put("DataVersion", DATA_VERSION_1_21_1);
        root.put("size", List.of(sizeX, sizeY, sizeZ));
        root.put("palette", List.of(floor, air));
        root.put("blocks", blocks);
        root.put("entities", List.of());
        return root;
    }
}
```

`src/test/java/diamondvending/art/ArtGenerator.java`:

```java
package diamondvending.art;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates every derived file (textures, models, test structures) from the machine's layout.
 * Run with {@code ./gradlew :26.1-neoforge:generateArt}; {@link GeneratedFilesTest} fails if the committed files drift.
 */
public final class ArtGenerator {
    public enum Kind { PNG, JSON, NBT }

    /** One generated file: its path relative to the repository root, and its content. */
    public record Output(String path, Kind kind, byte[] bytes) {}

    private ArtGenerator() {}

    public static List<Output> generateAll() {
        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output("src/gametest/resources/data/diamondvending/structure/gametest_platform.nbt",
                Kind.NBT, NbtWriter.toGzip(Structures.gametestPlatform())));
        return outputs;
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        List<Output> outputs = generateAll();
        for (Output output : outputs) {
            Path file = root.resolve(output.path());
            Files.createDirectories(file.getParent());
            Files.write(file, output.bytes());
        }
        System.out.println("Wrote " + outputs.size() + " generated files under " + root.toAbsolutePath());
    }
}
```

- [ ] **Step 5: Wire the generator into both build scripts**

In **both** `build.fabric.gradle.kts` and `build.neoforge.gradle.kts`, replace the `test { useJUnitPlatform() }` block inside `tasks { ... }` with:

```kotlin
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
```

- [ ] **Step 6: Generate the platform and watch the tests pass**

```bash
./gradlew :26.1-neoforge:generateArt
```
Expected: `Wrote 1 generated files under ...mcvending`, and the file `src/gametest/resources/data/diamondvending/structure/gametest_platform.nbt` exists.
```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: `BUILD SUCCESSFUL`; result XMLs for `NbtWriterTest` (4 tests), `StructuresTest` (1), `GeneratedFilesTest` (1) with 0 failures, plus Plan 1's 69 tests.

- [ ] **Step 7: Write the shared smoke test and the loader adapters**

`src/gametest/java/diamondvending/gametest/MachineTests.java`:

```java
package diamondvending.gametest;

import diamondvending.DiamondVending;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.function.Consumer;

/**
 * The in-game tests, shared by every loader and version. Each test also needs a method in
 * {@code fabric/FabricGameTests} and (for 1.21.1) {@code neoforge/NeoForgeGameTests}; NeoForge 26.1 registers {@link #ALL}.
 */
public final class MachineTests {
    public static final String STRUCTURE_NAME = "gametest_platform";
    public static final String STRUCTURE = DiamondVending.MOD_ID + ":" + STRUCTURE_NAME;
    public static final int MAX_TICKS = 100;

    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("platform_is_ready", MachineTests::platformIsReady));

    private MachineTests() {}

    /** Smoke test: the generated platform loaded — floor at y = 0, air above. */
    public static void platformIsReady(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.POLISHED_ANDESITE, new BlockPos(0, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 3));
        helper.succeed();
    }
}
```

`src/gametest/java/diamondvending/gametest/fabric/FabricGameTests.java`:

```java
package diamondvending.gametest.fabric;

import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/**
 * Fabric's game-test entrypoint (declared in the test mod's fabric.mod.json). One public, non-static method per
 * test in {@link MachineTests}. 26.1 uses Fabric's {@code @GameTest(structure)}; 1.21.1 uses vanilla's {@code @GameTest(template)}.
 */
public final class FabricGameTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void platformIsReady(GameTestHelper helper) {
        MachineTests.platformIsReady(helper);
    }
}
```

`src/gametest/java/diamondvending/gametest/neoforge/NeoForgeGameTests.java`:

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * NeoForge 1.21.1 finds these through {@code @GameTestHolder}; templates resolve to {@code diamondvending:<name>}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeGameTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void platformIsReady(GameTestHelper helper) {
        MachineTests.platformIsReady(helper);
    }
    *///?}
}
```

`src/gametest/java/diamondvending/gametest/neoforge/NeoForgeGameTestMod.java`:

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//? if >=26.1 {
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

/** The test-only mod. On 26.1 it registers each {@link MachineTests#ALL} entry as a test function and a test instance. */
@Mod("diamondvending_gametest")
public final class NeoForgeGameTestMod {
    public NeoForgeGameTestMod(IEventBus modBus) {
        //? if >=26.1 {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, DiamondVending.MOD_ID);
        MachineTests.ALL.forEach((name, test) -> functions.register(name, () -> test));
        functions.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, event -> {
            Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(DiamondVending.id("default"));
            MachineTests.ALL.keySet().forEach(name -> event.registerTest(DiamondVending.id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, DiamondVending.id(name)),
                    new TestData<>(environment, DiamondVending.id(MachineTests.STRUCTURE_NAME), MachineTests.MAX_TICKS, 0, true))));
        });
        //?}
    }
}
```

`src/gametest/resources/fabric.mod.json`:

```json
{
  "schemaVersion": 1,
  "id": "diamondvending_gametest",
  "version": "0.0.0",
  "name": "Diamond Vending game tests",
  "environment": "*",
  "entrypoints": {
    "fabric-gametest": ["diamondvending.gametest.fabric.FabricGameTests"]
  },
  "depends": {
    "diamondvending": "*"
  }
}
```

`src/gametest/resources/META-INF/neoforge.mods.toml`:

```toml
modLoader = "javafml"
loaderVersion = "[1,)"
license = "MIT"

[[mods]]
modId = "diamondvending_gametest"
version = "0.0.0"
displayName = "Diamond Vending game tests"

[[dependencies.diamondvending_gametest]]
modId = "diamondvending"
type = "required"
versionRange = "[0,)"
ordering = "AFTER"
side = "BOTH"
```

- [ ] **Step 8: Run a game test to verify it fails (no harness yet)**

```bash
./gradlew :26.1-neoforge:runGameTestServer
```
Expected: FAIL — `Task 'runGameTestServer' not found in project ':26.1-neoforge'`.

- [ ] **Step 9: Add the gametest source set, test mod and run to both build scripts**

In `build.neoforge.gradle.kts`, add after the `sourceSets.main { ... }` block:

```kotlin
// Game tests live in their own source set and test mod, so no test code ships in the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
    java.exclude("diamondvending/gametest/fabric/**")
}
```

and change the `neoForge { ... }` block to:

```kotlin
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
```

In `build.fabric.gradle.kts`, add after the `sourceSets.main { ... }` block:

```kotlin
// Game tests live in their own source set and test mod, so no test code ships in the release jar.
val gametest: SourceSet = sourceSets.create("gametest") {
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
    java.exclude("diamondvending/gametest/neoforge/**")
}
```

and add inside the existing `loom { ... }` block, after `runConfigs.all { ... }`:

```kotlin
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
            name("Game Test")
            source(gametest)
            property("fabric-api.gametest")
            property("fabric-api.gametest.report-file", file("build/gametest/report.xml").absolutePath)
            runDirectory = file("build/gametest")
            ideConfigGenerated(false)
        }
    }
```

- [ ] **Step 10: Run the game tests on every node (one at a time)**

```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
Expected: `BUILD SUCCESSFUL`, log contains `All` … `required tests passed`. Then, one call each:
```bash
./gradlew :26.1-fabric:runGametest --stacktrace
```
```bash
./gradlew :1.21.1-neoforge:runGameTestServer --stacktrace
```
```bash
./gradlew :1.21.1-fabric:runGametest --stacktrace
```
Expected: `BUILD SUCCESSFUL` each, with the `platform_is_ready`/`platformIsReady` test passing.

Confirm Stonecutter processed the new source set for a non-active node:
```bash
ls versions/1.21.1-fabric/build/generated/stonecutter
```
Expected: `gametest  main  test`.

Confirm no test code ships:
```bash
unzip -l versions/26.1-fabric/build/libs/diamondvending-fabric-0.1.0+26.1.2.jar
```
Expected: no `diamondvending/gametest/` entries and no `gametest_platform.nbt`.

- [ ] **Step 11: Add game tests to CI**

In `.github/workflows/build.yml`, add after the `Build and test` step:

```yaml
      - name: Game tests
        run: ./gradlew :${{ matrix.target }}:${{ endsWith(matrix.target, '-fabric') && 'runGametest' || 'runGameTestServer' }} --stacktrace

      - uses: actions/upload-artifact@v7
        if: failure()
        with:
          name: gametest-${{ matrix.target }}
          path: versions/${{ matrix.target }}/build/gametest/
          if-no-files-found: ignore
```

- [ ] **Step 12: Check Stonecutter comment state, then commit**

```bash
./gradlew "Refresh active project"
```
```bash
git -C /c/Users/benet/mcvending status --short
```
Expected: only the files this task created/changed (Refresh may normalize comment formatting in the new `.java` files — keep its output).
```bash
git -C /c/Users/benet/mcvending add build.fabric.gradle.kts build.neoforge.gradle.kts .github/workflows/build.yml src/test/java/diamondvending/art src/gametest
```
```bash
git -C /c/Users/benet/mcvending commit -m "test: GameTest harness with a generated test platform on all four targets"
```

---

### Task 2: Registration and the multiblock machine

Deliverable: the machine can be placed, owned, protected, broken and cleaned up — proven by GameTests on all four nodes.

**Files:**
- Create: `src/main/java/diamondvending/Messages.java`, `registry/Registrar.java`, `registry/RegistryCompat.java`, `registry/ModContent.java`, `block/MachineSide.java`, `block/MachinePart.java`, `block/MachineAccess.java`, `block/MachineItems.java`, `block/VendingMachineBlock.java`, `block/VendingMachineBlockEntity.java`, `platform/fabric/FabricRegistrar.java`, `platform/neoforge/NeoForgeRegistrar.java`
- Modify: `platform/fabric/DiamondVendingFabric.java`, `platform/neoforge/DiamondVendingNeoForge.java`
- Create: `src/main/resources/assets/diamondvending/lang/en_us.json`, `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`, `.../wither_immune.json`, `.../dragon_immune.json`
- Modify: `src/main/resources/fabric.mod.json`, `src/main/resources/META-INF/neoforge.mods.toml`, both build scripts (`processResources`)
- Modify: `src/gametest/java/diamondvending/gametest/MachineTests.java`, `fabric/FabricGameTests.java`, `neoforge/NeoForgeGameTests.java`

**Interfaces:**
- Consumes: `core.Facing` (`rightDx()`, `rightDz()`) from Plan 1; `MachineTests` harness from Task 1.
- Produces:
  - `ModContent.VENDING_MACHINE: Supplier<VendingMachineBlock>`, `ModContent.VENDING_MACHINE_ITEM: Supplier<BlockItem>`, `ModContent.VENDING_MACHINE_BLOCK_ENTITY: Supplier<BlockEntityType<VendingMachineBlockEntity>>`, `ModContent.register(Registrar)`.
  - `VendingMachineBlock.FACING: Property<Direction>`, `HALF: EnumProperty<DoubleBlockHalf>`, `SIDE: EnumProperty<MachineSide>`, `COLOR: EnumProperty<DyeColor>`, `static UUID ownerOf(BlockGetter, BlockPos, BlockState)` (null = no owner).
  - `MachinePart` enum `LOWER_LEFT, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT` with `of(BlockState)`, `applyTo(BlockState)`, `posFrom(BlockPos master, Direction facing)`, `masterFrom(BlockPos pos, Direction facing)`, `static masterOf(BlockPos, BlockState)`.
  - `VendingMachineBlockEntity.getOwner(): UUID` (nullable), `getOwnerName(): String`, `setOwner(UUID, String)`.
  - `MachineAccess.isAdmin(Player)`, `canManage(Player, UUID owner)`.
  - `MachineItems.forColor(DyeColor): ItemStack`, `colorOf(ItemStack): DyeColor`.
  - `Messages.OWNER_ONLY = "message.diamondvending.owner_only"`, `Messages.actionBar(Player, Component)`.

- [ ] **Step 1: Write the failing GameTests**

Replace `src/gametest/java/diamondvending/gametest/MachineTests.java` with:

```java
package diamondvending.gametest;

import diamondvending.DiamondVending;
import diamondvending.block.MachineItems;
import diamondvending.block.MachinePart;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The in-game tests, shared by every loader and version. Each test also needs a method in
 * {@code fabric/FabricGameTests} and (for 1.21.1) {@code neoforge/NeoForgeGameTests}; NeoForge 26.1 registers {@link #ALL}.
 *
 * <p>Mock players have yaw 0 (they face south), so a machine they place faces north and its right-hand column is at x − 1.
 */
public final class MachineTests {
    public static final String STRUCTURE_NAME = "gametest_platform";
    public static final String STRUCTURE = DiamondVending.MOD_ID + ":" + STRUCTURE_NAME;
    public static final int MAX_TICKS = 100;

    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("platform_is_ready", MachineTests::platformIsReady),
            Map.entry("places_all_four_parts", MachineTests::placesAllFourParts),
            Map.entry("placement_needs_room", MachineTests::placementNeedsRoom),
            Map.entry("places_in_the_items_color", MachineTests::placesInTheItemsColor),
            Map.entry("works_in_every_facing", MachineTests::worksInEveryFacing),
            Map.entry("breaking_any_part_removes_the_machine_and_drops_it", MachineTests::breakingAnyPartRemovesTheMachineAndDropsIt),
            Map.entry("creative_breaking_drops_nothing", MachineTests::creativeBreakingDropsNothing),
            Map.entry("only_owners_and_admins_can_mine_it", MachineTests::onlyOwnersAndAdminsCanMineIt),
            Map.entry("ownerless_machines_are_admin_only", MachineTests::ownerlessMachinesAreAdminOnly),
            Map.entry("a_broken_machine_cleans_itself_up", MachineTests::aBrokenMachineCleansItselfUp),
            Map.entry("neighbouring_machines_stay_separate", MachineTests::neighbouringMachinesStaySeparate));

    static final BlockPos FLOOR = new BlockPos(3, 0, 3);
    static final BlockPos MASTER = FLOOR.above();
    static final BlockPos LOWER_RIGHT = MASTER.west();
    static final BlockPos UPPER_LEFT = MASTER.above();
    static final BlockPos UPPER_RIGHT = LOWER_RIGHT.above();

    private MachineTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    static ItemStack machineItem(int count) {
        return new ItemStack(ModContent.VENDING_MACHINE_ITEM.get(), count);
    }

    /** Uses the stack on the top of {@code floor}, as a player would. */
    static void placeOn(GameTestHelper helper, Player player, ItemStack stack, BlockPos floor) {
        helper.placeAt(player, stack, floor, Direction.UP);
    }

    /** Breaks a block the way the server does for a player: playerWillDestroy, then remove. */
    static void breakAsPlayer(GameTestHelper helper, BlockPos relative, Player player) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        BlockState state = level.getBlockState(pos);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        level.removeBlock(pos, false);
    }

    static List<ItemEntity> droppedMachines(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(MASTER)).inflate(3);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.getItem().is(ModContent.VENDING_MACHINE_ITEM.get()));
    }

    static VendingMachineBlockEntity machineAt(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBlockEntity(helper.absolutePos(relative)) instanceof VendingMachineBlockEntity machine ? machine : null;
    }

    static void assertPart(GameTestHelper helper, BlockPos relative, MachinePart part, Direction facing, DyeColor color) {
        BlockState state = helper.getBlockState(relative);
        helper.assertTrue(state.is(ModContent.VENDING_MACHINE.get()), "expected a vending machine at " + relative + " but found " + state);
        helper.assertTrue(MachinePart.of(state) == part, "expected " + part + " at " + relative + " but found " + MachinePart.of(state));
        helper.assertTrue(state.getValue(VendingMachineBlock.FACING) == facing, "expected facing " + facing + " at " + relative);
        helper.assertTrue(state.getValue(VendingMachineBlock.COLOR) == color, "expected " + color + " at " + relative);
    }

    static void assertWholeMachine(GameTestHelper helper, DyeColor color) {
        assertPart(helper, MASTER, MachinePart.LOWER_LEFT, Direction.NORTH, color);
        assertPart(helper, LOWER_RIGHT, MachinePart.LOWER_RIGHT, Direction.NORTH, color);
        assertPart(helper, UPPER_LEFT, MachinePart.UPPER_LEFT, Direction.NORTH, color);
        assertPart(helper, UPPER_RIGHT, MachinePart.UPPER_RIGHT, Direction.NORTH, color);
    }

    static void assertAir(GameTestHelper helper, BlockPos... relatives) {
        for (BlockPos relative : relatives) {
            BlockState state = helper.getBlockState(relative);
            helper.assertTrue(state.isAir(), "expected air at " + relative + " but found " + state);
        }
    }

    // ---- tests ---------------------------------------------------------------------------------------------------

    /** Smoke test: the generated platform loaded — floor at y = 0, air above. */
    public static void platformIsReady(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.POLISHED_ANDESITE, new BlockPos(0, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 3));
        helper.succeed();
    }

    public static void placesAllFourParts(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = machineItem(2);
        placeOn(helper, owner, stack, FLOOR);
        assertWholeMachine(helper, DyeColor.RED);
        helper.assertTrue(stack.getCount() == 1, "placing should use one item, stack is now " + stack.getCount());
        VendingMachineBlockEntity machine = machineAt(helper, MASTER);
        helper.assertTrue(machine != null && owner.getUUID().equals(machine.getOwner()), "the placer should own the machine");
        helper.assertTrue(machineAt(helper, UPPER_RIGHT) == null, "only the master part has a block entity");
        helper.succeed();
    }

    public static void placementNeedsRoom(GameTestHelper helper) {
        helper.setBlock(UPPER_RIGHT, Blocks.STONE);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = machineItem(1);
        placeOn(helper, owner, stack, FLOOR);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT);
        helper.assertTrue(stack.getCount() == 1, "a blocked placement must not use the item");
        helper.succeed();
    }

    public static void placesInTheItemsColor(GameTestHelper helper) {
        placeOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), MachineItems.forColor(DyeColor.BLUE), FLOOR);
        assertWholeMachine(helper, DyeColor.BLUE);
        helper.succeed();
    }

    /** Yaw → the player's facing → the machine faces back at them; its right column is on the viewer's right. */
    public static void worksInEveryFacing(GameTestHelper helper) {
        float[] yaws = {0, 90, 180, 270};
        Direction[] machineFacings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        BlockPos[] rightOffsets = {new BlockPos(-1, 0, 0), new BlockPos(0, 0, -1), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1)};
        for (int i = 0; i < yaws.length; i++) {
            Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
            owner.setYRot(yaws[i]);
            placeOn(helper, owner, machineItem(1), FLOOR);
            BlockPos right = MASTER.offset(rightOffsets[i]);
            assertPart(helper, MASTER, MachinePart.LOWER_LEFT, machineFacings[i], DyeColor.RED);
            assertPart(helper, right, MachinePart.LOWER_RIGHT, machineFacings[i], DyeColor.RED);
            assertPart(helper, MASTER.above(), MachinePart.UPPER_LEFT, machineFacings[i], DyeColor.RED);
            assertPart(helper, right.above(), MachinePart.UPPER_RIGHT, machineFacings[i], DyeColor.RED);
            breakAsPlayer(helper, right.above(), owner);
            assertAir(helper, MASTER, right, MASTER.above(), right.above());
        }
        helper.succeed();
    }

    public static void breakingAnyPartRemovesTheMachineAndDropsIt(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, MachineItems.forColor(DyeColor.LIME), FLOOR);
        breakAsPlayer(helper, UPPER_RIGHT, owner);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        List<ItemEntity> drops = droppedMachines(helper);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                "expected exactly one machine item, found " + drops.size());
        helper.assertTrue(MachineItems.colorOf(drops.getFirst().getItem()) == DyeColor.LIME, "the dropped machine should stay lime");
        helper.succeed();
    }

    public static void creativeBreakingDropsNothing(GameTestHelper helper) {
        Player admin = helper.makeMockPlayer(GameType.CREATIVE);
        placeOn(helper, admin, machineItem(1), FLOOR);
        breakAsPlayer(helper, MASTER, admin);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        helper.assertTrue(droppedMachines(helper).isEmpty(), "creative breaking must not drop the machine");
        helper.succeed();
    }

    public static void onlyOwnersAndAdminsCanMineIt(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        Player admin = helper.makeMockPlayer(GameType.CREATIVE);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(UPPER_LEFT);
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.getDestroyProgress(owner, level, pos) > 0, "the owner should be able to mine the machine");
        helper.assertTrue(state.getDestroyProgress(stranger, level, pos) == 0, "a stranger must not be able to mine the machine");
        helper.assertTrue(state.getDestroyProgress(admin, level, pos) > 0, "an admin should be able to mine the machine");
        helper.succeed();
    }

    /** A machine built without a player (e.g. /setblock) has no owner: only admins may mine it. */
    public static void ownerlessMachinesAreAdminOnly(GameTestHelper helper) {
        BlockState master = ModContent.VENDING_MACHINE.get().defaultBlockState();
        for (MachinePart part : MachinePart.values()) {
            helper.setBlock(part.posFrom(MASTER, Direction.NORTH), part.applyTo(master));
        }
        assertWholeMachine(helper, DyeColor.RED);
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(MASTER);
        BlockState state = level.getBlockState(pos);
        helper.assertTrue(state.getDestroyProgress(helper.makeMockPlayer(GameType.SURVIVAL), level, pos) == 0,
                "nobody but an admin may mine an ownerless machine");
        helper.assertTrue(state.getDestroyProgress(helper.makeMockPlayer(GameType.CREATIVE), level, pos) > 0,
                "an admin may mine an ownerless machine");
        helper.succeed();
    }

    /** Removing one part without a player (a world edit) must not leave ghost parts behind. */
    public static void aBrokenMachineCleansItselfUp(GameTestHelper helper) {
        placeOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), machineItem(1), FLOOR);
        helper.setBlock(MASTER, Blocks.AIR);
        assertAir(helper, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        helper.succeed();
    }

    public static void neighbouringMachinesStaySeparate(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);                // occupies x = 3 and 2
        BlockPos otherFloor = FLOOR.east(2);
        placeOn(helper, owner, machineItem(1), otherFloor);           // occupies x = 5 and 4
        BlockPos otherMaster = otherFloor.above();
        breakAsPlayer(helper, MASTER, owner);
        assertAir(helper, MASTER, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT);
        assertPart(helper, otherMaster, MachinePart.LOWER_LEFT, Direction.NORTH, DyeColor.RED);
        assertPart(helper, otherMaster.west(), MachinePart.LOWER_RIGHT, Direction.NORTH, DyeColor.RED);
        assertPart(helper, otherMaster.above(), MachinePart.UPPER_LEFT, Direction.NORTH, DyeColor.RED);
        assertPart(helper, otherMaster.west().above(), MachinePart.UPPER_RIGHT, Direction.NORTH, DyeColor.RED);
        helper.succeed();
    }
}
```

Replace `src/gametest/java/diamondvending/gametest/fabric/FabricGameTests.java` with:

```java
package diamondvending.gametest.fabric;

import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/**
 * Fabric's game-test entrypoint (declared in the test mod's fabric.mod.json). One public, non-static method per
 * test in {@link MachineTests}. 26.1 uses Fabric's {@code @GameTest(structure)}; 1.21.1 uses vanilla's {@code @GameTest(template)}.
 */
public final class FabricGameTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void platformIsReady(GameTestHelper helper) {
        MachineTests.platformIsReady(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void placesAllFourParts(GameTestHelper helper) {
        MachineTests.placesAllFourParts(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void placementNeedsRoom(GameTestHelper helper) {
        MachineTests.placementNeedsRoom(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void placesInTheItemsColor(GameTestHelper helper) {
        MachineTests.placesInTheItemsColor(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void worksInEveryFacing(GameTestHelper helper) {
        MachineTests.worksInEveryFacing(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void breakingAnyPartRemovesTheMachineAndDropsIt(GameTestHelper helper) {
        MachineTests.breakingAnyPartRemovesTheMachineAndDropsIt(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creativeBreakingDropsNothing(GameTestHelper helper) {
        MachineTests.creativeBreakingDropsNothing(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void onlyOwnersAndAdminsCanMineIt(GameTestHelper helper) {
        MachineTests.onlyOwnersAndAdminsCanMineIt(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void ownerlessMachinesAreAdminOnly(GameTestHelper helper) {
        MachineTests.ownerlessMachinesAreAdminOnly(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aBrokenMachineCleansItselfUp(GameTestHelper helper) {
        MachineTests.aBrokenMachineCleansItselfUp(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void neighbouringMachinesStaySeparate(GameTestHelper helper) {
        MachineTests.neighbouringMachinesStaySeparate(helper);
    }
}
```

Replace `src/gametest/java/diamondvending/gametest/neoforge/NeoForgeGameTests.java` with:

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * NeoForge 1.21.1 finds these through {@code @GameTestHolder}; templates resolve to {@code diamondvending:<name>}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeGameTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void platformIsReady(GameTestHelper helper) {
        MachineTests.platformIsReady(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placesAllFourParts(GameTestHelper helper) {
        MachineTests.placesAllFourParts(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placementNeedsRoom(GameTestHelper helper) {
        MachineTests.placementNeedsRoom(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placesInTheItemsColor(GameTestHelper helper) {
        MachineTests.placesInTheItemsColor(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void worksInEveryFacing(GameTestHelper helper) {
        MachineTests.worksInEveryFacing(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void breakingAnyPartRemovesTheMachineAndDropsIt(GameTestHelper helper) {
        MachineTests.breakingAnyPartRemovesTheMachineAndDropsIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creativeBreakingDropsNothing(GameTestHelper helper) {
        MachineTests.creativeBreakingDropsNothing(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void onlyOwnersAndAdminsCanMineIt(GameTestHelper helper) {
        MachineTests.onlyOwnersAndAdminsCanMineIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ownerlessMachinesAreAdminOnly(GameTestHelper helper) {
        MachineTests.ownerlessMachinesAreAdminOnly(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aBrokenMachineCleansItselfUp(GameTestHelper helper) {
        MachineTests.aBrokenMachineCleansItselfUp(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void neighbouringMachinesStaySeparate(GameTestHelper helper) {
        MachineTests.neighbouringMachinesStaySeparate(helper);
    }
    *///?}
}
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :26.1-neoforge:compileGametestJava --stacktrace
```
Expected: FAIL — `package diamondvending.block does not exist`, `package diamondvending.registry does not exist`.

- [ ] **Step 3: Registration layer**

`src/main/java/diamondvending/registry/Registrar.java`:

```java
package diamondvending.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

/** Registers content with the running loader. Each loader provides one implementation. */
public interface Registrar {
    <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties);

    <I extends Item> Supplier<I> item(String name, Function<Item.Properties, I> factory, Item.Properties properties);

    <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory,
                                                                     Supplier<? extends Block> block);
}
```

`src/main/java/diamondvending/registry/RegistryCompat.java`:

```java
package diamondvending.registry;

import diamondvending.DiamondVending;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registration details that differ between Minecraft versions. */
public final class RegistryCompat {
    private RegistryCompat() {}

    /** 26.1 requires blocks to know their id when constructed. */
    public static BlockBehaviour.Properties blockProperties(BlockBehaviour.Properties properties, String name) {
        //? if >=26.1 {
        return properties.setId(ResourceKey.create(Registries.BLOCK, DiamondVending.id(name)));
        //?} else {
        /*return properties;
        *///?}
    }

    /** 26.1 requires items to know their id when constructed. */
    public static Item.Properties itemProperties(Item.Properties properties, String name) {
        //? if >=26.1 {
        return properties.setId(ResourceKey.create(Registries.ITEM, DiamondVending.id(name)));
        //?} else {
        /*return properties;
        *///?}
    }

    /** Properties for a block's item: its name comes from the block's translation key. */
    public static Item.Properties blockItemProperties() {
        //? if >=26.1 {
        return new Item.Properties().useBlockDescriptionPrefix();
        //?} else {
        /*return new Item.Properties();
        *///?}
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        //? if >=26.1 {
        return new BlockEntityType<>(factory, block);
        //?} else {
        /*return BlockEntityType.Builder.of(factory, block).build(null);
        *///?}
    }
}
```

`src/main/java/diamondvending/registry/ModContent.java`:

```java
package diamondvending.registry;

import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/** Everything this mod registers. Filled in once by {@link #register}, called from each loader's entrypoint. */
public final class ModContent {
    public static Supplier<VendingMachineBlock> VENDING_MACHINE;
    public static Supplier<BlockItem> VENDING_MACHINE_ITEM;
    public static Supplier<BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE_BLOCK_ENTITY;

    private ModContent() {}

    public static void register(Registrar registrar) {
        VENDING_MACHINE = registrar.block("vending_machine", VendingMachineBlock::new, VendingMachineBlock.properties());
        VENDING_MACHINE_ITEM = registrar.item("vending_machine",
                properties -> new BlockItem(VENDING_MACHINE.get(), properties), RegistryCompat.blockItemProperties());
        VENDING_MACHINE_BLOCK_ENTITY = registrar.blockEntity("vending_machine", VendingMachineBlockEntity::new, VENDING_MACHINE);
    }
}
```

`src/main/java/diamondvending/platform/fabric/FabricRegistrar.java`:

```java
package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.registry.Registrar;
import diamondvending.registry.RegistryCompat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;
import java.util.function.Supplier;

/** Fabric registers immediately during mod initialization. */
final class FabricRegistrar implements Registrar {
    @Override
    public <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        B block = Registry.register(BuiltInRegistries.BLOCK, DiamondVending.id(name), factory.apply(RegistryCompat.blockProperties(properties, name)));
        return () -> block;
    }

    @Override
    public <I extends Item> Supplier<I> item(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        I item = Registry.register(BuiltInRegistries.ITEM, DiamondVending.id(name), factory.apply(RegistryCompat.itemProperties(properties, name)));
        return () -> item;
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory,
                                                                            Supplier<? extends Block> block) {
        BlockEntityType<T> type = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DiamondVending.id(name),
                RegistryCompat.blockEntityType(factory, block.get()));
        return () -> type;
    }
}
```

`src/main/java/diamondvending/platform/neoforge/NeoForgeRegistrar.java`:

```java
package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.registry.Registrar;
import diamondvending.registry.RegistryCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** NeoForge registers lazily through DeferredRegisters attached to the mod event bus. */
final class NeoForgeRegistrar implements Registrar {
    private final DeferredRegister<Block> blocks = DeferredRegister.create(Registries.BLOCK, DiamondVending.MOD_ID);
    private final DeferredRegister<Item> items = DeferredRegister.create(Registries.ITEM, DiamondVending.MOD_ID);
    private final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DiamondVending.MOD_ID);

    void registerAll(IEventBus modBus) {
        blocks.register(modBus);
        items.register(modBus);
        blockEntities.register(modBus);
    }

    @Override
    public <B extends Block> Supplier<B> block(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
        return blocks.register(name, () -> factory.apply(RegistryCompat.blockProperties(properties, name)));
    }

    @Override
    public <I extends Item> Supplier<I> item(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        return items.register(name, () -> factory.apply(RegistryCompat.itemProperties(properties, name)));
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BlockEntityType.BlockEntitySupplier<T> factory,
                                                                            Supplier<? extends Block> block) {
        return blockEntities.register(name, () -> RegistryCompat.blockEntityType(factory, block.get()));
    }
}
```

Replace `DiamondVendingFabric.java`:

```java
package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.CreativeModeTabs;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
*///?}

/** Fabric entrypoint (declared in fabric.mod.json). Excluded from NeoForge builds. */
public final class DiamondVendingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DiamondVending.init();
        ModContent.register(new FabricRegistrar());
        //? if >=26.1 {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> output.accept(ModContent.VENDING_MACHINE_ITEM.get()));
        //?} else {
        /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(ModContent.VENDING_MACHINE_ITEM.get()));
        *///?}
    }
}
```

Replace `DiamondVendingNeoForge.java`:

```java
package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.registry.ModContent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** NeoForge entrypoint. Excluded from Fabric builds. */
@Mod(DiamondVending.MOD_ID)
public final class DiamondVendingNeoForge {
    public DiamondVendingNeoForge(IEventBus modBus, ModContainer container) {
        DiamondVending.init();
        NeoForgeRegistrar registrar = new NeoForgeRegistrar();
        ModContent.register(registrar);
        registrar.registerAll(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                event.accept(ModContent.VENDING_MACHINE_ITEM.get());
            }
        });
    }
}
```

- [ ] **Step 4: Small helpers**

`src/main/java/diamondvending/Messages.java`:

```java
package diamondvending;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Player-facing messages. Every failure names the reason and who can fix it (spec §3.5). */
public final class Messages {
    public static final String OWNER_ONLY = "message.diamondvending.owner_only";

    private Messages() {}

    /** Shows a short message above the hotbar, only to this player. */
    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }
}
```

`src/main/java/diamondvending/block/MachineSide.java`:

```java
package diamondvending.block;

import net.minecraft.util.StringRepresentable;

/** Which column a part is in, as seen by someone facing the machine's front. */
public enum MachineSide implements StringRepresentable {
    LEFT("left"), RIGHT("right");

    private final String name;

    MachineSide(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
```

`src/main/java/diamondvending/block/MachinePart.java`:

```java
package diamondvending.block;

import diamondvending.core.Facing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** The four block positions of a machine. {@link #LOWER_LEFT} is the master. */
public enum MachinePart {
    LOWER_LEFT(false, false), LOWER_RIGHT(true, false), UPPER_LEFT(false, true), UPPER_RIGHT(true, true);

    private final boolean right;
    private final boolean upper;

    MachinePart(boolean right, boolean upper) {
        this.right = right;
        this.upper = upper;
    }

    public boolean right() {
        return right;
    }

    public boolean upper() {
        return upper;
    }

    public static MachinePart of(BlockState state) {
        boolean right = state.getValue(VendingMachineBlock.SIDE) == MachineSide.RIGHT;
        boolean upper = state.getValue(VendingMachineBlock.HALF) == DoubleBlockHalf.UPPER;
        if (upper) return right ? UPPER_RIGHT : UPPER_LEFT;
        return right ? LOWER_RIGHT : LOWER_LEFT;
    }

    /** This part's state, taking facing and color from any part's state. */
    public BlockState applyTo(BlockState anyPart) {
        return anyPart
                .setValue(VendingMachineBlock.SIDE, right ? MachineSide.RIGHT : MachineSide.LEFT)
                .setValue(VendingMachineBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    /** Where this part sits, given the master's position and the machine's facing. */
    public BlockPos posFrom(BlockPos master, Direction facing) {
        Facing f = Facing.valueOf(facing.name());
        return master.offset(right ? f.rightDx() : 0, upper ? 1 : 0, right ? f.rightDz() : 0);
    }

    /** The master's position, given this part's position and the machine's facing. */
    public BlockPos masterFrom(BlockPos pos, Direction facing) {
        Facing f = Facing.valueOf(facing.name());
        return pos.offset(right ? -f.rightDx() : 0, upper ? -1 : 0, right ? -f.rightDz() : 0);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState state) {
        return of(state).masterFrom(pos, state.getValue(VendingMachineBlock.FACING));
    }
}
```

`src/main/java/diamondvending/block/MachineAccess.java`:

```java
package diamondvending.block;

import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
import net.minecraft.server.permissions.Permissions;
//?}

import java.util.UUID;

/** Who may set up, dye and break a machine (spec §4, §5.3). */
public final class MachineAccess {
    private MachineAccess() {}

    /** Admin = creative mode or permission level 2 (op). */
    public static boolean isAdmin(Player player) {
        if (player.isCreative()) return true;
        //? if >=26.1 {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //?} else {
        /*return player.hasPermissions(2);
        *///?}
    }

    /** Owners and admins. Machines without an owner ({@code owner == null}) are admin-only. */
    public static boolean canManage(Player player, UUID owner) {
        return isAdmin(player) || (owner != null && owner.equals(player.getUUID()));
    }
}
```

`src/main/java/diamondvending/block/MachineItems.java`:

```java
package diamondvending.block;

import diamondvending.registry.ModContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
//? if <26.1 {
/*import net.minecraft.world.item.component.CustomModelData;
*///?}

/** Machine items and their color. */
public final class MachineItems {
    private MachineItems() {}

    /** A machine item that places a machine of this color. */
    public static ItemStack forColor(DyeColor color) {
        ItemStack stack = new ItemStack(ModContent.VENDING_MACHINE_ITEM.get());
        stack.set(DataComponents.BASE_COLOR, color);
        //? if <26.1 {
        /*// 1.21.1 item models can only switch on custom_model_data: dye id + 1
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(color.getId() + 1));
        *///?}
        return stack;
    }

    /** The color a machine item places; items without a color place red machines. */
    public static DyeColor colorOf(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.RED);
    }
}
```

`src/main/resources/assets/diamondvending/lang/en_us.json`:

```json
{
  "block.diamondvending.vending_machine": "Vending Machine",
  "message.diamondvending.owner_only": "Only the owner can do that."
}
```

`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json`, `.../wither_immune.json`, `.../dragon_immune.json` — each:

```json
{
  "replace": false,
  "values": ["diamondvending:vending_machine"]
}
```

- [ ] **Step 5: The block entity**

`src/main/java/diamondvending/block/VendingMachineBlockEntity.java`:

```java
package diamondvending.block;

import diamondvending.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

import java.util.UUID;

/** Data held by the master part. Plan 2: the owner. Synced to clients (for mining progress and, later, the HUD). */
public class VendingMachineBlockEntity extends BlockEntity {
    private UUID owner;
    private String ownerName = "";

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), pos, state);
    }

    /** The owner's UUID, or null for machines placed without a player. */
    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    //? if >=26.1 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("owner_name", ownerName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID("owner", owner);
        tag.putString("owner_name", ownerName);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName = tag.getString("owner_name");
    }
    *///?}

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

- [ ] **Step 6: The block**

`src/main/java/diamondvending/block/VendingMachineBlock.java`:

```java
package diamondvending.block;

import com.mojang.serialization.MapCodec;
import diamondvending.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
//? if >=26.1 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}

import java.util.UUID;

/**
 * The 2×2 vending machine (spec §2). Four block positions share this block; the lower-left part (as seen from the
 * front) is the master and owns the {@link VendingMachineBlockEntity}.
 */
public class VendingMachineBlock extends BaseEntityBlock {
    public static final MapCodec<VendingMachineBlock> CODEC = simpleCodec(VendingMachineBlock::new);
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<MachineSide> SIDE = EnumProperty.create("side", MachineSide.class);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    /** Owners and admins mine it like an iron block. */
    private static final float OWNER_HARDNESS = 5.0F;
    /** Placing/recoloring parts must not trigger shape checks on half-built neighbours. */
    private static final int PLACE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;
    private static final int REMOVE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public VendingMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(SIDE, MachineSide.LEFT)
                .setValue(COLOR, DyeColor.RED));
    }

    /**
     * Spec §2.1: lit, immovable, blast-proof. Hardness −1 keeps drills, quarries and other automation out; owners
     * and admins get their own mining speed from {@link #getDestroyProgress}.
     */
    public static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(COLOR).getMapColor())
                .strength(-1.0F, 1200.0F)
                .sound(SoundType.METAL)
                .lightLevel(state -> 6)
                .pushReaction(PushReaction.BLOCK)
                .noLootTable();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SIDE, COLOR);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return MachinePart.of(state) == MachinePart.LOWER_LEFT ? new VendingMachineBlockEntity(pos, state) : null;
    }

    /** The machine's owner, read from the master part; null if it has none. */
    public static UUID ownerOf(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine ? machine.getOwner() : null;
    }

    // ---- placing -------------------------------------------------------------------------------------------------

    /** The clicked spot becomes the lower-left part; the other three spots must be free and inside the world. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos master = context.getClickedPos();
        for (MachinePart part : MachinePart.values()) {
            if (part == MachinePart.LOWER_LEFT) continue;
            BlockPos pos = part.posFrom(master, facing);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(COLOR, MachineItems.colorOf(context.getItemInHand()));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        for (MachinePart part : MachinePart.values()) {
            if (part != MachinePart.LOWER_LEFT) {
                level.setBlock(part.posFrom(pos, facing), part.applyTo(state), PLACE_FLAGS);
            }
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof VendingMachineBlockEntity machine) {
            machine.setOwner(player.getUUID(), player.getName().getString());
        }
    }

    // ---- breaking ------------------------------------------------------------------------------------------------

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!MachineAccess.canManage(player, ownerOf(level, pos, state))) return 0.0F;
        int divisor = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / OWNER_HARDNESS / divisor;
    }

    /** Tells a non-owner why nothing happens when they start mining. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !MachineAccess.canManage(player, ownerOf(level, pos, state))) {
            Messages.actionBar(player, Component.translatable(Messages.OWNER_ONLY));
        }
        super.attack(state, level, pos, player);
    }

    /** One player break takes the whole machine; survival breaks drop one machine item in the machine's color. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos master = MachinePart.masterOf(pos, state);
            if (!player.isCreative()) {
                popResource(level, master, MachineItems.forColor(state.getValue(COLOR)));
            }
            removeOtherParts(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removeOtherParts(Level level, BlockPos pos, BlockState state) {
        MachinePart self = MachinePart.of(state);
        Direction facing = state.getValue(FACING);
        BlockPos master = self.masterFrom(pos, facing);
        for (MachinePart part : MachinePart.values()) {
            if (part == self) continue;
            BlockPos partPos = part.posFrom(master, facing);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, partPos, Block.getId(partState));
            }
        }
    }

    // ---- integrity -----------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return keepIfWhole(state, pos, neighborPos, neighborState);
    }
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return keepIfWhole(state, pos, neighborPos, neighborState);
    }
    *///?}

    /** A part whose neighbouring part is gone (or wrong) removes itself, so broken machines never linger (spec §9). */
    private BlockState keepIfWhole(BlockState state, BlockPos pos, BlockPos neighborPos, BlockState neighborState) {
        MachinePart self = MachinePart.of(state);
        Direction facing = state.getValue(FACING);
        BlockPos master = self.masterFrom(pos, facing);
        for (MachinePart part : MachinePart.values()) {
            if (part == self || !part.posFrom(master, facing).equals(neighborPos)) continue;
            boolean intact = neighborState.is(this)
                    && neighborState.getValue(FACING) == facing
                    && MachinePart.of(neighborState) == part;
            return intact ? state : Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}
```

- [ ] **Step 7: Minimum loader versions in metadata**

In `src/main/resources/fabric.mod.json`, change `"fabric-api": "*"` to `"fabric-api": ">=${fabric_api}"`.

In `build.fabric.gradle.kts`, inside `processResources`' `buildMap { ... }`, add after `register("fabric_loader", "deps.fabric_loader")`:

```kotlin
            val fabricApiVersion: String = sc.properties["deps.fabric_api"]
            val fabricApi = fabricApiVersion.substringBefore('+')
            inputs.property("fabric_api", fabricApi)
            put("fabric_api", fabricApi)
```

In `src/main/resources/META-INF/neoforge.mods.toml`, add before the `minecraft` dependency:

```toml
[[dependencies.${id}]]
modId = "neoforge"
type = "required"
versionRange = "[${neoforge},)"
ordering = "NONE"
side = "BOTH"
```

In `build.neoforge.gradle.kts`, inside `processResources`' `buildMap { ... }`, add `register("neoforge", "deps.neo_loader")`.

- [ ] **Step 8: Run the game tests to verify they pass (every node)**

```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
Expected: `BUILD SUCCESSFUL`; all 11 tests pass. Then one call each:
```bash
./gradlew :26.1-fabric:runGametest --stacktrace
```
```bash
./gradlew :1.21.1-neoforge:runGameTestServer --stacktrace
```
```bash
./gradlew :1.21.1-fabric:runGametest --stacktrace
```
Expected: `BUILD SUCCESSFUL` each, 11 tests passing. If a node fails to compile, the version-specific branch is wrong: fix it against the extracted sources (`$SCRATCH/src-1211`, `$SCRATCH/src-261`, `$SCRATCH/src-261-neo`) — do not weaken the test.

Also confirm the metadata:
```bash
unzip -p versions/1.21.1-neoforge/build/libs/diamondvending-neoforge-0.1.0+1.21.1.jar META-INF/neoforge.mods.toml
```
Expected: a `neoforge` dependency with `versionRange = "[21.1.250,)"`.
```bash
unzip -p versions/26.1-fabric/build/libs/diamondvending-fabric-0.1.0+26.1.2.jar fabric.mod.json
```
Expected: `"fabric-api": ">=0.155.3"`.

- [ ] **Step 9: Unit tests still green, Stonecutter state canonical, commit**

```bash
./gradlew :26.1-neoforge:test
```
Expected: `BUILD SUCCESSFUL` (Plan 1 tests + Task 1 tests).
```bash
./gradlew "Refresh active project"
```
```bash
git -C /c/Users/benet/mcvending status --short
```
```bash
git -C /c/Users/benet/mcvending add src build.fabric.gradle.kts build.neoforge.gradle.kts
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat: 2x2 vending machine block with ownership, protection and clean-up"
```

---

### Task 3: Dyeing

Deliverable: owners and admins recolor the whole machine with a dye; strangers can't.

**Files:**
- Modify: `src/main/java/diamondvending/block/MachineItems.java` (add `dyeColorOf`), `block/VendingMachineBlock.java` (add `useItemOn` + `tryDye`)
- Modify: `src/gametest/java/diamondvending/gametest/MachineTests.java`, `fabric/FabricGameTests.java`, `neoforge/NeoForgeGameTests.java`

**Interfaces:**
- Consumes: `MachineAccess.canManage`, `VendingMachineBlock.ownerOf`, `Messages` (Task 2).
- Produces: `MachineItems.dyeColorOf(ItemStack): DyeColor` (null if not a dye).

- [ ] **Step 1: Write the failing GameTests**

Add to `MachineTests` (imports: `net.minecraft.world.InteractionHand`, `net.minecraft.world.item.Items`):

```java
    public static void ownersCanDyeTheWholeMachine(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        ItemStack dye = new ItemStack(Items.BLUE_DYE, 2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(UPPER_RIGHT, owner);
        assertWholeMachine(helper, DyeColor.BLUE);
        helper.assertTrue(dye.getCount() == 1, "dyeing should use one dye, stack is now " + dye.getCount());
        helper.succeed();
    }

    public static void strangersCannotDyeIt(GameTestHelper helper) {
        placeOn(helper, helper.makeMockPlayer(GameType.SURVIVAL), machineItem(1), FLOOR);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack dye = new ItemStack(Items.BLUE_DYE, 2);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(MASTER, stranger);
        assertWholeMachine(helper, DyeColor.RED);
        helper.assertTrue(dye.getCount() == 2, "a refused dye must not be used up");
        helper.succeed();
    }

    public static void dyeingTheSameColorUsesNoDye(GameTestHelper helper) {
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        placeOn(helper, owner, machineItem(1), FLOOR);
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        owner.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(MASTER, owner);
        assertWholeMachine(helper, DyeColor.RED);
        helper.assertTrue(dye.getCount() == 2, "dyeing to the same color must not use a dye");
        helper.succeed();
    }

    public static void creativeDyeingKeepsTheDye(GameTestHelper helper) {
        Player admin = helper.makeMockPlayer(GameType.CREATIVE);
        placeOn(helper, admin, machineItem(1), FLOOR);
        ItemStack dye = new ItemStack(Items.GREEN_DYE, 1);
        admin.setItemInHand(InteractionHand.MAIN_HAND, dye);
        helper.useBlock(MASTER, admin);
        assertWholeMachine(helper, DyeColor.GREEN);
        helper.assertTrue(dye.getCount() == 1, "creative dyeing must not use up the dye");
        helper.succeed();
    }
```

Add to `ALL`:

```java
            Map.entry("owners_can_dye_the_whole_machine", MachineTests::ownersCanDyeTheWholeMachine),
            Map.entry("strangers_cannot_dye_it", MachineTests::strangersCannotDyeIt),
            Map.entry("dyeing_the_same_color_uses_no_dye", MachineTests::dyeingTheSameColorUsesNoDye),
            Map.entry("creative_dyeing_keeps_the_dye", MachineTests::creativeDyeingKeepsTheDye)
```

Add to `FabricGameTests` (before its closing brace):

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void ownersCanDyeTheWholeMachine(GameTestHelper helper) {
        MachineTests.ownersCanDyeTheWholeMachine(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void strangersCannotDyeIt(GameTestHelper helper) {
        MachineTests.strangersCannotDyeIt(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void dyeingTheSameColorUsesNoDye(GameTestHelper helper) {
        MachineTests.dyeingTheSameColorUsesNoDye(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creativeDyeingKeepsTheDye(GameTestHelper helper) {
        MachineTests.creativeDyeingKeepsTheDye(helper);
    }
```

Add inside `NeoForgeGameTests`' `//? if <26.1` block, just before its closing `*///?}`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ownersCanDyeTheWholeMachine(GameTestHelper helper) {
        MachineTests.ownersCanDyeTheWholeMachine(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void strangersCannotDyeIt(GameTestHelper helper) {
        MachineTests.strangersCannotDyeIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void dyeingTheSameColorUsesNoDye(GameTestHelper helper) {
        MachineTests.dyeingTheSameColorUsesNoDye(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creativeDyeingKeepsTheDye(GameTestHelper helper) {
        MachineTests.creativeDyeingKeepsTheDye(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
Expected: FAIL — `owners_can_dye_the_whole_machine` fails ("expected BLUE …") and `creative_dyeing_keeps_the_dye` fails; the other two pass trivially; report shows 2 failed required tests.

- [ ] **Step 3: Implement dyeing**

Add to `MachineItems` (imports: `net.minecraft.world.item.DyeItem`):

```java
    /** The dye color of a dye item, or null if the stack isn't a dye. */
    public static DyeColor dyeColorOf(ItemStack stack) {
        //? if >=26.1 {
        return stack.getItem() instanceof DyeItem ? stack.get(DataComponents.DYE) : null;
        //?} else {
        /*return stack.getItem() instanceof DyeItem dye ? dye.getDyeColor() : null;
        *///?}
    }
```

Add these imports to `VendingMachineBlock` (plain imports with the others; the 1.21.1-only one goes into the existing version import block, which becomes):

```java
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
//? if >=26.1 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.LevelAccessor;
*///?}
```

Then add to the class body:

```java
    // ---- dyeing --------------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        return tryDye(stack, state, level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        return tryDye(stack, state, level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    /** Spec §3.2 rule 2: owners and admins repaint the whole machine. Returns false if the stack isn't a dye. */
    private boolean tryDye(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        DyeColor color = MachineItems.dyeColorOf(stack);
        if (color == null) return false;
        if (level.isClientSide()) return true;
        if (!MachineAccess.canManage(player, ownerOf(level, pos, state))) {
            Messages.actionBar(player, Component.translatable(Messages.OWNER_ONLY));
            return true;
        }
        if (state.getValue(COLOR) == color) return true;
        Direction facing = state.getValue(FACING);
        BlockPos master = MachinePart.masterOf(pos, state);
        for (MachinePart part : MachinePart.values()) {
            BlockPos partPos = part.posFrom(master, facing);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlock(partPos, partState.setValue(COLOR, color), PLACE_FLAGS);
            }
        }
        stack.consume(1, player);
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
```

- [ ] **Step 4: Run the game tests on every node**

```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
Expected: `BUILD SUCCESSFUL`, 15 tests pass. Then:
```bash
./gradlew :26.1-fabric:runGametest --stacktrace
```
```bash
./gradlew :1.21.1-neoforge:runGameTestServer --stacktrace
```
```bash
./gradlew :1.21.1-fabric:runGametest --stacktrace
```
Expected: `BUILD SUCCESSFUL` each.

- [ ] **Step 5: Commit**

```bash
./gradlew "Refresh active project"
```
```bash
git -C /c/Users/benet/mcvending add src
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat: owners and admins dye the vending machine"
```

---

### Task 4: Visuals — generated textures, models, blockstates, item models, icon

Deliverable: the machine renders in all 16 colors with a front that matches `MachineLayout`; the item shows its color; the mod has an icon. All generated and checked by JUnit.

**Files:**
- Create: `src/test/java/diamondvending/art/Dye.java`, `Json.java`, `Textures.java`, `Models.java`, `TexturesTest.java`, `ModelsTest.java`
- Modify: `src/test/java/diamondvending/art/ArtGenerator.java`
- Generate: `src/main/resources/assets/diamondvending/{textures,models,blockstates}/**`, `src/main/resources/assets/diamondvending/icon.png`, `src/main/resources-1.21.1/assets/diamondvending/models/item/vending_machine.json`, `src/main/resources-26.1/assets/diamondvending/items/vending_machine.json`, `docs/images/machine-colors.png`
- Modify: both build scripts (version resources dir), `src/main/resources/fabric.mod.json`, `src/main/resources/META-INF/neoforge.mods.toml`, `README.md`

**Interfaces:**
- Consumes: `core.MachineLayout` (`WINDOW`, `LAMP`, `DISPLAY`, `COIN_SLOT`, `COIN_RETURN`, `TRAY`, `button(i)`, `shelfSlot(i)`), `core.Rect`.
- Produces: `Textures.front(Dye)` (32×32, the whole front canvas at 16 px/block), `Textures.side(Dye)` (16×16), `Textures.item(Dye)` (16×16), `Textures.icon()` (128×128), `Textures.preview()`; color constants `Textures.GLASS`, `BUTTON`, `BUTTON_SHADE`, `DISPLAY_OFF`, `LAMP_OFF`, `SLOT`, `COIN_RETURN`, `TRAY`, `PANEL` (RGB ints); `Models.Part` (the generator's own 4-part enum, independent of the mod's `MachinePart`), `Models.blockstate()`, `Models.partModelName(Dye, Part)`, `Models.partModel(Dye, Part)`, `Models.itemModel(Dye)`, `Models.itemDefinition26()`, `Models.itemModel1211()`.

- [ ] **Step 1: Write the failing tests**

`src/test/java/diamondvending/art/TexturesTest.java`:

```java
package diamondvending.art;

import diamondvending.core.MachineLayout;
import diamondvending.core.Rect;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TexturesTest {
    private static final BufferedImage RED_FRONT = Textures.front(Dye.RED);

    private static int rgbAt(BufferedImage image, double u, double v) {
        return image.getRGB((int) Math.floor(u), (int) Math.floor(v)) & 0xFFFFFF;
    }

    private static int centerRgb(Rect rect) {
        return rgbAt(RED_FRONT, rect.centerU(), rect.centerV());
    }

    @Test
    void frontIsTheWholeCanvasAt16PixelsPerBlock() {
        assertEquals(MachineLayout.CANVAS, RED_FRONT.getWidth());
        assertEquals(MachineLayout.CANVAS, RED_FRONT.getHeight());
    }

    @Test
    void everyButtonIsPaintedWhereMachineLayoutPutsIt() {
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Rect button = MachineLayout.button(i);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    if (!button.contains(x + 0.5, y + 0.5)) continue;
                    int rgb = RED_FRONT.getRGB(x, y) & 0xFFFFFF;
                    assertTrue(rgb == Textures.BUTTON || rgb == Textures.BUTTON_SHADE,
                            "button " + (i + 1) + " pixel " + x + "," + y + " is " + Integer.toHexString(rgb));
                }
            }
        }
    }

    @Test
    void panelPartsHaveTheirColors() {
        // Glass just inside the frame, below the shine row and above the first shelf line
        assertEquals(Textures.GLASS, rgbAt(RED_FRONT, MachineLayout.WINDOW.u0() + 2, MachineLayout.WINDOW.v0() + 3));
        assertEquals(Textures.DISPLAY_OFF, centerRgb(MachineLayout.DISPLAY));
        assertEquals(Textures.LAMP_OFF, centerRgb(MachineLayout.LAMP));
        assertEquals(Textures.SLOT, centerRgb(MachineLayout.COIN_SLOT));
        assertEquals(Textures.COIN_RETURN, centerRgb(MachineLayout.COIN_RETURN));
        assertEquals(Textures.TRAY, centerRgb(MachineLayout.TRAY));
    }

    @Test
    void bodyTakesTheDyeColor() {
        // (23, 12) is plain body between the window and the panel
        assertEquals(Dye.RED.rgb, rgbAt(Textures.front(Dye.RED), 23, 12));
        assertEquals(Dye.BLUE.rgb, rgbAt(Textures.front(Dye.BLUE), 23, 12));
        assertNotEquals(rgbAt(Textures.side(Dye.RED), 8, 8), rgbAt(Textures.side(Dye.BLUE), 8, 8));
    }

    @Test
    void sizesAreRight() {
        assertEquals(16, Textures.side(Dye.RED).getWidth());
        assertEquals(16, Textures.item(Dye.RED).getWidth());
        assertEquals(128, Textures.icon().getWidth());
    }

    @Test
    void dyeColorsMatchMinecraft() {
        assertEquals(16, Dye.values().length);
        assertEquals(0xB02E26, Dye.RED.rgb);
        assertEquals(14, Dye.RED.ordinal(), "Dye order must match DyeColor ids");
        assertEquals(0x1D1D21, Dye.BLACK.rgb);
    }
}
```

`src/test/java/diamondvending/art/ModelsTest.java`:

```java
package diamondvending.art;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelsTest {
    /** Model references: blockstate variants, item definitions and overrides. */
    private static final Pattern MODEL_REF = Pattern.compile("\"model\": \"diamondvending:([a-z_/]+)\"");
    /** Texture references: the texture variables used by our models. */
    private static final Pattern TEXTURE_REF = Pattern.compile("\"(?:particle|front|side|layer0)\": \"diamondvending:([a-z_/]+)\"");

    private static Set<String> generatedPaths() {
        Set<String> paths = new HashSet<>();
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) paths.add(output.path());
        return paths;
    }

    private static String textOf(String path) {
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) {
            if (output.path().equals(path)) return new String(output.bytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        throw new AssertionError("not generated: " + path);
    }

    @Test
    void blockstateHasAVariantForEveryStateCombination() {
        String blockstate = textOf("src/main/resources/assets/diamondvending/blockstates/vending_machine.json");
        int variants = blockstate.split("\"color=").length - 1;
        assertEquals(16 * 4 * 2 * 2, variants);
    }

    @Test
    void everyReferencedModelAndTextureIsGenerated() {
        Set<String> paths = generatedPaths();
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) {
            if (output.kind() != ArtGenerator.Kind.JSON) continue;
            String json = new String(output.bytes(), java.nio.charset.StandardCharsets.UTF_8);
            Matcher models = MODEL_REF.matcher(json);
            while (models.find()) {
                String file = "src/main/resources/assets/diamondvending/models/" + models.group(1) + ".json";
                assertTrue(paths.contains(file), output.path() + " references missing model " + models.group(1));
            }
            Matcher textures = TEXTURE_REF.matcher(json);
            while (textures.find()) {
                String file = "src/main/resources/assets/diamondvending/textures/" + textures.group(1) + ".png";
                assertTrue(paths.contains(file), output.path() + " references missing texture " + textures.group(1));
            }
        }
    }

    @Test
    void versionSpecificItemModelsCoverEveryColor() {
        String modern = textOf("src/main/resources-26.1/assets/diamondvending/items/vending_machine.json");
        String legacy = textOf("src/main/resources-1.21.1/assets/diamondvending/models/item/vending_machine.json");
        for (Dye dye : Dye.values()) {
            assertTrue(modern.contains("\"when\": \"" + dye.id() + "\""), "26.1 item definition misses " + dye.id());
            assertTrue(legacy.contains("\"custom_model_data\": " + (dye.ordinal() + 1)), "1.21.1 item model misses " + dye.id());
        }
    }
}
```

- [ ] **Step 2: Run to verify they fail**

```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: FAIL — `cannot find symbol: class Textures`, `Dye`.

- [ ] **Step 3: Implement the art**

`src/test/java/diamondvending/art/Dye.java`:

```java
package diamondvending.art;

import java.util.Locale;

/** Minecraft's 16 dye colors, in DyeColor id order, with their texture colors (DyeColor#getTextureDiffuseColor). */
enum Dye {
    WHITE(0xF9FFFE), ORANGE(0xF9801D), MAGENTA(0xC74EBD), LIGHT_BLUE(0x3AB3DA), YELLOW(0xFED83D), LIME(0x80C71F),
    PINK(0xF38BAA), GRAY(0x474F52), LIGHT_GRAY(0x9D9D97), CYAN(0x169C9C), PURPLE(0x8932B8), BLUE(0x3C44AA),
    BROWN(0x835432), GREEN(0x5E7C16), RED(0xB02E26), BLACK(0x1D1D21);

    final int rgb;

    Dye(int rgb) {
        this.rgb = rgb;
    }

    /** The block-state / component value, e.g. {@code light_blue}. */
    String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
```

`src/test/java/diamondvending/art/Json.java`:

```java
package diamondvending.art;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A tiny deterministic JSON writer (2-space indent, keys in insertion order, short scalar lists inline). */
final class Json {
    private Json() {}

    /** An insertion-ordered object from alternating keys and values. */
    static Map<String, Object> obj(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value, 0);
        return out.append('\n').toString();
    }

    private static void write(StringBuilder out, Object value, int depth) {
        switch (value) {
            case String s -> out.append('"').append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            case Number n -> out.append(n);
            case Boolean b -> out.append(b);
            case List<?> list -> writeList(out, list, depth);
            case Map<?, ?> map -> writeMap(out, map, depth);
            default -> throw new IllegalArgumentException("unsupported JSON value: " + value);
        }
    }

    private static void writeList(StringBuilder out, List<?> list, int depth) {
        boolean inline = list.stream().allMatch(e -> e instanceof Number || e instanceof String);
        if (list.isEmpty() || inline) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(", ");
                write(out, list.get(i), depth);
            }
            out.append(']');
            return;
        }
        out.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(out, depth + 1);
            write(out, list.get(i), depth + 1);
            out.append(i < list.size() - 1 ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append(']');
    }

    private static void writeMap(StringBuilder out, Map<?, ?> map, int depth) {
        out.append("{\n");
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            indent(out, depth + 1);
            out.append('"').append(entry.getKey()).append("\": ");
            write(out, entry.getValue(), depth + 1);
            out.append(++i < map.size() ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append('}');
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(depth));
    }
}
```

`src/test/java/diamondvending/art/Textures.java`:

```java
package diamondvending.art;

import diamondvending.core.MachineLayout;
import diamondvending.core.Rect;

import java.awt.image.BufferedImage;

/**
 * Paints the machine's textures. The front is the whole 32×32 canvas from {@link MachineLayout} (16 px per block);
 * each part's model shows its quarter. Because the painting reads MachineLayout, textures always match the click regions.
 */
final class Textures {
    static final int GLASS = 0xBFE3EF;
    static final int GLASS_SHINE = 0xE4F4FA;
    static final int WINDOW_FRAME = 0x3A3A3A;
    static final int SHELF = 0x5A5A5A;
    static final int PANEL = 0x2B2B2B;
    static final int BUTTON = 0xFFD23F;
    static final int BUTTON_SHADE = 0xC9A227;
    static final int DISPLAY_OFF = 0x0E2A12;
    static final int LAMP_OFF = 0x5A1414;
    static final int SLOT = 0x9A9A9A;
    static final int COIN_RETURN = 0xB0B0B0;
    static final int TRAY = 0x141414;
    static final int TRAY_LIP = 0x555555;

    /** Visual-only panel behind the display and buttons (not a click region). */
    static final Rect PANEL_AREA = new Rect(24, 2, 30.5, 24);

    private Textures() {}

    static BufferedImage front(Dye dye) {
        BufferedImage image = new BufferedImage(MachineLayout.CANVAS, MachineLayout.CANVAS, BufferedImage.TYPE_INT_ARGB);
        fill(image, dye.rgb);
        outline(image, shade(dye.rgb, 0.7));

        Rect window = MachineLayout.WINDOW;
        paint(image, window, WINDOW_FRAME);
        paint(image, inset(window, 1), GLASS);
        paint(image, new Rect(window.u0() + 1, window.v0() + 1, window.u1() - 1, window.v0() + 2), GLASS_SHINE);
        // A shelf line under the top three rows; the bottom row stands on the window frame
        for (int row = 0; row < 3; row++) {
            Rect slot = MachineLayout.shelfSlot(row * 3);
            paint(image, new Rect(window.u0() + 1, slot.v1() + 0.25, window.u1() - 1, slot.v1() + 1.25), SHELF);
        }

        paint(image, PANEL_AREA, PANEL);
        paint(image, MachineLayout.DISPLAY, DISPLAY_OFF);
        paint(image, MachineLayout.LAMP, LAMP_OFF);
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Rect button = MachineLayout.button(i);
            paint(image, button, BUTTON);
            paint(image, new Rect(button.u0(), button.v1() - 1, button.u1(), button.v1()), BUTTON_SHADE);
        }

        Rect coinSlot = MachineLayout.COIN_SLOT;
        paint(image, coinSlot, PANEL);
        paint(image, new Rect(coinSlot.centerU() - 0.5, coinSlot.v0() + 0.5, coinSlot.centerU() + 0.5, coinSlot.v1() - 0.5), SLOT);
        paint(image, MachineLayout.COIN_RETURN, COIN_RETURN);

        Rect tray = MachineLayout.TRAY;
        paint(image, tray, TRAY);
        paint(image, new Rect(tray.u0(), tray.v0(), tray.u1(), tray.v0() + 1), TRAY_LIP);
        return image;
    }

    /** Sides, top, bottom and back: the body color with a faint deterministic grain. */
    static BufferedImage side(Dye dye) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double grain = 0.94 + 0.06 * (((x * 7 + y * 13) % 5) / 4.0);
                image.setRGB(x, y, 0xFF000000 | shade(dye.rgb, grain));
            }
        }
        return image;
    }

    /** The inventory icon: the front scaled down to 16×16. */
    static BufferedImage item(Dye dye) {
        return downscale(front(dye), 2);
    }

    /** The mod icon: a red front scaled up to 128×128. */
    static BufferedImage icon() {
        return upscale(front(Dye.RED), 4);
    }

    /** All 16 colors side by side at 4× (for the README). */
    static BufferedImage preview() {
        int scale = 4;
        int cell = MachineLayout.CANVAS * scale;
        int gap = 8;
        BufferedImage image = new BufferedImage(8 * (cell + gap) - gap, 2 * (cell + gap) - gap, BufferedImage.TYPE_INT_ARGB);
        Dye[] dyes = Dye.values();
        for (int i = 0; i < dyes.length; i++) {
            BufferedImage big = upscale(front(dyes[i]), scale);
            int ox = (i % 8) * (cell + gap);
            int oy = (i / 8) * (cell + gap);
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) image.setRGB(ox + x, oy + y, big.getRGB(x, y));
            }
        }
        return image;
    }

    // ---- painting helpers ----------------------------------------------------------------------------------------

    /** Paints every pixel whose center lies inside {@code rect}. */
    static void paint(BufferedImage image, Rect rect, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (rect.contains(x + 0.5, y + 0.5)) image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
    }

    private static void fill(BufferedImage image, int rgb) {
        paint(image, new Rect(0, 0, image.getWidth(), image.getHeight()), rgb);
    }

    private static void outline(BufferedImage image, int rgb) {
        int w = image.getWidth();
        int h = image.getHeight();
        for (int i = 0; i < w; i++) {
            image.setRGB(i, 0, 0xFF000000 | rgb);
            image.setRGB(i, h - 1, 0xFF000000 | rgb);
        }
        for (int i = 0; i < h; i++) {
            image.setRGB(0, i, 0xFF000000 | rgb);
            image.setRGB(w - 1, i, 0xFF000000 | rgb);
        }
    }

    private static Rect inset(Rect rect, double by) {
        return new Rect(rect.u0() + by, rect.v0() + by, rect.u1() - by, rect.v1() - by);
    }

    static int shade(int rgb, double factor) {
        int r = (int) Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = (int) Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = (int) Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private static BufferedImage downscale(BufferedImage source, int factor) {
        int w = source.getWidth() / factor;
        int h = source.getHeight() / factor;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                for (int dy = 0; dy < factor; dy++) {
                    for (int dx = 0; dx < factor; dx++) {
                        int rgb = source.getRGB(x * factor + dx, y * factor + dy);
                        r += (rgb >> 16) & 0xFF;
                        g += (rgb >> 8) & 0xFF;
                        b += rgb & 0xFF;
                    }
                }
                int n = factor * factor;
                image.setRGB(x, y, 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n));
            }
        }
        return image;
    }

    private static BufferedImage upscale(BufferedImage source, int factor) {
        BufferedImage image = new BufferedImage(source.getWidth() * factor, source.getHeight() * factor, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, source.getRGB(x / factor, y / factor));
        }
        return image;
    }
}
```

`src/test/java/diamondvending/art/Models.java`:

```java
package diamondvending.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static diamondvending.art.Json.obj;

/**
 * Block models, the blockstate, and item models. Part models face north; the blockstate rotates them.
 * Looking at a north face, texture u runs toward the viewer's right, matching the canvas.
 */
final class Models {
    /** Part name → {right column?, upper row?}. */
    enum Part {
        LOWER_LEFT(false, false), LOWER_RIGHT(true, false), UPPER_LEFT(false, true), UPPER_RIGHT(true, true);

        final boolean right;
        final boolean upper;

        Part(boolean right, boolean upper) {
            this.right = right;
            this.upper = upper;
        }

        String half() {
            return upper ? "upper" : "lower";
        }

        String side() {
            return right ? "right" : "left";
        }
    }

    private static final String[] FACINGS = {"east", "north", "south", "west"};
    private static final int[] FACING_Y = {90, 0, 180, 270};

    private Models() {}

    static String partModelName(Dye dye, Part part) {
        return "block/vending_machine/" + dye.id() + "_" + part.half() + "_" + part.side();
    }

    static Map<String, Object> partModel(Dye dye, Part part) {
        int u0 = part.right ? 8 : 0;
        int v0 = part.upper ? 0 : 8;
        String side = "diamondvending:block/vending_machine_side_" + dye.id();
        Map<String, Object> faces = obj(
                "north", obj("uv", List.of(u0, v0, u0 + 8, v0 + 8), "texture", "#front", "cullface", "north"),
                "south", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "south"),
                "east", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "east"),
                "west", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "west"),
                "up", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "up"),
                "down", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "down"));
        return obj(
                "parent", "minecraft:block/block",
                "textures", obj(
                        "particle", side,
                        "front", "diamondvending:block/vending_machine_front_" + dye.id(),
                        "side", side),
                "elements", List.of(obj("from", List.of(0, 0, 0), "to", List.of(16, 16, 16), "faces", faces)));
    }

    static Map<String, Object> blockstate() {
        Map<String, Object> variants = obj();
        for (Dye dye : Dye.values()) {
            for (int f = 0; f < FACINGS.length; f++) {
                for (Part part : Part.values()) {
                    String key = "color=" + dye.id() + ",facing=" + FACINGS[f] + ",half=" + part.half() + ",side=" + part.side();
                    Map<String, Object> variant = obj("model", "diamondvending:" + partModelName(dye, part));
                    if (FACING_Y[f] != 0) variant.put("y", FACING_Y[f]);
                    variants.put(key, variant);
                }
            }
        }
        return obj("variants", variants);
    }

    static Map<String, Object> itemModel(Dye dye) {
        return obj("parent", "minecraft:item/generated",
                "textures", obj("layer0", "diamondvending:item/vending_machine_" + dye.id()));
    }

    /** 26.1: pick the model from the item's base_color component. */
    static Map<String, Object> itemDefinition26() {
        List<Object> cases = new ArrayList<>();
        for (Dye dye : Dye.values()) {
            cases.add(obj("when", dye.id(),
                    "model", obj("type", "minecraft:model", "model", "diamondvending:item/vending_machine_" + dye.id())));
        }
        return obj("model", obj(
                "type", "minecraft:select",
                "property", "minecraft:component",
                "component", "minecraft:base_color",
                "cases", cases,
                "fallback", obj("type", "minecraft:model", "model", "diamondvending:item/vending_machine_red")));
    }

    /** 1.21.1: overrides on custom_model_data (dye id + 1); no data means red. */
    static Map<String, Object> itemModel1211() {
        List<Object> overrides = new ArrayList<>();
        for (Dye dye : Dye.values()) {
            overrides.add(obj("predicate", obj("custom_model_data", dye.ordinal() + 1),
                    "model", "diamondvending:item/vending_machine_" + dye.id()));
        }
        return obj("parent", "minecraft:item/generated",
                "textures", obj("layer0", "diamondvending:item/vending_machine_red"),
                "overrides", overrides);
    }
}
```

Replace `ArtGenerator.generateAll()` and add PNG/JSON helpers (imports: `javax.imageio.ImageIO`, `java.awt.image.BufferedImage`, `java.io.ByteArrayOutputStream`, `java.io.UncheckedIOException`, `static java.nio.charset.StandardCharsets.UTF_8`):

```java
    private static final String ASSETS = "src/main/resources/assets/diamondvending/";

    public static List<Output> generateAll() {
        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output("src/gametest/resources/data/diamondvending/structure/gametest_platform.nbt",
                Kind.NBT, NbtWriter.toGzip(Structures.gametestPlatform())));

        for (Dye dye : Dye.values()) {
            outputs.add(png(ASSETS + "textures/block/vending_machine_front_" + dye.id() + ".png", Textures.front(dye)));
            outputs.add(png(ASSETS + "textures/block/vending_machine_side_" + dye.id() + ".png", Textures.side(dye)));
            outputs.add(png(ASSETS + "textures/item/vending_machine_" + dye.id() + ".png", Textures.item(dye)));
            outputs.add(json(ASSETS + "models/item/vending_machine_" + dye.id() + ".json", Models.itemModel(dye)));
            for (Models.Part part : Models.Part.values()) {
                outputs.add(json(ASSETS + "models/" + Models.partModelName(dye, part) + ".json", Models.partModel(dye, part)));
            }
        }
        outputs.add(json(ASSETS + "blockstates/vending_machine.json", Models.blockstate()));
        outputs.add(json("src/main/resources-26.1/assets/diamondvending/items/vending_machine.json", Models.itemDefinition26()));
        outputs.add(json("src/main/resources-1.21.1/assets/diamondvending/models/item/vending_machine.json", Models.itemModel1211()));
        outputs.add(png(ASSETS + "icon.png", Textures.icon()));
        outputs.add(png("docs/images/machine-colors.png", Textures.preview()));
        return outputs;
    }

    private static Output json(String path, Object value) {
        return new Output(path, Kind.JSON, Json.write(value).getBytes(UTF_8));
    }

    private static Output png(String path, BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Output(path, Kind.PNG, bytes.toByteArray());
    }
```

- [ ] **Step 4: Add the per-version resources dir to both build scripts**

In **both** build scripts, change the `sourceSets.main { ... }` block to:

```kotlin
sourceSets.main {
    java.exclude("diamondvending/platform/neoforge/**")   // in build.neoforge.gradle.kts: "diamondvending/platform/fabric/**"
    // JSON that differs between Minecraft versions (recipes, item models)
    resources.srcDir(rootProject.file("src/main/resources-" + if (sc.current.parsed >= "26.1") "26.1" else "1.21.1"))
}
```

(Keep each script's own `java.exclude` line as it is today.)

- [ ] **Step 5: Generate, then watch the tests pass**

```bash
./gradlew :26.1-neoforge:generateArt
```
Expected: `Wrote 134 generated files …` (1 structure + 16 × (3 textures + 1 item model + 4 part models) + blockstate + 2 version-specific item models + icon + preview).
```bash
./gradlew :26.1-neoforge:test --stacktrace
```
Expected: `BUILD SUCCESSFUL`; `TexturesTest` 6/6, `ModelsTest` 3/3, `GeneratedFilesTest` 1/1.

Look at the preview to sanity-check the art (Read the image): `docs/images/machine-colors.png`. The buttons, display, window with 4 shelves, coin slot, coin return and tray must be recognizable, and match the approved mockup layout.

- [ ] **Step 6: Icon metadata and README**

In `src/main/resources/fabric.mod.json`, add after `"license": "MIT",`:

```json
  "icon": "assets/diamondvending/icon.png",
```

In `src/main/resources/META-INF/neoforge.mods.toml`, add under `[[mods]]` after `description`:

```toml
logoFile = "assets/diamondvending/icon.png"
```

In `README.md`, add after the first paragraph:

```markdown
![The vending machine in all 16 dye colors](docs/images/machine-colors.png)
```

- [ ] **Step 7: Build all nodes and run game tests (models must load)**

```bash
./gradlew :26.1-neoforge:build --stacktrace
```
```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
Then, one call each:
```bash
./gradlew :26.1-fabric:build :26.1-fabric:runGametest --stacktrace
```
```bash
./gradlew :1.21.1-neoforge:build :1.21.1-neoforge:runGameTestServer --stacktrace
```
```bash
./gradlew :1.21.1-fabric:build :1.21.1-fabric:runGametest --stacktrace
```
Expected: all `BUILD SUCCESSFUL`. (Game-test servers don't load models; in-game rendering is checked on the client in Plan 5's QA checklist. The generator tests are what guarantee every model and texture reference resolves.)

- [ ] **Step 8: Commit**

```bash
./gradlew "Refresh active project"
```
```bash
git -C /c/Users/benet/mcvending add src docs/images README.md build.fabric.gradle.kts build.neoforge.gradle.kts
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat: generated textures, models and icon for all 16 colors"
```

---

### Task 5: Recipe, unlock, docs, and the PR

Deliverable: the machine is craftable (recipe unlocked by getting a diamond) on both versions; docs updated; PR merged after green CI.

**Files:**
- Create: `src/main/resources-1.21.1/data/diamondvending/recipe/vending_machine.json`, `src/main/resources-26.1/data/diamondvending/recipe/vending_machine.json`, `src/main/resources/data/diamondvending/advancement/recipes/misc/vending_machine.json`
- Modify: `src/gametest/java/diamondvending/gametest/MachineTests.java`, `fabric/FabricGameTests.java`, `neoforge/NeoForgeGameTests.java`
- Modify: `docs/dev-setup.md`, `CLAUDE.md`, `CHANGELOG.md`, `docs/superpowers/plans/2026-09-23-roadmap.md`

**Interfaces:**
- Consumes: the version resources dirs (Task 4), GameTest harness (Task 1).

- [ ] **Step 1: Write the failing GameTest**

Add to `MachineTests`' imports:

```java
//? if >=26.1 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}
```

and this test:

```java
    public static void machineRecipeLoads(GameTestHelper helper) {
        //? if >=26.1 {
        boolean found = helper.getLevel().recipeAccess()
                .byKey(ResourceKey.create(Registries.RECIPE, DiamondVending.id("vending_machine"))).isPresent();
        //?} else {
        /*boolean found = helper.getLevel().getRecipeManager().byKey(DiamondVending.id("vending_machine")).isPresent();
        *///?}
        helper.assertTrue(found, "the vending machine recipe did not load (check the log for recipe parse errors)");
        helper.succeed();
    }
```

Add `Map.entry("machine_recipe_loads", MachineTests::machineRecipeLoads)` to `ALL`.

Add to `FabricGameTests`:

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void machineRecipeLoads(GameTestHelper helper) {
        MachineTests.machineRecipeLoads(helper);
    }
```

Add inside `NeoForgeGameTests`' 1.21.1 block, before its closing `*///?}`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void machineRecipeLoads(GameTestHelper helper) {
        MachineTests.machineRecipeLoads(helper);
    }
```

- [ ] **Step 2: Run to verify it fails**

```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
Expected: FAIL — `machine_recipe_loads`: "the vending machine recipe did not load".

- [ ] **Step 3: Add the recipes and the unlock**

`src/main/resources-26.1/data/diamondvending/recipe/vending_machine.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "B": "minecraft:iron_block",
    "D": "minecraft:diamond",
    "G": "minecraft:glass_pane",
    "R": "minecraft:redstone"
  },
  "pattern": [
    "BGB",
    "BRB",
    "BDB"
  ],
  "result": {
    "id": "diamondvending:vending_machine"
  }
}
```

`src/main/resources-1.21.1/data/diamondvending/recipe/vending_machine.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "key": {
    "B": { "item": "minecraft:iron_block" },
    "D": { "item": "minecraft:diamond" },
    "G": { "item": "minecraft:glass_pane" },
    "R": { "item": "minecraft:redstone" }
  },
  "pattern": [
    "BGB",
    "BRB",
    "BDB"
  ],
  "result": {
    "count": 1,
    "id": "diamondvending:vending_machine"
  }
}
```

`src/main/resources/data/diamondvending/advancement/recipes/misc/vending_machine.json`:

```json
{
  "parent": "minecraft:recipes/root",
  "criteria": {
    "has_diamond": {
      "conditions": {
        "items": [
          { "items": "minecraft:diamond" }
        ]
      },
      "trigger": "minecraft:inventory_changed"
    },
    "has_the_recipe": {
      "conditions": {
        "recipe": "diamondvending:vending_machine"
      },
      "trigger": "minecraft:recipe_unlocked"
    }
  },
  "requirements": [
    ["has_the_recipe", "has_diamond"]
  ],
  "rewards": {
    "recipes": ["diamondvending:vending_machine"]
  }
}
```

- [ ] **Step 4: Run the game tests on every node**

```bash
./gradlew :26.1-neoforge:runGameTestServer --stacktrace
```
```bash
./gradlew :26.1-fabric:runGametest --stacktrace
```
```bash
./gradlew :1.21.1-neoforge:runGameTestServer --stacktrace
```
```bash
./gradlew :1.21.1-fabric:runGametest --stacktrace
```
Expected: `BUILD SUCCESSFUL` each; 16 tests pass. Check no advancement/recipe parse errors:
```bash
grep -iE "diamondvending.*(error|couldn't|failed)" versions/1.21.1-neoforge/build/gametest/logs/latest.log
```
Expected: no output.

- [ ] **Step 5: Docs**

Append to `docs/dev-setup.md` before `## Workflow`:

````markdown
## In-game tests (GameTests)
Game tests live in `src/gametest/` and build a separate test-only mod, so they never ship. Run one node at a time:

```bash
./gradlew :26.1-neoforge:runGameTestServer
./gradlew :26.1-fabric:runGametest
```

Add a test in three places: a `public static void` method in `gametest/MachineTests.java` (+ its `ALL` entry), a
method in `gametest/fabric/FabricGameTests.java`, and one in the 1.21.1 block of `gametest/neoforge/NeoForgeGameTests.java`.

## Generated art
Textures, block/item models, the blockstate, the mod icon and the GameTest platform are generated from
`core/MachineLayout` by `src/test/java/diamondvending/art/`. Never edit them by hand — change the generator and run:

```bash
./gradlew :26.1-neoforge:generateArt
```

`GeneratedFilesTest` fails the build if the committed files drift from the generator.

## Version-specific resources
JSON that differs between Minecraft versions lives in `src/main/resources-1.21.1/` and `src/main/resources-26.1/`
(recipes, item models). Everything else goes in `src/main/resources/`.
````

In `CLAUDE.md`, add:

```markdown
- Generated art/models/structures: change `src/test/java/diamondvending/art/`, run `./gradlew :26.1-neoforge:generateArt`, never hand-edit the outputs.
- GameTests: `./gradlew :<node>:runGameTestServer` (NeoForge) / `:<node>:runGametest` (Fabric). New tests go in three files (see docs/dev-setup.md).
```

In `CHANGELOG.md` under `### Added`, add:

```markdown
- The Vending Machine block: a 2×2 machine that faces you when placed, glows softly, and can't be moved by pistons or blown up.
- Only the owner (or an admin) can break or dye a machine; breaking keeps its color on the item.
- Dye it in any of the 16 colors.
- Crafting recipe (iron blocks, glass pane, redstone, diamond), unlocked when you get a diamond.
- In-game automated tests on all four targets.
```

In the roadmap, set Plan 2's status to `Done` and change the Plan 2 name cell to `**2 — The machine** ([plan](2026-09-23-plan-2-the-machine.md))`.

- [ ] **Step 6: Commit, push, PR, CI**

```bash
./gradlew "Refresh active project"
```
```bash
git -C /c/Users/benet/mcvending add src docs CLAUDE.md CHANGELOG.md
```
```bash
git -C /c/Users/benet/mcvending commit -m "feat: vending machine recipe and unlock; docs for game tests and generated art"
```
```bash
git -C /c/Users/benet/mcvending push -u origin plan-2/the-machine
```

Write `$SCRATCH/plan2-pr.md` (What changed / Why / Checklist — manual box: "n/a in Plan 2: the rules added here (owner-only breaking/dyeing) get manual pages in Plan 5 — noted in the roadmap"), ending with the session's PR attribution lines. Then:

```bash
gh pr create -R benethcopilot/diamondvending --base main --head plan-2/the-machine --milestone v1.0 --label enhancement --title "Plan 2: The machine — placeable, protected, dyeable 2x2 vending machine" --body-file "$SCRATCH/plan2-pr.md"
```
```bash
gh pr checks -R benethcopilot/diamondvending --watch --interval 30
```
Expected: guard + four `Build …` jobs pass (each now also runs its game tests). Merge happens after the final review (executor's finishing step).
