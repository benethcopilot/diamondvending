# Diamond Vending — Plan 3: Buying Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A machine can be stocked (by `/data` until the setup screen exists) and anyone can buy from it: coin slot and credit, coin return, buttons, the pickup tray, problems, plain-language messages and sounds — all on the server, on all four targets, verified by GameTests.

**Architecture:** The master block entity holds the machine's contents (12 selections, stock, cash box, tray, per-player credit, infinite flag), saves them in a `/data`-friendly format, and sends clients only what they draw (plus a few derived numbers). `VendingMachineBlock.useItemOn` routes every click on the server: dye (owners/admins) first, otherwise `FrontFace` turns the exact hit location into a `core/Hit`, and small classes in the new `shop/` package do the work (`PickupTray`, `CoinSlot`, `Purchase`). `Purchase` measures everything, asks `core/PurchaseRules`, and changes nothing unless approved. Nothing in this plan runs on the client — rendering and the HUD are Plan 4.

**Tech Stack:** as Plans 1–2. No new dependencies.

**Spec:** [`docs/superpowers/specs/2026-09-23-diamond-vending-design.md`](../specs/2026-09-23-diamond-vending-design.md) · **Roadmap:** [`2026-09-23-roadmap.md`](2026-09-23-roadmap.md) · **Previous:** [Plan 2](2026-09-23-plan-2-the-machine.md)

## Global Constraints

- Everything in Plans 1–2's Global Constraints still holds (mod id and package `diamondvending`, nodes, vcsVersion `26.1-neoforge`, no runtime deps, `core/` has no Minecraft imports, one Gradle node at a time, one shell command per Bash call, branch → PR → squash, write `Identifier` not `ResourceLocation`).
- Branch: `plan-3/buying` (already created; this plan is its first commit).
- If `java` isn't on PATH in the Bash tool, prefix Gradle with `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"`.
- `$SCRATCH` = the session's scratchpad directory. To check a compile error against real sources, extract them there as Plan 2 describes (`src-1211`, `src-261`, `src-261-neo`). Vanilla 26.1 (what Fabric sees) is `~/.gradle/caches/fabric-loom/26.1.2/minecraft-merged.jar` — unobfuscated, so `javap -cp <jar> <class>` shows real signatures. Every API in this plan was checked against these on 2026-09-23.
- **Sizes (spec §3):** 12 selections; price **0–999**; quantity **1 to the item's max stack size**; stock **27** slots; cash box **27**; tray **9**; credit **9 stacks per player per machine**.
- **Currency (spec §5.5, Plan 3 part):** the item tag **`#diamondvending:currency`**, shipped containing only `minecraft:diamond`. Matching is by item type — names and other components are ignored. (The admin currency slot and catalog currency are Plan 5.)
- **Funds (spec §3.3):** the buyer's credit on this machine + currency in their **main inventory, hotbar and offhand** (inventory slots 0–35 and 40) — never armor, never inside shulker boxes or bundles. **Credit is spent first**, then inventory. All checks happen before anything changes.
- **Owned machines:** payment → cash box, goods come out of stock. **Infinite machines:** stock is ignored and payment is destroyed.
- **Clicks (spec §3.2):** resolved on the server from the hit location. Order: owner/admin holding a dye → recolor; otherwise the **front-face** region decides (button → buy, coin slot → insert credit, coin return → return credit, tray → collect). Other regions and other faces do nothing. **Every click on any part is consumed**, so held blocks are never placed against the machine. A non-owner holding a dye who clicks something that isn't a button/coin slot/coin return/tray gets "Only the owner can do that."
- **Coin slot:** takes the whole held stack of currency — main hand if it's currency, otherwise the offhand. **Coin return and tray** move everything to the player's inventory; whatever doesn't fit drops at their feet.
- **Messages (spec §3.5, exact English; keys in `core/Texts`):**
  - `message.diamondvending.button_empty` "Button %s has nothing for sale."
  - `message.diamondvending.sold_out` "Button %s is sold out."
  - `message.diamondvending.need_money` "Button %s costs %s. You have %s." (button number, money, credit + inventory)
  - `message.diamondvending.wrong_currency` "This machine takes %s."
  - `message.diamondvending.credit_full` "You can't put in any more. Buy something or press coin return."
  - `problem.diamondvending.<problem>.explanation` — the five explanations from spec §3.5 b, word for word; the cash box one names the currency with `%s`.
  - Money text: `currency.diamondvending.<namespace>.<path>.one` / `.many` / `.name` ("%s diamond", "%s diamonds", "diamonds"). Currencies without these keys fall back to "3 × Emerald" / "Emerald".
- **Sounds (spec §7):** button press `STONE_BUTTON_CLICK_ON`; credit inserted `CHAIN_PLACE`; vend `DISPENSER_DISPENSE`; thank you `EXPERIENCE_ORB_PICKUP` (quiet); error `NOTE_BLOCK_BASS` (low pitch); coin return and tray `ITEM_PICKUP`.
- **Saved data** (what map makers write with `/data`; keep these names stable):
  `owner` (int-array UUID), `owner_name`, `infinite` (byte), `selections: [{slot: 0–11, item: {id, count, components?}, price}]` (only set-up buttons; slot 0 = button 1), `stock` / `cash_box` / `tray`: `{Items: [{Slot, id, count, …}]}`, `credits: [{player: <UUID>, Items: […]}]`.
- **Update tag (sent to clients):** the saved data **minus** `stock`, `cash_box` and `credits`, **plus** `sync_stock` (int[12]: matching items in stock per button), `sync_credits` (int[]: for each player with credit, their UUID as 4 ints then their spendable total) and `sync_problems` (int[]: `Problem` ordinals). Never saved to disk.
- **Breaking** (spec §5.3): the tray, every player's credit, the stock and the cash box all spill — in any mode (nobody's items vanish). Recoloring spills nothing.
- **Deferred out of this plan:** everything drawn on the client (Plan 4); the setup screen, owner sneak-with-item hint, currency slot, catalogs and setup-on-item (Plan 5); the manual (Plan 6).

## Review Focus

1. **Currency inside a shulker box or bundle** — it must not pay; only loose stacks count (spec §3.3 says so, and it's easy to break by scanning container components). → Task 5, `currencyInsideContainersDoesNotPay`.
2. **Items with names or enchantments** — renamed diamonds still count as money and come back from coin return still renamed; stock only counts items whose components match the button's template exactly. → Task 4, `coinReturnGivesBackTheExactItems`; Task 5, `stockMustMatchTheTemplateExactly`.
3. **A full inventory** when collecting the tray or returning credit — the rest drops at the player's feet; nothing vanishes. → Task 3, `aFullInventoryDropsTheRestAtYourFeet` (coin return uses the same `PlayerItems.give`).
4. **Two players with credit on one machine** — neither can spend or take back the other's. → Task 4, `creditBelongsToOnePlayer`; Task 5, `nobodyElseCanSpendYourCredit`.
5. **Paying partly from credit and partly from the inventory, or being one short** — split exactly, credit first; one short takes nothing at all. → Task 5, `creditIsSpentFirst`, `oneShortTakesNothing`.

---

## File Structure

```
src/main/java/diamondvending/
  core/Texts.java                        every player-facing translation key (unit-tested against en_us.json)
  Messages.java                          action bar + problem explanations (keys moved to core/Texts)
  shop/Currency.java                     the currency tag, matching, money text
  shop/Selection.java                    one button: template stack (count = quantity) + price
  shop/ItemSlots.java                    count / insert / take helpers for item lists
  shop/PlayerItems.java                  a player's pay slots; give-or-drop
  shop/MachineSounds.java                the spec §7 sounds
  shop/PickupTray.java                   collect the tray
  shop/CoinSlot.java                     insert credit, coin return
  shop/Purchase.java                     the all-or-nothing purchase
  block/FrontFace.java                   click location → core/Hit
  block/VendingMachineBlock.java         click routing; 1.21.1 spill hook
  block/VendingMachineBlockEntity.java   contents, save/load, update tag, problems, spill
src/main/resources/
  assets/diamondvending/lang/en_us.json  + messages, problem explanations, diamond money text
  data/diamondvending/tags/item/currency.json
src/test/java/diamondvending/core/TextsTest.java
src/gametest/java/diamondvending/gametest/
  BuyingTests.java                       Plan 3's test bodies (+ ALL)
  RecordingPlayer.java                   mock player that remembers its action-bar messages
  AllTests.java                          MachineTests.ALL + BuyingTests.ALL, for NeoForge 26.1
  fabric/FabricBuyingTests.java          Fabric entrypoint for BuyingTests
  neoforge/NeoForgeBuyingTests.java      NeoForge 1.21.1 @GameTestHolder for BuyingTests
  neoforge/NeoForgeGameTestMod.java      registers AllTests.ALL on 26.1
src/gametest/resources/fabric.mod.json   + FabricBuyingTests entrypoint
docs/dev-setup.md, CHANGELOG.md, docs/superpowers/plans/2026-09-23-roadmap.md
```

**Adding a buying test (every task):** a `public static void name(GameTestHelper)` in `BuyingTests` + its `ALL` entry (snake_case name), a method in `FabricBuyingTests`, and a method inside the 1.21.1 block of `NeoForgeBuyingTests` — exactly the Plan 2 pattern. Each task below lists all three.

**Test counts** ("All N required tests passed"): today 19 on 26.1 (18 ours + vanilla's `always_pass`) and 18 on 1.21.1. Each task adds: Task 1 +2, Task 2 +6, Task 3 +6, Task 4 +6, Task 5 +12, Task 6 +3.

**Gradle commands used below** (prefix `JAVA_HOME=…` if needed):
- `G26N` = `./gradlew :26.1-neoforge:test :26.1-neoforge:runGameTestServer`
- `G26F` = `./gradlew :26.1-fabric:test :26.1-fabric:runGametest`
- `G121N` = `./gradlew :1.21.1-neoforge:test :1.21.1-neoforge:runGameTestServer`
- `G121F` = `./gradlew :1.21.1-fabric:test :1.21.1-fabric:runGametest`

Run them one at a time, redirect output to a log in the workspace, and read the tail (`All N required tests passed :)` or the failures).

---

### Task 1: Currency and message keys

Deliverable: the default currency (diamonds via a tag) with natural money text, all player-facing keys in one core class checked against `en_us.json`, and the buying test harness wired into all four targets.

**Files:**
- Create: `src/main/java/diamondvending/core/Texts.java`, `src/test/java/diamondvending/core/TextsTest.java`
- Create: `src/main/java/diamondvending/shop/Currency.java`, `src/main/resources/data/diamondvending/tags/item/currency.json`
- Modify: `src/main/java/diamondvending/Messages.java`, `src/main/java/diamondvending/block/VendingMachineBlock.java`, `src/main/resources/assets/diamondvending/lang/en_us.json`
- Create: `src/gametest/java/diamondvending/gametest/BuyingTests.java`, `AllTests.java`, `fabric/FabricBuyingTests.java`, `neoforge/NeoForgeBuyingTests.java`
- Modify: `src/gametest/java/diamondvending/gametest/neoforge/NeoForgeGameTestMod.java`, `src/gametest/resources/fabric.mod.json`, `docs/dev-setup.md`

**Interfaces:**
- Produces: `core.Texts` — `String OWNER_ONLY`, `String DIAMOND` (key prefix `currency.diamondvending.minecraft.diamond`), `static List<String> all()`. Later tasks add constants and extend `all()`.
- Produces: `shop.Currency` — `static final Currency DEFAULT`, `static final TagKey<Item> TAG`, `boolean matches(ItemStack)`, `Item displayItem()`, `Component money(int count)`, `Component name()`.
- Produces: `BuyingTests.ALL`, `BuyingTests.translation(GameTestHelper, Component, String): TranslatableContents`; `AllTests.ALL`.
- Consumes: `Messages.actionBar(Player, Component)` (Plan 2).

- [ ] **Step 1: Write the failing unit test**

`src/test/java/diamondvending/core/TextsTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Every key a player can be shown must have English text (spec §10: lang completeness). */
class TextsTest {
    private static final Path LANG = Path.of(System.getProperty("diamondvending.root", "../.."),
            "src/main/resources/assets/diamondvending/lang/en_us.json");
    private static final Pattern ENTRY = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static Map<String, String> english() throws IOException {
        Map<String, String> entries = new HashMap<>();
        Matcher matcher = ENTRY.matcher(Files.readString(LANG));
        while (matcher.find()) entries.put(matcher.group(1), matcher.group(2));
        return entries;
    }

    @Test
    void everyKeyHasEnglishText() throws IOException {
        Map<String, String> english = english();
        for (String key : Texts.all()) {
            assertFalse(english.getOrDefault(key, "").isBlank(), "en_us.json has no text for " + key);
        }
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :26.1-neoforge:test --tests diamondvending.core.TextsTest`
Expected: FAIL — compilation error, `cannot find symbol: variable Texts`.

- [ ] **Step 3: Create `Texts`, move the owner-only key there, add the English text**

`src/main/java/diamondvending/core/Texts.java`:

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
    /** Prefix of the keys that name the default currency: {@code .one}, {@code .many}, {@code .name} (see shop/Currency). */
    public static final String DIAMOND = "currency.diamondvending.minecraft.diamond";

    private Texts() {}

    /** Every key a player can see. */
    public static List<String> all() {
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
}
```

Replace `src/main/java/diamondvending/Messages.java` with:

```java
package diamondvending;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Sends player-facing messages. Every failure names the reason and who can fix it (spec §3.5); keys live in core/Texts. */
public final class Messages {
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

In `VendingMachineBlock.java`, add `import diamondvending.core.Texts;` and change both `Component.translatable(Messages.OWNER_ONLY)` to `Component.translatable(Texts.OWNER_ONLY)`.

Replace `src/main/resources/assets/diamondvending/lang/en_us.json` with:

```json
{
  "block.diamondvending.vending_machine": "Vending Machine",
  "message.diamondvending.owner_only": "Only the owner can do that.",
  "currency.diamondvending.minecraft.diamond.one": "%s diamond",
  "currency.diamondvending.minecraft.diamond.many": "%s diamonds",
  "currency.diamondvending.minecraft.diamond.name": "diamonds"
}
```

- [ ] **Step 4: Run it to verify it passes**

Run: `./gradlew :26.1-neoforge:test --tests diamondvending.core.TextsTest`
Expected: PASS (1 test).

- [ ] **Step 5: Write the failing currency GameTests and the buying test harness**

`src/gametest/java/diamondvending/gametest/BuyingTests.java`:

```java
package diamondvending.gametest;

import diamondvending.shop.Currency;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game tests for buying (Plan 3). Each test also needs a method in {@code fabric/FabricBuyingTests} and (for 1.21.1)
 * {@code neoforge/NeoForgeBuyingTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 *
 * <p>Machines here are placed by mock players facing south, so they face north: the front is the machine's north side
 * and its right-hand column is at x − 1 (see {@link MachineTests}).
 */
public final class BuyingTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("diamonds_are_the_default_currency", BuyingTests::diamondsAreTheDefaultCurrency),
            Map.entry("money_reads_naturally", BuyingTests::moneyReadsNaturally));

    private BuyingTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** The component's translation, after checking its key. */
    static TranslatableContents translation(GameTestHelper helper, Component component, String key) {
        helper.assertTrue(component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected the text " + key + " but got " + component);
        return (TranslatableContents) component.getContents();
    }

    // ---- currency ------------------------------------------------------------------------------------------------

    public static void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        Currency currency = Currency.DEFAULT;
        ItemStack renamed = new ItemStack(Items.DIAMOND);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Lucky"));
        helper.assertTrue(currency.matches(new ItemStack(Items.DIAMOND, 5)), "diamonds should be money");
        helper.assertTrue(currency.matches(renamed), "a renamed diamond is still a diamond");
        helper.assertFalse(currency.matches(new ItemStack(Items.EMERALD)), "emeralds are not money unless a datapack says so");
        helper.assertFalse(currency.matches(ItemStack.EMPTY), "an empty hand is not money");
        helper.assertTrue(currency.displayItem() == Items.DIAMOND, "prices should show a diamond");
        helper.succeed();
    }

    public static void moneyReadsNaturally(GameTestHelper helper) {
        TranslatableContents one = translation(helper, Currency.DEFAULT.money(1), "currency.diamondvending.minecraft.diamond.one");
        TranslatableContents three = translation(helper, Currency.DEFAULT.money(3), "currency.diamondvending.minecraft.diamond.many");
        helper.assertTrue(one.getArgs()[0].equals(1) && three.getArgs()[0].equals(3), "the amount should be the first argument");
        helper.assertTrue("%s × %s".equals(three.getFallback()), "unnamed currencies should read like \"3 × Emerald\"");
        translation(helper, Currency.DEFAULT.name(), "currency.diamondvending.minecraft.diamond.name");
        helper.succeed();
    }
}
```

`src/gametest/java/diamondvending/gametest/AllTests.java`:

```java
package diamondvending.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Every game test by name, for loaders that register tests from a list (NeoForge 26.1). */
public final class AllTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = combine(List.of(MachineTests.ALL, BuyingTests.ALL));

    private AllTests() {}

    private static Map<String, Consumer<GameTestHelper>> combine(List<Map<String, Consumer<GameTestHelper>>> groups) {
        Map<String, Consumer<GameTestHelper>> all = new LinkedHashMap<>();
        for (Map<String, Consumer<GameTestHelper>> group : groups) {
            group.forEach((name, test) -> {
                if (all.putIfAbsent(name, test) != null) throw new IllegalStateException("two game tests are called " + name);
            });
        }
        return Map.copyOf(all);
    }
}
```

`src/gametest/java/diamondvending/gametest/fabric/FabricBuyingTests.java`:

```java
package diamondvending.gametest.fabric;

import diamondvending.gametest.BuyingTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/** Fabric entrypoint for {@link BuyingTests} (see {@link FabricGameTests} for why the annotations differ per version). */
public final class FabricBuyingTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        BuyingTests.diamondsAreTheDefaultCurrency(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void moneyReadsNaturally(GameTestHelper helper) {
        BuyingTests.moneyReadsNaturally(helper);
    }
}
```

`src/gametest/java/diamondvending/gametest/neoforge/NeoForgeBuyingTests.java`:

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.BuyingTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * {@link BuyingTests} for NeoForge 1.21.1, found through {@code @GameTestHolder} like {@link NeoForgeGameTests}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeBuyingTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        BuyingTests.diamondsAreTheDefaultCurrency(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void moneyReadsNaturally(GameTestHelper helper) {
        BuyingTests.moneyReadsNaturally(helper);
    }
    *///?}
}
```

Replace `src/gametest/java/diamondvending/gametest/neoforge/NeoForgeGameTestMod.java` with (only `AllTests` is new — the structure and tick constants still come from `MachineTests`):

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.AllTests;
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

/** The test-only mod. On 26.1 it registers each {@link AllTests#ALL} entry as a test function and a test instance. */
@Mod("diamondvending_gametest")
public final class NeoForgeGameTestMod {
    public NeoForgeGameTestMod(IEventBus modBus) {
        //? if >=26.1 {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, DiamondVending.MOD_ID);
        AllTests.ALL.forEach((name, test) -> functions.register(name, () -> test));
        functions.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, event -> {
            Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(DiamondVending.id("default"));
            AllTests.ALL.keySet().forEach(name -> event.registerTest(DiamondVending.id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, DiamondVending.id(name)),
                    new TestData<>(environment, DiamondVending.id(MachineTests.STRUCTURE_NAME), MachineTests.MAX_TICKS, 0, true))));
        });
        //?}
    }
}
```

In `src/gametest/resources/fabric.mod.json`, change the entrypoint list to:

```json
    "fabric-gametest": [
      "diamondvending.gametest.fabric.FabricGameTests",
      "diamondvending.gametest.fabric.FabricBuyingTests"
    ]
```

- [ ] **Step 6: Run the GameTests to verify they fail**

Run: `G26N`
Expected: FAIL — compilation error in the gametest source set, `package diamondvending.shop does not exist`.

- [ ] **Step 7: Create the currency**

`src/main/resources/data/diamondvending/tags/item/currency.json`:

```json
{
  "values": [
    "minecraft:diamond"
  ]
}
```

`src/main/java/diamondvending/shop/Currency.java`:

```java
package diamondvending.shop;

import diamondvending.DiamondVending;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a machine takes as money (spec §5.5). Plan 3 has only the default: the item tag {@code #diamondvending:currency},
 * which ships with just diamonds and which packs may change by datapack. Items match by type; names, enchantments and
 * other components are ignored.
 */
public final class Currency {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, DiamondVending.id("currency"));
    public static final Currency DEFAULT = new Currency();

    private Currency() {}

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TAG);
    }

    /** The item shown on price tags and in messages: the tag's first item, or a diamond if a datapack emptied the tag. */
    public Item displayItem() {
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TAG)) {
            return item.value();
        }
        return Items.DIAMOND;
    }

    /**
     * An amount of money, e.g. "3 diamonds". Lang files can name any currency with
     * {@code currency.diamondvending.<namespace>.<path>.one} / {@code .many}; without them it reads "3 × Emerald".
     */
    public Component money(int count) {
        String key = langKey() + (count == 1 ? ".one" : ".many");
        return Component.translatableWithFallback(key, "%s × %s", count, itemName());
    }

    /** The currency's plural name, e.g. "diamonds" ({@code currency.diamondvending.<namespace>.<path>.name}). */
    public Component name() {
        return Component.translatableWithFallback(langKey() + ".name", "%s", itemName());
    }

    private String langKey() {
        return "currency." + DiamondVending.MOD_ID + "." + BuiltInRegistries.ITEM.getKey(displayItem()).toLanguageKey();
    }

    private Component itemName() {
        return new ItemStack(displayItem()).getHoverName();
    }
}
```

- [ ] **Step 8: Run the GameTests to verify they pass**

Run: `G26N`
Expected: PASS — unit tests green; `All 21 required tests passed :)`.

- [ ] **Step 9: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: `All 21 required tests passed` on 26.1 Fabric; `All 20 required tests passed` on both 1.21.1 nodes; unit tests green.

- [ ] **Step 10: Document the new test layout**

In `docs/dev-setup.md`, replace the paragraph starting "Add a test in three places" with:

```markdown
Add a test in three places: a `public static void` method in `gametest/MachineTests.java` or `gametest/BuyingTests.java`
(+ its `ALL` entry), a method in the matching `gametest/fabric/Fabric*Tests.java`, and one in the 1.21.1 block of the
matching `gametest/neoforge/NeoForge*Tests.java`. A new test class also goes into `AllTests` and the test mod's
`fabric.mod.json` entrypoints. Use `MachineTests.platform(x, y, z)` for fixed positions: 1.21.1 and 26.1 measure test
coordinates from different origins. Put items in the mock player's hand before `placeAt`/`useBlock` — placement reads
the item in hand. `RecordingPlayer` is a mock player that remembers its action-bar messages.
```

- [ ] **Step 11: Check the Stonecutter state and commit**

Run: `./gradlew "Refresh active project"`; `git -C /c/Users/benet/mcvending status --short` must list only this task's files. (If Refresh rewrote any `//?` block, keep its version — that is what CI checks.)

```bash
git -C /c/Users/benet/mcvending add -A src docs/dev-setup.md
git -C /c/Users/benet/mcvending commit -m "feat: diamond currency, message keys, buying test harness"
```

---

### Task 2: Machine contents — save, load, sync, problems

Deliverable: the master block entity holds 12 selections, stock, cash box, tray, per-player credit and the infinite flag; saves them in the documented format; loads unknown items as empty buttons; sends clients only what they draw plus derived numbers; and works out its problems.

**Files:**
- Create: `src/main/java/diamondvending/shop/Selection.java`, `src/main/java/diamondvending/shop/ItemSlots.java`
- Modify (rewrite): `src/main/java/diamondvending/block/VendingMachineBlockEntity.java`
- Modify: `BuyingTests.java`, `FabricBuyingTests.java`, `NeoForgeBuyingTests.java`

**Interfaces:**
- Consumes: `core.MachineLayout.SELECTIONS`, `core.MachineProblems.of(MachineFacts)`, `core.Problem` (Plan 1); `shop.Currency` (Task 1).
- Produces: `shop.Selection` — `record Selection(ItemStack template, int price)`, `static final int MAX_PRICE = 999`, `static final Selection EMPTY`, `static Selection of(ItemStack, int)`, `boolean isSetUp()`, `int quantity()`, `boolean sells(ItemStack)`. The template is never modified; copy it.
- Produces: `shop.ItemSlots` — `count(List<ItemStack>, Predicate<ItemStack>)`, `isEmpty(List)`, `hasEmptySlot(List)`, `insert(List, ItemStack): ItemStack` (returns what didn't fit, input untouched), `fitsAll(List, List): boolean`, `take(List, Predicate, int): List<ItemStack>`, `takeAll(List): List<ItemStack>`, `copyOf(List): List<ItemStack>`, `clear(List)`.
- Produces on `VendingMachineBlockEntity`: `STOCK_SLOTS = 27`, `CASH_BOX_SLOTS = 27`, `TRAY_SLOTS = 9`, `CREDIT_SLOTS = 9`; `getSelection(int)`, `setSelection(int, Selection)`, `isInfinite()`, `setInfinite(boolean)`, `stock()`, `cashBox()`, `tray()` (live `NonNullList<ItemStack>`), `credit(UUID): List<ItemStack>` (live, or an empty list), `creditOf(UUID): NonNullList<ItemStack>` (live, created on demand), `currency(): Currency`, `stockCountFor(int)`, `problems(): List<Problem>`, `changed()` (save + resync after any edit), and client-side `syncedStockCount(int)`, `syncedCredit(UUID)`, `syncedProblems()`. `getOwner`/`getOwnerName`/`setOwner` keep their Plan 2 signatures.

- [ ] **Step 1: Write the failing GameTests**

In `BuyingTests.java`, add these imports:

```java
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Problem;
import diamondvending.shop.Selection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if <26.1 {
/*import net.minecraft.nbt.Tag;
*///?}

import java.util.List;
import java.util.UUID;
```

Add to `ALL` (keep the existing entries):

```java
            Map.entry("contents_survive_save_and_load", BuyingTests::contentsSurviveSaveAndLoad),
            Map.entry("unknown_items_load_as_empty_selections", BuyingTests::unknownItemsLoadAsEmptySelections),
            Map.entry("clients_get_only_what_they_need", BuyingTests::clientsGetOnlyWhatTheyNeed),
            Map.entry("problems_follow_the_machine", BuyingTests::problemsFollowTheMachine),
            Map.entry("infinite_machines_have_no_owner_problems", BuyingTests::infiniteMachinesHaveNoOwnerProblems),
            Map.entry("selections_are_kept_in_range", BuyingTests::selectionsAreKeptInRange)
```

Add these helpers under `// ---- helpers ----`:

```java
    static VendingMachineBlockEntity placeMachine(GameTestHelper helper, Player owner) {
        MachineTests.placeOn(helper, owner, MachineTests.machineItem(1), MachineTests.FLOOR);
        VendingMachineBlockEntity machine = MachineTests.machineAt(helper, MachineTests.MASTER);
        helper.assertTrue(machine != null, "the machine should have been placed");
        return machine;
    }

    static HolderLookup.Provider registries(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    /** Loads {@code tag} into a new block entity at the machine's spot, the way the game loads a chunk or a client does. */
    static VendingMachineBlockEntity reload(GameTestHelper helper, VendingMachineBlockEntity machine, CompoundTag tag) {
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), tag, registries(helper));
        helper.assertTrue(loaded instanceof VendingMachineBlockEntity, "the saved machine should load, got " + loaded);
        return (VendingMachineBlockEntity) loaded;
    }

    static void assertSelection(GameTestHelper helper, VendingMachineBlockEntity machine, int index, Item item, int quantity, int price) {
        Selection selection = machine.getSelection(index);
        helper.assertTrue(selection.template().is(item) && selection.quantity() == quantity && selection.price() == price,
                "button " + (index + 1) + " should sell " + quantity + " × " + item + " for " + price + ", but has " + selection);
    }

    static int countIn(List<ItemStack> slots, Item item) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    static void fill(List<ItemStack> slots, Item item) {
        for (int i = 0; i < slots.size(); i++) slots.set(i, new ItemStack(item, 64));
    }

    static void assertProblems(GameTestHelper helper, VendingMachineBlockEntity machine, Problem... expected) {
        helper.assertTrue(machine.problems().equals(List.of(expected)),
                "expected problems " + List.of(expected) + " but got " + machine.problems());
    }
```

Add the tests at the end of the class:

```java
    // ---- contents ------------------------------------------------------------------------------------------------

    public static void contentsSurviveSaveAndLoad(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        UUID buyer = UUID.randomUUID();
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.setSelection(11, Selection.of(new ItemStack(Items.ARROW, 16), 0));
        machine.setInfinite(true);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        machine.cashBox().set(26, new ItemStack(Items.DIAMOND, 7));
        machine.tray().set(8, new ItemStack(Items.BREAD, 1));
        machine.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));

        VendingMachineBlockEntity loaded = reload(helper, machine, machine.saveWithFullMetadata(registries(helper)));
        assertSelection(helper, loaded, 0, Items.APPLE, 2, 3);
        assertSelection(helper, loaded, 11, Items.ARROW, 16, 0);
        helper.assertFalse(loaded.getSelection(5).isSetUp(), "button 6 was never set up");
        helper.assertTrue(loaded.isInfinite(), "the infinite flag should be saved");
        helper.assertTrue(countIn(loaded.stock(), Items.APPLE) == 10, "stock should be saved");
        helper.assertTrue(loaded.cashBox().get(26).getCount() == 7, "the cash box should be saved slot for slot");
        helper.assertTrue(loaded.tray().get(8).is(Items.BREAD), "the tray should be saved");
        helper.assertTrue(countIn(loaded.credit(buyer), Items.DIAMOND) == 4, "credit should be saved per player");
        helper.assertTrue(machine.getOwner().equals(loaded.getOwner()), "the owner should be saved");
        helper.succeed();
    }

    /** Spec §9: an item from a removed mod loads as an empty button; the rest of the machine keeps working. */
    public static void unknownItemsLoadAsEmptySelections(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE), 1));
        machine.setSelection(1, Selection.of(new ItemStack(Items.BREAD), 2));
        CompoundTag saved = machine.saveWithFullMetadata(registries(helper));
        // Pretend button 1's item came from a mod that has since been removed.
        //? if >=26.1 {
        saved.getListOrEmpty("selections").getCompoundOrEmpty(0).getCompoundOrEmpty("item").putString("id", "notamod:gadget");
        //?} else {
        /*saved.getList("selections", Tag.TAG_COMPOUND).getCompound(0).getCompound("item").putString("id", "notamod:gadget");
        *///?}
        VendingMachineBlockEntity loaded = reload(helper, machine, saved);
        helper.assertFalse(loaded.getSelection(0).isSetUp(), "an unknown item should load as an empty button");
        assertSelection(helper, loaded, 1, Items.BREAD, 1, 2);
        helper.succeed();
    }

    /** Spec §8.3: clients get what they draw and a few totals, never the stock, cash box or anyone's credit items. */
    public static void clientsGetOnlyWhatTheyNeed(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        UUID buyer = UUID.randomUUID();
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        machine.cashBox().set(0, new ItemStack(Items.DIAMOND, 5));
        fill(machine.tray(), Items.BREAD);
        machine.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));

        CompoundTag update = machine.getUpdateTag(registries(helper));
        for (String secret : List.of("stock", "cash_box", "credits")) {
            helper.assertFalse(update.contains(secret), "clients must not be sent the machine's " + secret);
        }
        update.putString("id", "diamondvending:vending_machine");
        VendingMachineBlockEntity client = reload(helper, machine, update);
        assertSelection(helper, client, 0, Items.APPLE, 2, 3);
        helper.assertTrue(client.tray().get(0).is(Items.BREAD), "clients draw the tray, so they need its items");
        helper.assertTrue(client.syncedStockCount(0) == 10, "clients need how much of each item is in stock");
        helper.assertTrue(client.syncedCredit(buyer) == 4, "each player's display shows their own credit");
        helper.assertTrue(client.syncedCredit(UUID.randomUUID()) == 0, "players without credit have none");
        helper.assertTrue(client.syncedProblems().equals(List.of(Problem.TRAY_FULL)), "clients show the machine's problems");
        helper.succeed();
    }

    public static void problemsFollowTheMachine(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        assertProblems(helper, machine, Problem.NOT_SET_UP);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 1));
        assertProblems(helper, machine, Problem.SOLD_OUT);
        machine.stock().set(0, new ItemStack(Items.APPLE, 2));
        assertProblems(helper, machine);
        fill(machine.cashBox(), Items.COBBLESTONE);
        assertProblems(helper, machine, Problem.CASH_BOX_FULL);
        fill(machine.tray(), Items.COBBLESTONE);
        assertProblems(helper, machine, Problem.CASH_BOX_FULL, Problem.TRAY_FULL);
        machine.cashBox().clear(); // NonNullList.clear() empties every slot
        assertProblems(helper, machine, Problem.TRAY_FULL);
        helper.succeed();
    }

    public static void infiniteMachinesHaveNoOwnerProblems(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        machine.setInfinite(true);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE), 1));
        fill(machine.cashBox(), Items.COBBLESTONE);
        assertProblems(helper, machine);
        fill(machine.tray(), Items.COBBLESTONE);
        assertProblems(helper, machine, Problem.TRAY_FULL);
        helper.succeed();
    }

    public static void selectionsAreKeptInRange(GameTestHelper helper) {
        Selection pearls = Selection.of(new ItemStack(Items.ENDER_PEARL, 40), 5000);
        helper.assertTrue(pearls.quantity() == 16, "quantity can't be more than one stack (16 pearls), was " + pearls.quantity());
        helper.assertTrue(pearls.price() == Selection.MAX_PRICE, "prices stop at 999, was " + pearls.price());
        helper.assertTrue(Selection.of(new ItemStack(Items.APPLE), -3).price() == 0, "prices can't be negative");
        helper.assertFalse(Selection.of(ItemStack.EMPTY, 3).isSetUp(), "no item means an empty button");
        helper.succeed();
    }
```

Add to `FabricBuyingTests.java` (one method per test, same annotation block as the existing ones):

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void contentsSurviveSaveAndLoad(GameTestHelper helper) {
        BuyingTests.contentsSurviveSaveAndLoad(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void unknownItemsLoadAsEmptySelections(GameTestHelper helper) {
        BuyingTests.unknownItemsLoadAsEmptySelections(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void clientsGetOnlyWhatTheyNeed(GameTestHelper helper) {
        BuyingTests.clientsGetOnlyWhatTheyNeed(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void problemsFollowTheMachine(GameTestHelper helper) {
        BuyingTests.problemsFollowTheMachine(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void infiniteMachinesHaveNoOwnerProblems(GameTestHelper helper) {
        BuyingTests.infiniteMachinesHaveNoOwnerProblems(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void selectionsAreKeptInRange(GameTestHelper helper) {
        BuyingTests.selectionsAreKeptInRange(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeBuyingTests.java` (before `*///?}`):

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void contentsSurviveSaveAndLoad(GameTestHelper helper) {
        BuyingTests.contentsSurviveSaveAndLoad(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void unknownItemsLoadAsEmptySelections(GameTestHelper helper) {
        BuyingTests.unknownItemsLoadAsEmptySelections(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void clientsGetOnlyWhatTheyNeed(GameTestHelper helper) {
        BuyingTests.clientsGetOnlyWhatTheyNeed(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void problemsFollowTheMachine(GameTestHelper helper) {
        BuyingTests.problemsFollowTheMachine(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void infiniteMachinesHaveNoOwnerProblems(GameTestHelper helper) {
        BuyingTests.infiniteMachinesHaveNoOwnerProblems(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void selectionsAreKeptInRange(GameTestHelper helper) {
        BuyingTests.selectionsAreKeptInRange(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors (`cannot find symbol: class Selection`, `method setSelection`, …).

- [ ] **Step 3: Create `Selection` and `ItemSlots`**

`src/main/java/diamondvending/shop/Selection.java`:

```java
package diamondvending.shop;

import net.minecraft.world.item.ItemStack;

/**
 * One of a machine's 12 buttons (spec §3.1): what it sells and for how much. The template's count is the quantity per
 * purchase and its components (names, enchantments…) are part of what's sold. Never modify the template — copy it.
 */
public record Selection(ItemStack template, int price) {
    public static final int MAX_PRICE = 999;
    public static final Selection EMPTY = new Selection(ItemStack.EMPTY, 0);

    /** A valid selection: quantity kept to 1..max stack size and price to 0..999. An empty stack gives {@link #EMPTY}. */
    public static Selection of(ItemStack template, int price) {
        if (template.isEmpty()) return EMPTY;
        int quantity = Math.max(1, Math.min(template.getCount(), template.getMaxStackSize()));
        return new Selection(template.copyWithCount(quantity), Math.max(0, Math.min(price, MAX_PRICE)));
    }

    public boolean isSetUp() {
        return !template.isEmpty();
    }

    /** Items handed out per purchase. */
    public int quantity() {
        return template.getCount();
    }

    /** Whether a stack is what this button sells: same item and same components (spec §3.3). */
    public boolean sells(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(stack, template);
    }
}
```

`src/main/java/diamondvending/shop/ItemSlots.java`:

```java
package diamondvending.shop;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Helpers for item lists: the machine's stock, cash box, tray and credit, and a player's pay slots. Empty stacks are free slots. */
public final class ItemSlots {
    private ItemSlots() {}

    /** Total items in stacks that match. */
    public static int count(List<ItemStack> slots, Predicate<ItemStack> matching) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && matching.test(stack)) total += stack.getCount();
        }
        return total;
    }

    public static boolean isEmpty(List<ItemStack> slots) {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public static boolean hasEmptySlot(List<ItemStack> slots) {
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Adds a copy of {@code stack}: first topping up stacks of the same item and components, then filling empty slots.
     * Returns what didn't fit (empty if it all did). {@code stack} itself is not changed.
     */
    public static ItemStack insert(List<ItemStack> slots, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
            ItemStack slot = slots.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (moved > 0) {
                    slot.grow(moved);
                    rest.shrink(moved);
                }
            }
        }
        for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
            }
        }
        return rest;
    }

    /** Whether all of {@code stacks} would fit, without changing {@code slots}. */
    public static boolean fitsAll(List<ItemStack> slots, List<ItemStack> stacks) {
        List<ItemStack> trial = copyOf(slots);
        for (ItemStack stack : stacks) {
            if (!insert(trial, stack).isEmpty()) return false;
        }
        return true;
    }

    /**
     * Removes up to {@code amount} matching items in slot order and returns them, names and other components kept.
     * Works on live lists such as {@link PlayerItems#paySlots}: the stacks shrink where they are.
     */
    public static List<ItemStack> take(List<ItemStack> slots, Predicate<ItemStack> matching, int amount) {
        List<ItemStack> taken = new ArrayList<>();
        int left = amount;
        for (ItemStack stack : slots) {
            if (left <= 0) break;
            if (stack.isEmpty() || !matching.test(stack)) continue;
            ItemStack part = stack.split(Math.min(left, stack.getCount()));
            left -= part.getCount();
            taken.add(part);
        }
        return taken;
    }

    /** Empties every slot and returns what was in them. */
    public static List<ItemStack> takeAll(List<ItemStack> slots) {
        List<ItemStack> taken = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).isEmpty()) taken.add(slots.get(i));
            slots.set(i, ItemStack.EMPTY);
        }
        return taken;
    }

    /** A deep copy, for trying changes out. */
    public static List<ItemStack> copyOf(List<ItemStack> slots) {
        List<ItemStack> copy = new ArrayList<>(slots.size());
        for (ItemStack stack : slots) copy.add(stack.copy());
        return copy;
    }

    public static void clear(List<ItemStack> slots) {
        Collections.fill(slots, ItemStack.EMPTY);
    }
}
```

- [ ] **Step 4: Rewrite the block entity**

Replace `src/main/java/diamondvending/block/VendingMachineBlockEntity.java` with:

```java
package diamondvending.block;

import diamondvending.DiamondVending;
import diamondvending.core.MachineFacts;
import diamondvending.core.MachineLayout;
import diamondvending.core.MachineProblems;
import diamondvending.core.Problem;
import diamondvending.registry.ModContent;
import diamondvending.shop.Currency;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.Optional;
//?} else {
/*import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
*///?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything a machine holds, kept on the master part (spec §8.3): owner, 12 selections, stock, cash box, tray,
 * each player's credit and the infinite flag. Saved in a format map makers can write with {@code /data}; clients get
 * a trimmed copy (see {@link #getUpdateTag}).
 */
public class VendingMachineBlockEntity extends BlockEntity {
    public static final int STOCK_SLOTS = 27;
    public static final int CASH_BOX_SLOTS = 27;
    public static final int TRAY_SLOTS = 9;
    /** Credit is capped at 9 stacks per player per machine (spec §3.4). */
    public static final int CREDIT_SLOTS = 9;

    // Saved keys — also what map makers write with /data, so keep them stable.
    private static final String OWNER = "owner";
    private static final String OWNER_NAME = "owner_name";
    private static final String INFINITE = "infinite";
    private static final String SELECTIONS = "selections";
    private static final String SLOT = "slot";
    private static final String ITEM = "item";
    private static final String PRICE = "price";
    private static final String STOCK = "stock";
    private static final String CASH_BOX = "cash_box";
    private static final String TRAY = "tray";
    private static final String CREDITS = "credits";
    private static final String PLAYER = "player";
    // Sent to clients in the update tag only, never saved.
    private static final String SYNC_STOCK = "sync_stock";
    private static final String SYNC_CREDITS = "sync_credits";
    private static final String SYNC_PROBLEMS = "sync_problems";

    private UUID owner;
    private String ownerName = "";
    private boolean infinite;
    private final Selection[] selections = new Selection[MachineLayout.SELECTIONS];
    private final NonNullList<ItemStack> stock = NonNullList.withSize(STOCK_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> cashBox = NonNullList.withSize(CASH_BOX_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> tray = NonNullList.withSize(TRAY_SLOTS, ItemStack.EMPTY);
    private final Map<UUID, NonNullList<ItemStack>> credits = new HashMap<>();
    private int[] syncedStock = new int[0];
    private int[] syncedCredits = new int[0];
    private int[] syncedProblems = new int[0];

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), pos, state);
        Arrays.fill(selections, Selection.EMPTY);
    }

    // ---- owner ---------------------------------------------------------------------------------------------------

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
        changed();
    }

    // ---- contents ------------------------------------------------------------------------------------------------

    public Selection getSelection(int index) {
        return selections[index];
    }

    public void setSelection(int index, Selection selection) {
        selections[index] = selection;
        changed();
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
        changed();
    }

    /** The 27 stock slots, live. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> stock() {
        return stock;
    }

    /** The 27 cash box slots, live. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> cashBox() {
        return cashBox;
    }

    /** The 9 tray slots, live. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> tray() {
        return tray;
    }

    /** A player's credit, live — or an empty list if they have none. Call {@link #changed()} after editing. */
    public List<ItemStack> credit(UUID player) {
        NonNullList<ItemStack> items = credits.get(player);
        return items != null ? items : List.of();
    }

    /** A player's credit slots, live, created empty if they have none yet. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> creditOf(UUID player) {
        return credits.computeIfAbsent(player, id -> NonNullList.withSize(CREDIT_SLOTS, ItemStack.EMPTY));
    }

    /** What this machine takes as money (spec §5.5). Plan 5 adds the admin currency slot and catalog currency. */
    public Currency currency() {
        return Currency.DEFAULT;
    }

    /** Items in stock that button {@code index} sells; 0 for an empty button. */
    public int stockCountFor(int index) {
        Selection selection = selections[index];
        return selection.isSetUp() ? ItemSlots.count(stock, selection::sells) : 0;
    }

    /** Active problems, in display order (spec §3.5 b). */
    public List<Problem> problems() {
        int setUp = 0;
        int inStock = 0;
        for (int i = 0; i < selections.length; i++) {
            if (!selections[i].isSetUp()) continue;
            setUp++;
            if (stockCountFor(i) >= selections[i].quantity()) inStock++;
        }
        return MachineProblems.of(new MachineFacts(infinite, false, setUp, inStock,
                ItemSlots.hasEmptySlot(cashBox), ItemSlots.hasEmptySlot(tray)));
    }

    /** Saves and re-syncs the machine. Call after any change to its contents. */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- client view ---------------------------------------------------------------------------------------------

    /** Client side: items in stock for button {@code index}, as last synced. */
    public int syncedStockCount(int index) {
        return index < syncedStock.length ? syncedStock[index] : 0;
    }

    /** Client side: how much spendable credit this player has, as last synced. */
    public int syncedCredit(UUID player) {
        for (int i = 0; i + 4 < syncedCredits.length; i += 5) {
            if (UUIDUtil.uuidFromIntArray(Arrays.copyOfRange(syncedCredits, i, i + 4)).equals(player)) return syncedCredits[i + 4];
        }
        return 0;
    }

    /** Client side: the machine's problems, as last synced. */
    public List<Problem> syncedProblems() {
        List<Problem> problems = new ArrayList<>();
        for (int ordinal : syncedProblems) {
            if (ordinal >= 0 && ordinal < Problem.values().length) problems.add(Problem.values()[ordinal]);
        }
        return problems;
    }

    // ---- saving --------------------------------------------------------------------------------------------------

    private void clearContents() {
        Arrays.fill(selections, Selection.EMPTY);
        ItemSlots.clear(stock);
        ItemSlots.clear(cashBox);
        ItemSlots.clear(tray);
        credits.clear();
    }

    private void warnLostSelection(int slot) {
        DiamondVending.LOGGER.warn("Vending machine at {}: the item for button {} no longer exists, so that button is now empty",
                worldPosition, slot + 1);
    }

    //? if >=26.1 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store(OWNER, UUIDUtil.CODEC, owner);
        output.putString(OWNER_NAME, ownerName);
        output.putBoolean(INFINITE, infinite);
        ValueOutput.ValueOutputList selectionList = output.childrenList(SELECTIONS);
        for (int i = 0; i < selections.length; i++) {
            if (!selections[i].isSetUp()) continue;
            ValueOutput entry = selectionList.addChild();
            entry.putInt(SLOT, i);
            entry.store(ITEM, ItemStack.CODEC, selections[i].template());
            entry.putInt(PRICE, selections[i].price());
        }
        ContainerHelper.saveAllItems(output.child(STOCK), stock);
        ContainerHelper.saveAllItems(output.child(CASH_BOX), cashBox);
        ContainerHelper.saveAllItems(output.child(TRAY), tray);
        ValueOutput.ValueOutputList creditList = output.childrenList(CREDITS);
        credits.forEach((player, items) -> {
            if (ItemSlots.isEmpty(items)) return;
            ValueOutput entry = creditList.addChild();
            entry.store(PLAYER, UUIDUtil.CODEC, player);
            ContainerHelper.saveAllItems(entry, items);
        });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        clearContents();
        owner = input.read(OWNER, UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr(OWNER_NAME, "");
        infinite = input.getBooleanOr(INFINITE, false);
        for (ValueInput entry : input.childrenListOrEmpty(SELECTIONS)) {
            int slot = entry.getIntOr(SLOT, -1);
            if (slot < 0 || slot >= selections.length) continue;
            Optional<ItemStack> item = entry.read(ITEM, ItemStack.CODEC);
            if (item.isEmpty()) {
                warnLostSelection(slot);
                continue;
            }
            selections[slot] = Selection.of(item.get(), entry.getIntOr(PRICE, 0));
        }
        ContainerHelper.loadAllItems(input.childOrEmpty(STOCK), stock);
        ContainerHelper.loadAllItems(input.childOrEmpty(CASH_BOX), cashBox);
        ContainerHelper.loadAllItems(input.childOrEmpty(TRAY), tray);
        for (ValueInput entry : input.childrenListOrEmpty(CREDITS)) {
            Optional<UUID> player = entry.read(PLAYER, UUIDUtil.CODEC);
            if (player.isEmpty()) continue;
            NonNullList<ItemStack> items = NonNullList.withSize(CREDIT_SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(entry, items);
            if (!ItemSlots.isEmpty(items)) credits.put(player.get(), items);
        }
        syncedStock = input.getIntArray(SYNC_STOCK).orElse(new int[0]);
        syncedCredits = input.getIntArray(SYNC_CREDITS).orElse(new int[0]);
        syncedProblems = input.getIntArray(SYNC_PROBLEMS).orElse(new int[0]);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID(OWNER, owner);
        tag.putString(OWNER_NAME, ownerName);
        tag.putBoolean(INFINITE, infinite);
        ListTag selectionList = new ListTag();
        for (int i = 0; i < selections.length; i++) {
            if (!selections[i].isSetUp()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT, i);
            entry.put(ITEM, selections[i].template().save(registries));
            entry.putInt(PRICE, selections[i].price());
            selectionList.add(entry);
        }
        tag.put(SELECTIONS, selectionList);
        tag.put(STOCK, ContainerHelper.saveAllItems(new CompoundTag(), stock, registries));
        tag.put(CASH_BOX, ContainerHelper.saveAllItems(new CompoundTag(), cashBox, registries));
        tag.put(TRAY, ContainerHelper.saveAllItems(new CompoundTag(), tray, registries));
        ListTag creditList = new ListTag();
        credits.forEach((player, items) -> {
            if (ItemSlots.isEmpty(items)) return;
            CompoundTag entry = ContainerHelper.saveAllItems(new CompoundTag(), items, registries);
            entry.putUUID(PLAYER, player);
            creditList.add(entry);
        });
        tag.put(CREDITS, creditList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        clearContents();
        owner = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        ownerName = tag.getString(OWNER_NAME);
        infinite = tag.getBoolean(INFINITE);
        ListTag selectionList = tag.getList(SELECTIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < selectionList.size(); i++) {
            CompoundTag entry = selectionList.getCompound(i);
            int slot = entry.contains(SLOT) ? entry.getInt(SLOT) : -1;
            if (slot < 0 || slot >= selections.length) continue;
            ItemStack item = entry.contains(ITEM) ? ItemStack.parse(registries, entry.get(ITEM)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            if (item.isEmpty()) {
                warnLostSelection(slot);
                continue;
            }
            selections[slot] = Selection.of(item, entry.getInt(PRICE));
        }
        ContainerHelper.loadAllItems(tag.getCompound(STOCK), stock, registries);
        ContainerHelper.loadAllItems(tag.getCompound(CASH_BOX), cashBox, registries);
        ContainerHelper.loadAllItems(tag.getCompound(TRAY), tray, registries);
        ListTag creditList = tag.getList(CREDITS, Tag.TAG_COMPOUND);
        for (int i = 0; i < creditList.size(); i++) {
            CompoundTag entry = creditList.getCompound(i);
            if (!entry.hasUUID(PLAYER)) continue;
            NonNullList<ItemStack> items = NonNullList.withSize(CREDIT_SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(entry, items, registries);
            if (!ItemSlots.isEmpty(items)) credits.put(entry.getUUID(PLAYER), items);
        }
        syncedStock = tag.getIntArray(SYNC_STOCK);
        syncedCredits = tag.getIntArray(SYNC_CREDITS);
        syncedProblems = tag.getIntArray(SYNC_PROBLEMS);
    }
    *///?}

    // ---- syncing -------------------------------------------------------------------------------------------------

    /**
     * What clients get (spec §8.3): everything they draw — selections, tray, owner name, infinite — plus stock counts,
     * each player's credit total and the problems. Never the stock, cash box or credit items themselves.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove(STOCK);
        tag.remove(CASH_BOX);
        tag.remove(CREDITS);
        int[] stockCounts = new int[selections.length];
        for (int i = 0; i < selections.length; i++) stockCounts[i] = stockCountFor(i);
        tag.putIntArray(SYNC_STOCK, stockCounts);
        tag.putIntArray(SYNC_CREDITS, creditTotals());
        tag.putIntArray(SYNC_PROBLEMS, problems().stream().mapToInt(Enum::ordinal).toArray());
        return tag;
    }

    /** For each player with spendable credit: their UUID as 4 ints, then the total. */
    private int[] creditTotals() {
        List<Integer> totals = new ArrayList<>();
        credits.forEach((player, items) -> {
            int total = ItemSlots.count(items, currency()::matches);
            if (total == 0) return;
            for (int part : UUIDUtil.uuidToIntArray(player)) totals.add(part);
            totals.add(total);
        });
        return totals.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

- [ ] **Step 5: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 27 required tests passed :)`. The log shows one `WARN` line "…the item for button 1 no longer exists…" from `unknown_items_load_as_empty_selections` (and a vanilla "invalid item" error for the same item) — that's the point of the test.

- [ ] **Step 6: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 27 on 26.1 Fabric, 26 on both 1.21.1 nodes, all passing.

- [ ] **Step 7: Check the Stonecutter state and commit**

Run: `./gradlew "Refresh active project"`; `git -C /c/Users/benet/mcvending status --short` must list only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: machine contents with save, sync and problems"
```

---

### Task 3: Front-face clicks and the pickup tray

Deliverable: every click on the machine is consumed on the server and routed: owners/admins holding dye repaint it; otherwise the exact spot on the front decides. The tray works for anyone, overflow drops at the player's feet, other faces do nothing, and the owner's name stays current.

**Files:**
- Create: `src/main/java/diamondvending/block/FrontFace.java`
- Create: `src/main/java/diamondvending/shop/PlayerItems.java`, `shop/MachineSounds.java`, `shop/PickupTray.java`
- Modify: `src/main/java/diamondvending/block/VendingMachineBlock.java` (replace the dyeing section), `block/VendingMachineBlockEntity.java` (add `refreshOwnerName`)
- Create: `src/gametest/java/diamondvending/gametest/RecordingPlayer.java`
- Modify: `BuyingTests.java`, `FabricBuyingTests.java`, `NeoForgeBuyingTests.java`

**Interfaces:**
- Consumes: `core.MachineLayout.hitOnFront(Facing, boolean right, boolean upper, double fx, double fy, double fz): Hit`, `core.Hit(Region region, int button)`, `core.Region` (Plan 1); `MachinePart.of/masterOf`, `MachineAccess.canManage`, `MachineItems.dyeColorOf` (Plan 2); `ItemSlots`, `VendingMachineBlockEntity` (Task 2).
- Produces: `block.FrontFace.hit(BlockState, BlockPos, BlockHitResult): Hit` — `Hit.NONE` unless the click is on the front face.
- Produces: `shop.PlayerItems.paySlots(Player): List<ItemStack>` (live: slots 0–35 then 40), `shop.PlayerItems.give(Player, ItemStack)`.
- Produces: `shop.MachineSounds.button/credit/vend/thankYou/error/pickup(Level, BlockPos)`.
- Produces: `shop.PickupTray.collect(VendingMachineBlockEntity, Player)`; `VendingMachineBlockEntity.refreshOwnerName(Player)`.
- Produces: in `VendingMachineBlock`, a private `use(ItemStack, BlockState, Level, BlockPos, Player, BlockHitResult)` whose `switch (target.region())` Tasks 4 and 5 extend with `COIN_SLOT`, `COIN_RETURN` and `BUTTON` cases.
- Produces (tests): `RecordingPlayer(GameTestHelper, GameType)`, `RecordingPlayer(GameTestHelper, GameType, UUID, String)`, `messages(): List<Component>`, `lastMessage(): Component`; `BuyingTests.click(helper, player, u, v)`, `click(helper, player, Rect)`, `pressButton(helper, player, index)`, `countHeld(Player, Item)`, `droppedNear(helper, BlockPos, Item)`, `lastMessage(helper, RecordingPlayer, key): TranslatableContents`.

- [ ] **Step 1: Write the recording mock player**

`src/gametest/java/diamondvending/gametest/RecordingPlayer.java`:

```java
package diamondvending.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
//? if <26.1 {
/*import net.minecraft.core.BlockPos;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A mock player like {@link GameTestHelper#makeMockPlayer} that also remembers the action-bar messages it's sent, so
 * tests can check exactly what a player was told. Not added to the world.
 */
public final class RecordingPlayer extends Player {
    private final GameType gameType;
    private final List<Component> messages = new ArrayList<>();

    public RecordingPlayer(GameTestHelper helper, GameType gameType) {
        this(helper, gameType, UUID.randomUUID(), "test-player");
    }

    public RecordingPlayer(GameTestHelper helper, GameType gameType, UUID id, String name) {
        //? if >=26.1 {
        super(helper.getLevel(), new GameProfile(id, name));
        //?} else {
        /*super(helper.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(id, name));
        *///?}
        this.gameType = gameType;
    }

    /** Every action-bar message so far, oldest first. */
    public List<Component> messages() {
        return messages;
    }

    /** The latest action-bar message, or null if there was none. */
    public Component lastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    // gameType is still null while the Player constructor runs, so these must cope with that.
    //? if >=26.1 {
    @Override
    public GameType gameMode() {
        return gameType;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    public void sendOverlayMessage(Component message) {
        messages.add(message);
    }
    //?} else {
    /*@Override
    public boolean isSpectator() {
        return gameType == GameType.SPECTATOR;
    }

    @Override
    public boolean isCreative() {
        return gameType != null && gameType.isCreative();
    }

    @Override
    public boolean isLocalPlayer() {
        return true;
    }

    @Override
    public void displayClientMessage(Component message, boolean actionBar) {
        if (actionBar) messages.add(message);
    }
    *///?}
}
```

- [ ] **Step 2: Write the failing GameTests**

In `BuyingTests.java`, add imports:

```java
import diamondvending.core.MachineLayout;
import diamondvending.core.Rect;
import diamondvending.shop.ItemSlots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
```

Add to `ALL`:

```java
            Map.entry("tray_goes_to_whoever_clicks_it", BuyingTests::trayGoesToWhoeverClicksIt),
            Map.entry("a_full_inventory_drops_the_rest_at_your_feet", BuyingTests::aFullInventoryDropsTheRestAtYourFeet),
            Map.entry("only_the_front_does_anything", BuyingTests::onlyTheFrontDoesAnything),
            Map.entry("held_blocks_are_never_placed", BuyingTests::heldBlocksAreNeverPlaced),
            Map.entry("owner_name_follows_renames", BuyingTests::ownerNameFollowsRenames),
            Map.entry("strangers_holding_dye_can_still_use_the_tray", BuyingTests::strangersHoldingDyeCanStillUseTheTray)
```

Add helpers:

```java
    /** Right-clicks canvas point (u, v) on the front, like a player looking at the machine. */
    static void click(GameTestHelper helper, Player player, double u, double v) {
        boolean right = u >= 16;
        boolean upper = v < 16;
        BlockPos part = upper ? (right ? MachineTests.UPPER_RIGHT : MachineTests.UPPER_LEFT)
                : (right ? MachineTests.LOWER_RIGHT : MachineTests.MASTER);
        double faceU = (u - (right ? 16 : 0)) / 16;
        double faceV = (v - (upper ? 0 : 16)) / 16;
        BlockPos at = helper.absolutePos(part);
        // Facing north, the front is each block's z = 0 side, and u runs from east (x + 1) to west (x).
        Vec3 point = new Vec3(at.getX() + 1 - faceU, at.getY() + 1 - faceV, at.getZ());
        helper.useBlock(part, player, new BlockHitResult(point, Direction.NORTH, at, false));
    }

    static void click(GameTestHelper helper, Player player, Rect region) {
        click(helper, player, region.centerU(), region.centerV());
    }

    /** Presses button {@code index} (0–11; the player sees 1–12). */
    static void pressButton(GameTestHelper helper, Player player, int index) {
        click(helper, player, MachineLayout.button(index));
    }

    /** Items of this kind anywhere in the player's inventory. */
    static int countHeld(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** Dropped items of this kind within 3 blocks of a test position. */
    static int droppedNear(GameTestHelper helper, BlockPos relative, Item item) {
        AABB area = new AABB(helper.absolutePos(relative)).inflate(3);
        int total = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(item))) {
            total += entity.getItem().getCount();
        }
        return total;
    }

    /** The player's latest action-bar message, after checking its key. */
    static TranslatableContents lastMessage(GameTestHelper helper, RecordingPlayer player, String key) {
        Component message = player.lastMessage();
        helper.assertTrue(message != null, "expected the player to be told " + key + ", but they were told nothing");
        return translation(helper, message, key);
    }
```

Add the tests:

```java
    // ---- clicks and the tray -------------------------------------------------------------------------------------

    public static void trayGoesToWhoeverClicksIt(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 3));
        machine.tray().set(4, new ItemStack(Items.APPLE, 2));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 3 && countHeld(stranger, Items.APPLE) == 2,
                "anyone may take what's in the tray");
        helper.assertTrue(ItemSlots.isEmpty(machine.tray()), "the tray should be empty afterwards");
        helper.succeed();
    }

    public static void aFullInventoryDropsTheRestAtYourFeet(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 5));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            stranger.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        BlockPos feet = MachineTests.platform(3, 1, 1);
        stranger.setPos(Vec3.atBottomCenterOf(helper.absolutePos(feet)));
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(droppedNear(helper, feet, Items.BREAD) == 5, "bread that doesn't fit should drop at the player's feet");
        helper.assertTrue(ItemSlots.isEmpty(machine.tray()), "nothing should stay behind or vanish");
        helper.succeed();
    }

    public static void onlyTheFrontDoesAnything(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 1));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        // The left column's outer side faces east. A click low on it is level with the tray, but it isn't the front.
        BlockPos master = helper.absolutePos(MachineTests.MASTER);
        helper.useBlock(MachineTests.MASTER, stranger, new BlockHitResult(
                new Vec3(master.getX() + 1, master.getY() + 0.2, master.getZ() + 0.5), Direction.EAST, master, false));
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 0, "clicking the side must not empty the tray");
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 1, "the tray on the front still works");
        helper.succeed();
    }

    /** Spec §3.2: clicks are always used up by the machine, so a block in hand is never placed against it. */
    public static void heldBlocksAreNeverPlaced(GameTestHelper helper) {
        placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 5);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, cobblestone);
        click(helper, stranger, MachineLayout.WINDOW);
        helper.assertTrue(cobblestone.getCount() == 5, "clicking the machine must never place the block in your hand");
        MachineTests.assertAir(helper, MachineTests.UPPER_LEFT.north());
        helper.succeed();
    }

    /** Spec §5.3: the owner's last-known name is kept current. */
    public static void ownerNameFollowsRenames(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL, id, "Alex"));
        helper.assertTrue(machine.getOwnerName().equals("Alex"), "the placer's name should be saved");
        click(helper, new RecordingPlayer(helper, GameType.SURVIVAL, id, "Sam"), MachineLayout.WINDOW);
        helper.assertTrue(machine.getOwnerName().equals("Sam"), "the owner's new name should be picked up, was " + machine.getOwnerName());
        helper.succeed();
    }

    /** Dyeing is for owners and admins; a customer who happens to hold dye still gets to use the machine. */
    public static void strangersHoldingDyeCanStillUseTheTray(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 1));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        ItemStack dye = new ItemStack(Items.BLUE_DYE, 2);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, dye);
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 1, "a dye in hand shouldn't stop a customer using the tray");
        helper.assertTrue(dye.getCount() == 2 && stranger.messages().isEmpty(), "and it isn't treated as an attempt to dye");
        MachineTests.assertWholeMachine(helper, DyeColor.RED);
        helper.succeed();
    }
```

Add to `FabricBuyingTests.java`, one method per test, each with the same 5-line annotation block as the others:

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void trayGoesToWhoeverClicksIt(GameTestHelper helper) {
        BuyingTests.trayGoesToWhoeverClicksIt(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aFullInventoryDropsTheRestAtYourFeet(GameTestHelper helper) {
        BuyingTests.aFullInventoryDropsTheRestAtYourFeet(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void onlyTheFrontDoesAnything(GameTestHelper helper) {
        BuyingTests.onlyTheFrontDoesAnything(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void heldBlocksAreNeverPlaced(GameTestHelper helper) {
        BuyingTests.heldBlocksAreNeverPlaced(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void ownerNameFollowsRenames(GameTestHelper helper) {
        BuyingTests.ownerNameFollowsRenames(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void strangersHoldingDyeCanStillUseTheTray(GameTestHelper helper) {
        BuyingTests.strangersHoldingDyeCanStillUseTheTray(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeBuyingTests.java`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void trayGoesToWhoeverClicksIt(GameTestHelper helper) {
        BuyingTests.trayGoesToWhoeverClicksIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aFullInventoryDropsTheRestAtYourFeet(GameTestHelper helper) {
        BuyingTests.aFullInventoryDropsTheRestAtYourFeet(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void onlyTheFrontDoesAnything(GameTestHelper helper) {
        BuyingTests.onlyTheFrontDoesAnything(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void heldBlocksAreNeverPlaced(GameTestHelper helper) {
        BuyingTests.heldBlocksAreNeverPlaced(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ownerNameFollowsRenames(GameTestHelper helper) {
        BuyingTests.ownerNameFollowsRenames(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void strangersHoldingDyeCanStillUseTheTray(GameTestHelper helper) {
        BuyingTests.strangersHoldingDyeCanStillUseTheTray(helper);
    }
```

- [ ] **Step 3: Run to verify they fail**

Run: `G26N`
Expected: FAIL — 6 failures: the tray tests ("anyone may take what's in the tray", "bread that doesn't fit…", "the tray on the front still works", "a dye in hand shouldn't stop…"), `held_blocks_are_never_placed` (the cobblestone gets placed: "clicking the machine must never place the block in your hand"), `owner_name_follows_renames` ("the owner's new name should be picked up").

- [ ] **Step 4: Add the hit mapping, player helpers, sounds and tray**

`src/main/java/diamondvending/block/FrontFace.java`:

```java
package diamondvending.block;

import diamondvending.core.Facing;
import diamondvending.core.Hit;
import diamondvending.core.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Works out what on the front a click hit (spec §2.2), from the exact click spot — like vanilla's chiseled bookshelf. */
public final class FrontFace {
    private FrontFace() {}

    /** What the click hit, or {@link Hit#NONE} if it wasn't on the machine's front face. */
    public static Hit hit(BlockState state, BlockPos pos, BlockHitResult click) {
        Direction facing = state.getValue(VendingMachineBlock.FACING);
        if (click.getDirection() != facing) return Hit.NONE;
        MachinePart part = MachinePart.of(state);
        Vec3 local = click.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return MachineLayout.hitOnFront(Facing.valueOf(facing.name()), part.right(), part.upper(), local.x, local.y, local.z);
    }
}
```

`src/main/java/diamondvending/shop/PlayerItems.java`:

```java
package diamondvending.shop;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A player's own item slots, as far as the machine is concerned. */
public final class PlayerItems {
    private PlayerItems() {}

    /**
     * The stacks a player can pay from (spec §3.3): main inventory and hotbar (slots 0–35), then the offhand (slot 40) —
     * never armor, never anything inside a shulker box or bundle. The stacks are live: splitting one takes from the player.
     */
    public static List<ItemStack> paySlots(Player player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>(Inventory.INVENTORY_SIZE + 1);
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) slots.add(inventory.getItem(i));
        slots.add(inventory.getItem(Inventory.SLOT_OFFHAND));
        return slots;
    }

    /** Puts a copy of the stack in the player's inventory; whatever doesn't fit drops at their feet (spec §3.4, §3.6). */
    public static void give(Player player, ItemStack stack) {
        ItemStack rest = stack.copy();
        player.getInventory().add(rest);
        if (!rest.isEmpty()) {
            Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), rest);
        }
    }
}
```

`src/main/java/diamondvending/shop/MachineSounds.java`:

```java
package diamondvending.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** The machine's sounds (spec §7): all vanilla for v1. */
public final class MachineSounds {
    private MachineSounds() {}

    public static void button(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.STONE_BUTTON_CLICK_ON, 0.6F, 1.0F);
    }

    public static void credit(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
    }

    public static void vend(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.DISPENSER_DISPENSE, 1.0F, 1.0F);
    }

    public static void thankYou(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.25F, 1.0F);
    }

    public static void error(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
    }

    /** Coin return and taking items from the tray. */
    public static void pickup(Level level, BlockPos pos) {
        play(level, pos, SoundEvents.ITEM_PICKUP, 0.4F, 1.0F);
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, pitch);
    }
}
```

`src/main/java/diamondvending/shop/PickupTray.java`:

```java
package diamondvending.shop;

import diamondvending.block.VendingMachineBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The pickup tray (spec §3.6). Anyone may take what's in it, like a real machine. */
public final class PickupTray {
    private PickupTray() {}

    /** Moves everything in the tray to the player; whatever doesn't fit drops at their feet. */
    public static void collect(VendingMachineBlockEntity machine, Player player) {
        List<ItemStack> items = ItemSlots.takeAll(machine.tray());
        if (items.isEmpty()) return;
        items.forEach(stack -> PlayerItems.give(player, stack));
        MachineSounds.pickup(machine.getLevel(), machine.getBlockPos());
        machine.changed();
    }
}
```

In `VendingMachineBlockEntity.java`, add under `setOwner` (and `import net.minecraft.world.entity.player.Player;`):

```java
    /** Keeps the owner's last-known name current (spec §5.3) — players can change their names. */
    public void refreshOwnerName(Player player) {
        String name = player.getName().getString();
        if (player.getUUID().equals(owner) && !name.equals(ownerName)) setOwner(owner, name);
    }
```

- [ ] **Step 5: Route every click through the front face**

In `VendingMachineBlock.java`, replace everything from the line `// ---- dyeing ----…` up to (not including) `// ---- integrity ----…` with:

```java
    // ---- using ---------------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) use(stack, state, level, pos, player, hit);
        return InteractionResult.SUCCESS;
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) use(stack, state, level, pos, player, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    /**
     * Spec §3.2, on the server. Owners and admins holding a dye repaint the machine; otherwise the spot clicked on the
     * front decides. Every click is used up (see {@link #useItemOn}), so blocks in hand are never placed against it.
     */
    private void use(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine)) return;
        machine.refreshOwnerName(player);
        DyeColor dye = MachineItems.dyeColorOf(stack);
        if (dye != null && MachineAccess.canManage(player, machine.getOwner())) {
            dye(stack, state, level, pos, player, dye);
            return;
        }
        Hit target = FrontFace.hit(state, pos, hit);
        switch (target.region()) {
            case TRAY -> PickupTray.collect(machine, player);
            default -> {
                // Spec §3.5 c: someone who isn't the owner tried to dye it.
                if (dye != null) Messages.actionBar(player, Component.translatable(Texts.OWNER_ONLY));
            }
        }
    }

    /** Spec §3.2 rule 2: repaints the whole machine, using one dye unless in creative. The same color changes nothing. */
    private void dye(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, DyeColor color) {
        if (state.getValue(COLOR) == color) return;
        Direction facing = state.getValue(FACING);
        BlockPos master = MachinePart.masterOf(pos, state);
        for (MachinePart part : MachinePart.values()) {
            BlockPos partPos = part.posFrom(master, facing);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlock(partPos, partState.setValue(COLOR, color), Block.UPDATE_ALL);
            }
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

```

Add imports `diamondvending.core.Hit` and `diamondvending.shop.PickupTray`. The old `tryDye` method is gone (its body is now `use` + `dye`).

- [ ] **Step 6: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 33 required tests passed :)`, including Plan 2's dye tests.

- [ ] **Step 7: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 33 on 26.1 Fabric, 32 on both 1.21.1 nodes, all passing.

- [ ] **Step 8: Check the Stonecutter state and commit**

Run: `./gradlew "Refresh active project"`; `git -C /c/Users/benet/mcvending status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: route clicks by front-face region; pickup tray"
```

---

### Task 4: Credit and coin return

Deliverable: the coin slot loads the held currency stack as that player's own credit (capped at 9 stacks), refuses anything else with a message, and coin return gives back exactly what went in.

**Files:**
- Create: `src/main/java/diamondvending/shop/CoinSlot.java`
- Modify: `core/Texts.java`, `lang/en_us.json`, `block/VendingMachineBlockEntity.java` (add `takeCredit`), `block/VendingMachineBlock.java` (two cases)
- Modify: `BuyingTests.java`, `FabricBuyingTests.java`, `NeoForgeBuyingTests.java`

**Interfaces:**
- Consumes: `Currency.matches/name` (Task 1), `VendingMachineBlockEntity.credit/creditOf/currency/changed`, `ItemSlots.insert/takeAll` (Task 2), `PlayerItems.give`, `MachineSounds.credit/error/pickup` (Task 3).
- Produces: `Texts.WRONG_CURRENCY`, `Texts.CREDIT_FULL`; `VendingMachineBlockEntity.takeCredit(UUID): List<ItemStack>` (removes and returns that player's credit); `shop.CoinSlot.insert(VendingMachineBlockEntity, Player)`, `shop.CoinSlot.giveBack(VendingMachineBlockEntity, Player)`.

- [ ] **Step 1: Write the failing tests**

In `BuyingTests.java`, add imports `diamondvending.core.Texts` and `net.minecraft.core.NonNullList`. Add to `ALL`:

```java
            Map.entry("the_coin_slot_takes_the_whole_stack", BuyingTests::theCoinSlotTakesTheWholeStack),
            Map.entry("offhand_money_works_too", BuyingTests::offhandMoneyWorksToo),
            Map.entry("the_coin_slot_only_takes_money", BuyingTests::theCoinSlotOnlyTakesMoney),
            Map.entry("credit_stops_at_nine_stacks", BuyingTests::creditStopsAtNineStacks),
            Map.entry("coin_return_gives_back_the_exact_items", BuyingTests::coinReturnGivesBackTheExactItems),
            Map.entry("credit_belongs_to_one_player", BuyingTests::creditBelongsToOnePlayer)
```

Add the tests:

```java
    // ---- credit --------------------------------------------------------------------------------------------------

    public static void theCoinSlotTakesTheWholeStack(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 12));
        click(helper, buyer, MachineLayout.COIN_SLOT);
        helper.assertTrue(buyer.getMainHandItem().isEmpty(), "the whole held stack should go in");
        helper.assertTrue(countIn(machine.credit(buyer.getUUID()), Items.DIAMOND) == 12, "and become that player's credit");
        helper.assertTrue(buyer.messages().isEmpty(), "a good insert needs no message");
        helper.succeed();
    }

    public static void offhandMoneyWorksToo(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        buyer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.DIAMOND, 5));
        click(helper, buyer, MachineLayout.COIN_SLOT);
        helper.assertTrue(countIn(machine.credit(buyer.getUUID()), Items.DIAMOND) == 5, "diamonds in the offhand should go in");
        helper.assertTrue(buyer.getMainHandItem().is(Items.APPLE) && buyer.getOffhandItem().isEmpty(), "the apple stays in hand");
        helper.succeed();
    }

    public static void theCoinSlotOnlyTakesMoney(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        ItemStack emeralds = new ItemStack(Items.EMERALD, 3);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, emeralds);
        click(helper, buyer, MachineLayout.COIN_SLOT);
        TranslatableContents told = lastMessage(helper, buyer, Texts.WRONG_CURRENCY);
        translation(helper, (Component) told.getArgs()[0], Texts.DIAMOND + ".name");
        helper.assertTrue(emeralds.getCount() == 3 && machine.credit(buyer.getUUID()).isEmpty(), "emeralds are refused, not taken");
        buyer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        buyer.messages().clear();
        click(helper, buyer, MachineLayout.COIN_SLOT);
        lastMessage(helper, buyer, Texts.WRONG_CURRENCY);
        helper.succeed();
    }

    public static void creditStopsAtNineStacks(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        NonNullList<ItemStack> credit = machine.creditOf(buyer.getUUID());
        for (int i = 0; i < 8; i++) credit.set(i, new ItemStack(Items.DIAMOND, 64));
        credit.set(8, new ItemStack(Items.DIAMOND, 60));
        ItemStack held = new ItemStack(Items.DIAMOND, 10);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, held);
        click(helper, buyer, MachineLayout.COIN_SLOT);
        helper.assertTrue(countIn(machine.credit(buyer.getUUID()), Items.DIAMOND) == 9 * 64, "credit should fill up to 9 stacks");
        helper.assertTrue(held.getCount() == 6, "the rest should stay in hand, but " + held.getCount() + " did");
        lastMessage(helper, buyer, Texts.CREDIT_FULL);
        helper.succeed();
    }

    public static void coinReturnGivesBackTheExactItems(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        ItemStack lucky = new ItemStack(Items.DIAMOND, 3);
        lucky.set(DataComponents.CUSTOM_NAME, Component.literal("Lucky"));
        machine.creditOf(buyer.getUUID()).set(0, lucky.copy());
        machine.creditOf(buyer.getUUID()).set(1, new ItemStack(Items.DIAMOND, 2));
        click(helper, buyer, MachineLayout.COIN_RETURN);
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 5, "all 5 diamonds should come back");
        boolean luckyBack = false;
        for (int i = 0; i < buyer.getInventory().getContainerSize(); i++) {
            ItemStack stack = buyer.getInventory().getItem(i);
            luckyBack |= ItemStack.isSameItemSameComponents(stack, lucky) && stack.getCount() == 3;
        }
        helper.assertTrue(luckyBack, "renamed diamonds should come back still renamed");
        helper.assertTrue(machine.credit(buyer.getUUID()).isEmpty(), "and the credit should be gone");
        helper.succeed();
    }

    public static void creditBelongsToOnePlayer(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer alex = new RecordingPlayer(helper, GameType.SURVIVAL);
        RecordingPlayer sam = new RecordingPlayer(helper, GameType.SURVIVAL);
        machine.creditOf(alex.getUUID()).set(0, new ItemStack(Items.DIAMOND, 5));
        click(helper, sam, MachineLayout.COIN_RETURN);
        helper.assertTrue(countHeld(sam, Items.DIAMOND) == 0, "coin return only gives back your own credit");
        helper.assertTrue(countIn(machine.credit(alex.getUUID()), Items.DIAMOND) == 5, "Alex's credit should stay put");
        click(helper, alex, MachineLayout.COIN_RETURN);
        helper.assertTrue(countHeld(alex, Items.DIAMOND) == 5, "and Alex gets it back from the coin return");
        helper.succeed();
    }
```

Add to `FabricBuyingTests.java`:

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCoinSlotTakesTheWholeStack(GameTestHelper helper) {
        BuyingTests.theCoinSlotTakesTheWholeStack(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void offhandMoneyWorksToo(GameTestHelper helper) {
        BuyingTests.offhandMoneyWorksToo(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCoinSlotOnlyTakesMoney(GameTestHelper helper) {
        BuyingTests.theCoinSlotOnlyTakesMoney(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creditStopsAtNineStacks(GameTestHelper helper) {
        BuyingTests.creditStopsAtNineStacks(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void coinReturnGivesBackTheExactItems(GameTestHelper helper) {
        BuyingTests.coinReturnGivesBackTheExactItems(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creditBelongsToOnePlayer(GameTestHelper helper) {
        BuyingTests.creditBelongsToOnePlayer(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeBuyingTests.java`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCoinSlotTakesTheWholeStack(GameTestHelper helper) {
        BuyingTests.theCoinSlotTakesTheWholeStack(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void offhandMoneyWorksToo(GameTestHelper helper) {
        BuyingTests.offhandMoneyWorksToo(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCoinSlotOnlyTakesMoney(GameTestHelper helper) {
        BuyingTests.theCoinSlotOnlyTakesMoney(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creditStopsAtNineStacks(GameTestHelper helper) {
        BuyingTests.creditStopsAtNineStacks(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void coinReturnGivesBackTheExactItems(GameTestHelper helper) {
        BuyingTests.coinReturnGivesBackTheExactItems(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creditBelongsToOnePlayer(GameTestHelper helper) {
        BuyingTests.creditBelongsToOnePlayer(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation error first (`cannot find symbol: variable WRONG_CURRENCY`). Add just the two constants to `Texts` (Step 3's first block) and re-run: **6** test failures ("the whole held stack should go in", "diamonds in the offhand should go in", "expected the player to be told message.diamondvending.wrong_currency…", "credit should fill up to 9 stacks", "all 5 diamonds should come back", "and Alex gets it back from the coin return").

- [ ] **Step 3: Implement the coin slot**

In `core/Texts.java`, add the constants and extend `all()`:

```java
    public static final String WRONG_CURRENCY = "message.diamondvending.wrong_currency";
    public static final String CREDIT_FULL = "message.diamondvending.credit_full";
```

```java
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY, WRONG_CURRENCY, CREDIT_FULL));
```

In `en_us.json`, add after `owner_only`:

```json
  "message.diamondvending.wrong_currency": "This machine takes %s.",
  "message.diamondvending.credit_full": "You can't put in any more. Buy something or press coin return.",
```

In `VendingMachineBlockEntity.java`, add after `creditOf`:

```java
    /** Removes a player's credit and returns it, exactly the items they put in. */
    public List<ItemStack> takeCredit(UUID player) {
        NonNullList<ItemStack> items = credits.remove(player);
        return items == null ? List.of() : ItemSlots.takeAll(items);
    }
```

`src/main/java/diamondvending/shop/CoinSlot.java`:

```java
package diamondvending.shop;

import diamondvending.Messages;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** The coin slot and the coin-return button (spec §3.4). Credit belongs to one player and is kept as the real items. */
public final class CoinSlot {
    private CoinSlot() {}

    /** Loads the whole held stack of money as this player's credit, up to 9 stacks; the rest stays in hand. */
    public static void insert(VendingMachineBlockEntity machine, Player player) {
        Level level = machine.getLevel();
        BlockPos pos = machine.getBlockPos();
        Currency currency = machine.currency();
        InteractionHand hand = handHoldingMoney(player, currency);
        if (hand == null) {
            MachineSounds.error(level, pos);
            Messages.actionBar(player, Component.translatable(Texts.WRONG_CURRENCY, currency.name()));
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        ItemStack left = ItemSlots.insert(machine.creditOf(player.getUUID()), held);
        int accepted = held.getCount() - left.getCount();
        if (accepted > 0) {
            held.shrink(accepted);
            MachineSounds.credit(level, pos);
            machine.changed();
        }
        if (!left.isEmpty()) {
            if (accepted == 0) MachineSounds.error(level, pos);
            Messages.actionBar(player, Component.translatable(Texts.CREDIT_FULL));
        }
    }

    /** Gives this player back all of their credit, exactly as inserted; what doesn't fit drops at their feet. */
    public static void giveBack(VendingMachineBlockEntity machine, Player player) {
        List<ItemStack> credit = machine.takeCredit(player.getUUID());
        if (credit.isEmpty()) return;
        credit.forEach(stack -> PlayerItems.give(player, stack));
        MachineSounds.pickup(machine.getLevel(), machine.getBlockPos());
        machine.changed();
    }

    /** The main hand if it holds money, otherwise the offhand if that does; null if neither. */
    private static InteractionHand handHoldingMoney(Player player, Currency currency) {
        if (currency.matches(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (currency.matches(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }
}
```

In `VendingMachineBlock.use`, add these cases to the `switch` (above `case TRAY`), and `import diamondvending.shop.CoinSlot;`:

```java
            case COIN_SLOT -> CoinSlot.insert(machine, player);
            case COIN_RETURN -> CoinSlot.giveBack(machine, player);
```

- [ ] **Step 4: Run to verify they pass**

Run: `G26N`
Expected: PASS — unit tests green (TextsTest covers the new keys); `All 39 required tests passed :)`.

- [ ] **Step 5: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 39 on 26.1 Fabric, 38 on both 1.21.1 nodes.

- [ ] **Step 6: Check the Stonecutter state and commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: coin slot credit and coin return"
```

---

### Task 5: Buying

Deliverable: pressing a button runs the all-or-nothing purchase — credit first, then inventory; goods into the tray; money into the cash box (owned) or gone (infinite) — or tells the buyer exactly why not.

**Files:**
- Create: `src/main/java/diamondvending/shop/Purchase.java`
- Modify: `core/Texts.java`, `Messages.java`, `lang/en_us.json`, `block/VendingMachineBlock.java` (one case)
- Modify: `BuyingTests.java`, `FabricBuyingTests.java`, `NeoForgeBuyingTests.java`

**Interfaces:**
- Consumes: `core.PurchaseRules.decide(PurchaseInput): PurchaseDecision`, `PurchaseInput(catalogMissing, selectionSetUp, infinite, quantity, stockCount, trayHasRoom, cashBoxHasRoom, price, credit, inventoryCurrency)`, `PurchaseDecision.Approved(int fromCredit, int fromInventory)`, `PurchaseDecision.Denied(DenyReason)`, `DenyReason` (Plan 1); everything from Tasks 1–4.
- Produces: `Texts.BUTTON_EMPTY`, `Texts.SOLD_OUT`, `Texts.NEED_MONEY`, `Texts.explanation(Problem): String`; `Messages.explanation(Problem, Currency): Component`; `shop.Purchase.pressButton(VendingMachineBlockEntity, Player, int index)`.

- [ ] **Step 1: Write the failing tests**

In `BuyingTests.java`, add imports `net.minecraft.world.item.component.ItemContainerContents`. Add to `ALL`:

```java
            Map.entry("buying_moves_goods_into_the_tray", BuyingTests::buyingMovesGoodsIntoTheTray),
            Map.entry("credit_is_spent_first", BuyingTests::creditIsSpentFirst),
            Map.entry("one_short_takes_nothing", BuyingTests::oneShortTakesNothing),
            Map.entry("free_items_cost_nothing", BuyingTests::freeItemsCostNothing),
            Map.entry("empty_buttons_say_so", BuyingTests::emptyButtonsSaySo),
            Map.entry("sold_out_buttons_say_so", BuyingTests::soldOutButtonsSaySo),
            Map.entry("stock_must_match_the_template_exactly", BuyingTests::stockMustMatchTheTemplateExactly),
            Map.entry("a_full_tray_stops_sales", BuyingTests::aFullTrayStopsSales),
            Map.entry("a_full_cash_box_stops_sales", BuyingTests::aFullCashBoxStopsSales),
            Map.entry("infinite_machines_never_run_out", BuyingTests::infiniteMachinesNeverRunOut),
            Map.entry("currency_inside_containers_does_not_pay", BuyingTests::currencyInsideContainersDoesNotPay),
            Map.entry("nobody_else_can_spend_your_credit", BuyingTests::nobodyElseCanSpendYourCredit)
```

Add helpers:

```java
    /** A machine selling 2 apples for 3 diamonds on button 1, with 10 apples in stock. */
    static VendingMachineBlockEntity appleMachine(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        return machine;
    }

    static RecordingPlayer buyerWith(GameTestHelper helper, int diamonds) {
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        if (diamonds > 0) buyer.getInventory().add(new ItemStack(Items.DIAMOND, diamonds));
        return buyer;
    }

    /** Checks the "Button N costs X. You have Y." message. */
    static void assertNeedMoney(GameTestHelper helper, RecordingPlayer buyer, int button, int funds) {
        Object[] args = lastMessage(helper, buyer, Texts.NEED_MONEY).getArgs();
        helper.assertTrue(args[0].equals(button), "the message should name button " + button + ", named " + args[0]);
        translation(helper, (Component) args[1], Texts.DIAMOND + ".many");
        helper.assertTrue(args[2].equals(funds), "the buyer has " + funds + " to spend, the message said " + args[2]);
    }
```

Add the tests:

```java
    // ---- buying --------------------------------------------------------------------------------------------------

    public static void buyingMovesGoodsIntoTheTray(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        RecordingPlayer buyer = buyerWith(helper, 5);
        pressButton(helper, buyer, 0);
        helper.assertTrue(countIn(machine.tray(), Items.APPLE) == 2, "2 apples should drop into the tray");
        helper.assertTrue(countIn(machine.stock(), Items.APPLE) == 8, "and come out of stock");
        helper.assertTrue(countIn(machine.cashBox(), Items.DIAMOND) == 3, "the 3 diamonds should go in the cash box");
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 2, "the buyer keeps the other 2");
        helper.assertTrue(buyer.messages().isEmpty(), "a good purchase needs no message");
        helper.succeed();
    }

    public static void creditIsSpentFirst(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        RecordingPlayer buyer = buyerWith(helper, 5);
        machine.creditOf(buyer.getUUID()).set(0, new ItemStack(Items.DIAMOND, 2));
        pressButton(helper, buyer, 0);
        helper.assertTrue(countIn(machine.credit(buyer.getUUID()), Items.DIAMOND) == 0, "both credit diamonds are spent first");
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 4, "then 1 from the inventory");
        helper.assertTrue(countIn(machine.cashBox(), Items.DIAMOND) == 3, "all 3 end up in the cash box");
        helper.succeed();
    }

    public static void oneShortTakesNothing(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        RecordingPlayer buyer = buyerWith(helper, 1);
        machine.creditOf(buyer.getUUID()).set(0, new ItemStack(Items.DIAMOND, 1));
        pressButton(helper, buyer, 0);
        assertNeedMoney(helper, buyer, 1, 2);
        helper.assertTrue(countIn(machine.credit(buyer.getUUID()), Items.DIAMOND) == 1 && countHeld(buyer, Items.DIAMOND) == 1,
                "a failed purchase must not take anything");
        helper.assertTrue(ItemSlots.isEmpty(machine.tray()) && countIn(machine.stock(), Items.APPLE) == 10
                && ItemSlots.isEmpty(machine.cashBox()), "and must not change the machine");
        helper.succeed();
    }

    public static void freeItemsCostNothing(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.BREAD), 0));
        machine.stock().set(0, new ItemStack(Items.BREAD, 1));
        RecordingPlayer buyer = buyerWith(helper, 0);
        pressButton(helper, buyer, 0);
        helper.assertTrue(countIn(machine.tray(), Items.BREAD) == 1, "a price of 0 means free");
        helper.assertTrue(buyer.messages().isEmpty(), "no message for a free item");
        helper.succeed();
    }

    public static void emptyButtonsSaySo(GameTestHelper helper) {
        appleMachine(helper);
        RecordingPlayer buyer = buyerWith(helper, 5);
        pressButton(helper, buyer, 6);
        helper.assertTrue(lastMessage(helper, buyer, Texts.BUTTON_EMPTY).getArgs()[0].equals(7), "the message should say button 7");
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 5, "nothing is taken");
        helper.succeed();
    }

    public static void soldOutButtonsSaySo(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        machine.stock().set(0, new ItemStack(Items.APPLE, 1));
        RecordingPlayer buyer = buyerWith(helper, 5);
        pressButton(helper, buyer, 0);
        helper.assertTrue(lastMessage(helper, buyer, Texts.SOLD_OUT).getArgs()[0].equals(1), "the message should say button 1");
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 5, "nothing is taken");
        helper.succeed();
    }

    public static void stockMustMatchTheTemplateExactly(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        ItemStack namedApples = new ItemStack(Items.APPLE, 10);
        namedApples.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
        machine.stock().set(0, namedApples);
        RecordingPlayer buyer = buyerWith(helper, 5);
        pressButton(helper, buyer, 0);
        lastMessage(helper, buyer, Texts.SOLD_OUT);
        helper.assertTrue(countIn(machine.stock(), Items.APPLE) == 10, "renamed apples aren't the apples this button sells");
        helper.succeed();
    }

    public static void aFullTrayStopsSales(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        fill(machine.tray(), Items.COBBLESTONE);
        RecordingPlayer buyer = buyerWith(helper, 5);
        pressButton(helper, buyer, 0);
        lastMessage(helper, buyer, Texts.explanation(Problem.TRAY_FULL));
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 5 && countIn(machine.stock(), Items.APPLE) == 10, "nothing changes");
        helper.succeed();
    }

    public static void aFullCashBoxStopsSales(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        fill(machine.cashBox(), Items.COBBLESTONE);
        RecordingPlayer buyer = buyerWith(helper, 5);
        pressButton(helper, buyer, 0);
        TranslatableContents told = lastMessage(helper, buyer, Texts.explanation(Problem.CASH_BOX_FULL));
        translation(helper, (Component) told.getArgs()[0], Texts.DIAMOND + ".name");
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 5 && countIn(machine.stock(), Items.APPLE) == 10, "nothing changes");
        helper.succeed();
    }

    public static void infiniteMachinesNeverRunOut(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        machine.setInfinite(true);
        machine.stock().set(0, ItemStack.EMPTY);
        RecordingPlayer buyer = buyerWith(helper, 3);
        pressButton(helper, buyer, 0);
        helper.assertTrue(countIn(machine.tray(), Items.APPLE) == 2, "infinite machines sell without stock");
        helper.assertTrue(countHeld(buyer, Items.DIAMOND) == 0, "the buyer still pays");
        helper.assertTrue(ItemSlots.isEmpty(machine.cashBox()), "and the diamonds are destroyed, not kept");
        helper.succeed();
    }

    public static void currencyInsideContainersDoesNotPay(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        RecordingPlayer buyer = buyerWith(helper, 0);
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 10))));
        buyer.getInventory().add(box);
        pressButton(helper, buyer, 0);
        assertNeedMoney(helper, buyer, 1, 0);
        helper.assertTrue(ItemSlots.isEmpty(machine.tray()), "diamonds packed in a shulker box don't count");
        helper.succeed();
    }

    public static void nobodyElseCanSpendYourCredit(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachine(helper);
        RecordingPlayer alex = buyerWith(helper, 0);
        machine.creditOf(alex.getUUID()).set(0, new ItemStack(Items.DIAMOND, 5));
        RecordingPlayer sam = buyerWith(helper, 0);
        pressButton(helper, sam, 0);
        assertNeedMoney(helper, sam, 1, 0);
        helper.assertTrue(countIn(machine.credit(alex.getUUID()), Items.DIAMOND) == 5, "Alex's credit is only Alex's");
        helper.succeed();
    }
```

Add to `FabricBuyingTests.java`:

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void buyingMovesGoodsIntoTheTray(GameTestHelper helper) {
        BuyingTests.buyingMovesGoodsIntoTheTray(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creditIsSpentFirst(GameTestHelper helper) {
        BuyingTests.creditIsSpentFirst(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void oneShortTakesNothing(GameTestHelper helper) {
        BuyingTests.oneShortTakesNothing(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void freeItemsCostNothing(GameTestHelper helper) {
        BuyingTests.freeItemsCostNothing(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void emptyButtonsSaySo(GameTestHelper helper) {
        BuyingTests.emptyButtonsSaySo(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void soldOutButtonsSaySo(GameTestHelper helper) {
        BuyingTests.soldOutButtonsSaySo(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void stockMustMatchTheTemplateExactly(GameTestHelper helper) {
        BuyingTests.stockMustMatchTheTemplateExactly(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aFullTrayStopsSales(GameTestHelper helper) {
        BuyingTests.aFullTrayStopsSales(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aFullCashBoxStopsSales(GameTestHelper helper) {
        BuyingTests.aFullCashBoxStopsSales(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void infiniteMachinesNeverRunOut(GameTestHelper helper) {
        BuyingTests.infiniteMachinesNeverRunOut(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void currencyInsideContainersDoesNotPay(GameTestHelper helper) {
        BuyingTests.currencyInsideContainersDoesNotPay(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void nobodyElseCanSpendYourCredit(GameTestHelper helper) {
        BuyingTests.nobodyElseCanSpendYourCredit(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeBuyingTests.java`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void buyingMovesGoodsIntoTheTray(GameTestHelper helper) {
        BuyingTests.buyingMovesGoodsIntoTheTray(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creditIsSpentFirst(GameTestHelper helper) {
        BuyingTests.creditIsSpentFirst(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void oneShortTakesNothing(GameTestHelper helper) {
        BuyingTests.oneShortTakesNothing(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void freeItemsCostNothing(GameTestHelper helper) {
        BuyingTests.freeItemsCostNothing(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void emptyButtonsSaySo(GameTestHelper helper) {
        BuyingTests.emptyButtonsSaySo(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void soldOutButtonsSaySo(GameTestHelper helper) {
        BuyingTests.soldOutButtonsSaySo(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void stockMustMatchTheTemplateExactly(GameTestHelper helper) {
        BuyingTests.stockMustMatchTheTemplateExactly(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aFullTrayStopsSales(GameTestHelper helper) {
        BuyingTests.aFullTrayStopsSales(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aFullCashBoxStopsSales(GameTestHelper helper) {
        BuyingTests.aFullCashBoxStopsSales(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void infiniteMachinesNeverRunOut(GameTestHelper helper) {
        BuyingTests.infiniteMachinesNeverRunOut(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void currencyInsideContainersDoesNotPay(GameTestHelper helper) {
        BuyingTests.currencyInsideContainersDoesNotPay(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void nobodyElseCanSpendYourCredit(GameTestHelper helper) {
        BuyingTests.nobodyElseCanSpendYourCredit(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation error first (`cannot find symbol: variable NEED_MONEY` / `method explanation`). Add the `Texts` constants and `explanation` method from Step 3 and re-run: **12** test failures (no button does anything yet — no goods, no messages).

- [ ] **Step 3: Implement buying**

In `core/Texts.java`, add:

```java
    public static final String BUTTON_EMPTY = "message.diamondvending.button_empty";
    public static final String SOLD_OUT = "message.diamondvending.sold_out";
    public static final String NEED_MONEY = "message.diamondvending.need_money";

    /** A problem's plain-language explanation (spec §3.5 b), e.g. "The tray is full! …". */
    public static String explanation(Problem problem) {
        return "problem.diamondvending." + problem.key() + ".explanation";
    }
```

and make `all()`:

```java
    public static List<String> all() {
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY, WRONG_CURRENCY, CREDIT_FULL, BUTTON_EMPTY, SOLD_OUT, NEED_MONEY));
        for (Problem problem : Problem.values()) keys.add(explanation(problem));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
```

Replace `en_us.json` with:

```json
{
  "block.diamondvending.vending_machine": "Vending Machine",
  "message.diamondvending.owner_only": "Only the owner can do that.",
  "message.diamondvending.button_empty": "Button %s has nothing for sale.",
  "message.diamondvending.sold_out": "Button %s is sold out.",
  "message.diamondvending.need_money": "Button %s costs %s. You have %s.",
  "message.diamondvending.wrong_currency": "This machine takes %s.",
  "message.diamondvending.credit_full": "You can't put in any more. Buy something or press coin return.",
  "problem.diamondvending.catalog_missing.explanation": "This machine's catalog is missing. An admin needs to pick a new one.",
  "problem.diamondvending.not_set_up.explanation": "Nothing is for sale yet. The owner can empty both hands, then sneak + right-click to set it up.",
  "problem.diamondvending.cash_box_full.explanation": "The cash box is full of %s! The owner needs to take them out.",
  "problem.diamondvending.sold_out.explanation": "Everything is sold out. The owner needs to put more items in the Stock.",
  "problem.diamondvending.tray_full.explanation": "The tray is full! Right-click the tray to take the items out.",
  "currency.diamondvending.minecraft.diamond.one": "%s diamond",
  "currency.diamondvending.minecraft.diamond.many": "%s diamonds",
  "currency.diamondvending.minecraft.diamond.name": "diamonds"
}
```

In `Messages.java`, add (with imports `diamondvending.core.Problem`, `diamondvending.core.Texts`, `diamondvending.shop.Currency`):

```java
    /** A problem's plain-language explanation (spec §3.5 b); "diamonds" becomes the machine's real currency. */
    public static Component explanation(Problem problem, Currency currency) {
        return Component.translatable(Texts.explanation(problem), currency.name());
    }
```

`src/main/java/diamondvending/shop/Purchase.java`:

```java
package diamondvending.shop;

import diamondvending.Messages;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.DenyReason;
import diamondvending.core.Problem;
import diamondvending.core.PurchaseDecision;
import diamondvending.core.PurchaseInput;
import diamondvending.core.PurchaseRules;
import diamondvending.core.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Pressing a button: the all-or-nothing purchase (spec §3.3). */
public final class Purchase {
    private Purchase() {}

    /** Buys button {@code index} (0–11) for the player, or tells them exactly why not. */
    public static void pressButton(VendingMachineBlockEntity machine, Player player, int index) {
        MachineSounds.button(machine.getLevel(), machine.getBlockPos());
        Selection selection = machine.getSelection(index);
        Currency currency = machine.currency();
        List<ItemStack> credit = machine.credit(player.getUUID());
        List<ItemStack> wallet = PlayerItems.paySlots(player);
        int inCredit = ItemSlots.count(credit, currency::matches);
        int inWallet = ItemSlots.count(wallet, currency::matches);
        int price = selection.price();

        // Work out the exact stacks that would be paid, on copies: the cash box must have room for those.
        int fromCredit = Math.min(inCredit, price);
        int fromWallet = Math.min(inWallet, price - fromCredit);
        List<ItemStack> payment = new ArrayList<>(ItemSlots.take(ItemSlots.copyOf(credit), currency::matches, fromCredit));
        payment.addAll(ItemSlots.take(ItemSlots.copyOf(wallet), currency::matches, fromWallet));

        PurchaseInput input = new PurchaseInput(
                false,
                selection.isSetUp(),
                machine.isInfinite(),
                selection.quantity(),
                machine.stockCountFor(index),
                ItemSlots.fitsAll(machine.tray(), List.of(selection.template())),
                ItemSlots.fitsAll(machine.cashBox(), payment),
                price,
                inCredit,
                inWallet);
        switch (PurchaseRules.decide(input)) {
            case PurchaseDecision.Approved approved -> complete(machine, player, selection, approved);
            case PurchaseDecision.Denied denied -> refuse(machine, player, index, denied.reason(), inCredit + inWallet);
        }
    }

    private static void complete(VendingMachineBlockEntity machine, Player player, Selection selection, PurchaseDecision.Approved approved) {
        Currency currency = machine.currency();
        List<ItemStack> paid = new ArrayList<>(ItemSlots.take(machine.credit(player.getUUID()), currency::matches, approved.fromCredit()));
        paid.addAll(ItemSlots.take(PlayerItems.paySlots(player), currency::matches, approved.fromInventory()));
        player.getInventory().setChanged();
        if (!machine.isInfinite()) {
            // Owned: the money goes in the cash box and the goods come out of stock. Infinite: the money is destroyed.
            paid.forEach(stack -> ItemSlots.insert(machine.cashBox(), stack));
            ItemSlots.take(machine.stock(), selection::sells, selection.quantity());
        }
        ItemSlots.insert(machine.tray(), selection.template());
        MachineSounds.vend(machine.getLevel(), machine.getBlockPos());
        MachineSounds.thankYou(machine.getLevel(), machine.getBlockPos());
        machine.changed();
    }

    /** Spec §3.5 c: tells the buyer what's wrong and who can fix it, with an error buzz. */
    private static void refuse(VendingMachineBlockEntity machine, Player player, int index, DenyReason reason, int funds) {
        Currency currency = machine.currency();
        int button = index + 1;
        Component message = switch (reason) {
            case CATALOG_MISSING -> Messages.explanation(Problem.CATALOG_MISSING, currency);
            case EMPTY -> Component.translatable(Texts.BUTTON_EMPTY, button);
            case SOLD_OUT -> Component.translatable(Texts.SOLD_OUT, button);
            case TRAY_FULL -> Messages.explanation(Problem.TRAY_FULL, currency);
            case CASH_BOX_FULL -> Messages.explanation(Problem.CASH_BOX_FULL, currency);
            case NOT_ENOUGH_MONEY -> Component.translatable(Texts.NEED_MONEY, button,
                    currency.money(machine.getSelection(index).price()), funds);
        };
        MachineSounds.error(machine.getLevel(), machine.getBlockPos());
        Messages.actionBar(player, message);
    }
}
```

In `VendingMachineBlock.use`, add this case at the top of the `switch`, and `import diamondvending.shop.Purchase;`:

```java
            case BUTTON -> Purchase.pressButton(machine, player, target.button());
```

- [ ] **Step 4: Run to verify they pass**

Run: `G26N`
Expected: PASS — unit tests green; `All 51 required tests passed :)`.

- [ ] **Step 5: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 51 on 26.1 Fabric, 50 on both 1.21.1 nodes.

- [ ] **Step 6: Check the Stonecutter state and commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: buying with credit-first payment and per-click messages"
```

---

### Task 6: Contents spill on break; docs

Deliverable: breaking a machine drops the tray, every player's credit, the stock and the cash box (repainting drops nothing); the docs say how to stock a machine by command until the setup screen exists; roadmap and changelog updated.

**Files:**
- Modify: `block/VendingMachineBlockEntity.java` (`spillContents`, 26.1 `preRemoveSideEffects`), `block/VendingMachineBlock.java` (1.21.1 `onRemove`)
- Modify: `BuyingTests.java`, `FabricBuyingTests.java`, `NeoForgeBuyingTests.java`
- Modify: `docs/dev-setup.md`, `CHANGELOG.md`, `docs/superpowers/plans/2026-09-23-roadmap.md`

**Interfaces:**
- Consumes: everything above; `MachineTests.breakAsPlayer`, `MachineTests.droppedMachines`, `MachineTests.assertWholeMachine` (Plan 2).
- Produces: `VendingMachineBlockEntity.spillContents()`.

- [ ] **Step 1: Write the failing tests**

Add to `ALL`:

```java
            Map.entry("breaking_spills_everything", BuyingTests::breakingSpillsEverything),
            Map.entry("creative_breaking_still_spills", BuyingTests::creativeBreakingStillSpills),
            Map.entry("dyeing_keeps_the_contents", BuyingTests::dyeingKeepsTheContents)
```

Add the tests:

```java
    // ---- breaking ------------------------------------------------------------------------------------------------

    /** Spec §5.3: nobody's items vanish when a machine is broken. */
    public static void breakingSpillsEverything(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = placeMachine(helper, owner);
        machine.tray().set(0, new ItemStack(Items.BREAD, 1));
        machine.creditOf(UUID.randomUUID()).set(0, new ItemStack(Items.DIAMOND, 4));
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        machine.cashBox().set(0, new ItemStack(Items.EMERALD, 3));
        MachineTests.breakAsPlayer(helper, MachineTests.UPPER_RIGHT, owner);
        helper.assertTrue(droppedNear(helper, MachineTests.MASTER, Items.BREAD) == 1, "the tray should spill");
        helper.assertTrue(droppedNear(helper, MachineTests.MASTER, Items.DIAMOND) == 4, "everyone's credit should spill");
        helper.assertTrue(droppedNear(helper, MachineTests.MASTER, Items.APPLE) == 10, "the stock should spill");
        helper.assertTrue(droppedNear(helper, MachineTests.MASTER, Items.EMERALD) == 3, "the cash box should spill");
        helper.assertTrue(MachineTests.droppedMachines(helper).size() == 1, "and the machine itself drops");
        helper.succeed();
    }

    public static void creativeBreakingStillSpills(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = placeMachine(helper, admin);
        machine.tray().set(0, new ItemStack(Items.BREAD, 2));
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, admin);
        helper.assertTrue(droppedNear(helper, MachineTests.MASTER, Items.BREAD) == 2, "contents spill even in creative");
        helper.assertTrue(MachineTests.droppedMachines(helper).isEmpty(), "but creative breaking still drops no machine");
        helper.succeed();
    }

    public static void dyeingKeepsTheContents(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = placeMachine(helper, owner);
        machine.tray().set(0, new ItemStack(Items.BREAD, 2));
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BLUE_DYE));
        click(helper, owner, MachineLayout.WINDOW);
        MachineTests.assertWholeMachine(helper, DyeColor.BLUE);
        helper.assertTrue(countIn(machine.tray(), Items.BREAD) == 2 && droppedNear(helper, MachineTests.MASTER, Items.BREAD) == 0,
                "repainting must not spill the machine");
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, owner);
        helper.assertTrue(droppedNear(helper, MachineTests.MASTER, Items.BREAD) == 2, "breaking it afterwards still spills the tray");
        helper.succeed();
    }
```

Add to `FabricBuyingTests.java`:

```java
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void breakingSpillsEverything(GameTestHelper helper) {
        BuyingTests.breakingSpillsEverything(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creativeBreakingStillSpills(GameTestHelper helper) {
        BuyingTests.creativeBreakingStillSpills(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void dyeingKeepsTheContents(GameTestHelper helper) {
        BuyingTests.dyeingKeepsTheContents(helper);
    }
```

Add inside the 1.21.1 block of `NeoForgeBuyingTests.java`:

```java

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void breakingSpillsEverything(GameTestHelper helper) {
        BuyingTests.breakingSpillsEverything(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creativeBreakingStillSpills(GameTestHelper helper) {
        BuyingTests.creativeBreakingStillSpills(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void dyeingKeepsTheContents(GameTestHelper helper) {
        BuyingTests.dyeingKeepsTheContents(helper);
    }
```

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — 3 failures ("the tray should spill", "contents spill even in creative", "breaking it afterwards still spills the tray").

- [ ] **Step 3: Spill on removal**

In `VendingMachineBlockEntity.java`, add `import net.minecraft.world.Containers;` and, after `changed()`:

```java
    /** Drops everything the machine holds (spec §5.3): the tray, every player's credit, the stock and the cash box. */
    public void spillContents() {
        if (level == null || level.isClientSide()) return;
        Containers.dropContents(level, worldPosition, tray);
        credits.values().forEach(items -> Containers.dropContents(level, worldPosition, items));
        Containers.dropContents(level, worldPosition, stock);
        Containers.dropContents(level, worldPosition, cashBox);
        ItemSlots.clear(tray);
        ItemSlots.clear(stock);
        ItemSlots.clear(cashBox);
        credits.clear();
    }

    //? if >=26.1 {
    /** 26.1 calls this when the block is really removed — not when it's only repainted. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        spillContents();
    }
    //?}
```

In `VendingMachineBlock.java`, in the breaking section after `playerWillDestroy`, add:

```java
    //? if <26.1 {
    /*// 1.21.1: spill when the master is really removed — a repaint keeps the same block, so it must not spill.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VendingMachineBlockEntity machine) {
            machine.spillContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
```

(A `//` comment inside the commented-out block is fine; a `/** … */` there would end the block early.)

- [ ] **Step 4: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 54 required tests passed :)`.

- [ ] **Step 5: Run the other three nodes**

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 54 on 26.1 Fabric, 53 on both 1.21.1 nodes. On 1.21.1, also confirm `dyeing_keeps_the_contents` fails if you temporarily delete `!state.is(newState.getBlock()) &&` from `onRemove` (then put it back) — that's the check it guards.

- [ ] **Step 6: Docs, changelog, roadmap**

In `docs/dev-setup.md`, add before `## Vanilla vs NeoForge sources`:

````markdown
## Stocking a machine by command
Until the setup screen exists (Plan 5), stock a machine with `/data`. Look at its lower-left part and run, for example:

```
/data merge block <x> <y> <z> {selections:[{slot:0,item:{id:"minecraft:apple",count:2},price:3}],stock:{Items:[{Slot:0b,id:"minecraft:apple",count:64}]}}
```

`slot` 0–11 is button 1–12; `count` is how many one purchase gives; `price` is 0–999 diamonds. Add `infinite:1b` for a
machine that never runs out and destroys what it's paid. The other saved fields are `owner`, `owner_name`, `cash_box`,
`tray` and `credits` (see `VendingMachineBlockEntity`).
````

In `CHANGELOG.md`, under `## [Unreleased]` → `### Added`, append:

```markdown
- Buying: press a numbered button to buy; pay with diamonds from your inventory or with credit loaded through the coin slot (credit is spent first and only you can use or return yours).
- Coin return gives back exactly what you put in; your item drops into the pickup tray, which anyone can empty.
- Every refused click tells you why (nothing for sale, sold out, not enough diamonds, tray full, cash box full), with vanilla sounds.
- Breaking a machine spills its tray, credit, stock and cash box.
```

In `docs/superpowers/plans/2026-09-23-roadmap.md`, change Plan 3's status cell from `Written` to `Done` (the split into six plans was committed with this plan).

- [ ] **Step 7: Check the Stonecutter state and commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src docs CHANGELOG.md
git -C /c/Users/benet/mcvending commit -m "feat: spill contents on break; stocking docs; roadmap split"
```

- [ ] **Step 8: Push and open the PR**

```bash
git -C /c/Users/benet/mcvending push -u origin plan-3/buying
```

Open a PR titled "Plan 3: Buying — credit, coin return, buttons, tray, problems and messages" with the repo's PR template; wait for CI to be green on all four targets.
