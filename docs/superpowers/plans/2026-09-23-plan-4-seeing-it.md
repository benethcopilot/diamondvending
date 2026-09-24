# Diamond Vending — Plan 4: Seeing It Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players can see what a machine sells and what it's saying: items on the shelves with price tags, the tray's contents, the LED display (green status, red scrolling problems, 2-second flashes), the blinking warning lamp, the item dropping into the tray, and a hover tooltip — on all four targets.

**Architecture:** Everything a machine shows is decided by plain code that runs anywhere: rules in `core/` (`Display`, `Flash`, `DropAnimation`, unit-tested) and two models in a new `scene/` package — `MachineScene` (flat drawing instructions in front-canvas pixels) and `HoverText` (tooltip lines) — built from the block entity's synced client view and GameTested on the server. The client code is thin: a block entity renderer that turns a `MachineScene` into item, text and quad draw calls (26.1's extract/submit API and 1.21.1's immediate API, side by side under Stonecutter), and a HUD layer that draws `HoverText`. Flashes and the drop animation reach nearby clients as vanilla block events. A Fabric 26.1 client game test builds a stocked machine in a real game window and saves screenshots for review.

**Tech Stack:** as Plans 1–3, plus Fabric API `fabric-client-gametest-api-v1` (26.1 only, test source set).

**Spec:** [`docs/superpowers/specs/2026-09-23-diamond-vending-design.md`](../specs/2026-09-23-diamond-vending-design.md) · **Roadmap:** [`2026-09-23-roadmap.md`](2026-09-23-roadmap.md) · **Previous:** [Plan 3](2026-09-23-plan-3-buying.md)

## Global Constraints

- Everything in Plans 1–3's Global Constraints still holds (mod id and package `diamondvending`, nodes, vcsVersion `26.1-neoforge`, no runtime deps, `core/` has no Minecraft imports, one Gradle node at a time, one shell command per Bash call, branch → PR → squash, write `Identifier`, player-facing keys live in `core/Texts` and `TextsTest` checks en_us.json).
- Branch: `plan-4/seeing-it` (already created; this plan is its first commit).
- If `java` isn't on PATH in the Bash tool, prefix Gradle with `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"`. `$SCRATCH` sources as in Plan 3. Every API in this plan was checked on 2026-09-23 against NeoForge-patched 1.21.1/26.1 sources, vanilla 26.1 (`javap` on `~/.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar`) and the cached Fabric API jars.
- **Client code never loads on a dedicated server.** Client classes live only in `diamondvending/client/` and in the loaders' client entrypoints (`platform/fabric/DiamondVendingFabricClient` — Fabric `client` entrypoint; `platform/neoforge/DiamondVendingNeoForgeClient` — `@Mod(dist = Dist.CLIENT)`). `core/`, `scene/`, `block/`, `shop/` must not import `net.minecraft.client`. The game-test server runs are dedicated servers, so a leak crashes them.
- **Inside Stonecutter `//? if` blocks use `//` comments only** — a `/** … */` inside a block breaks when the block is commented out for the other version.
- **Canvas** (spec §2.2): the front is 32 × 32 pixels, `u` left→right, `v` top→bottom as seen from the front; positions come from `core/MachineLayout`. The renderer maps a canvas pixel to 1/16 block on the front face (`client/FrontCanvas.enter`).
- **Shelf items** are drawn as real item models just in front of the glass, flattened to 20 % thickness (the machine is a solid cube, so "behind the glass" would be inside the block). Recessing the window is art polish for Plan 6.
- **Display** (spec §3.5): green `SELECT ITEM` / `CREDIT <n>` (the viewing player's own credit); every active problem in red, scrolling one after another; a flash (`THANK YOU`, `NEED 3`, `SOLD OUT`, …) for 40 ticks (≈2 s) wins over both. `Display.WINDOW` = 7 characters; longer text scrolls one character per 3 ticks and loops.
- **Lamp** blinks (10 ticks on, 10 off) while any problem is active. **Drop animation**: 10 ticks (≈0.5 s), shelf slot → tray, speeding up.
- **Price tags**: a white label under each shelf slot: `<button> · <price>` plus the currency's icon; `<button> · FREE` for price 0; a red `SOLD OUT` when an owned machine hasn't enough stock.
- **Hover tooltip** (spec §2.3): only when the crosshair is on the machine's **front face**; every active problem's explanation first (red), then the region's lines, then `Owned by <name>` or `Shop machine` (infinite or no owner).
- **Cheap rendering** (spec §2.3): nothing drawn beyond 32 blocks (`getViewDistance`), nothing when the camera is behind the front plane (`Facing.frontVisible`). `shouldRenderOffScreen` is on because the block entity sits in one block of a 2 × 2 machine.
- **Block events**: id 1 = vend (param = button index 0–11; also flashes THANK YOU), id 2 = flash (param = `Flash.ordinal() | number << 8`). Sent from the server with `level.blockEvent`; `VendingMachineBlockEntity.triggerEvent` remembers them on both sides (never saved).
- **Client game test** (Fabric 26.1 only — 1.21.1's Fabric API has no client tests): `./gradlew :26.1-fabric:runClientGametest` opens a game window, builds a machine with commands and saves screenshots to `versions/26.1-fabric/build/clientgametest/screenshots/`. It is a local check (CI runners have no display). Read every screenshot with the Read tool and compare it with the step's Expected description.

## Review Focus

1. **Looking at a machine from behind, the side or far away** — nothing should float in the air or show through the back; distant machines cost nothing. → Task 1, `FacingTest.theFrontIsOnlyVisibleFromInFront`; Task 4 screenshot `machine_back`.
2. **Several problems at once** — the display must cycle through all of them and the tooltip must list all of them, not just the first. → Task 1, `DisplayTest.everyProblemShowsInRed`; Task 5, `problemsAreExplainedAnywhereOnTheFront` (fills the tray *and* the cash box).
3. **Two players looking at the same machine** — each sees their own credit on the display and in the tooltip, never someone else's. → Task 3, `theDisplaySaysSelectItemThenYourCredit`; Task 5, `theCoinSlotReturnAndTrayShowCounts`.
4. **3D block items, enchanted items and tools on the shelves** — they must look right (upright, facing out, not poking through the face). → Task 4 screenshot `machine_front` (oak log, diamond sword, enchanted golden apple).
5. **A dedicated server** — no client class may load there. → every GameTest run is a dedicated server; Task 4 Step 7 greps `scene/`, `core/`, `block/`, `shop/` for `net.minecraft.client`.

---

## File Structure

```
src/main/java/diamondvending/
  core/Flash.java                         the short messages the display flashes
  core/Display.java                       what the LED says, scrolling, lamp blink
  core/DropAnimation.java                 where a vended item is while it falls
  core/Facing.java                        + frontVisible
  core/Texts.java                         + display, tag and hud keys
  block/VendingMachineBlockEntity.java    + block events (send + remember)
  shop/Purchase.java, shop/CoinSlot.java  + send flashes / vend events
  scene/MachineScene.java                 drawing instructions for a machine's front
  scene/HoverText.java                    tooltip lines for what the crosshair is on
  client/FrontCanvas.java                 canvas → pose, culling, glow quads
  client/VendingMachineRenderer.java      26.1 extract/submit and 1.21.1 render, side by side
  client/HoverHud.java                    draws HoverText next to the crosshair
  platform/fabric/DiamondVendingFabricClient.java     Fabric client entrypoint
  platform/neoforge/DiamondVendingNeoForgeClient.java NeoForge client-only @Mod
src/main/resources/fabric.mod.json        + "client" entrypoint
src/main/resources/assets/diamondvending/lang/en_us.json  + display, tag, hud text
src/test/java/diamondvending/core/        DisplayTest, DropAnimationTest, FacingTest
src/gametest/java/diamondvending/gametest/
  DisplayTests.java                       block events, scenes, hover text (+ ALL)
  AllTests.java                           + DisplayTests.ALL
  fabric/FabricDisplayTests.java          Fabric entrypoint for DisplayTests
  fabric/FabricClientTests.java           Fabric 26.1 client game test (screenshots)
  neoforge/NeoForgeDisplayTests.java      NeoForge 1.21.1 @GameTestHolder for DisplayTests
src/gametest/resources/fabric.mod.json    + FabricDisplayTests, + fabric-client-gametest entrypoint
build.fabric.gradle.kts                   + clientGametest run (26.1)
docs/dev-setup.md, CHANGELOG.md, docs/superpowers/plans/2026-09-23-roadmap.md
```

**Adding a display test (Tasks 2, 3, 5):** a `public static void name(GameTestHelper)` in `DisplayTests` + its `ALL` entry, a method in `FabricDisplayTests`, and a method in the 1.21.1 block of `NeoForgeDisplayTests` — the Plan 2/3 pattern.

**Test counts** ("All N required tests passed"): today 56 on 26.1 (55 ours + vanilla's `always_pass`), 55 on 1.21.1. Task 2 +3, Task 3 +6, Task 5 +5.

**Gradle commands** (prefix `JAVA_HOME=…` if needed; one at a time; log to the workspace and read the tail):
- `G26N` = `./gradlew :26.1-neoforge:test :26.1-neoforge:runGameTestServer`
- `G26F` = `./gradlew :26.1-fabric:test :26.1-fabric:runGametest`
- `G121N` = `./gradlew :1.21.1-neoforge:test :1.21.1-neoforge:runGameTestServer`
- `G121F` = `./gradlew :1.21.1-fabric:test :1.21.1-fabric:runGametest`
- `GCLIENT` = `./gradlew :26.1-fabric:runClientGametest` (Task 4 onward; opens a window for a minute or two)

---

### Task 1: Display rules

Deliverable: the pure rules for what the display says, how it scrolls, when the lamp is lit, where a falling item is, and whether the front is visible — unit-tested — plus every display and tag text in en_us.json.

**Files:**
- Create: `src/main/java/diamondvending/core/Flash.java`, `core/Display.java`, `core/DropAnimation.java`
- Modify: `src/main/java/diamondvending/core/Facing.java`, `core/Texts.java`, `src/main/resources/assets/diamondvending/lang/en_us.json`
- Create: `src/test/java/diamondvending/core/DisplayTest.java`, `DropAnimationTest.java`, `FacingTest.java`

**Interfaces:**
- Consumes: `core.Problem`, `core.DenyReason`, `core.MachineLayout` (`shelfSlot(int)`, `TRAY`), `core.Rect` (Plan 1); `core.Texts` (Plan 3).
- Produces: `core.Flash` — enum `THANK_YOU, NOTHING_HERE, SOLD_OUT, TRAY_FULL, CASH_BOX_FULL, CATALOG_MISSING, NEED_MONEY, WRONG_COIN, CREDIT_FULL`; `key()`, `alarm()`, `static Flash of(DenyReason)`.
- Produces: `core.Display` — `FLASH_TICKS = 40`, `WINDOW = 7`, `TICKS_PER_CHARACTER = 3`, `BLINK_TICKS = 10`, `GAP = "   "`; `record Line(List<String> keys, int number, boolean alarm)`; `static Line line(List<Problem> problems, Flash flash, int flashNumber, long ticksSinceFlash, int credit)` (flash may be null); `static String window(String text, long ticks)`; `static boolean lampLit(boolean anyProblem, long ticks)`.
- Produces: `core.DropAnimation` — `TICKS = 10`; `static double[] position(int selection, double ticks)` → `{u, v}` or null once landed.
- Produces: `Facing.frontVisible(int masterX, int masterZ, double x, double z)`.
- Produces: `Texts.SELECT_ITEM`, `Texts.CREDIT`, `Texts.TAG_FREE`, `Texts.TAG_SOLD_OUT`, `Texts.problemDisplay(Problem)`, `Texts.flash(Flash)`; `Texts.all()` includes them.

- [ ] **Step 1: Write the failing unit tests**

`src/test/java/diamondvending/core/DisplayTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTest {
    @Test
    void idleSaysSelectItem() {
        assertEquals(new Display.Line(List.of(Texts.SELECT_ITEM), 0, false), Display.line(List.of(), null, 0, 0, 0));
    }

    @Test
    void aViewerWithCreditSeesTheirCredit() {
        assertEquals(new Display.Line(List.of(Texts.CREDIT), 4, false), Display.line(List.of(), null, 0, 0, 4));
    }

    @Test
    void everyProblemShowsInRed() {
        Display.Line line = Display.line(List.of(Problem.CASH_BOX_FULL, Problem.TRAY_FULL), null, 0, 0, 4);
        assertEquals(List.of(Texts.problemDisplay(Problem.CASH_BOX_FULL), Texts.problemDisplay(Problem.TRAY_FULL)), line.keys());
        assertTrue(line.alarm());
    }

    @Test
    void aFlashWinsForTwoSeconds() {
        List<Problem> problems = List.of(Problem.TRAY_FULL);
        assertEquals(new Display.Line(List.of(Texts.flash(Flash.NEED_MONEY)), 3, true),
                Display.line(problems, Flash.NEED_MONEY, 3, Display.FLASH_TICKS - 1, 0));
        assertEquals(List.of(Texts.problemDisplay(Problem.TRAY_FULL)),
                Display.line(problems, Flash.NEED_MONEY, 3, Display.FLASH_TICKS, 0).keys());
    }

    @Test
    void thankYouIsGreen() {
        assertFalse(Display.line(List.of(), Flash.THANK_YOU, 0, 0, 0).alarm());
    }

    @Test
    void everyRefusalHasAFlash() {
        for (DenyReason reason : DenyReason.values()) assertNotNull(Flash.of(reason));
        assertEquals(Flash.NEED_MONEY, Flash.of(DenyReason.NOT_ENOUGH_MONEY));
    }

    @Test
    void shortTextStaysStill() {
        assertEquals("NEED 3", Display.window("NEED 3", 0));
        assertEquals("NEED 3", Display.window("NEED 3", 999));
    }

    @Test
    void longTextScrollsAndLoops() {
        String text = "SELECT ITEM"; // 11 characters + a 3-space gap = 14 per loop
        assertEquals("SELECT ", Display.window(text, 0));
        assertEquals("ELECT I", Display.window(text, Display.TICKS_PER_CHARACTER));
        assertEquals("M   SEL", Display.window(text, 10L * Display.TICKS_PER_CHARACTER));
        assertEquals("SELECT ", Display.window(text, 14L * Display.TICKS_PER_CHARACTER));
    }

    @Test
    void theLampBlinksOnlyWhileSomethingIsWrong() {
        assertFalse(Display.lampLit(false, 0));
        assertTrue(Display.lampLit(true, 0));
        assertFalse(Display.lampLit(true, Display.BLINK_TICKS));
        assertTrue(Display.lampLit(true, 2L * Display.BLINK_TICKS));
    }
}
```

`src/test/java/diamondvending/core/DropAnimationTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropAnimationTest {
    @Test
    void startsOnItsShelf() {
        double[] at = DropAnimation.position(4, 0);
        assertEquals(MachineLayout.shelfSlot(4).centerU(), at[0], 1e-9);
        assertEquals(MachineLayout.shelfSlot(4).centerV(), at[1], 1e-9);
    }

    @Test
    void landsInTheTrayThenDisappears() {
        double[] almost = DropAnimation.position(4, DropAnimation.TICKS - 0.001);
        assertEquals(MachineLayout.TRAY.centerV(), almost[1], 0.01);
        assertNull(DropAnimation.position(4, DropAnimation.TICKS));
    }

    @Test
    void fallsFasterAndFaster() {
        double v0 = DropAnimation.position(0, 0)[1];
        double v1 = DropAnimation.position(0, 1)[1];
        double v2 = DropAnimation.position(0, 2)[1];
        assertTrue(v2 - v1 > v1 - v0);
    }
}
```

`src/test/java/diamondvending/core/FacingTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacingTest {
    @Test
    void theFrontIsOnlyVisibleFromInFront() {
        // A north-facing machine with its master at x 10, z 20: the front is the z = 20 plane, seen from z < 20.
        assertTrue(Facing.NORTH.frontVisible(10, 20, 10.5, 15));
        assertFalse(Facing.NORTH.frontVisible(10, 20, 10.5, 25));
        // East-facing: the front is the x = 11 plane, seen from x > 11.
        assertTrue(Facing.EAST.frontVisible(10, 20, 14, 20.5));
        assertFalse(Facing.EAST.frontVisible(10, 20, 9, 20.5));
    }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :26.1-neoforge:test`
Expected: FAIL — compilation errors (`cannot find symbol: class Display`, `Flash`, `DropAnimation`, `method frontVisible`).

- [ ] **Step 3: Write the rules**

`src/main/java/diamondvending/core/Flash.java`:

```java
package diamondvending.core;

/** Short messages the display flashes for about 2 seconds, for everyone nearby (spec §3.5 c). */
public enum Flash {
    THANK_YOU("thank_you", false),
    NOTHING_HERE("nothing_here", true),
    SOLD_OUT("sold_out", true),
    TRAY_FULL("tray_full", true),
    CASH_BOX_FULL("cash_box_full", true),
    CATALOG_MISSING("catalog_missing", true),
    NEED_MONEY("need_money", true),
    WRONG_COIN("wrong_coin", true),
    CREDIT_FULL("credit_full", true);

    private final String key;
    private final boolean alarm;

    Flash(String key, boolean alarm) {
        this.key = key;
        this.alarm = alarm;
    }

    /** Stable snake_case id, used to build translation keys. */
    public String key() {
        return key;
    }

    /** Red for trouble, green for good news. */
    public boolean alarm() {
        return alarm;
    }

    /** What the display flashes when a purchase is refused. */
    public static Flash of(DenyReason reason) {
        return switch (reason) {
            case CATALOG_MISSING -> CATALOG_MISSING;
            case EMPTY -> NOTHING_HERE;
            case SOLD_OUT -> SOLD_OUT;
            case TRAY_FULL -> TRAY_FULL;
            case CASH_BOX_FULL -> CASH_BOX_FULL;
            case NOT_ENOUGH_MONEY -> NEED_MONEY;
        };
    }
}
```

`src/main/java/diamondvending/core/Display.java`:

```java
package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/**
 * What the LED display and the warning lamp show (spec §3.5). Pure rules on translation keys and tick counts; the
 * client turns the keys into words and draws them.
 */
public final class Display {
    /** How long a flash (THANK YOU, NEED 3, …) stays up: about 2 seconds. */
    public static final int FLASH_TICKS = 40;
    /** Characters that fit on the display at once; longer text scrolls. */
    public static final int WINDOW = 7;
    /** Scroll speed: one character every this many ticks. */
    public static final int TICKS_PER_CHARACTER = 3;
    /** The warning lamp is on for this many ticks, then off for as many. */
    public static final int BLINK_TICKS = 10;
    /** Space between problems, and before scrolling text starts over. */
    public static final String GAP = "   ";

    private Display() {}

    /** One thing the display says: translation keys shown one after another, their number argument, and whether it's red. */
    public record Line(List<String> keys, int number, boolean alarm) {}

    /**
     * What the display says now (spec §3.5): a flash from the last {@link #FLASH_TICKS} wins; then every active
     * problem, in red; then the viewing player's credit; otherwise "SELECT ITEM".
     *
     * @param flash           the last flash, or null if there hasn't been one
     * @param ticksSinceFlash ticks since that flash
     * @param credit          the viewing player's credit
     */
    public static Line line(List<Problem> problems, Flash flash, int flashNumber, long ticksSinceFlash, int credit) {
        if (flash != null && ticksSinceFlash >= 0 && ticksSinceFlash < FLASH_TICKS) {
            return new Line(List.of(Texts.flash(flash)), flashNumber, flash.alarm());
        }
        if (!problems.isEmpty()) {
            List<String> keys = new ArrayList<>();
            for (Problem problem : problems) keys.add(Texts.problemDisplay(problem));
            return new Line(List.copyOf(keys), 0, true);
        }
        if (credit > 0) return new Line(List.of(Texts.CREDIT), credit, false);
        return new Line(List.of(Texts.SELECT_ITEM), 0, false);
    }

    /** The part of {@code text} on the display now: text that fits stays still; longer text scrolls left and loops, like an LED sign. */
    public static String window(String text, long ticks) {
        if (text.length() <= WINDOW) return text;
        String loop = text + GAP;
        int start = (int) Math.floorMod(ticks / TICKS_PER_CHARACTER, (long) loop.length());
        return (loop + loop).substring(start, start + WINDOW);
    }

    /** Whether the warning lamp is lit now: it blinks while any problem is active (spec §3.5 b). */
    public static boolean lampLit(boolean anyProblem, long ticks) {
        return anyProblem && Math.floorMod(ticks / BLINK_TICKS, 2L) == 0;
    }
}
```

`src/main/java/diamondvending/core/DropAnimation.java`:

```java
package diamondvending.core;

/** The bought item dropping from its shelf into the tray (spec §2.3): about half a second, speeding up as it falls. */
public final class DropAnimation {
    public static final int TICKS = 10;

    private DropAnimation() {}

    /** Where button {@code selection}'s item is, {@code {u, v}} on the canvas, {@code ticks} after the sale; null once it has landed. */
    public static double[] position(int selection, double ticks) {
        if (ticks < 0 || ticks >= TICKS) return null;
        Rect from = MachineLayout.shelfSlot(selection);
        Rect to = MachineLayout.TRAY;
        double t = ticks / TICKS;
        double u = from.centerU() + (to.centerU() - from.centerU()) * t;
        double v = from.centerV() + (to.centerV() - from.centerV()) * t * t;
        return new double[] {u, v};
    }
}
```

In `src/main/java/diamondvending/core/Facing.java`, add before the class's closing brace:

```java

    /** Whether a camera at (x, z) is in front of the machine's front face; the master block is at (masterX, masterZ). */
    public boolean frontVisible(int masterX, int masterZ, double x, double z) {
        double faceX = masterX + 0.5 + dx * 0.5;
        double faceZ = masterZ + 0.5 + dz * 0.5;
        return (x - faceX) * dx + (z - faceZ) * dz > 0;
    }
```

Replace `src/main/java/diamondvending/core/Texts.java` with:

```java
package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation keys for everything the mod tells players (spec §3.5). Plain strings, so {@code TextsTest} can check
 * that en_us.json has text for every one of them.
 */
public final class Texts {
    public static final String OWNER_ONLY = "message.diamondvending.owner_only";
    public static final String WRONG_CURRENCY = "message.diamondvending.wrong_currency";
    public static final String CREDIT_FULL = "message.diamondvending.credit_full";
    public static final String BUTTON_EMPTY = "message.diamondvending.button_empty";
    public static final String SOLD_OUT = "message.diamondvending.sold_out";
    public static final String NEED_MONEY = "message.diamondvending.need_money";
    /** Prefix of the keys that name the default currency: {@code .one}, {@code .many}, {@code .name} (see shop/Currency). */
    public static final String DIAMOND = "currency.diamondvending.minecraft.diamond";
    // The display (spec §3.5 a) and the price tags under the shelves.
    public static final String SELECT_ITEM = "display.diamondvending.select_item";
    public static final String CREDIT = "display.diamondvending.credit";
    public static final String TAG_FREE = "tag.diamondvending.free";
    public static final String TAG_SOLD_OUT = "tag.diamondvending.sold_out";

    private Texts() {}

    /** A problem's plain-language explanation (spec §3.5 b), e.g. "The tray is full! …". */
    public static String explanation(Problem problem) {
        return "problem.diamondvending." + problem.key() + ".explanation";
    }

    /** What the display scrolls for a problem (spec §3.5 b), e.g. "TRAY FULL - TAKE YOUR ITEMS". */
    public static String problemDisplay(Problem problem) {
        return "problem.diamondvending." + problem.key() + ".display";
    }

    /** What the display flashes, e.g. "NEED %s". */
    public static String flash(Flash flash) {
        return "display.diamondvending." + flash.key();
    }

    /** Every key a player can see. */
    public static List<String> all() {
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY, WRONG_CURRENCY, CREDIT_FULL, BUTTON_EMPTY, SOLD_OUT, NEED_MONEY,
                SELECT_ITEM, CREDIT, TAG_FREE, TAG_SOLD_OUT));
        for (Problem problem : Problem.values()) {
            keys.add(explanation(problem));
            keys.add(problemDisplay(problem));
        }
        for (Flash flash : Flash.values()) keys.add(flash(flash));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
}
```

In `en_us.json`, add after the last `problem.diamondvending.*.explanation` line (keep every existing line):

```json
  "problem.diamondvending.catalog_missing.display": "CATALOG MISSING - ASK AN ADMIN",
  "problem.diamondvending.not_set_up.display": "NOT SET UP YET",
  "problem.diamondvending.cash_box_full.display": "CASH BOX FULL - OWNER MUST EMPTY IT",
  "problem.diamondvending.sold_out.display": "SOLD OUT - OWNER MUST RESTOCK",
  "problem.diamondvending.tray_full.display": "TRAY FULL - TAKE YOUR ITEMS",
  "display.diamondvending.select_item": "SELECT ITEM",
  "display.diamondvending.credit": "CREDIT %s",
  "display.diamondvending.thank_you": "THANK YOU",
  "display.diamondvending.nothing_here": "NOTHING HERE",
  "display.diamondvending.sold_out": "SOLD OUT",
  "display.diamondvending.tray_full": "TRAY FULL",
  "display.diamondvending.cash_box_full": "CASH BOX FULL",
  "display.diamondvending.catalog_missing": "NO CATALOG",
  "display.diamondvending.need_money": "NEED %s",
  "display.diamondvending.wrong_coin": "WRONG COIN",
  "display.diamondvending.credit_full": "CREDIT FULL",
  "tag.diamondvending.free": "FREE",
  "tag.diamondvending.sold_out": "SOLD OUT",
```

- [ ] **Step 4: Run them to verify they pass**

Run: `./gradlew :26.1-neoforge:test`
Expected: PASS — `DisplayTest` 9/9, `DropAnimationTest` 3/3, `FacingTest` 1/1, `TextsTest` 1/1 (now covering the new keys); all older unit tests still green.

- [ ] **Step 5: Run the unit tests on the other three nodes**

Run, one at a time: `./gradlew :26.1-fabric:test`, `./gradlew :1.21.1-neoforge:test`, `./gradlew :1.21.1-fabric:test`
Expected: BUILD SUCCESSFUL each.

- [ ] **Step 6: Commit**

Run: `./gradlew "Refresh active project"`; `git -C /c/Users/benet/mcvending status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: display, lamp and drop-animation rules"
```

---

### Task 2: Block events for flashes and the drop

Deliverable: purchases, refusals and coin-slot problems send block events; every nearby machine block entity (server and client) remembers the last flash and the last vend.

**Files:**
- Modify: `src/main/java/diamondvending/block/VendingMachineBlockEntity.java`, `shop/Purchase.java`, `shop/CoinSlot.java`
- Create: `src/gametest/java/diamondvending/gametest/DisplayTests.java`, `fabric/FabricDisplayTests.java`, `neoforge/NeoForgeDisplayTests.java`
- Modify: `src/gametest/java/diamondvending/gametest/AllTests.java`, `src/gametest/resources/fabric.mod.json`

**Interfaces:**
- Consumes: `core.Flash` (Task 1); `BuyingTests.placeMachine/appleMachine/buyerWith/pressButton/click/registries/reload/translation` (Plan 3, package-private static).
- Produces on `VendingMachineBlockEntity`: `EVENT_VEND = 1`, `EVENT_FLASH = 2`; `sendVend(int selection)`, `sendFlash(Flash flash, int number)` (server); `triggerEvent(int, int)` override; `lastFlash(): Flash` (null if none), `lastFlashNumber(): int`, `lastFlashTime(): long`, `lastVendSelection(): int` (−1 if none), `lastVendTime(): long`.
- Produces: `DisplayTests.ALL`, `DisplayTests.clientView(GameTestHelper, VendingMachineBlockEntity)`.

- [ ] **Step 1: Write the failing GameTests and their plumbing**

`src/gametest/java/diamondvending/gametest/DisplayTests.java`:

```java
package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Flash;
import diamondvending.core.MachineLayout;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game tests for what machines show (Plan 4). Each test also needs a method in {@code fabric/FabricDisplayTests}
 * and (for 1.21.1) {@code neoforge/NeoForgeDisplayTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 */
public final class DisplayTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("a_purchase_says_thank_you_and_drops_the_item", DisplayTests::aPurchaseSaysThankYouAndDropsTheItem),
            Map.entry("a_refused_purchase_flashes_the_reason", DisplayTests::aRefusedPurchaseFlashesTheReason),
            Map.entry("the_coin_slot_flashes_wrong_coin", DisplayTests::theCoinSlotFlashesWrongCoin));

    private DisplayTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** What a client knows about the machine: its update tag, loaded into a fresh block entity (which has no level). */
    static VendingMachineBlockEntity clientView(GameTestHelper helper, VendingMachineBlockEntity machine) {
        CompoundTag update = machine.getUpdateTag(BuyingTests.registries(helper));
        update.putString("id", "diamondvending:vending_machine");
        return BuyingTests.reload(helper, machine, update);
    }

    // ---- block events --------------------------------------------------------------------------------------------

    public static void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.pressButton(helper, BuyingTests.buyerWith(helper, 5), 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.lastFlash() == Flash.THANK_YOU, "the display should say thank you, not " + machine.lastFlash());
            helper.assertTrue(machine.lastVendSelection() == 0, "button 1's item should drop, not " + machine.lastVendSelection());
        });
    }

    public static void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.pressButton(helper, BuyingTests.buyerWith(helper, 1), 0);
        helper.succeedWhen(() -> helper.assertTrue(machine.lastFlash() == Flash.NEED_MONEY && machine.lastFlashNumber() == 3,
                "the display should flash NEED 3, not " + machine.lastFlash() + " " + machine.lastFlashNumber()));
    }

    public static void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
        BuyingTests.click(helper, buyer, MachineLayout.COIN_SLOT);
        helper.succeedWhen(() -> helper.assertTrue(machine.lastFlash() == Flash.WRONG_COIN,
                "the display should flash WRONG COIN, not " + machine.lastFlash()));
    }
}
```

`src/gametest/java/diamondvending/gametest/fabric/FabricDisplayTests.java`:

```java
package diamondvending.gametest.fabric;

import diamondvending.gametest.DisplayTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/** Fabric entrypoint for {@link DisplayTests} (see {@link FabricGameTests} for why the annotations differ per version). */
public final class FabricDisplayTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        DisplayTests.aPurchaseSaysThankYouAndDropsTheItem(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        DisplayTests.aRefusedPurchaseFlashesTheReason(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        DisplayTests.theCoinSlotFlashesWrongCoin(helper);
    }
}
```

`src/gametest/java/diamondvending/gametest/neoforge/NeoForgeDisplayTests.java`:

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.DisplayTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * {@link DisplayTests} for NeoForge 1.21.1, found through {@code @GameTestHolder} like {@link NeoForgeGameTests}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeDisplayTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        DisplayTests.aPurchaseSaysThankYouAndDropsTheItem(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        DisplayTests.aRefusedPurchaseFlashesTheReason(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        DisplayTests.theCoinSlotFlashesWrongCoin(helper);
    }
    *///?}
}
```

In `AllTests.java`, change the list to `List.of(MachineTests.ALL, BuyingTests.ALL, DisplayTests.ALL)`.

In `src/gametest/resources/fabric.mod.json`, add `"diamondvending.gametest.fabric.FabricDisplayTests"` to the `fabric-gametest` entrypoint list (after `FabricBuyingTests`).

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors (`cannot find symbol: method lastFlash()`, `lastVendSelection()`, `lastFlashNumber()`).

- [ ] **Step 3: Teach the block entity to remember block events**

In `VendingMachineBlockEntity.java`, add `import diamondvending.core.Flash;`. After the `REPEAT_TICKS` constant add:

```java
    /** Block event: button {@code param}'s item was sold — it drops on screen and the display says THANK YOU. */
    public static final int EVENT_VEND = 1;
    /** Block event: the display flashes {@code Flash.values()[param & 0xFF]} with the number {@code param >>> 8}. */
    public static final int EVENT_FLASH = 2;
```

After the `lastPresses` field add:

```java
    // What the display last showed, from block events. Kept on both sides, never saved.
    private Flash lastFlash;
    private int lastFlashNumber;
    private long lastFlashTime;
    private int lastVendSelection = -1;
    private long lastVendTime;
```

Add these methods after `isRepeatPress`:

```java
    /** Server side: makes every nearby display flash (spec §3.5 c); {@code number} fills texts like "NEED %s". */
    public void sendFlash(Flash flash, int number) {
        if (level != null) level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FLASH, flash.ordinal() | (number << 8));
    }

    /** Server side: button {@code selection}'s item drops into the tray on every nearby screen (spec §2.3). */
    public void sendVend(int selection) {
        if (level != null) level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_VEND, selection);
    }

    /** Remembers the machine's block events, on the server and on each client (the client draws from them). */
    @Override
    public boolean triggerEvent(int id, int param) {
        long now = level != null ? level.getGameTime() : 0;
        if (id == EVENT_VEND) {
            lastVendSelection = param >= 0 && param < selections.length ? param : -1;
            lastVendTime = now;
            lastFlash = Flash.THANK_YOU;
            lastFlashNumber = 0;
            lastFlashTime = now;
            return true;
        }
        if (id == EVENT_FLASH) {
            int ordinal = param & 0xFF;
            if (ordinal >= Flash.values().length) return false;
            lastFlash = Flash.values()[ordinal];
            lastFlashNumber = param >>> 8;
            lastFlashTime = now;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** The last flash, or null if there hasn't been one. */
    public Flash lastFlash() {
        return lastFlash;
    }

    public int lastFlashNumber() {
        return lastFlashNumber;
    }

    /** Game time of the last flash. */
    public long lastFlashTime() {
        return lastFlashTime;
    }

    /** The button whose item last dropped, or −1. */
    public int lastVendSelection() {
        return lastVendSelection;
    }

    /** Game time of the last sale. */
    public long lastVendTime() {
        return lastVendTime;
    }
```

- [ ] **Step 4: Run to verify they fail for the right reason**

Run: `G26N`
Expected: FAIL — 3 failures: "the display should say thank you, not null", "the display should flash NEED 3, not null 0", "the display should flash WRONG COIN, not null" (nothing sends events yet; `succeedWhen` gives up at the 100-tick timeout).

- [ ] **Step 5: Send the events**

In `shop/Purchase.java`, add `import diamondvending.core.Flash;`, then:
- change `case PurchaseDecision.Approved approved -> complete(machine, player, selection, approved);` to `case PurchaseDecision.Approved approved -> complete(machine, player, index, selection, approved);`
- change the `complete` signature to `private static void complete(VendingMachineBlockEntity machine, Player player, int index, Selection selection, PurchaseDecision.Approved approved) {` and add `machine.sendVend(index);` as its last line (after `machine.changed();`);
- add as the last line of `refuse` (after `Messages.actionBar(player, message);`):

```java
        machine.sendFlash(Flash.of(reason), reason == DenyReason.NOT_ENOUGH_MONEY ? machine.getSelection(index).price() : 0);
```

In `shop/CoinSlot.java`, add `import diamondvending.core.Flash;`, then add `machine.sendFlash(Flash.WRONG_COIN, 0);` right after the wrong-currency `Messages.actionBar(…)` line (before `return;`), and `machine.sendFlash(Flash.CREDIT_FULL, 0);` right after the credit-full `Messages.actionBar(…)` line.

- [ ] **Step 6: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 59 required tests passed :)`.

- [ ] **Step 7: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 59 on 26.1 Fabric, 58 on both 1.21.1 nodes.

- [ ] **Step 8: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: block events for display flashes and the drop animation"
```

---

### Task 3: The machine scene

Deliverable: `MachineScene.of(...)` turns what a client knows about a machine into drawing instructions — shelf items, price labels and tags, tray items, the LED text, the lamp and the falling item — GameTested on the server.

**Files:**
- Create: `src/main/java/diamondvending/scene/MachineScene.java`
- Modify: `DisplayTests.java`, `FabricDisplayTests.java`, `NeoForgeDisplayTests.java`

**Interfaces:**
- Consumes: `Display`, `DropAnimation`, `Flash`, `MachineLayout`, `Texts` (Task 1); `VendingMachineBlockEntity` client view — `getSelection`, `isInfinite`, `syncedStockCount`, `syncedCredit`, `syncedProblems`, `tray`, `currency`, `lastFlash*`, `lastVend*`, `triggerEvent` (Plan 3 + Task 2); `Currency.displayItem()`.
- Produces: `scene.MachineScene` — `record MachineScene(List<Item> items, List<Text> texts, List<Glow> glows)`, `static final MachineScene EMPTY`, `enum Align { LEFT, CENTER, RIGHT }`, `record Item(ItemStack stack, float u, float v, float size)`, `record Text(String text, float u, float v, float height, int color, Align align)`, `record Glow(float u0, float v0, float u1, float v1, int color)`, colors `LED_OK`, `LED_ALARM`, `TAG_TEXT`, `TAG_ALARM`, `LABEL`, `LAMP`, sizes `SHELF_ITEM`, `TRAY_ITEM`, `ICON`, `TAG_TEXT_HEIGHT`, `LED_TEXT_HEIGHT`, and `static MachineScene of(VendingMachineBlockEntity machine, UUID viewer, float time, Function<Component, String> words)`.
- Produces (tests): `DisplayTests.KEYS` — a `words` function that returns a translation's key and arguments instead of a language's words.

- [ ] **Step 1: Write the failing GameTests**

In `DisplayTests.java`, add imports:

```java
import diamondvending.core.Display;
import diamondvending.core.DropAnimation;
import diamondvending.core.Problem;
import diamondvending.core.Rect;
import diamondvending.core.Texts;
import diamondvending.scene.MachineScene;
import diamondvending.shop.Selection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
```

Add to `ALL`:

```java
            Map.entry("shelves_show_what_each_button_sells", DisplayTests::shelvesShowWhatEachButtonSells),
            Map.entry("the_tray_shows_whats_waiting", DisplayTests::theTrayShowsWhatsWaiting),
            Map.entry("the_display_says_select_item_then_your_credit", DisplayTests::theDisplaySaysSelectItemThenYourCredit),
            Map.entry("problems_turn_the_display_red_and_light_the_lamp", DisplayTests::problemsTurnTheDisplayRedAndLightTheLamp),
            Map.entry("a_flash_shows_for_two_seconds", DisplayTests::aFlashShowsForTwoSeconds),
            Map.entry("a_bought_item_falls_into_the_tray", DisplayTests::aBoughtItemFallsIntoTheTray)
```

Add helpers:

```java
    /** Words for scenes in tests: a translation's key and arguments, so tests don't depend on a language being loaded. */
    static final Function<Component, String> KEYS = component -> component.getContents() instanceof TranslatableContents t
            ? t.getKey() + Arrays.toString(t.getArgs()) : component.getString();

    static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    static void assertItem(GameTestHelper helper, MachineScene scene, Item item, double u, double v) {
        helper.assertTrue(scene.items().stream().anyMatch(i -> i.stack().is(item) && near(i.u(), u) && near(i.v(), v)),
                "expected " + item + " at (" + u + ", " + v + ") in " + scene.items());
    }

    static int count(MachineScene scene, Item item) {
        return (int) scene.items().stream().filter(i -> i.stack().is(item)).count();
    }

    /** The LED display's text. */
    static MachineScene.Text led(GameTestHelper helper, MachineScene scene) {
        return scene.texts().stream().filter(t -> near(t.v(), MachineLayout.DISPLAY.centerV())).findFirst()
                .orElseThrow(() -> new AssertionError("the scene has no display text: " + scene.texts()));
    }

    static void assertLed(GameTestHelper helper, MachineScene scene, Component says, long ticks, int color) {
        MachineScene.Text led = led(helper, scene);
        String expected = Display.window(KEYS.apply(says), ticks);
        helper.assertTrue(led.text().equals(expected) && led.color() == color,
                "the display should show \"" + expected + "\" in " + Integer.toHexString(color) + ", but shows \"" + led.text()
                        + "\" in " + Integer.toHexString(led.color()));
    }

    static boolean lampLit(MachineScene scene) {
        return scene.glows().stream().anyMatch(g -> g.color() == MachineScene.LAMP);
    }
```

Add the tests:

```java
    // ---- scene ---------------------------------------------------------------------------------------------------

    public static void shelvesShowWhatEachButtonSells(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // button 1: 2 apples for 3, 10 in stock
        machine.setSelection(4, Selection.of(new ItemStack(Items.BREAD), 0));
        machine.stock().set(1, new ItemStack(Items.BREAD, 5));
        machine.setSelection(11, Selection.of(new ItemStack(Items.CAKE), 5)); // none in stock
        MachineScene scene = MachineScene.of(clientView(helper, machine), UUID.randomUUID(), 0, KEYS);
        for (int button : new int[] {0, 4, 11}) {
            Rect slot = MachineLayout.shelfSlot(button);
            Item item = button == 0 ? Items.APPLE : button == 4 ? Items.BREAD : Items.CAKE;
            assertItem(helper, scene, item, slot.centerU(), slot.centerV());
        }
        helper.assertTrue(scene.texts().stream().anyMatch(t -> t.text().equals("1 · 3")), "button 1's tag should read 1 · 3");
        helper.assertTrue(count(scene, Items.DIAMOND) == 1, "the priced tag shows the currency's icon");
        String free = "5 · " + KEYS.apply(Component.translatable(Texts.TAG_FREE));
        helper.assertTrue(scene.texts().stream().anyMatch(t -> t.text().equals(free)), "a price of 0 reads FREE");
        String soldOut = KEYS.apply(Component.translatable(Texts.TAG_SOLD_OUT));
        helper.assertTrue(scene.texts().stream().anyMatch(t -> t.text().equals(soldOut) && t.color() == MachineScene.TAG_ALARM),
                "button 12, with nothing in stock, is marked SOLD OUT in red");
        helper.assertTrue(scene.glows().stream().filter(g -> g.color() == MachineScene.LABEL).count() == 3, "each set-up button has a white label");
        helper.succeed();
    }

    public static void theTrayShowsWhatsWaiting(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        machine.tray().set(0, new ItemStack(Items.BREAD, 3));
        machine.tray().set(4, new ItemStack(Items.COOKIE, 1));
        MachineScene scene = MachineScene.of(clientView(helper, machine), UUID.randomUUID(), 0, KEYS);
        Rect tray = MachineLayout.TRAY;
        assertItem(helper, scene, Items.BREAD, tray.u0() + 1, tray.centerV());
        assertItem(helper, scene, Items.COOKIE, tray.u0() + 1 + 4 * 2, tray.centerV());
        helper.succeed();
    }

    /** Spec §3.5 a: each player's display shows their own credit. */
    public static void theDisplaySaysSelectItemThenYourCredit(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        UUID buyer = UUID.randomUUID();
        machine.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));
        VendingMachineBlockEntity client = clientView(helper, machine);
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), 0, KEYS), Component.translatable(Texts.SELECT_ITEM, 0), 0, MachineScene.LED_OK);
        assertLed(helper, MachineScene.of(client, buyer, 0, KEYS), Component.translatable(Texts.CREDIT, 4), 0, MachineScene.LED_OK);
        helper.succeed();
    }

    public static void problemsTurnTheDisplayRedAndLightTheLamp(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.fill(machine.tray(), Items.COBBLESTONE);
        VendingMachineBlockEntity client = clientView(helper, machine);
        MachineScene lit = MachineScene.of(client, UUID.randomUUID(), 0, KEYS);
        assertLed(helper, lit, Component.translatable(Texts.problemDisplay(Problem.TRAY_FULL), 0), 0, MachineScene.LED_ALARM);
        helper.assertTrue(lampLit(lit), "the lamp should be lit at the start of a blink");
        helper.assertFalse(lampLit(MachineScene.of(client, UUID.randomUUID(), Display.BLINK_TICKS, KEYS)), "and dark half a second later");
        helper.succeed();
    }

    public static void aFlashShowsForTwoSeconds(GameTestHelper helper) {
        VendingMachineBlockEntity client = clientView(helper, BuyingTests.appleMachine(helper));
        client.triggerEvent(VendingMachineBlockEntity.EVENT_FLASH, Flash.NEED_MONEY.ordinal() | 3 << 8); // at game time 0
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), 5, KEYS),
                Component.translatable(Texts.flash(Flash.NEED_MONEY), 3), 5, MachineScene.LED_ALARM);
        long later = Display.FLASH_TICKS + 5;
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), later, KEYS),
                Component.translatable(Texts.SELECT_ITEM, 0), later, MachineScene.LED_OK);
        helper.succeed();
    }

    public static void aBoughtItemFallsIntoTheTray(GameTestHelper helper) {
        VendingMachineBlockEntity client = clientView(helper, BuyingTests.appleMachine(helper));
        client.triggerEvent(VendingMachineBlockEntity.EVENT_VEND, 0); // button 1, at game time 0
        MachineScene falling = MachineScene.of(client, UUID.randomUUID(), 5, KEYS);
        double[] at = DropAnimation.position(0, 5);
        helper.assertTrue(count(falling, Items.APPLE) == 2, "the shelf apple plus one falling");
        assertItem(helper, falling, Items.APPLE, at[0], at[1]);
        helper.assertTrue(count(MachineScene.of(client, UUID.randomUUID(), DropAnimation.TICKS, KEYS), Items.APPLE) == 1,
                "once it lands only the shelf apple is left");
        helper.succeed();
    }
```

Add to `FabricDisplayTests.java` (after the existing methods):

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void shelvesShowWhatEachButtonSells(GameTestHelper helper) {
        DisplayTests.shelvesShowWhatEachButtonSells(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theTrayShowsWhatsWaiting(GameTestHelper helper) {
        DisplayTests.theTrayShowsWhatsWaiting(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theDisplaySaysSelectItemThenYourCredit(GameTestHelper helper) {
        DisplayTests.theDisplaySaysSelectItemThenYourCredit(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void problemsTurnTheDisplayRedAndLightTheLamp(GameTestHelper helper) {
        DisplayTests.problemsTurnTheDisplayRedAndLightTheLamp(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aFlashShowsForTwoSeconds(GameTestHelper helper) {
        DisplayTests.aFlashShowsForTwoSeconds(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aBoughtItemFallsIntoTheTray(GameTestHelper helper) {
        DisplayTests.aBoughtItemFallsIntoTheTray(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeDisplayTests.java` (before `*///?}`):

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void shelvesShowWhatEachButtonSells(GameTestHelper helper) {
        DisplayTests.shelvesShowWhatEachButtonSells(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theTrayShowsWhatsWaiting(GameTestHelper helper) {
        DisplayTests.theTrayShowsWhatsWaiting(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theDisplaySaysSelectItemThenYourCredit(GameTestHelper helper) {
        DisplayTests.theDisplaySaysSelectItemThenYourCredit(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void problemsTurnTheDisplayRedAndLightTheLamp(GameTestHelper helper) {
        DisplayTests.problemsTurnTheDisplayRedAndLightTheLamp(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aFlashShowsForTwoSeconds(GameTestHelper helper) {
        DisplayTests.aFlashShowsForTwoSeconds(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aBoughtItemFallsIntoTheTray(GameTestHelper helper) {
        DisplayTests.aBoughtItemFallsIntoTheTray(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation error, `package diamondvending.scene does not exist`.

- [ ] **Step 3: Build the scene**

`src/main/java/diamondvending/scene/MachineScene.java`:

```java
package diamondvending.scene;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Display;
import diamondvending.core.DropAnimation;
import diamondvending.core.MachineLayout;
import diamondvending.core.Problem;
import diamondvending.core.Rect;
import diamondvending.core.Texts;
import diamondvending.shop.Selection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Everything drawn on a machine's front (spec §2.3), as flat instructions in canvas pixels — u left→right, v top→bottom
 * on the 32 × 32 front (spec §2.2). Built from what a client knows about the machine, so every loader and version draws
 * the same thing and the server can test it; the renderer only turns it into draw calls.
 */
public record MachineScene(List<Item> items, List<Text> texts, List<Glow> glows) {
    public static final MachineScene EMPTY = new MachineScene(List.of(), List.of(), List.of());

    public static final int LED_OK = 0xFF3CFF6A;
    public static final int LED_ALARM = 0xFFFF3B3B;
    public static final int TAG_TEXT = 0xFF1E1E1E;
    public static final int TAG_ALARM = 0xFFC00000;
    public static final int LABEL = 0xFFF4F4F4;
    public static final int LAMP = 0xFFFF2A2A;

    public static final float SHELF_ITEM = 3.2F;
    public static final float TRAY_ITEM = 1.8F;
    public static final float ICON = 1.1F;
    public static final float TAG_TEXT_HEIGHT = 0.9F;
    public static final float LED_TEXT_HEIGHT = 0.85F;

    public enum Align { LEFT, CENTER, RIGHT }

    /** An item model centred at (u, v), {@code size} pixels across. */
    public record Item(ItemStack stack, float u, float v, float size) {}

    /** One line of text, {@code height} pixels tall, whose middle is at v; u is its left edge, centre or right edge. ARGB colour. */
    public record Text(String text, float u, float v, float height, int color, Align align) {}

    /** A flat glowing rectangle: price labels and the warning lamp. ARGB colour. */
    public record Glow(float u0, float v0, float u1, float v1, int color) {}

    /**
     * The scene for one viewer at one moment.
     *
     * @param machine what the client knows about the machine (its synced block entity)
     * @param viewer  whose credit the display shows (spec §3.5 a)
     * @param time    game time, with the fraction of the current tick
     * @param words   turns a translation into words: {@code Component::getString} on the client
     */
    public static MachineScene of(VendingMachineBlockEntity machine, UUID viewer, float time, Function<Component, String> words) {
        List<Item> items = new ArrayList<>();
        List<Text> texts = new ArrayList<>();
        List<Glow> glows = new ArrayList<>();
        long ticks = (long) Math.floor(time);
        ItemStack coin = new ItemStack(machine.currency().displayItem());

        // Shelves: each set-up button's item, with a white label underneath: "7 · 3" and the currency's icon.
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Selection selection = machine.getSelection(i);
            if (!selection.isSetUp()) continue;
            Rect slot = MachineLayout.shelfSlot(i);
            float u = (float) slot.centerU();
            float tagV = (float) slot.v1() + 0.8F;
            items.add(new Item(selection.template(), u, (float) slot.centerV(), SHELF_ITEM));
            glows.add(new Glow(u - 3.3F, tagV - 0.55F, u + 2.8F, tagV + 0.55F, LABEL));
            boolean soldOut = !machine.isInfinite() && machine.syncedStockCount(i) < selection.quantity();
            if (soldOut) {
                texts.add(new Text(words.apply(Component.translatable(Texts.TAG_SOLD_OUT)), u - 0.25F, tagV, TAG_TEXT_HEIGHT, TAG_ALARM, Align.CENTER));
            } else if (selection.price() == 0) {
                texts.add(new Text((i + 1) + " · " + words.apply(Component.translatable(Texts.TAG_FREE)), u - 0.25F, tagV, TAG_TEXT_HEIGHT, TAG_TEXT, Align.CENTER));
            } else {
                texts.add(new Text((i + 1) + " · " + selection.price(), u + 1.4F, tagV, TAG_TEXT_HEIGHT, TAG_TEXT, Align.RIGHT));
                items.add(new Item(coin, u + 2.1F, tagV, ICON));
            }
        }

        // The tray: what's waiting, side by side.
        for (int k = 0; k < machine.tray().size(); k++) {
            ItemStack stack = machine.tray().get(k);
            if (!stack.isEmpty()) items.add(new Item(stack, (float) MachineLayout.TRAY.u0() + 1 + k * 2, (float) MachineLayout.TRAY.centerV(), TRAY_ITEM));
        }

        // The item that was just bought, dropping from its shelf into the tray.
        int vended = machine.lastVendSelection();
        if (vended >= 0 && machine.getSelection(vended).isSetUp()) {
            double[] at = DropAnimation.position(vended, time - machine.lastVendTime());
            if (at != null) items.add(new Item(machine.getSelection(vended).template(), (float) at[0], (float) at[1], SHELF_ITEM));
        }

        // The LED display and the warning lamp (spec §3.5).
        List<Problem> problems = machine.syncedProblems();
        Display.Line line = Display.line(problems, machine.lastFlash(), machine.lastFlashNumber(), ticks - machine.lastFlashTime(),
                machine.syncedCredit(viewer));
        String full = line.keys().stream()
                .map(key -> words.apply(Component.translatable(key, line.number())))
                .collect(Collectors.joining(Display.GAP));
        boolean scrolls = full.length() > Display.WINDOW;
        Rect display = MachineLayout.DISPLAY;
        texts.add(new Text(Display.window(full, ticks),
                scrolls ? (float) display.u0() + 0.2F : (float) display.centerU(), (float) display.centerV(),
                LED_TEXT_HEIGHT, line.alarm() ? LED_ALARM : LED_OK, scrolls ? Align.LEFT : Align.CENTER));
        if (Display.lampLit(!problems.isEmpty(), ticks)) {
            Rect lamp = MachineLayout.LAMP;
            glows.add(new Glow((float) lamp.u0(), (float) lamp.v0(), (float) lamp.u1(), (float) lamp.v1(), LAMP));
        }
        return new MachineScene(List.copyOf(items), List.copyOf(texts), List.copyOf(glows));
    }
}
```

- [ ] **Step 4: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 65 required tests passed :)`.

- [ ] **Step 5: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 65 on 26.1 Fabric, 64 on both 1.21.1 nodes.

- [ ] **Step 6: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: machine scene — shelves, tags, tray, display, lamp, drop"
```

---

### Task 4: The renderer, and a client test that looks at it

Deliverable: the machine's front is drawn in the world on all four targets, registered from client-only entrypoints; a Fabric 26.1 client game test builds a stocked machine and saves screenshots that show it working.

**Files:**
- Create: `src/main/java/diamondvending/client/FrontCanvas.java`, `client/VendingMachineRenderer.java`
- Create: `src/main/java/diamondvending/platform/fabric/DiamondVendingFabricClient.java`, `platform/neoforge/DiamondVendingNeoForgeClient.java`
- Modify: `src/main/resources/fabric.mod.json`
- Create: `src/gametest/java/diamondvending/gametest/fabric/FabricClientTests.java`
- Modify: `src/gametest/resources/fabric.mod.json`, `build.fabric.gradle.kts`, `docs/dev-setup.md`

**Interfaces:**
- Consumes: `MachineScene` (Task 3); `Facing.frontVisible` (Task 1); `VendingMachineBlock.FACING`, `ModContent.VENDING_MACHINE_BLOCK_ENTITY` (Plan 2).
- Produces: `client.VendingMachineRenderer(BlockEntityRendererProvider.Context)`; `client.FrontCanvas` (package-private helpers); `DiamondVendingFabricClient` (Fabric `client` entrypoint) and `DiamondVendingNeoForgeClient` (NeoForge `@Mod(dist = CLIENT)`), each with a constructor/initializer that Task 5 extends with the HUD.
- Produces: Gradle task `:26.1-fabric:runClientGametest`; `FabricClientTests` (Task 5 adds hover screenshots).

- [ ] **Step 1: Write the client game test (visual RED)**

`src/gametest/java/diamondvending/gametest/fabric/FabricClientTests.java`:

```java
package diamondvending.gametest.fabric;

//? if >=26.1 {
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;
//?}

/**
 * Client game test, Fabric 26.1 only (older Fabric API has no client tests). Builds a stocked machine in a real game
 * window and saves screenshots for a person to check: {@code ./gradlew :26.1-fabric:runClientGametest}, then look in
 * versions/26.1-fabric/build/clientgametest/screenshots/.
 */
//? if >=26.1 {
public final class FabricClientTests implements FabricClientGameTest {
    // A south-facing machine: master (lower-left) at 0 -60 0, right column at x = 1, front face at z = 1.
    private static final String MACHINE = "diamondvending:vending_machine[facing=south,";
    private static final String CONTENTS = "{owner_name:\"Tester\","
            + "selections:[{slot:0,item:{id:\"minecraft:apple\",count:2},price:3},"
            + "{slot:1,item:{id:\"minecraft:oak_log\",count:4},price:1},"
            + "{slot:2,item:{id:\"minecraft:diamond_sword\",count:1},price:12},"
            + "{slot:4,item:{id:\"minecraft:bread\",count:1},price:0},"
            + "{slot:7,item:{id:\"minecraft:enchanted_golden_apple\",count:1},price:64},"
            + "{slot:11,item:{id:\"minecraft:cake\",count:1},price:5}],"
            + "stock:{Items:[{Slot:0b,id:\"minecraft:apple\",count:64},{Slot:1b,id:\"minecraft:oak_log\",count:64},"
            + "{Slot:2b,id:\"minecraft:diamond_sword\",count:1},{Slot:3b,id:\"minecraft:bread\",count:64},"
            + "{Slot:4b,id:\"minecraft:enchanted_golden_apple\",count:3}]},"
            + "tray:{Items:[{Slot:0b,id:\"minecraft:cookie\",count:3}]}}";
    private static final String FULL_TRAY = "{tray:{Items:["
            + "{Slot:0b,id:\"minecraft:cobblestone\",count:64},{Slot:1b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:2b,id:\"minecraft:cobblestone\",count:64},{Slot:3b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:4b,id:\"minecraft:cobblestone\",count:64},{Slot:5b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:6b,id:\"minecraft:cobblestone\",count:64},{Slot:7b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:8b,id:\"minecraft:cobblestone\",count:64}]}}";

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("tp @a 1 -60 3.5 180 10"); // first, so the chunks around 0 0 are loaded
            context.waitTicks(20);
            server.runCommand("fill -4 -61 -4 5 -61 6 minecraft:smooth_stone");
            server.runCommand("fill -4 -60 -4 5 -54 6 minecraft:air");
            server.runCommand("setblock 0 -60 0 " + MACHINE + "half=lower,side=left]" + CONTENTS);
            server.runCommand("setblock 1 -60 0 " + MACHINE + "half=lower,side=right]");
            server.runCommand("setblock 0 -59 0 " + MACHINE + "half=upper,side=left]");
            server.runCommand("setblock 1 -59 0 " + MACHINE + "half=upper,side=right]");

            // In front, looking at the whole machine.
            world.getClientLevel().waitForChunksRender();
            context.waitTicks(20);
            context.takeScreenshot("machine_front");

            // Crosshair on button 1 (canvas 26, 8 → x 1.625, y -58.5 on the z = 1 face); eyes are 1.62 above the feet.
            server.runCommand("tp @a 1.625 -60 3.5 180 2.75");
            context.waitTicks(5);
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT); // no diamonds yet
            context.waitTicks(5);
            context.takeScreenshot("need_money");

            server.runCommand("give @a minecraft:diamond 10");
            context.waitTicks(20); // longer than the held-click window
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitTicks(4);
            context.takeScreenshot("item_falling");
            context.waitTicks(10);
            context.takeScreenshot("thank_you");

            server.runCommand("data merge block 0 -60 0 " + FULL_TRAY);
            server.runCommand("tp @a 1 -60 3.5 180 10");
            context.waitTicks(45); // past the THANK YOU flash
            context.takeScreenshot("problem_1");
            context.waitTicks(10);
            context.takeScreenshot("problem_2");

            // Behind the machine: nothing may float in the air or show through.
            server.runCommand("tp @a 1 -60 -3 0 10");
            context.waitTicks(10);
            context.takeScreenshot("machine_back");
        }
    }
}
//?} else {
/*public final class FabricClientTests {
}
*///?}
```

In `src/gametest/resources/fabric.mod.json`, add to `entrypoints`:

```json
    "fabric-client-gametest": ["diamondvending.gametest.fabric.FabricClientTests"],
```

In `build.fabric.gradle.kts`, inside `loom { runs { … } }` after the `gametest` run, add:

```kotlin
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
```

- [ ] **Step 2: Run it to see the machine without a renderer**

Run: `GCLIENT` (a game window opens for a minute or two), then `find /c/Users/benet/mcvending/versions/26.1-fabric/build/clientgametest -name "*.png"` and Read each screenshot.
Expected: the run finishes (BUILD SUCCESSFUL) and saves `machine_front`, `need_money`, `item_falling`, `thank_you`, `problem_1`, `problem_2`, `machine_back`. They show the red textured machine only — no shelf items, labels, tray items, display text or lamp. This is the visual "failing" state.

- [ ] **Step 3: Write the renderer**

`src/main/java/diamondvending/client/FrontCanvas.java`:

```java
package diamondvending.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Facing;
import diamondvending.scene.MachineScene;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Where things go on a machine's front: turns the canvas pixels of a {@link MachineScene} into places for the renderer. */
final class FrontCanvas {
    /** Spec §2.3: nothing is drawn beyond 32 blocks. */
    static final int VIEW_DISTANCE = 32;
    /** Block light 15 and sky light 15: the display, lamp and labels glow in the dark. */
    static final int FULL_BRIGHT = 0xF000F0;
    /** Text is drawn this far up from its v, so the middle of the 8-unit-tall line sits on v. */
    static final float TEXT_Y = -4.0F;
    private static final float GLOW_DEPTH = -0.02F;
    private static final float TEXT_DEPTH = -0.05F;
    private static final float ITEM_DEPTH = -0.7F;
    /** Items are squashed flat against the glass so block items don't poke into the machine. */
    private static final float ITEM_THICKNESS = 0.2F;

    private FrontCanvas() {}

    /** Whether the camera is in front of the machine (spec §2.3: nothing is drawn when the front isn't visible). */
    static boolean visibleFrom(VendingMachineBlockEntity machine, double cameraX, double cameraZ) {
        Direction facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        BlockPos pos = machine.getBlockPos();
        return Facing.valueOf(facing.name()).frontVisible(pos.getX(), pos.getZ(), cameraX, cameraZ);
    }

    /** The scene for this client's player, now. */
    static MachineScene scene(VendingMachineBlockEntity machine, float partialTicks) {
        Player player = Minecraft.getInstance().player;
        UUID viewer = player != null ? player.getUUID() : new UUID(0, 0);
        return MachineScene.of(machine, viewer, machine.getLevel().getGameTime() + partialTicks, Component::getString);
    }

    /**
     * Moves the pose from the master block's corner to the front canvas: origin at the canvas's top-left, u to the right,
     * v down, one unit per canvas pixel, depth pointing into the machine.
     */
    static void enter(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5F, 2.0F, 0.5F);
        pose.scale(1 / 16F, -1 / 16F, -1 / 16F);
    }

    static void placeItem(PoseStack pose, MachineScene.Item item) {
        pose.translate(item.u(), item.v(), ITEM_DEPTH);
        // Item models are y-up and face +z; the canvas is y-down with z into the machine.
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.scale(item.size(), item.size(), item.size() * ITEM_THICKNESS);
    }

    static void placeText(PoseStack pose, MachineScene.Text text) {
        pose.translate(text.u(), text.v(), TEXT_DEPTH);
        float scale = text.height() / 8F;
        pose.scale(scale, scale, scale);
    }

    /** Where a line of {@code width} font units starts, for its alignment. */
    static float textX(MachineScene.Text text, int width) {
        return switch (text.align()) {
            case LEFT -> 0;
            case CENTER -> -width / 2F;
            case RIGHT -> -width;
        };
    }

    /** A flat glowing rectangle, drawn both ways round so it shows whichever side the render type culls. */
    static void glow(PoseStack.Pose at, VertexConsumer out, MachineScene.Glow glow) {
        vertex(out, at, glow.u0(), glow.v0(), glow.color());
        vertex(out, at, glow.u0(), glow.v1(), glow.color());
        vertex(out, at, glow.u1(), glow.v1(), glow.color());
        vertex(out, at, glow.u1(), glow.v0(), glow.color());
        vertex(out, at, glow.u1(), glow.v0(), glow.color());
        vertex(out, at, glow.u1(), glow.v1(), glow.color());
        vertex(out, at, glow.u0(), glow.v1(), glow.color());
        vertex(out, at, glow.u0(), glow.v0(), glow.color());
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose at, float u, float v, int color) {
        out.addVertex(at, u, v, GLOW_DEPTH).setColor(color).setLight(FULL_BRIGHT);
    }
}
```

`src/main/java/diamondvending/client/VendingMachineRenderer.java`:

```java
package diamondvending.client;

import com.mojang.blaze3d.vertex.PoseStack;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.scene.MachineScene;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
*///?}

/**
 * Draws a machine's front — shelf items, price labels, the display, the lamp, the tray, the falling item — from its
 * {@link MachineScene}. 26.1 gathers everything first and then submits draw calls; 1.21.1 draws straight away.
 */
//? if >=26.1 {
public final class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity, VendingMachineRenderer.State> {
    private final ItemModelResolver itemModels;
    private final Font font;

    public VendingMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
        this.font = context.font();
    }

    // What one frame needs, gathered from the machine before drawing.
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        MachineScene scene = MachineScene.EMPTY;
        final List<ItemStackRenderState> items = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VendingMachineBlockEntity machine, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTicks, camera, breakProgress);
        state.facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        state.items.clear();
        if (machine.getLevel() == null || !FrontCanvas.visibleFrom(machine, camera.x, camera.z)) {
            state.scene = MachineScene.EMPTY;
            return;
        }
        // Light from the air in front of the machine: the machine's own block is solid, so its light is dark.
        state.lightCoords = LevelRenderer.getLightCoords(machine.getLevel(), machine.getBlockPos().relative(state.facing));
        state.scene = FrontCanvas.scene(machine, partialTicks);
        for (MachineScene.Item item : state.scene.items()) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModels.updateForTopItem(itemState, item.stack(), ItemDisplayContext.FIXED, machine.getLevel(), null, 0);
            state.items.add(itemState);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        FrontCanvas.enter(pose, state.facing);
        for (MachineScene.Glow glow : state.scene.glows()) {
            out.submitCustomGeometry(pose, RenderTypes.textBackground(), (at, vertices) -> FrontCanvas.glow(at, vertices, glow));
        }
        for (int i = 0; i < state.items.size(); i++) {
            pose.pushPose();
            FrontCanvas.placeItem(pose, state.scene.items().get(i));
            state.items.get(i).submit(pose, out, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        for (MachineScene.Text text : state.scene.texts()) {
            FormattedCharSequence line = Component.literal(text.text()).getVisualOrderText();
            pose.pushPose();
            FrontCanvas.placeText(pose, text);
            out.submitText(pose, FrontCanvas.textX(text, font.width(line)), FrontCanvas.TEXT_Y, line, false,
                    Font.DisplayMode.POLYGON_OFFSET, FrontCanvas.FULL_BRIGHT, text.color(), 0, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    // The machine is 2 x 2 but its block entity sits in one block: culling by that block would hide it too early.
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return FrontCanvas.VIEW_DISTANCE;
    }
}
//?} else {
/*public final class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final Font font;

    public VendingMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(VendingMachineBlockEntity machine, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (machine.getLevel() == null || !FrontCanvas.visibleFrom(machine, camera.x, camera.z)) return;
        Direction facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        // Light from the air in front of the machine: the machine's own block is solid, so its light is dark.
        int frontLight = LevelRenderer.getLightColor(machine.getLevel(), machine.getBlockPos().relative(facing));
        MachineScene scene = FrontCanvas.scene(machine, partialTicks);
        pose.pushPose();
        FrontCanvas.enter(pose, facing);
        VertexConsumer glows = buffers.getBuffer(RenderType.textBackground());
        for (MachineScene.Glow glow : scene.glows()) FrontCanvas.glow(pose.last(), glows, glow);
        for (MachineScene.Item item : scene.items()) {
            pose.pushPose();
            FrontCanvas.placeItem(pose, item);
            itemRenderer.renderStatic(item.stack(), ItemDisplayContext.FIXED, frontLight, OverlayTexture.NO_OVERLAY, pose, buffers, machine.getLevel(), 0);
            pose.popPose();
        }
        for (MachineScene.Text text : scene.texts()) {
            pose.pushPose();
            FrontCanvas.placeText(pose, text);
            font.drawInBatch(text.text(), FrontCanvas.textX(text, font.width(text.text())), FrontCanvas.TEXT_Y, text.color(), false,
                    pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FrontCanvas.FULL_BRIGHT);
            pose.popPose();
        }
        pose.popPose();
    }

    // The machine is 2 x 2 but its block entity sits in one block: culling by that block would hide it too early.
    @Override
    public boolean shouldRenderOffScreen(VendingMachineBlockEntity machine) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return FrontCanvas.VIEW_DISTANCE;
    }
}
*///?}
```

`src/main/java/diamondvending/platform/fabric/DiamondVendingFabricClient.java`:

```java
package diamondvending.platform.fabric;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.client.VendingMachineRenderer;
import diamondvending.registry.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

/** Fabric client entrypoint (the {@code client} entrypoint in fabric.mod.json): only ever loaded on a game client. */
public final class DiamondVendingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //? if >=26.1 {
        BlockEntityRendererRegistry.<VendingMachineBlockEntity, VendingMachineRenderer.State>register(
                ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new);
        //?} else {
        /*BlockEntityRendererRegistry.<VendingMachineBlockEntity>register(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new);
        *///?}
    }
}
```

`src/main/java/diamondvending/platform/neoforge/DiamondVendingNeoForgeClient.java`:

```java
package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.client.VendingMachineRenderer;
import diamondvending.registry.ModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** NeoForge client entrypoint: a second {@code @Mod} class that NeoForge only loads on a game client. */
@Mod(value = DiamondVending.MOD_ID, dist = Dist.CLIENT)
public final class DiamondVendingNeoForgeClient {
    public DiamondVendingNeoForgeClient(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new));
    }
}
```

In `src/main/resources/fabric.mod.json`, change `entrypoints` to:

```json
  "entrypoints": {
    "main": ["diamondvending.platform.fabric.DiamondVendingFabric"],
    "client": ["diamondvending.platform.fabric.DiamondVendingFabricClient"]
  },
```

- [ ] **Step 4: Build every node and run the server tests**

Run, one at a time: `G26N`, `G26F`, `G121N`, `G121F`
Expected: all compile; 65 / 65 / 64 / 64 game tests pass (the dedicated game-test servers load the mod without touching client classes).

- [ ] **Step 5: Look at it (visual GREEN)**

Run: `GCLIENT`, then Read every screenshot again (the files are overwritten).
Expected:
- `machine_front`: apple, oak log (a small flattened block), diamond sword, bread, enchanted golden apple (glinting) and cake on the shelves, upright and facing you; a white label under each: `1 · 3` + diamond icon, `2 · 1`, `3 · 12`, `5 · FREE`, `8 · 64`, and a red `SOLD OUT` under the cake; three cookies in the tray; green text on the display (`SELECT ` scrolling).
- `need_money`: the display shows red `NEED 3`; the action bar says "Button 1 costs 3 diamonds. You have 0."
- `item_falling`: an apple between button 1's shelf and the tray.
- `thank_you`: green scrolling `THANK YOU` text; two apples in the tray next to the cookies.
- `problem_1` / `problem_2`: red scrolling `TRAY FULL - TAKE YOUR ITEMS`; the lamp (above the display) is red in one of the two and dark in the other.
- `machine_back`: the plain back of the machine — nothing floating, nothing showing through.

If the items appear back-to-front (for example the sword points the other way from how it looks in the hotbar), change `Axis.XP.rotationDegrees(180)` in `FrontCanvas.placeItem` to `Axis.ZP.rotationDegrees(180)` and re-run this step. If a label, text or the lamp is missing, check the Step 2 screenshot against the same spot and ledger what changed. Record what the screenshots show in the ledger.

- [ ] **Step 6: Document the client test**

In `docs/dev-setup.md`, add after the GameTests section:

````markdown
## Client game test (26.1 Fabric)
Rendering can't be checked by a server, so one test runs in a real game window and takes screenshots:

```bash
./gradlew :26.1-fabric:runClientGametest
```

It builds a stocked machine with commands, clicks it, and saves screenshots to
`versions/26.1-fabric/build/clientgametest/screenshots/`. Look at them after any change to `client/` or `scene/`. It needs
a display, so it only runs locally (CI runners have none), and only on 26.1 — 1.21.1's Fabric API has no client tests. The
1.21.1 renderer shares `FrontCanvas` and `MachineScene` with 26.1; only the draw calls differ.
````

- [ ] **Step 7: Check that no common code reaches for client classes, then commit**

Run: `grep -rn "net.minecraft.client" src/main/java/diamondvending/core src/main/java/diamondvending/scene src/main/java/diamondvending/block src/main/java/diamondvending/shop`
Expected: no output.

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src build.fabric.gradle.kts docs/dev-setup.md
git -C /c/Users/benet/mcvending commit -m "feat: draw the machine's front; client screenshot test"
```

---

### Task 5: The hover tooltip

Deliverable: looking at a machine's front shows a small tooltip next to the crosshair — what a button sells and costs, the coin slot and your credit, coin return, the tray, every active problem, and who owns it.

**Files:**
- Create: `src/main/java/diamondvending/scene/HoverText.java`, `src/main/java/diamondvending/client/HoverHud.java`
- Modify: `core/Texts.java`, `lang/en_us.json`, `platform/fabric/DiamondVendingFabricClient.java`, `platform/neoforge/DiamondVendingNeoForgeClient.java`
- Modify: `DisplayTests.java`, `FabricDisplayTests.java`, `NeoForgeDisplayTests.java`, `fabric/FabricClientTests.java`
- Modify: `CHANGELOG.md`, `docs/superpowers/plans/2026-09-23-roadmap.md`

**Interfaces:**
- Consumes: `core.Hit`, `core.Region` (Plan 1); `Messages.explanation(Problem, Currency)`, `Currency.money/name/displayItem`, `ItemSlots.count` (Plan 3); `FrontFace.hit`, `MachinePart.masterOf` (Plans 2–3); client view accessors (Task 2/Plan 3).
- Produces: `Texts.HUD_ITEM`, `HUD_FREE`, `HUD_SOLD_OUT`, `HUD_NOTHING`, `HUD_INSERT`, `HUD_YOUR_CREDIT`, `HUD_RETURN_CREDIT`, `HUD_TAKE_ITEMS`, `HUD_OWNED_BY`, `HUD_SHOP_MACHINE`; `scene.HoverText` — `record Line(Component text, ItemStack icon, boolean alarm)`, `static List<Line> lines(VendingMachineBlockEntity machine, Hit hit, UUID viewer)`; `client.HoverHud.render(<gui graphics>, DeltaTracker)`.

- [ ] **Step 1: Write the failing GameTests**

In `DisplayTests.java`, add imports `diamondvending.core.Hit`, `diamondvending.scene.HoverText` and `java.util.List`. Add to `ALL`:

```java
            Map.entry("hovering_a_button_shows_what_it_sells", DisplayTests::hoveringAButtonShowsWhatItSells),
            Map.entry("sold_out_and_empty_buttons_say_so", DisplayTests::soldOutAndEmptyButtonsSaySo),
            Map.entry("the_coin_slot_return_and_tray_show_counts", DisplayTests::theCoinSlotReturnAndTrayShowCounts),
            Map.entry("problems_are_explained_anywhere_on_the_front", DisplayTests::problemsAreExplainedAnywhereOnTheFront),
            Map.entry("shop_machines_say_so", DisplayTests::shopMachinesSaySo)
```

Add a helper and the tests:

```java
    static HoverText.Line line(GameTestHelper helper, List<HoverText.Line> lines, String key) {
        return lines.stream().filter(l -> l.text().getContents() instanceof TranslatableContents t && t.getKey().equals(key))
                .findFirst().orElseThrow(() -> new AssertionError("expected a tooltip line " + key + " in " + lines));
    }

    // ---- hover tooltip -------------------------------------------------------------------------------------------

    public static void hoveringAButtonShowsWhatItSells(GameTestHelper helper) {
        VendingMachineBlockEntity machine = clientView(helper, BuyingTests.appleMachine(helper));
        List<HoverText.Line> lines = HoverText.lines(machine, Hit.button(0), UUID.randomUUID());
        HoverText.Line item = line(helper, lines, Texts.HUD_ITEM);
        helper.assertTrue(item.icon().is(Items.APPLE) && ((TranslatableContents) item.text().getContents()).getArgs()[1].equals(2),
                "the first line should show the apple and how many one purchase gives");
        helper.assertTrue(line(helper, lines, Texts.DIAMOND + ".many").icon().is(Items.DIAMOND), "then the price, with the currency's icon");
        HoverText.Line owner = lines.getLast();
        BuyingTests.translation(helper, owner.text(), Texts.HUD_OWNED_BY);
        helper.assertTrue(((TranslatableContents) owner.text().getContents()).getArgs()[0].equals("test-player"), "and who owns the machine");
        helper.succeed();
    }

    public static void soldOutAndEmptyButtonsSaySo(GameTestHelper helper) {
        VendingMachineBlockEntity server = BuyingTests.appleMachine(helper);
        server.stock().set(0, ItemStack.EMPTY);
        VendingMachineBlockEntity machine = clientView(helper, server);
        helper.assertTrue(line(helper, HoverText.lines(machine, Hit.button(0), UUID.randomUUID()), Texts.HUD_SOLD_OUT).alarm(),
                "an owned machine out of stock shows SOLD OUT in red");
        line(helper, HoverText.lines(machine, Hit.button(5), UUID.randomUUID()), Texts.HUD_NOTHING);
        helper.succeed();
    }

    /** Spec §2.3: the coin slot shows your own credit; coin return and the tray show counts. */
    public static void theCoinSlotReturnAndTrayShowCounts(GameTestHelper helper) {
        VendingMachineBlockEntity server = BuyingTests.appleMachine(helper);
        UUID buyer = UUID.randomUUID();
        server.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));
        server.tray().set(0, new ItemStack(Items.BREAD, 3));
        VendingMachineBlockEntity machine = clientView(helper, server);
        List<HoverText.Line> coinSlot = HoverText.lines(machine, Hit.of(diamondvending.core.Region.COIN_SLOT), buyer);
        line(helper, coinSlot, Texts.HUD_INSERT);
        Component credit = (Component) ((TranslatableContents) line(helper, coinSlot, Texts.HUD_YOUR_CREDIT).text().getContents()).getArgs()[0];
        helper.assertTrue(((TranslatableContents) credit.getContents()).getArgs()[0].equals(4), "the coin slot shows this player's 4 diamonds");
        HoverText.Line strangerCredit = line(helper, HoverText.lines(machine, Hit.of(diamondvending.core.Region.COIN_SLOT), UUID.randomUUID()), Texts.HUD_YOUR_CREDIT);
        Component none = (Component) ((TranslatableContents) strangerCredit.text().getContents()).getArgs()[0];
        helper.assertTrue(((TranslatableContents) none.getContents()).getArgs()[0].equals(0), "and nobody else's");
        HoverText.Line giveBack = line(helper, HoverText.lines(machine, Hit.of(diamondvending.core.Region.COIN_RETURN), buyer), Texts.HUD_RETURN_CREDIT);
        helper.assertTrue(((TranslatableContents) giveBack.text().getContents()).getArgs()[0].equals(4), "coin return shows 4");
        HoverText.Line tray = line(helper, HoverText.lines(machine, Hit.of(diamondvending.core.Region.TRAY), buyer), Texts.HUD_TAKE_ITEMS);
        helper.assertTrue(((TranslatableContents) tray.text().getContents()).getArgs()[0].equals(3), "the tray shows 3 items");
        helper.succeed();
    }

    /** Spec §3.5 b: every problem is explained wherever you look on the front. */
    public static void problemsAreExplainedAnywhereOnTheFront(GameTestHelper helper) {
        VendingMachineBlockEntity server = BuyingTests.appleMachine(helper);
        BuyingTests.fill(server.tray(), Items.COBBLESTONE);
        BuyingTests.fill(server.cashBox(), Items.COBBLESTONE);
        List<HoverText.Line> lines = HoverText.lines(clientView(helper, server), Hit.NONE, UUID.randomUUID());
        helper.assertTrue(line(helper, lines, Texts.explanation(Problem.CASH_BOX_FULL)).alarm()
                && line(helper, lines, Texts.explanation(Problem.TRAY_FULL)).alarm(), "both problems should be explained, in red");
        helper.succeed();
    }

    public static void shopMachinesSaySo(GameTestHelper helper) {
        VendingMachineBlockEntity server = BuyingTests.appleMachine(helper);
        server.setInfinite(true);
        BuyingTests.translation(helper, HoverText.lines(clientView(helper, server), Hit.NONE, UUID.randomUUID()).getLast().text(), Texts.HUD_SHOP_MACHINE);
        server.setInfinite(false);
        server.setOwner(null, "");
        BuyingTests.translation(helper, HoverText.lines(clientView(helper, server), Hit.NONE, UUID.randomUUID()).getLast().text(), Texts.HUD_SHOP_MACHINE);
        helper.succeed();
    }
```

Add to `FabricDisplayTests.java`:

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void hoveringAButtonShowsWhatItSells(GameTestHelper helper) {
        DisplayTests.hoveringAButtonShowsWhatItSells(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void soldOutAndEmptyButtonsSaySo(GameTestHelper helper) {
        DisplayTests.soldOutAndEmptyButtonsSaySo(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCoinSlotReturnAndTrayShowCounts(GameTestHelper helper) {
        DisplayTests.theCoinSlotReturnAndTrayShowCounts(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void problemsAreExplainedAnywhereOnTheFront(GameTestHelper helper) {
        DisplayTests.problemsAreExplainedAnywhereOnTheFront(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void shopMachinesSaySo(GameTestHelper helper) {
        DisplayTests.shopMachinesSaySo(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeDisplayTests.java`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void hoveringAButtonShowsWhatItSells(GameTestHelper helper) {
        DisplayTests.hoveringAButtonShowsWhatItSells(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void soldOutAndEmptyButtonsSaySo(GameTestHelper helper) {
        DisplayTests.soldOutAndEmptyButtonsSaySo(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCoinSlotReturnAndTrayShowCounts(GameTestHelper helper) {
        DisplayTests.theCoinSlotReturnAndTrayShowCounts(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void problemsAreExplainedAnywhereOnTheFront(GameTestHelper helper) {
        DisplayTests.problemsAreExplainedAnywhereOnTheFront(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void shopMachinesSaySo(GameTestHelper helper) {
        DisplayTests.shopMachinesSaySo(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors (`cannot find symbol: variable HUD_ITEM`, `package diamondvending.scene` has no `HoverText`).

- [ ] **Step 3: Add the tooltip's words and model**

In `core/Texts.java`, add after `TAG_SOLD_OUT`:

```java
    // The hover tooltip (spec §2.3).
    public static final String HUD_ITEM = "hud.diamondvending.item";
    public static final String HUD_FREE = "hud.diamondvending.free";
    public static final String HUD_SOLD_OUT = "hud.diamondvending.sold_out";
    public static final String HUD_NOTHING = "hud.diamondvending.nothing";
    public static final String HUD_INSERT = "hud.diamondvending.insert";
    public static final String HUD_YOUR_CREDIT = "hud.diamondvending.your_credit";
    public static final String HUD_RETURN_CREDIT = "hud.diamondvending.return_credit";
    public static final String HUD_TAKE_ITEMS = "hud.diamondvending.take_items";
    public static final String HUD_OWNED_BY = "hud.diamondvending.owned_by";
    public static final String HUD_SHOP_MACHINE = "hud.diamondvending.shop_machine";
```

and in `all()` add a line after the `List.of(…)`:

```java
        keys.addAll(List.of(HUD_ITEM, HUD_FREE, HUD_SOLD_OUT, HUD_NOTHING, HUD_INSERT, HUD_YOUR_CREDIT, HUD_RETURN_CREDIT,
                HUD_TAKE_ITEMS, HUD_OWNED_BY, HUD_SHOP_MACHINE));
```

In `en_us.json`, add after the `tag.diamondvending.*` lines:

```json
  "hud.diamondvending.item": "%s × %s",
  "hud.diamondvending.free": "Free!",
  "hud.diamondvending.sold_out": "SOLD OUT",
  "hud.diamondvending.nothing": "Nothing for sale here",
  "hud.diamondvending.insert": "Insert %s",
  "hud.diamondvending.your_credit": "Your credit: %s",
  "hud.diamondvending.return_credit": "Return credit (%s)",
  "hud.diamondvending.take_items": "Take items (%s)",
  "hud.diamondvending.owned_by": "Owned by %s",
  "hud.diamondvending.shop_machine": "Shop machine",
```

`src/main/java/diamondvending/scene/HoverText.java`:

```java
package diamondvending.scene;

import diamondvending.Messages;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Hit;
import diamondvending.core.Problem;
import diamondvending.core.Texts;
import diamondvending.shop.Currency;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.Selection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The tooltip next to the crosshair when looking at a machine's front (spec §2.3), built from what the client knows. */
public final class HoverText {
    private HoverText() {}

    /** One tooltip line: its words, an optional icon drawn before them, and whether it's red. */
    public record Line(Component text, ItemStack icon, boolean alarm) {
        static Line of(Component text) {
            return new Line(text, ItemStack.EMPTY, false);
        }
    }

    /** Every problem's explanation first (spec §3.5 b), then what the crosshair is on, then who owns the machine. */
    public static List<Line> lines(VendingMachineBlockEntity machine, Hit hit, UUID viewer) {
        List<Line> lines = new ArrayList<>();
        Currency currency = machine.currency();
        ItemStack coin = new ItemStack(currency.displayItem());
        for (Problem problem : machine.syncedProblems()) {
            lines.add(new Line(Messages.explanation(problem, currency), ItemStack.EMPTY, true));
        }
        switch (hit.region()) {
            case BUTTON -> {
                Selection selection = machine.getSelection(hit.button());
                if (!selection.isSetUp()) {
                    lines.add(Line.of(Component.translatable(Texts.HUD_NOTHING)));
                } else {
                    lines.add(new Line(Component.translatable(Texts.HUD_ITEM, selection.template().getHoverName(), selection.quantity()),
                            selection.template(), false));
                    lines.add(selection.price() == 0
                            ? Line.of(Component.translatable(Texts.HUD_FREE))
                            : new Line(currency.money(selection.price()), coin, false));
                    if (!machine.isInfinite() && machine.syncedStockCount(hit.button()) < selection.quantity()) {
                        lines.add(new Line(Component.translatable(Texts.HUD_SOLD_OUT), ItemStack.EMPTY, true));
                    }
                }
            }
            case COIN_SLOT -> {
                lines.add(new Line(Component.translatable(Texts.HUD_INSERT, currency.name()), coin, false));
                lines.add(Line.of(Component.translatable(Texts.HUD_YOUR_CREDIT, currency.money(machine.syncedCredit(viewer)))));
            }
            case COIN_RETURN -> lines.add(Line.of(Component.translatable(Texts.HUD_RETURN_CREDIT, machine.syncedCredit(viewer))));
            case TRAY -> lines.add(Line.of(Component.translatable(Texts.HUD_TAKE_ITEMS, ItemSlots.count(machine.tray(), stack -> true))));
            default -> {
            }
        }
        boolean shopMachine = machine.isInfinite() || machine.getOwner() == null;
        lines.add(Line.of(shopMachine
                ? Component.translatable(Texts.HUD_SHOP_MACHINE)
                : Component.translatable(Texts.HUD_OWNED_BY, machine.getOwnerName())));
        return lines;
    }
}
```

- [ ] **Step 4: Run to verify they pass**

Run: `G26N`
Expected: PASS — unit tests green (`TextsTest` covers the hud keys); `All 70 required tests passed :)`.

- [ ] **Step 5: Draw it next to the crosshair**

`src/main/java/diamondvending/client/HoverHud.java`:

```java
package diamondvending.client;

import diamondvending.block.FrontFace;
import diamondvending.block.MachinePart;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.registry.ModContent;
import diamondvending.scene.HoverText;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

import java.util.List;

/** Draws the {@link HoverText} tooltip to the right of the crosshair while it's on a machine's front (spec §2.3). */
public final class HoverHud {
    private static final int LINE_HEIGHT = 18;
    private static final int ICON_WIDTH = 18;
    private static final int BACKGROUND = 0xA0000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int ALARM = 0xFFFF6060;

    private HoverHud() {}

    //? if >=26.1 {
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
    //?} else {
    /*public static void render(GuiGraphics graphics, DeltaTracker delta) {
    *///?}
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null || mc.player == null) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(ModContent.VENDING_MACHINE.get()) || hit.getDirection() != state.getValue(VendingMachineBlock.FACING)) return;
        if (!(mc.level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine)) return;

        List<HoverText.Line> lines = HoverText.lines(machine, FrontFace.hit(state, pos, hit), mc.player.getUUID());
        int width = 0;
        for (HoverText.Line line : lines) width = Math.max(width, ICON_WIDTH + mc.font.width(line.text()));
        int x = graphics.guiWidth() / 2 + 12;
        int y = graphics.guiHeight() / 2 - lines.size() * LINE_HEIGHT / 2;
        graphics.fill(x - 3, y - 2, x + width + 3, y + lines.size() * LINE_HEIGHT, BACKGROUND);
        for (HoverText.Line line : lines) {
            int color = line.alarm() ? ALARM : TEXT;
            //? if >=26.1 {
            if (!line.icon().isEmpty()) graphics.item(line.icon(), x, y);
            graphics.text(mc.font, line.text(), x + ICON_WIDTH, y + 4, color);
            //?} else {
            /*if (!line.icon().isEmpty()) graphics.renderItem(line.icon(), x, y);
            graphics.drawString(mc.font, line.text(), x + ICON_WIDTH, y + 4, color);
            *///?}
            y += LINE_HEIGHT;
        }
    }
}
```

In `DiamondVendingFabricClient.java`, add after the renderer registration (inside `onInitializeClient`):

```java
        //? if >=26.1 {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, DiamondVending.id("hover"), HoverHud::render);
        //?} else {
        /*HudRenderCallback.EVENT.register(HoverHud::render);
        *///?}
```

and the imports:

```java
import diamondvending.DiamondVending;
import diamondvending.client.HoverHud;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?}
```

In `DiamondVendingNeoForgeClient.java`, add to the constructor:

```java
        modBus.addListener(RegisterGuiLayersEvent.class, event ->
                event.registerAbove(VanillaGuiLayers.CROSSHAIR, DiamondVending.id("hover"), HoverHud::render));
```

and imports `diamondvending.client.HoverHud`, `net.neoforged.neoforge.client.event.RegisterGuiLayersEvent`, `net.neoforged.neoforge.client.gui.VanillaGuiLayers`.

In `FabricClientTests.java`, after `server.runCommand("tp @a 1.625 -60 3.5 180 2.75");` and its `context.waitTicks(5);`, add:

```java
            context.takeScreenshot("hover_button_1");
```

and after the `problem_2` screenshot add:

```java
            // Crosshair on the tray (canvas 12, 28.25 → x 0.75, y -59.77 on the z = 1 face; eyes at -58.38, 2.5 away).
            server.runCommand("tp @a 0.75 -60 3.5 180 29.0");
            context.waitTicks(5);
            context.takeScreenshot("hover_tray");
```

- [ ] **Step 6: Run everything**

Run, one at a time: `G26N`, `G26F`, `G121N`, `G121F`, then `GCLIENT` and Read `hover_button_1.png` and `hover_tray.png`.
Expected: 70 / 70 / 69 / 69 game tests pass. `hover_button_1`: to the right of the crosshair, a dark box with an apple icon + "Apple × 2", a diamond icon + "3 diamonds", and "Shop machine" (the test machine has no owner). `hover_tray`: a red line ("The tray is full! Right-click the tray to take the items out.") then "Take items (…)" and "Shop machine". The other screenshots are unchanged: shelf items with white labels (`1 · 3` + diamond icon, `5 · FREE`, red `SOLD OUT` under the cake) and cookies in the tray (`machine_front`), red `NEED 3` (`need_money`), an apple falling (`item_falling`), green `THANK YOU` (`thank_you`), red scrolling `TRAY FULL - TAKE YOUR ITEMS` with the lamp lit in one of `problem_1`/`problem_2`, and nothing floating behind the machine (`machine_back`). If the tray tooltip isn't showing, the pitch missed the tray: the crosshair must be on the front face between v 26 and 30.5 — adjust the pitch and ledger it.

- [ ] **Step 7: Changelog, roadmap, commit**

In `CHANGELOG.md`, under `## [Unreleased]` → `### Added`, append:

```markdown
- You can see what a machine sells: items on its shelves with price tags (FREE and SOLD OUT too), and what's waiting in the tray.
- The display shows SELECT ITEM or your own credit, flashes THANK YOU or what went wrong (NEED 3, SOLD OUT…), and scrolls every problem in red while the warning lamp blinks.
- Bought items drop from their shelf into the tray.
- Look at the front of a machine to see a tooltip: what a button sells and costs, your credit, what's in the tray, any problems, and who owns it.
```

In `docs/superpowers/plans/2026-09-23-roadmap.md`, change Plan 4's row to start with `| **4 — Seeing it** ([plan](2026-09-23-plan-4-seeing-it.md)) |` and its status cell to `Done`.

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src CHANGELOG.md docs
git -C /c/Users/benet/mcvending commit -m "feat: hover tooltip on the machine's front"
```

- [ ] **Step 8: Push and open the PR**

```bash
git -C /c/Users/benet/mcvending push -u origin plan-4/seeing-it
```

Open a PR titled "Plan 4: Seeing it — shelves, price tags, display, lamp, drop animation and hover tooltip" with the repo's PR template, attach or describe the Task 4/5 screenshots, and wait for CI to be green on all four targets.
