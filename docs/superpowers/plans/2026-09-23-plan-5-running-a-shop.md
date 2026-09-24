# Diamond Vending — Plan 5: Running a Shop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Owners and admins run a machine from a setup screen — what each button sells, how many and for how much, the Stock, the Cash Box — and admins can make it infinite, give it a datapack catalog or another currency; a broken machine keeps its setup on the item, and sneaking at your machine with something in hand tells you how to open setup.

**Architecture:** The server side stays plain and GameTested: per-machine currency resolution and datapack catalogs live on the block entity (effective selections, currency and problems are computed there and synced), `block/MachineSetup` is the item component, and `menu/VendingSetupMenu` is an ordinary container menu whose ghost slots, Stock and Cash Box slots use vanilla container clicks and whose buttons use vanilla "menu button" clicks with ids from `core/SetupButtons` — decoded and re-checked on the server, so no custom packets are needed. The loaders differ only in registration: the menu type (opened with the machine's position), the data component, the catalog reload listener, the "right-click block" event for the empty-hands hint, and the screen. The client screen draws with filled rectangles, text and items through a tiny adapter, so 26.1 and 1.21.1 share it except for four overridden methods.

**Tech Stack:** as Plans 1–4. New loader APIs: NeoForge `IMenuTypeExtension`, `RegisterMenuScreensEvent`, `AddServerReloadListenersEvent` (26.1) / `AddReloadListenerEvent` (1.21.1), `PlayerInteractEvent.RightClickBlock`; Fabric `ExtendedMenuType`/`ExtendedMenuProvider` (26.1) / `ExtendedScreenHandlerType`/`ExtendedScreenHandlerFactory` (1.21.1), `DataResourceLoader` (26.1) / `ResourceManagerHelper` (1.21.1), `UseBlockCallback`, vanilla `MenuScreens.register` (opened by Fabric's transitive access wideners on both versions).

**Spec:** [`docs/superpowers/specs/2026-09-23-diamond-vending-design.md`](../specs/2026-09-23-diamond-vending-design.md) (§3.2 rule 1, §4, §5) · **Roadmap:** [`2026-09-23-roadmap.md`](2026-09-23-roadmap.md) · **Previous:** [Plan 4](2026-09-23-plan-4-seeing-it.md)

## Global Constraints

- Everything in Plans 1–4's Global Constraints still holds (mod id and package `diamondvending`, nodes, vcsVersion `26.1-neoforge`, no runtime deps, `core/` has no Minecraft imports, one Gradle node at a time, one shell command per Bash call, branch → PR → squash, write `Identifier`, player-facing keys live in `core/Texts` and `TextsTest` checks en_us.json, client classes only in `client/` and the loaders' client entrypoints, `//` comments only inside Stonecutter `//? if` blocks).
- Branch: `plan-5/running-a-shop` (already created; this plan is its first commit).
- If `java` isn't on PATH in the Bash tool, prefix Gradle with `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"`. Every API in this plan was checked on 2026-09-24 against the NeoForge-patched 1.21.1/26.1 sources in the session scratchpad, the NeoForge 26.1.2.107 sources jar, DataFixerUpper 8.0.16 (1.21.1) and the cached Fabric API jars (`fabric-menu-api-v1` 2.0.15, `fabric-screen-handler-api-v1` 1.3.91, `fabric-resource-loader-v0` 1.3.1 / `-v1` 2.0.10, transitive access wideners 6.2.0 / 8.1.3, `fabric-client-gametest-api-v1` 5.1.0).
- **The client only asks** (spec §4): every setup change reaches the server as a vanilla container click (ghost slots, Stock, Cash Box, currency slot) or a vanilla menu-button click whose id `core/SetupButtons` encodes. `VendingSetupMenu` re-checks permission (owner or admin), ranges, admin-only fields and "a catalog decides the selections" on every one. Client-side the menu only switches tabs and remembers which button is being edited. (Spec §8.2 lists a `network/` package of payloads; vanilla's two packets carry everything the screen sends, are re-validated on the server as §4 requires, and need no per-loader payload registration — so there is no `network/` package.)
- **Catalog picker** (spec §4): `◀`/`▶` ask the server for the previous/next choice in "None, then every loaded catalog by id", so the catalog list never has to reach the client; clients get the chosen catalog's name through the machine's update tag. (Spec §4 says the list is sent when the screen opens; cycling on the server gives the same picker without a list payload.)
- **Screen size:** 208 × 224 — 32 px wider than a chest screen. Spec §4 says "standard-width"; the extra width lets the red banner show each problem's display text (up to about 190 px, e.g. `CASH BOX FULL - OWNER MUST EMPTY IT`) on one line, and still fits the default 854 × 480 window at GUI scale 2. The player inventory is centred in it.
- **Catalog sync** (spec §8.3 lists `catalogId` as synced): clients never look catalogs up, so the update tag carries the machine's *effective* selections, the catalog's name (`sync_catalog`) and the effective currency (`sync_currency`) instead of the id.
- **Admin** = creative mode or permission level 2 (`block/MachineAccess.isAdmin`, Plan 2).
- **Currency** (spec §5.5): the machine's currency slot → its catalog's `currency` → the item tag `#diamondvending:currency`. Credit is kept as the items inserted (Plan 3), so a currency change never swallows credit.
- **Catalogs** (spec §5.1): `data/<namespace>/diamondvending/catalog/<name>.json` → id `<namespace>:<name>`; 1–12 entries, price 0–999, optional `display_name` and `currency`. Decoded with registry access (item components may name enchantments); a bad file is skipped with an error log naming the file and the problem. A machine stores only the id; its effective selections are the catalog's. Machines using a catalog re-sync after `/reload`.
- **Setup kept on the item** (spec §5.4): component `diamondvending:machine_setup` = the machine's own 12 selections, catalog id, currency slot, infinite flag — never contents. Infinite is kept only when an admin places it. A machine with no setup drops a plain item (so it stacks with new ones). An unknown item in a kept setup empties just that button (spec §9).
- **The manual** is written in Plan 6 and covers the spec §6.2 v1 rule list, which already includes every rule this plan adds (empty both hands to open setup, stock and cash box, infinite needs empty Stock and Cash Box, infinite kept only for admin placers, catalogs, the currency slot). There is no manual to update yet.
- **Client game test** (Fabric 26.1 only): Task 5 adds setup-screen screenshots to `FabricClientTests`; read every screenshot with the Read tool and compare it with the step's Expected description. It needs a display, so it's a local check.

## Review Focus

1. **Two people with the same machine's setup screen open** (owner and an admin) — each sees the other's changes, and Stock or Cash Box items can't be taken twice. → Task 4, `two_open_screens_share_one_machine`.
2. **The machine is broken while its setup screen is open** — the screen closes, and what spilled on the ground can't also be taken from the screen. → Task 4, `breaking_the_machine_closes_its_setup`.
3. **Shift-clicking inventory items while the Items or Admin tab is showing** — nothing goes into the hidden Stock and nothing is lost. → Task 4, `shift_click_only_stocks_on_the_stock_tab`.
4. **A catalog removed from the datapack while machines use it**, then the admin picks "None" — the machine says CATALOG MISSING and sells nothing; with None it sells its own selections again. → Task 2, `a_missing_catalog_stops_sales_and_says_why`, `clearing_the_catalog_brings_back_own_selections`.
5. **A machine item carrying a setup that names an item from a mod since removed** — the machine item still loads and places; only that button comes back empty. → Task 3, `unknown_items_in_a_kept_setup_leave_that_button_empty`.

---

## File Structure

```
src/main/java/diamondvending/
  core/SetupTab.java                     the setup screen's tabs: who sees them, which problem each fixes
  core/SetupButtons.java                 setup button ids ⇄ what they ask for (pure, unit-tested)
  core/Texts.java                        + setup screen and empty-hands keys
  shop/Currency.java                     + Currency.of(item): one item instead of the tag
  shop/Selection.java                    MAX_PRICE now comes from core/SetupButtons
  shop/Purchase.java                     tells PurchaseRules when the catalog is missing
  catalog/Catalog.java                   one catalog file: record + codec
  catalog/Catalogs.java                  what's loaded, the reload generation, the picker's cycle
  catalog/CatalogLoader.java             the data reload listener (common; loaders register it)
  block/VendingMachineBlockEntity.java   + currency slot, catalog id, effective selections/currency, sync, ticker
  block/VendingMachineBlock.java         + ticker, sneak-click opens setup, drop/restore the setup
  block/MachineSetup.java                the machine_setup item component
  block/MachineItems.java                + forMachine(color, machine)
  block/SneakHint.java                   spec §3.2 rule 1: owner sneak-click holding something → hint
  menu/VendingSetupMenu.java             the setup menu: tabs, ghost slots, Stock, Cash Box, buttons
  menu/MachineSlots.java                 a machine slot list as a Container that re-syncs the machine
  registry/Registrar.java, MenuHandle.java, ModContent.java   + data component, + menu (type + open)
  platform/neoforge/…                    registrar (+components, +menus), reload listener, click event, screen
  platform/fabric/…                      registrar (+components, +menus), reload listener, click event, screen
  client/Gui.java                        fill/text/item on either version's GUI graphics
  client/VendingSetupScreen.java         the setup screen
src/main/resources/
  assets/diamondvending/lang/en_us.json  + setup and hint text
  data/diamondvending/diamondvending/catalog/example_snacks.json
src/test/java/diamondvending/core/       SetupTabTest, SetupButtonsTest
src/gametest/java/diamondvending/gametest/
  ShopTests.java                         currency, catalogs, kept setup, setup menu, hint (+ ALL)
  RecordingServerPlayer.java             a real ServerPlayer that remembers its action-bar messages
  BuyingTests.java                       + frontHit (click helper split in two)
  AllTests.java                          + ShopTests.ALL
  fabric/FabricShopTests.java, neoforge/NeoForgeShopTests.java, fabric/FabricClientTests.java
src/gametest/resources/
  fabric.mod.json                        + FabricShopTests
  data/diamondvending/diamondvending/catalog/test_emeralds.json, test_broken.json
docs/catalogs.md, docs/dev-setup.md, CHANGELOG.md, docs/superpowers/plans/2026-09-23-roadmap.md
```

**Adding a shop test:** a `public static void name(GameTestHelper)` in `ShopTests` + its `ALL` entry, a method in `FabricShopTests`, and a method in the 1.21.1 block of `NeoForgeShopTests`. The two adapter methods are always exactly these (with the test's own camelCase name):

```java
    // FabricShopTests (inside the class):
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void testName(GameTestHelper helper) {
        ShopTests.testName(helper);
    }

    // NeoForgeShopTests (inside the `//? if <26.1 {` … `*///?}` block, so no comment markers of its own):

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void testName(GameTestHelper helper) {
        ShopTests.testName(helper);
    }
```

**Test counts** ("All N required tests passed"): today 74 on 26.1 (73 ours + vanilla's `always_pass`), 73 on 1.21.1. Task 1 +4, Task 2 +8, Task 3 +5, Task 4 +15, Task 6 +2 → 108 / 107 at the end.

**Gradle commands** (prefix `JAVA_HOME=…` if needed; one at a time; log to the workspace and read the tail):
- `G26N` = `./gradlew :26.1-neoforge:test :26.1-neoforge:runGameTestServer`
- `G26F` = `./gradlew :26.1-fabric:test :26.1-fabric:runGametest`
- `G121N` = `./gradlew :1.21.1-neoforge:test :1.21.1-neoforge:runGameTestServer`
- `G121F` = `./gradlew :1.21.1-fabric:test :1.21.1-fabric:runGametest`
- `GCLIENT` = `./gradlew :26.1-fabric:runClientGametest` (opens a game window for a minute or two)

---

### Task 1: A currency per machine

Deliverable: an admin's currency slot on a machine changes what it takes as money (spec §5.5, first rule); the slot is saved and synced, and credit inserted earlier still comes back as it went in.

**Files:**
- Modify: `src/main/java/diamondvending/shop/Currency.java`
- Modify: `src/main/java/diamondvending/block/VendingMachineBlockEntity.java`
- Create: `src/gametest/java/diamondvending/gametest/ShopTests.java`, `fabric/FabricShopTests.java`, `neoforge/NeoForgeShopTests.java`
- Modify: `src/gametest/java/diamondvending/gametest/AllTests.java`, `src/gametest/resources/fabric.mod.json`, `docs/dev-setup.md`

**Interfaces:**
- Consumes: `BuyingTests` helpers (`appleMachine`, `placeMachine`, `buyerWith`, `pressButton`, `click`, `lastMessage`, `translation`, `countIn`, `countHeld`, `reload`, `registries`), `DisplayTests.clientView`, `RecordingPlayer` (Plans 3–4).
- Produces: `Currency.of(Item)`; `VendingMachineBlockEntity.currencySlot()` (Item or null), `setCurrencySlot(Item)` (null or air = empty), `currency()` (the slot, else the default — Task 2 adds the catalog); saved key `currency` (item id string); `ShopTests` + adapters.

- [ ] **Step 1: Write the failing GameTests**

`src/gametest/java/diamondvending/gametest/ShopTests.java`:

```java
package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.MachineLayout;
import diamondvending.core.Texts;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game tests for running a shop (Plan 5): currency, catalogs, the setup kept on the item, the setup menu and the
 * empty-hands hint. Each test also needs a method in {@code fabric/FabricShopTests} and (for 1.21.1)
 * {@code neoforge/NeoForgeShopTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 */
public final class ShopTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("the_currency_slot_changes_what_the_machine_takes", ShopTests::theCurrencySlotChangesWhatTheMachineTakes),
            Map.entry("credit_comes_back_as_it_went_in_after_a_currency_change", ShopTests::creditComesBackAsItWentInAfterACurrencyChange),
            Map.entry("the_currency_slot_is_saved_and_synced", ShopTests::theCurrencySlotIsSavedAndSynced),
            Map.entry("an_unknown_currency_loads_as_the_default", ShopTests::anUnknownCurrencyLoadsAsTheDefault));

    private ShopTests() {}

    // ---- currency (spec §5.5) -----------------------------------------------------------------------------------

    public static void theCurrencySlotChangesWhatTheMachineTakes(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // button 1: 2 apples for 3
        machine.setCurrencySlot(Items.EMERALD);
        RecordingPlayer diamonds = BuyingTests.buyerWith(helper, 5);
        BuyingTests.pressButton(helper, diamonds, 0);
        Object[] args = BuyingTests.lastMessage(helper, diamonds, Texts.NEED_MONEY).getArgs();
        BuyingTests.translation(helper, (Component) args[1], "currency.diamondvending.minecraft.emerald.many");
        helper.assertTrue(args[2].equals(0), "diamonds aren't money on a machine that takes emeralds, but the message counted " + args[2]);
        RecordingPlayer emeralds = new RecordingPlayer(helper, GameType.SURVIVAL);
        emeralds.getInventory().add(new ItemStack(Items.EMERALD, 3));
        BuyingTests.pressButton(helper, emeralds, 0);
        helper.assertTrue(BuyingTests.countIn(machine.tray(), Items.APPLE) == 2, "3 emeralds should buy the apples");
        helper.assertTrue(BuyingTests.countIn(machine.cashBox(), Items.EMERALD) == 3, "and go in the cash box");
        helper.assertTrue(BuyingTests.countHeld(diamonds, Items.DIAMOND) == 5, "nobody's diamonds were taken");
        helper.succeed();
    }

    /** Spec §3.4: credit is kept as the items that went in, so changing the currency never swallows it. */
    public static void creditComesBackAsItWentInAfterACurrencyChange(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 4));
        BuyingTests.click(helper, buyer, MachineLayout.COIN_SLOT);
        machine.setCurrencySlot(Items.EMERALD);
        BuyingTests.click(helper, buyer, MachineLayout.COIN_RETURN);
        int back = BuyingTests.countHeld(buyer, Items.DIAMOND);
        helper.assertTrue(back == 4, "the 4 diamonds should come back, got " + back);
        helper.succeed();
    }

    public static void theCurrencySlotIsSavedAndSynced(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setCurrencySlot(Items.EMERALD);
        VendingMachineBlockEntity loaded = BuyingTests.reload(helper, machine, machine.saveWithFullMetadata(BuyingTests.registries(helper)));
        helper.assertTrue(loaded.currencySlot() == Items.EMERALD, "the currency slot should be saved, got " + loaded.currencySlot());
        VendingMachineBlockEntity client = DisplayTests.clientView(helper, machine);
        helper.assertTrue(client.currency().displayItem() == Items.EMERALD, "clients should show emerald prices");
        helper.assertTrue(client.currency().matches(new ItemStack(Items.EMERALD)), "and count emerald credit");
        helper.succeed();
    }

    /** Spec §9: a currency item from a removed mod loads as the default currency. */
    public static void anUnknownCurrencyLoadsAsTheDefault(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        CompoundTag saved = machine.saveWithFullMetadata(BuyingTests.registries(helper));
        saved.putString("currency", "notamod:coin");
        VendingMachineBlockEntity loaded = BuyingTests.reload(helper, machine, saved);
        helper.assertTrue(loaded.currencySlot() == null, "an unknown currency should load as an empty slot");
        helper.assertTrue(loaded.currency().displayItem() == Items.DIAMOND, "and the machine takes diamonds again");
        helper.succeed();
    }
}
```

`src/gametest/java/diamondvending/gametest/fabric/FabricShopTests.java`:

```java
package diamondvending.gametest.fabric;

import diamondvending.gametest.MachineTests;
import diamondvending.gametest.ShopTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/** Fabric entrypoint for {@link ShopTests} (see {@link FabricGameTests} for why the annotations differ per version). */
public final class FabricShopTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCurrencySlotChangesWhatTheMachineTakes(GameTestHelper helper) {
        ShopTests.theCurrencySlotChangesWhatTheMachineTakes(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creditComesBackAsItWentInAfterACurrencyChange(GameTestHelper helper) {
        ShopTests.creditComesBackAsItWentInAfterACurrencyChange(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCurrencySlotIsSavedAndSynced(GameTestHelper helper) {
        ShopTests.theCurrencySlotIsSavedAndSynced(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void anUnknownCurrencyLoadsAsTheDefault(GameTestHelper helper) {
        ShopTests.anUnknownCurrencyLoadsAsTheDefault(helper);
    }
}
```

`src/gametest/java/diamondvending/gametest/neoforge/NeoForgeShopTests.java`:

```java
package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import diamondvending.gametest.ShopTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * {@link ShopTests} for NeoForge 1.21.1, found through {@code @GameTestHolder} like {@link NeoForgeGameTests}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeShopTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCurrencySlotChangesWhatTheMachineTakes(GameTestHelper helper) {
        ShopTests.theCurrencySlotChangesWhatTheMachineTakes(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creditComesBackAsItWentInAfterACurrencyChange(GameTestHelper helper) {
        ShopTests.creditComesBackAsItWentInAfterACurrencyChange(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCurrencySlotIsSavedAndSynced(GameTestHelper helper) {
        ShopTests.theCurrencySlotIsSavedAndSynced(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void anUnknownCurrencyLoadsAsTheDefault(GameTestHelper helper) {
        ShopTests.anUnknownCurrencyLoadsAsTheDefault(helper);
    }
    *///?}
}
```

In `AllTests.java` change the list to `List.of(MachineTests.ALL, BuyingTests.ALL, DisplayTests.ALL, ShopTests.ALL)`. In `src/gametest/resources/fabric.mod.json` add `"diamondvending.gametest.fabric.FabricShopTests"` after `FabricDisplayTests` in `fabric-gametest`. In `docs/dev-setup.md` change "a `public static void` method in `gametest/MachineTests.java`, `BuyingTests.java` or `DisplayTests.java`" to "… `BuyingTests.java`, `DisplayTests.java` or `ShopTests.java`".

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors: `cannot find symbol: method setCurrencySlot(Item)` and `currencySlot()`.

- [ ] **Step 3: Let a currency be one item**

Replace the body of `shop/Currency.java` from `public static final Currency DEFAULT` down to `displayItem()`'s closing brace with:

```java
    /** The default: the tag, which ships with just diamonds and which packs may change by datapack. */
    public static final Currency DEFAULT = new Currency(null);

    /** One kind of item, or null for the tag. */
    private final Item item;

    private Currency(Item item) {
        this.item = item;
    }

    /** Exactly one kind of item: an admin's currency slot or a catalog's currency (spec §5.5). */
    public static Currency of(Item item) {
        return new Currency(item);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return item != null ? stack.is(item) : stack.is(TAG);
    }

    /** The item shown on price tags and in messages: the currency's item, else the tag's first item, else a diamond. */
    public Item displayItem() {
        if (item != null) return item;
        for (Holder<Item> tagged : BuiltInRegistries.ITEM.getTagOrEmpty(TAG)) {
            return tagged.value();
        }
        return Items.DIAMOND;
    }
```

and update the class comment's second sentence to: "The default is the item tag {@code #diamondvending:currency} (just diamonds unless a datapack changes it); a machine's currency slot or catalog can name one item instead."

- [ ] **Step 4: Give the machine a currency slot**

In `block/VendingMachineBlockEntity.java`:

Add imports `net.minecraft.core.registries.BuiltInRegistries`, `net.minecraft.world.item.Item`, `net.minecraft.world.item.Items`; and in the 1.21.1 import block (`//?} else {` … `*///?}` at the top) add `import net.minecraft.resources.Identifier;`.

Add after `private static final String INFINITE = "infinite";`:

```java
    private static final String CURRENCY = "currency";
```

Add after `private boolean infinite;`:

```java
    /** The admin's currency slot (spec §4 Admin tab), or null when it's empty. */
    private Item currencySlot;
```

Replace the `currency()` method with:

```java
    /** The admin's currency slot, or null when it's empty. */
    public Item currencySlot() {
        return currencySlot;
    }

    /** Sets the currency slot; null (or air) empties it. */
    public void setCurrencySlot(Item item) {
        currencySlot = item == Items.AIR ? null : item;
        changed();
    }

    /** What this machine takes as money (spec §5.5): the currency slot, else the default. */
    public Currency currency() {
        return currencySlot != null ? Currency.of(currencySlot) : Currency.DEFAULT;
    }
```

In `clearContents()` add `currencySlot = null;`.

26.1 `saveAdditional`, after `output.putBoolean(INFINITE, infinite);`:

```java
        if (currencySlot != null) output.store(CURRENCY, BuiltInRegistries.ITEM.byNameCodec(), currencySlot);
```

26.1 `loadAdditional`, after the `infinite = …` line:

```java
        currencySlot = input.read(CURRENCY, BuiltInRegistries.ITEM.byNameCodec()).filter(item -> item != Items.AIR).orElse(null);
```

1.21.1 `saveAdditional` (inside the `/* … */` block), after `tag.putBoolean(INFINITE, infinite);`:

```java
        if (currencySlot != null) tag.putString(CURRENCY, BuiltInRegistries.ITEM.getKey(currencySlot).toString());
```

1.21.1 `loadAdditional`, after the `infinite = …` line:

```java
        currencySlot = itemOrNull(tag.getString(CURRENCY));
```

and, still inside the 1.21.1 block, after `loadAdditional`:

```java
    // An item id as saved, or null when it's missing, air or from a mod that's gone (spec §9).
    private static Item itemOrNull(String id) {
        Identifier key = Identifier.tryParse(id);
        Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.get(key);
        return item == Items.AIR ? null : item;
    }
```

- [ ] **Step 5: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 78 required tests passed :)`.

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 78 / 77 / 77 pass.

- [ ] **Step 6: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src docs/dev-setup.md
git -C /c/Users/benet/mcvending commit -m "feat: a currency slot per machine"
```

---

### Task 2: Datapack catalogs

Deliverable: datapacks can define catalogs; a machine given a catalog sells its entries (in its currency), says CATALOG MISSING when the catalog is gone, and clients see what it sells; the mod ships `diamondvending:example_snacks` and `docs/catalogs.md`.

**Files:**
- Create: `src/main/java/diamondvending/catalog/Catalog.java`, `Catalogs.java`, `CatalogLoader.java`
- Create: `src/main/resources/data/diamondvending/diamondvending/catalog/example_snacks.json`
- Create: `src/gametest/resources/data/diamondvending/diamondvending/catalog/test_emeralds.json`, `test_broken.json`
- Modify: `block/VendingMachineBlockEntity.java`, `block/VendingMachineBlock.java`, `shop/Purchase.java`
- Modify: `platform/neoforge/DiamondVendingNeoForge.java`, `platform/fabric/DiamondVendingFabric.java`
- Create: `docs/catalogs.md`
- Test: `ShopTests.java`, `FabricShopTests.java`, `NeoForgeShopTests.java`

**Interfaces:**
- Consumes: Task 1's `currencySlot()`, `currency()`, `Currency.of`; `Selection.of`, `Selection.MAX_PRICE`; `MachineProblems`/`MachineFacts.catalogMissing` (Plan 1); `PurchaseInput.catalogMissing` (Plan 3).
- Produces: `catalog.Catalog` — `record Catalog(Optional<String> displayName, Optional<Item> currency, List<Selection> selections)`, `CODEC`, `selection(int)`, `name(Identifier)`; `catalog.Catalogs.get(Identifier)`, `ids()`, `generation()`, `cycle(Identifier current, int step)`; `catalog.CatalogLoader(HolderLookup.Provider)`, `CatalogLoader.ID`; on `VendingMachineBlockEntity`: `catalogId()`, `setCatalog(Identifier)` (null clears), `catalogMissing()`, `usesCatalog()`, `catalogLabel()`, `getSelection(int)` now the **effective** selection, `ownSelection(int)`, `serverTick(...)`; saved key `catalog`, sync keys `sync_catalog`, `sync_currency`.

- [ ] **Step 1: Add the test catalogs**

`src/gametest/resources/data/diamondvending/diamondvending/catalog/test_emeralds.json`:

```json
{
  "display_name": "Emerald Emporium",
  "currency": { "id": "minecraft:emerald" },
  "entries": [
    { "item": { "id": "minecraft:apple", "count": 2 }, "price": 3 },
    { "item": { "id": "minecraft:bread" }, "price": 0 }
  ]
}
```

`src/gametest/resources/data/diamondvending/diamondvending/catalog/test_broken.json` (a price over 999, so it must be skipped):

```json
{
  "entries": [
    { "item": { "id": "minecraft:apple" }, "price": 5000 }
  ]
}
```

- [ ] **Step 2: Write the failing GameTests**

In `ShopTests.java` add imports:

```java
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import diamondvending.DiamondVending;
import diamondvending.catalog.Catalog;
import diamondvending.catalog.Catalogs;
import diamondvending.core.Problem;
import diamondvending.shop.Selection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Collections;
```

Add to `ALL`:

```java
            Map.entry("the_example_catalog_loads", ShopTests::theExampleCatalogLoads),
            Map.entry("a_broken_catalog_is_skipped", ShopTests::aBrokenCatalogIsSkipped),
            Map.entry("catalog_files_are_checked", ShopTests::catalogFilesAreChecked),
            Map.entry("a_catalog_machine_sells_the_catalog", ShopTests::aCatalogMachineSellsTheCatalog),
            Map.entry("an_owned_catalog_machine_sells_from_its_stock", ShopTests::anOwnedCatalogMachineSellsFromItsStock),
            Map.entry("a_missing_catalog_stops_sales_and_says_why", ShopTests::aMissingCatalogStopsSalesAndSaysWhy),
            Map.entry("clearing_the_catalog_brings_back_own_selections", ShopTests::clearingTheCatalogBringsBackOwnSelections),
            Map.entry("clients_see_what_the_catalog_sells", ShopTests::clientsSeeWhatTheCatalogSells)
```

(add a comma after the previous last entry), and the tests:

```java
    // ---- catalogs (spec §5.1) -----------------------------------------------------------------------------------

    /** Test data (src/gametest/resources): button 1 = 2 apples for 3 emeralds, button 2 = free bread. */
    static final Identifier EMERALDS = DiamondVending.id("test_emeralds");

    public static void theExampleCatalogLoads(GameTestHelper helper) {
        Identifier id = DiamondVending.id("example_snacks");
        Catalog catalog = Catalogs.get(id);
        helper.assertTrue(catalog != null, "the example catalog should load; loaded: " + Catalogs.ids());
        helper.assertTrue(catalog.name(id).equals("Snack Shack"), "its name comes from display_name, got " + catalog.name(id));
        helper.assertTrue(catalog.selections().size() == 6, "it has 6 entries, got " + catalog.selections().size());
        ItemStack book = catalog.selection(5).template();
        helper.assertTrue(book.is(Items.ENCHANTED_BOOK)
                        && !book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                "button 6 sells a book with mending on it, got " + book);
        helper.assertTrue(catalog.currency().isEmpty(), "it takes the default currency");
        helper.succeed();
    }

    public static void aBrokenCatalogIsSkipped(GameTestHelper helper) {
        helper.assertTrue(Catalogs.get(DiamondVending.id("test_broken")) == null, "a catalog with a price of 5000 must not load");
        helper.assertTrue(Catalogs.get(EMERALDS) != null, "the good test catalog next to it still loads");
        helper.succeed();
    }

    /** Spec §5.1: 1 to 12 entries, prices 0–999, real items. */
    public static void catalogFilesAreChecked(GameTestHelper helper) {
        String apple = "{\"item\":{\"id\":\"minecraft:apple\"},\"price\":1}";
        helper.assertTrue(parses(helper, "{\"entries\":[" + apple + "]}"), "one apple is a fine catalog");
        helper.assertFalse(parses(helper, "{\"entries\":[" + String.join(",", Collections.nCopies(13, apple)) + "]}"), "13 entries are too many");
        helper.assertFalse(parses(helper, "{\"entries\":[]}"), "a catalog needs at least one entry");
        helper.assertFalse(parses(helper, "{\"entries\":[{\"item\":{\"id\":\"minecraft:apple\"},\"price\":1000}]}"), "prices stop at 999");
        helper.assertFalse(parses(helper, "{\"entries\":[{\"item\":{\"id\":\"notamod:gadget\"},\"price\":1}]}"), "unknown items are rejected");
        helper.assertFalse(parses(helper, "{\"currency\":{\"id\":\"minecraft:air\"},\"entries\":[" + apple + "]}"), "air is no currency");
        helper.succeed();
    }

    private static boolean parses(GameTestHelper helper, String json) {
        return Catalog.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, BuyingTests.registries(helper)), JsonParser.parseString(json))
                .result().isPresent();
    }

    public static void aCatalogMachineSellsTheCatalog(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.BREAD), 1)); // the machine's own button 1, hidden by the catalog
        machine.setInfinite(true);
        machine.setCatalog(EMERALDS);
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.getInventory().add(new ItemStack(Items.EMERALD, 3));
        BuyingTests.pressButton(helper, buyer, 0);
        helper.assertTrue(BuyingTests.countIn(machine.tray(), Items.APPLE) == 2, "button 1 sells the catalog's 2 apples");
        helper.assertTrue(BuyingTests.countHeld(buyer, Items.EMERALD) == 0, "for the catalog's price, in its currency");
        helper.assertTrue(BuyingTests.countIn(machine.cashBox(), Items.EMERALD) == 0, "an infinite machine destroys the money");
        helper.succeed();
    }

    /** Spec §5.2: owned machines with a catalog still sell from their stock. */
    public static void anOwnedCatalogMachineSellsFromItsStock(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setCatalog(EMERALDS);
        BuyingTests.assertProblems(helper, machine, Problem.SOLD_OUT);
        machine.stock().set(0, new ItemStack(Items.APPLE, 4));
        machine.changed();
        BuyingTests.assertProblems(helper, machine);
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.getInventory().add(new ItemStack(Items.EMERALD, 3));
        BuyingTests.pressButton(helper, buyer, 0);
        helper.assertTrue(BuyingTests.countIn(machine.stock(), Items.APPLE) == 2, "the apples come out of stock");
        helper.assertTrue(BuyingTests.countIn(machine.cashBox(), Items.EMERALD) == 3, "the emeralds go in the cash box");
        helper.succeed();
    }

    /** Spec §5.1: a machine whose catalog isn't loaded says so and sells nothing. */
    public static void aMissingCatalogStopsSalesAndSaysWhy(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        BuyingTests.assertProblems(helper, machine, Problem.CATALOG_MISSING);
        RecordingPlayer buyer = BuyingTests.buyerWith(helper, 5);
        BuyingTests.pressButton(helper, buyer, 0);
        BuyingTests.lastMessage(helper, buyer, Texts.explanation(Problem.CATALOG_MISSING));
        helper.assertTrue(BuyingTests.countHeld(buyer, Items.DIAMOND) == 5 && BuyingTests.countIn(machine.tray(), Items.APPLE) == 0,
                "nothing is sold");
        helper.succeed();
    }

    public static void clearingTheCatalogBringsBackOwnSelections(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // own button 1: 2 apples for 3, 10 in stock
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        machine.setCatalog(null);
        BuyingTests.assertProblems(helper, machine);
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        helper.succeed();
    }

    /** Spec §8.3: clients get the catalog's selections, name and currency; the save keeps the id and the own selections. */
    public static void clientsSeeWhatTheCatalogSells(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.BREAD), 1));
        machine.setCatalog(EMERALDS);
        VendingMachineBlockEntity client = DisplayTests.clientView(helper, machine);
        BuyingTests.assertSelection(helper, client, 0, Items.APPLE, 2, 3);
        helper.assertTrue(client.currency().displayItem() == Items.EMERALD, "clients show the catalog's currency");
        helper.assertTrue(client.usesCatalog() && client.catalogLabel().equals("Emerald Emporium"),
                "clients know the catalog's name, got \"" + client.catalogLabel() + "\"");
        VendingMachineBlockEntity loaded = BuyingTests.reload(helper, machine, machine.saveWithFullMetadata(BuyingTests.registries(helper)));
        helper.assertTrue(EMERALDS.equals(loaded.catalogId()), "the save keeps the catalog id");
        helper.assertTrue(loaded.ownSelection(0).template().is(Items.BREAD), "and the machine's own button 1");
        helper.succeed();
    }
```

Add the eight adapter methods to `FabricShopTests.java` and to the 1.21.1 block of `NeoForgeShopTests.java`, using the templates under "Adding a shop test" at the top of this plan: `theExampleCatalogLoads`, `aBrokenCatalogIsSkipped`, `catalogFilesAreChecked`, `aCatalogMachineSellsTheCatalog`, `anOwnedCatalogMachineSellsFromItsStock`, `aMissingCatalogStopsSalesAndSaysWhy`, `clearingTheCatalogBringsBackOwnSelections`, `clientsSeeWhatTheCatalogSells`.

- [ ] **Step 3: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors: package `diamondvending.catalog` does not exist; `setCatalog`, `catalogId`, `ownSelection`, `usesCatalog`, `catalogLabel` not found.

- [ ] **Step 4: Write the catalog classes**

`src/main/java/diamondvending/catalog/Catalog.java`:

```java
package diamondvending.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import diamondvending.core.MachineLayout;
import diamondvending.shop.Selection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * A datapack catalog (spec §5.1): what an admin can make a machine sell, from
 * {@code data/<namespace>/diamondvending/catalog/<name>.json}. Entries fill buttons 1…N in order; the format is in
 * docs/catalogs.md.
 */
public record Catalog(Optional<String> displayName, Optional<Item> currency, List<Selection> selections) {
    /** One entry: a standard item stack (its count is the amount per purchase) and a price. */
    private static final Codec<Selection> ENTRY = RecordCodecBuilder.create(entry -> entry.group(
            ItemStack.CODEC.fieldOf("item").forGetter(Selection::template),
            Codec.intRange(0, Selection.MAX_PRICE).fieldOf("price").forGetter(Selection::price)
    ).apply(entry, Selection::of));

    /** {@code {"id": "minecraft:emerald"}} — shaped like an item stack, but a count is ignored. */
    private static final Codec<Item> CURRENCY = BuiltInRegistries.ITEM.byNameCodec()
            .validate(item -> item == Items.AIR ? DataResult.error(() -> "the currency can't be air") : DataResult.success(item))
            .fieldOf("id").codec();

    public static final Codec<Catalog> CODEC = RecordCodecBuilder.create(catalog -> catalog.group(
            Codec.STRING.optionalFieldOf("display_name").forGetter(Catalog::displayName),
            CURRENCY.optionalFieldOf("currency").forGetter(Catalog::currency),
            ENTRY.listOf(1, MachineLayout.SELECTIONS).fieldOf("entries").forGetter(Catalog::selections)
    ).apply(catalog, Catalog::new));

    /** Button {@code index}'s selection: the entry in that position, or empty past the last entry. */
    public Selection selection(int index) {
        return index < selections.size() ? selections.get(index) : Selection.EMPTY;
    }

    /** The name shown in the Admin tab: its display name, or its id when it has none. */
    public String name(Identifier id) {
        return displayName.orElse(id.toString());
    }
}
```

`src/main/java/diamondvending/catalog/Catalogs.java`:

```java
package diamondvending.catalog;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The catalogs the server has loaded (spec §5.1), replaced whenever datapacks load or /reload runs. Server side only. */
public final class Catalogs {
    private static volatile Map<Identifier, Catalog> loaded = Map.of();
    private static volatile int generation;

    private Catalogs() {}

    /** The catalog with this id, or null if none is loaded under it. */
    public static Catalog get(Identifier id) {
        return loaded.get(id);
    }

    /** Every loaded catalog's id, in order. */
    public static List<Identifier> ids() {
        return loaded.keySet().stream().sorted().toList();
    }

    /** Goes up on every load, so machines can tell that their catalog may have changed. */
    public static int generation() {
        return generation;
    }

    /**
     * The Admin tab's picker (spec §4): "None" (null), then every catalog in order, and round again. {@code step} is
     * +1 or −1; a catalog that isn't loaded counts as "None".
     */
    public static Identifier cycle(Identifier current, int step) {
        List<Identifier> choices = new ArrayList<>();
        choices.add(null);
        choices.addAll(ids());
        int at = Math.max(choices.indexOf(current), 0);
        return choices.get(Math.floorMod(at + step, choices.size()));
    }

    static void replace(Map<Identifier, Catalog> catalogs) {
        loaded = Map.copyOf(catalogs);
        generation++;
    }
}
```

`src/main/java/diamondvending/catalog/CatalogLoader.java`:

```java
package diamondvending.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import diamondvending.DiamondVending;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads catalogs from datapacks, like recipes (spec §5.1). Each loader registers it with the game's registries, because
 * catalog items can carry components that name registry entries (a book's enchantments). A file with a mistake is
 * skipped, and the log says which file and what's wrong.
 */
public class CatalogLoader extends SimplePreparableReloadListener<Map<Identifier, Catalog>> {
    public static final Identifier ID = DiamondVending.id("catalogs");
    private static final FileToIdConverter FILES = FileToIdConverter.json("diamondvending/catalog");

    private final HolderLookup.Provider registries;

    public CatalogLoader(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    protected Map<Identifier, Catalog> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Catalog> catalogs = new HashMap<>();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        FILES.listMatchingResources(manager).forEach((file, resource) -> {
            Identifier id = FILES.fileToId(file);
            try (Reader reader = resource.openAsReader()) {
                Catalog.CODEC.parse(ops, JsonParser.parseReader(reader))
                        .ifSuccess(catalog -> catalogs.put(id, catalog))
                        .ifError(error -> DiamondVending.LOGGER.error("Skipping vending machine catalog {} ({}): {}", id, file, error.message()));
            } catch (IOException | RuntimeException e) {
                DiamondVending.LOGGER.error("Skipping vending machine catalog {} ({}): {}", id, file, e.getMessage());
            }
        });
        return catalogs;
    }

    @Override
    protected void apply(Map<Identifier, Catalog> catalogs, ResourceManager manager, ProfilerFiller profiler) {
        Catalogs.replace(catalogs);
        DiamondVending.LOGGER.info("Loaded {} vending machine catalog(s)", catalogs.size());
    }
}
```

`src/main/resources/data/diamondvending/diamondvending/catalog/example_snacks.json` (the plain enchantment map is accepted by both versions' `ItemEnchantments` codec):

```json
{
  "display_name": "Snack Shack",
  "entries": [
    { "item": { "id": "minecraft:cookie", "count": 8 }, "price": 1 },
    { "item": { "id": "minecraft:bread", "count": 4 }, "price": 1 },
    { "item": { "id": "minecraft:cake" }, "price": 3 },
    { "item": { "id": "minecraft:golden_apple" }, "price": 4 },
    { "item": { "id": "minecraft:arrow", "count": 16 }, "price": 1 },
    { "item": { "id": "minecraft:enchanted_book", "components": { "minecraft:stored_enchantments": { "minecraft:mending": 1 } } }, "price": 12 }
  ]
}
```

- [ ] **Step 5: Register the loader on each loader**

`platform/neoforge/DiamondVendingNeoForge.java` — add imports:

```java
import diamondvending.catalog.CatalogLoader;
import net.neoforged.neoforge.common.NeoForge;
//? if >=26.1 {
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
//?} else {
/*import net.neoforged.neoforge.event.AddReloadListenerEvent;
*///?}
```

and at the end of the constructor:

```java
        //? if >=26.1 {
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class,
                event -> event.addListener(CatalogLoader.ID, new CatalogLoader(event.getRegistryAccess())));
        //?} else {
        /*NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class,
                event -> event.addListener(new CatalogLoader(event.getRegistryAccess())));
        *///?}
```

`platform/fabric/DiamondVendingFabric.java` — add imports:

```java
import diamondvending.catalog.CatalogLoader;
//? if >=26.1 {
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
//?} else {
/*import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
*///?}
```

at the end of `onInitialize()`:

```java
        //? if >=26.1 {
        DataResourceLoader.get().registerReloadListener(CatalogLoader.ID, CatalogLoader::new);
        //?} else {
        /*ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(CatalogLoader.ID, FabricCatalogLoader::new);
        *///?}
```

and before the class's closing brace:

```java
    //? if <26.1 {
    /*// Fabric 1.21.1 wants each reload listener to carry its id.
    private static final class FabricCatalogLoader extends CatalogLoader implements IdentifiableResourceReloadListener {
        FabricCatalogLoader(HolderLookup.Provider registries) {
            super(registries);
        }

        @Override
        public Identifier getFabricId() {
            return CatalogLoader.ID;
        }
    }
    *///?}
```

- [ ] **Step 6: Let machines use a catalog**

In `block/VendingMachineBlockEntity.java`:

Move `import net.minecraft.resources.Identifier;` from the 1.21.1 import block to the main imports, and add `diamondvending.catalog.Catalog`, `diamondvending.catalog.Catalogs`, `net.minecraft.world.level.Level`.

Add after `private static final String CURRENCY = "currency";`:

```java
    private static final String CATALOG = "catalog";
```

and after `private static final String SYNC_PROBLEMS = "sync_problems";`:

```java
    private static final String SYNC_CATALOG = "sync_catalog";
    private static final String SYNC_CURRENCY = "sync_currency";
```

Add after the `currencySlot` field:

```java
    /** The catalog this machine sells (spec §5.1), or null for its own selections. Server side. */
    private Identifier catalogId;
    /** {@link Catalogs#generation()} when this machine last synced, to re-sync after a /reload. */
    private int seenCatalogs;
    /** True while writing the clients' update tag (server thread only): selections are then the effective ones. */
    private boolean syncing;
```

and after `private int[] syncedProblems = new int[0];`:

```java
    private String syncedCatalog = "";
    private Item syncedCurrency;
```

Replace `getSelection`:

```java
    /**
     * What button {@code index} sells right now: the catalog's entry when the machine has a catalog (nothing if that
     * catalog isn't loaded), otherwise the machine's own selection. On clients: what the server last synced.
     */
    public Selection getSelection(int index) {
        if (catalogId == null) return selections[index];
        Catalog catalog = catalog();
        return catalog != null ? catalog.selection(index) : Selection.EMPTY;
    }

    /** The machine's own selection for button {@code index}, whether or not a catalog is hiding it. */
    public Selection ownSelection(int index) {
        return selections[index];
    }
```

Add after `setInfinite`:

```java
    /** The machine's catalog id, or null. Server side (clients get {@link #catalogLabel()}). */
    public Identifier catalogId() {
        return catalogId;
    }

    /** Assigns a catalog; null goes back to the machine's own selections. */
    public void setCatalog(Identifier id) {
        catalogId = id;
        changed();
    }

    private Catalog catalog() {
        return catalogId == null ? null : Catalogs.get(catalogId);
    }

    /** Whether the machine has a catalog that isn't loaded (spec §3.5 b "Catalog missing"). */
    public boolean catalogMissing() {
        return catalogId != null && catalog() == null;
    }

    /** Whether a catalog decides what this machine sells (on either side). */
    public boolean usesCatalog() {
        return catalogId != null || !syncedCatalog.isEmpty();
    }

    /** The catalog's name for players: its display name, or its id when it has none or isn't loaded; "" without one. */
    public String catalogLabel() {
        if (catalogId == null) return syncedCatalog;
        Catalog catalog = catalog();
        return catalog != null ? catalog.name(catalogId) : catalogId.toString();
    }
```

Replace `currency()` (from Task 1) with:

```java
    /** What this machine takes as money (spec §5.5): the currency slot, else the catalog's currency, else the default. */
    public Currency currency() {
        Item item = syncedCurrency != null ? syncedCurrency : effectiveCurrencyItem();
        return item != null ? Currency.of(item) : Currency.DEFAULT;
    }

    /** Server side: the currency slot, else the catalog's currency; null for the default. */
    private Item effectiveCurrencyItem() {
        if (currencySlot != null) return currencySlot;
        Catalog catalog = catalog();
        return catalog != null ? catalog.currency().orElse(null) : null;
    }
```

In `stockCountFor` replace `Selection selection = selections[index];` with `Selection selection = getSelection(index);`. Replace `problems()` with:

```java
    /** Active problems, in display order (spec §3.5 b). */
    public List<Problem> problems() {
        int setUp = 0;
        int inStock = 0;
        for (int i = 0; i < selections.length; i++) {
            Selection selection = getSelection(i);
            if (!selection.isSetUp()) continue;
            setUp++;
            if (stockCountFor(i) >= selection.quantity()) inStock++;
        }
        return MachineProblems.of(new MachineFacts(infinite, catalogMissing(), setUp, inStock,
                ItemSlots.hasEmptySlot(cashBox), ItemSlots.hasEmptySlot(tray)));
    }
```

Add after `isRepeatPress`:

```java
    /** Server tick: after a /reload, a machine using a catalog re-syncs so clients see the catalog's new entries (spec §5.1). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, VendingMachineBlockEntity machine) {
        if (machine.catalogId != null && machine.seenCatalogs != Catalogs.generation()) {
            machine.seenCatalogs = Catalogs.generation();
            machine.changed();
        }
    }
```

In `clearContents()` add `catalogId = null;`.

26.1 `saveAdditional`: change the selections loop to

```java
        for (int i = 0; i < selections.length; i++) {
            // Clients get what the machine really sells: the catalog's entries when it has one (spec §8.3).
            Selection selection = syncing ? getSelection(i) : selections[i];
            if (!selection.isSetUp()) continue;
            ValueOutput entry = selectionList.addChild();
            entry.putInt(SLOT, i);
            entry.store(ITEM, ItemStack.CODEC, selection.template());
            entry.putInt(PRICE, selection.price());
        }
```

and after the currency line add `if (catalogId != null) output.store(CATALOG, Identifier.CODEC, catalogId);`.

26.1 `loadAdditional`, after the currency line:

```java
        catalogId = input.read(CATALOG, Identifier.CODEC).orElse(null);
        syncedCatalog = input.getStringOr(SYNC_CATALOG, "");
        syncedCurrency = input.read(SYNC_CURRENCY, BuiltInRegistries.ITEM.byNameCodec()).orElse(null);
```

1.21.1 `saveAdditional`: the same loop change (`Selection selection = syncing ? getSelection(i) : selections[i];`, then `entry.put(ITEM, selection.template().save(registries)); entry.putInt(PRICE, selection.price());`), and after the currency line `if (catalogId != null) tag.putString(CATALOG, catalogId.toString());`.

1.21.1 `loadAdditional`, after the currency line:

```java
        catalogId = tag.contains(CATALOG) ? Identifier.tryParse(tag.getString(CATALOG)) : null;
        syncedCatalog = tag.getString(SYNC_CATALOG);
        syncedCurrency = itemOrNull(tag.getString(SYNC_CURRENCY));
```

Replace the start of `getUpdateTag` (up to and including `tag.remove(CREDITS);`) with:

```java
        CompoundTag tag;
        syncing = true;
        try {
            tag = saveCustomOnly(registries);
        } finally {
            syncing = false;
        }
        tag.remove(STOCK);
        tag.remove(CASH_BOX);
        tag.remove(CREDITS);
        // Clients never look catalogs up: they get its entries (above), its name and its currency instead of its id.
        tag.remove(CATALOG);
        String catalog = catalogLabel();
        if (!catalog.isEmpty()) tag.putString(SYNC_CATALOG, catalog);
        Item money = effectiveCurrencyItem();
        if (money != null) tag.putString(SYNC_CURRENCY, BuiltInRegistries.ITEM.getKey(money).toString());
```

and add "the catalog's name and the effective currency" to its comment's list.

In `block/VendingMachineBlock.java` add imports `diamondvending.registry.ModContent`, `net.minecraft.world.level.block.entity.BlockEntityTicker`, `net.minecraft.world.level.block.entity.BlockEntityType`, and after `newBlockEntity`:

```java
    /** Server only: machines using a catalog re-sync after /reload ({@link VendingMachineBlockEntity#serverTick}). */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineBlockEntity::serverTick);
    }
```

In `shop/Purchase.java` replace the first `PurchaseInput` argument `false,` with `machine.catalogMissing(),`.

- [ ] **Step 7: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 86 required tests passed :)`, and the log shows `Skipping vending machine catalog diamondvending:test_broken (…test_broken.json): …999…` and `Loaded 2 vending machine catalog(s)`.

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 86 / 85 / 85 pass (each log shows the same skip line and 2 loaded catalogs).

- [ ] **Step 8: Write the pack-maker reference**

`docs/catalogs.md`:

````markdown
# Catalogs and pre-set machines

For pack makers and server admins. Players never need this page — the in-game manual covers everything they can do.

## Catalogs

A catalog is a JSON file in a datapack: `data/<namespace>/diamondvending/catalog/<name>.json` becomes catalog
`<namespace>:<name>`. An admin assigns it in a machine's setup screen (Admin tab → Catalog). The machine then sells the
catalog's entries on buttons 1…N instead of its own selections; picking **None** brings its own selections back.

```json
{
  "display_name": "Snack Shack",
  "currency": { "id": "minecraft:emerald" },
  "entries": [
    { "item": { "id": "minecraft:golden_apple", "count": 1 }, "price": 4 },
    { "item": { "id": "minecraft:arrow", "count": 16 }, "price": 1 },
    { "item": { "id": "minecraft:enchanted_book", "count": 1,
                "components": { "minecraft:stored_enchantments": { "minecraft:mending": 1 } } },
      "price": 12 }
  ]
}
```

| Field | Needed | Meaning |
|---|---|---|
| `display_name` | no | The name in the Admin tab and on the setup screen. Default: the catalog's id. |
| `currency` | no | `{ "id": "<item>" }` — what machines using this catalog take as money (a count is ignored). A machine's own currency slot wins over it. |
| `entries` | yes | 1 to 12 entries. They fill buttons 1, 2, 3… in order. |
| `entries[].item` | yes | A standard item stack: `id`, `count` (how many one purchase gives, up to the item's stack size) and optional `components`. |
| `entries[].price` | yes | 0–999 currency items. 0 means free. |

- Catalogs load with the datapacks and again on `/reload`. Machines using one update right after a reload.
- A file with a mistake is skipped. The server log says which file and what's wrong, starting with
  `Skipping vending machine catalog`. A machine whose catalog isn't loaded shows **CATALOG MISSING - ASK AN ADMIN** and
  sells nothing until an admin picks another catalog or None.
- An owned machine with a catalog still sells from its Stock and fills its Cash Box. An infinite one never runs out and
  the money disappears.
- The mod ships `diamondvending:example_snacks`.

## Currency

A machine takes, first match wins:

1. its **currency slot** (Admin tab of the setup screen),
2. its catalog's `currency`,
3. the item tag **`#diamondvending:currency`**, which ships with just `minecraft:diamond`.

To change the default for a whole pack, override `data/diamondvending/tags/item/currency.json` in a datapack. The tag's
first item is the one shown on price tags. Currency matches by item type; names and enchantments on the coins don't
matter. Credit is stored as the items that went in, so coin return always gives those back, even after the currency
changes.

## Pre-set machines with `/setblock`

A machine is four blocks. The lower-left one (as seen from the front) holds all the data; place all four:

```
/setblock 0 64 0 diamondvending:vending_machine[facing=south,half=lower,side=left]{catalog:"diamondvending:example_snacks",infinite:1b}
/setblock 1 64 0 diamondvending:vending_machine[facing=south,half=lower,side=right]
/setblock 0 65 0 diamondvending:vending_machine[facing=south,half=upper,side=left]
/setblock 1 65 0 diamondvending:vending_machine[facing=south,half=upper,side=right]
```

`facing` is the way the front faces. The right-hand column is at x + 1 for `south`, x − 1 for `north`, z − 1 for
`east` and z + 1 for `west`. Add `color=<dye>` to every part for a color other than red. A machine placed this way has no
owner, so only admins can set it up or break it.

Fields on the lower-left block:

| Field | Type | Meaning |
|---|---|---|
| `owner` | UUID (int array) | The owner. Leave it out for an admin-only shop. |
| `owner_name` | string | Shown as "Owned by …". |
| `infinite` | byte, 0 or 1 | Never sells out; the money disappears. |
| `catalog` | string | A catalog id. |
| `currency` | string | An item id: the currency slot. |
| `selections` | list of `{slot: 0–11, item: {id, count, components}, price: 0–999}` | The machine's own buttons (`slot` 0 is button 1). |
| `stock`, `cash_box`, `tray` | `{Items: [{Slot: 0b, id, count}]}` | Contents. |
````

- [ ] **Step 9: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src docs/catalogs.md
git -C /c/Users/benet/mcvending commit -m "feat: datapack catalogs"
```

---

### Task 3: The setup rides on the machine item

Deliverable: breaking a machine drops an item that remembers its buttons, catalog, currency slot and infinite flag (never its contents); placing it restores all that for the new owner, infinite only for an admin; a plain machine still drops a plain, stackable item.

**Files:**
- Create: `src/main/java/diamondvending/block/MachineSetup.java`
- Modify: `registry/Registrar.java`, `registry/ModContent.java`, `platform/neoforge/NeoForgeRegistrar.java`, `platform/fabric/FabricRegistrar.java`
- Modify: `block/MachineItems.java`, `block/VendingMachineBlock.java`
- Test: `ShopTests.java`, `FabricShopTests.java`, `NeoForgeShopTests.java`

**Interfaces:**
- Consumes: Task 2's `ownSelection`, `catalogId`, `setCatalog`; Task 1's `currencySlot`, `setCurrencySlot`; `MachineAccess.isAdmin`; `MachineTests.breakAsPlayer`, `droppedMachines`, `placeOn`, `machineAt`.
- Produces: `block.MachineSetup` — `CODEC`, `of(VendingMachineBlockEntity)`, `isEmpty()`, `selection(int)`, `catalog()`, `currency()`, `infinite()`, `applyTo(VendingMachineBlockEntity, boolean placedByAdmin)`; `ModContent.MACHINE_SETUP` (`Supplier<DataComponentType<MachineSetup>>`, id `diamondvending:machine_setup`); `Registrar.dataComponent(String, Supplier<DataComponentType<T>>)`; `MachineItems.forMachine(DyeColor, VendingMachineBlockEntity)`; `ShopTests.breakAndPickUp(GameTestHelper, Player)`.

- [ ] **Step 1: Write the failing GameTests**

In `ShopTests.java` add imports:

```java
import diamondvending.block.MachineItems;
import diamondvending.block.MachineSetup;
import diamondvending.registry.ModContent;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

import java.util.List;
```

Add to `ALL`:

```java
            Map.entry("breaking_keeps_the_setup_on_the_item", ShopTests::breakingKeepsTheSetupOnTheItem),
            Map.entry("placing_restores_the_setup_for_the_new_owner", ShopTests::placingRestoresTheSetupForTheNewOwner),
            Map.entry("infinite_stays_only_for_admin_placers", ShopTests::infiniteStaysOnlyForAdminPlacers),
            Map.entry("an_unset_machine_drops_a_plain_item", ShopTests::anUnsetMachineDropsAPlainItem),
            Map.entry("unknown_items_in_a_kept_setup_leave_that_button_empty", ShopTests::unknownItemsInAKeptSetupLeaveThatButtonEmpty)
```

and the tests:

```java
    // ---- the setup kept on the item (spec §5.4) -----------------------------------------------------------------

    /** Breaks the test machine as {@code player} and picks up the machine item it dropped. */
    static ItemStack breakAndPickUp(GameTestHelper helper, Player player) {
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, player);
        List<ItemEntity> drops = MachineTests.droppedMachines(helper);
        helper.assertTrue(drops.size() == 1, "breaking should drop one machine item, dropped " + drops.size());
        ItemStack stack = drops.get(0).getItem().copy();
        drops.forEach(Entity::discard);
        return stack;
    }

    public static void breakingKeepsTheSetupOnTheItem(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.setSelection(11, Selection.of(new ItemStack(Items.ARROW, 16), 0));
        machine.setCurrencySlot(Items.EMERALD);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        MachineSetup setup = breakAndPickUp(helper, owner).get(ModContent.MACHINE_SETUP.get());
        helper.assertTrue(setup != null, "the dropped machine should carry its setup");
        Selection first = setup.selection(0);
        helper.assertTrue(first.template().is(Items.APPLE) && first.quantity() == 2 && first.price() == 3, "button 1 is kept, got " + first);
        helper.assertTrue(setup.selection(11).template().is(Items.ARROW) && setup.selection(11).quantity() == 16, "button 12 is kept");
        helper.assertTrue(setup.currency() == Items.EMERALD, "the currency slot is kept");
        helper.assertTrue(BuyingTests.droppedNear(helper, MachineTests.MASTER, Items.APPLE) == 10, "the stock spills instead of riding on the item");
        helper.succeed();
    }

    public static void placingRestoresTheSetupForTheNewOwner(GameTestHelper helper) {
        RecordingPlayer first = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, first);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.setCatalog(EMERALDS);
        machine.setCurrencySlot(Items.GOLD_INGOT);
        ItemStack item = breakAndPickUp(helper, first);
        RecordingPlayer second = new RecordingPlayer(helper, GameType.SURVIVAL);
        MachineTests.placeOn(helper, second, item, MachineTests.FLOOR);
        VendingMachineBlockEntity placed = MachineTests.machineAt(helper, MachineTests.MASTER);
        helper.assertTrue(placed != null && second.getUUID().equals(placed.getOwner()), "whoever places it owns it");
        helper.assertTrue(placed.ownSelection(0).template().is(Items.APPLE) && placed.ownSelection(0).price() == 3, "button 1 comes back");
        helper.assertTrue(EMERALDS.equals(placed.catalogId()), "the catalog comes back");
        helper.assertTrue(placed.currencySlot() == Items.GOLD_INGOT, "the currency slot comes back");
        helper.succeed();
    }

    /** Spec §5.4: an infinite machine comes back infinite only for an admin. */
    public static void infiniteStaysOnlyForAdminPlacers(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setInfinite(true);
        ItemStack item = MachineItems.forMachine(DyeColor.RED, machine);
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, new RecordingPlayer(helper, GameType.CREATIVE));
        MachineTests.placeOn(helper, new RecordingPlayer(helper, GameType.SURVIVAL), item.copy(), MachineTests.FLOOR);
        helper.assertFalse(MachineTests.machineAt(helper, MachineTests.MASTER).isInfinite(), "a player who isn't an admin gets an owned machine");
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, new RecordingPlayer(helper, GameType.CREATIVE));
        MachineTests.placeOn(helper, new RecordingPlayer(helper, GameType.CREATIVE), item.copy(), MachineTests.FLOOR);
        helper.assertTrue(MachineTests.machineAt(helper, MachineTests.MASTER).isInfinite(), "an admin gets it back infinite");
        helper.succeed();
    }

    /** A machine nobody set up drops a plain item, so it stacks with new ones. */
    public static void anUnsetMachineDropsAPlainItem(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        ItemStack item = MachineItems.forMachine(DyeColor.RED, machine);
        helper.assertTrue(ItemStack.isSameItemSameComponents(item, MachineItems.forColor(DyeColor.RED)), "expected a plain machine item, got " + item);
        helper.succeed();
    }

    /** Spec §9: a kept setup naming an item from a removed mod still loads; only that button comes back empty. */
    public static void unknownItemsInAKeptSetupLeaveThatButtonEmpty(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE), 1));
        machine.setSelection(1, Selection.of(new ItemStack(Items.BREAD), 2));
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, BuyingTests.registries(helper));
        MachineSetup setup = MachineSetup.of(machine);
        helper.assertTrue(setup.equals(MachineSetup.CODEC.parse(ops, MachineSetup.CODEC.encodeStart(ops, setup).getOrThrow()).getOrThrow()),
                "a setup survives a save unchanged");
        CompoundTag saved = (CompoundTag) MachineSetup.CODEC.encodeStart(ops, setup).getOrThrow();
        // Pretend button 1's item came from a mod that has since been removed.
        //? if >=26.1 {
        saved.getListOrEmpty("selections").getCompoundOrEmpty(0).getCompoundOrEmpty("item").putString("id", "notamod:gadget");
        //?} else {
        /*saved.getList("selections", Tag.TAG_COMPOUND).getCompound(0).getCompound("item").putString("id", "notamod:gadget");
        *///?}
        MachineSetup loaded = MachineSetup.CODEC.parse(ops, saved).getOrThrow();
        helper.assertFalse(loaded.selection(0).isSetUp(), "the unknown item leaves button 1 empty");
        helper.assertTrue(loaded.selection(1).template().is(Items.BREAD) && loaded.selection(1).price() == 2, "button 2 is untouched");
        helper.succeed();
    }
```

Add the five adapter methods to `FabricShopTests.java` and to the 1.21.1 block of `NeoForgeShopTests.java` (templates under "Adding a shop test"): `breakingKeepsTheSetupOnTheItem`, `placingRestoresTheSetupForTheNewOwner`, `infiniteStaysOnlyForAdminPlacers`, `anUnsetMachineDropsAPlainItem`, `unknownItemsInAKeptSetupLeaveThatButtonEmpty`.

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors: `MachineSetup`, `ModContent.MACHINE_SETUP` and `MachineItems.forMachine` not found.

- [ ] **Step 3: Write the component**

`src/main/java/diamondvending/block/MachineSetup.java`:

```java
package diamondvending.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import diamondvending.core.MachineLayout;
import diamondvending.shop.Selection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A machine's setup, carried by its item after it's broken (spec §5.4): what the 12 buttons sell, the catalog, the
 * currency slot and the infinite flag — never its contents. The {@code diamondvending:machine_setup} item component.
 */
public final class MachineSetup {
    /** One set-up button. An item from a mod that's gone leaves just that button empty (spec §9). */
    private record Entry(int slot, Optional<ItemStack> item, int price) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(entry -> entry.group(
                Codec.intRange(0, MachineLayout.SELECTIONS - 1).fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.lenientOptionalFieldOf("item").forGetter(Entry::item),
                Codec.intRange(0, Selection.MAX_PRICE).fieldOf("price").forGetter(Entry::price)
        ).apply(entry, Entry::new));
    }

    public static final Codec<MachineSetup> CODEC = RecordCodecBuilder.create(setup -> setup.group(
            Entry.CODEC.listOf().optionalFieldOf("selections", List.of()).forGetter(MachineSetup::entries),
            Identifier.CODEC.optionalFieldOf("catalog").forGetter(s -> Optional.ofNullable(s.catalog)),
            BuiltInRegistries.ITEM.byNameCodec().lenientOptionalFieldOf("currency").forGetter(s -> Optional.ofNullable(s.currency)),
            Codec.BOOL.optionalFieldOf("infinite", false).forGetter(s -> s.infinite)
    ).apply(setup, MachineSetup::new));

    private final Selection[] selections = new Selection[MachineLayout.SELECTIONS];
    private final Identifier catalog;
    private final Item currency;
    private final boolean infinite;

    private MachineSetup(List<Entry> entries, Optional<Identifier> catalog, Optional<Item> currency, boolean infinite) {
        Arrays.fill(selections, Selection.EMPTY);
        for (Entry entry : entries) {
            entry.item().ifPresent(item -> selections[entry.slot()] = Selection.of(item, entry.price()));
        }
        this.catalog = catalog.orElse(null);
        this.currency = currency.orElse(null);
        this.infinite = infinite;
    }

    /** What a machine is set up to do: its own selections (not a catalog's), catalog, currency slot and infinite flag. */
    public static MachineSetup of(VendingMachineBlockEntity machine) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Selection selection = machine.ownSelection(i);
            if (selection.isSetUp()) entries.add(new Entry(i, Optional.of(selection.template().copy()), selection.price()));
        }
        return new MachineSetup(entries, Optional.ofNullable(machine.catalogId()), Optional.ofNullable(machine.currencySlot()),
                machine.isInfinite());
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < selections.length; i++) {
            if (selections[i].isSetUp()) entries.add(new Entry(i, Optional.of(selections[i].template()), selections[i].price()));
        }
        return entries;
    }

    /** True for a machine nobody set up: its item then stays plain and stacks with new machines. */
    public boolean isEmpty() {
        return Arrays.stream(selections).noneMatch(Selection::isSetUp) && catalog == null && currency == null && !infinite;
    }

    public Selection selection(int index) {
        return selections[index];
    }

    /** The catalog id, or null. */
    public Identifier catalog() {
        return catalog;
    }

    /** The currency slot's item, or null. */
    public Item currency() {
        return currency;
    }

    public boolean infinite() {
        return infinite;
    }

    /** Puts this setup on a newly placed machine (spec §5.4); it stays infinite only when an admin placed it. */
    public void applyTo(VendingMachineBlockEntity machine, boolean placedByAdmin) {
        for (int i = 0; i < selections.length; i++) machine.setSelection(i, selections[i]);
        machine.setCatalog(catalog);
        machine.setCurrencySlot(currency);
        machine.setInfinite(infinite && placedByAdmin);
    }

    // Components are compared when items stack and when menus sync, so equal setups must be equal.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MachineSetup that)) return false;
        for (int i = 0; i < selections.length; i++) {
            Selection mine = selections[i];
            Selection theirs = that.selections[i];
            if (mine.price() != theirs.price() || !ItemStack.matches(mine.template(), theirs.template())) return false;
        }
        return Objects.equals(catalog, that.catalog) && currency == that.currency && infinite == that.infinite;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(catalog, currency, infinite);
        for (Selection selection : selections) {
            hash = 31 * hash + 31 * ItemStack.hashItemAndComponents(selection.template()) + 7 * selection.quantity() + selection.price();
        }
        return hash;
    }
}
```

In `registry/Registrar.java` add the import `net.minecraft.core.component.DataComponentType` and:

```java
    /** A data component type: a piece of data an item can carry, such as a machine's setup. */
    <T> Supplier<DataComponentType<T>> dataComponent(String name, Supplier<DataComponentType<T>> type);
```

In `platform/neoforge/NeoForgeRegistrar.java` add the import `net.minecraft.core.component.DataComponentType`, the field

```java
    private final DeferredRegister<DataComponentType<?>> components = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DiamondVending.MOD_ID);
```

`components.register(modBus);` in `registerAll`, and:

```java
    @Override
    public <T> Supplier<DataComponentType<T>> dataComponent(String name, Supplier<DataComponentType<T>> type) {
        return components.register(name, type);
    }
```

In `platform/fabric/FabricRegistrar.java` add the import `net.minecraft.core.component.DataComponentType` and:

```java
    @Override
    public <T> Supplier<DataComponentType<T>> dataComponent(String name, Supplier<DataComponentType<T>> type) {
        DataComponentType<T> registered = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, DiamondVending.id(name), type.get());
        return () -> registered;
    }
```

In `registry/ModContent.java` add imports `diamondvending.block.MachineSetup`, `net.minecraft.core.component.DataComponentType`, `net.minecraft.network.codec.ByteBufCodecs`, the field

```java
    public static Supplier<DataComponentType<MachineSetup>> MACHINE_SETUP;
```

and at the start of `register`:

```java
        MACHINE_SETUP = registrar.dataComponent("machine_setup", () -> DataComponentType.<MachineSetup>builder()
                .persistent(MachineSetup.CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(MachineSetup.CODEC))
                .build());
```

- [ ] **Step 4: Drop and restore it**

In `block/MachineItems.java` add:

```java
    /** The item a broken machine drops (spec §5.4): its color, and its setup when it has one. */
    public static ItemStack forMachine(DyeColor color, VendingMachineBlockEntity machine) {
        ItemStack stack = forColor(color);
        if (machine != null) {
            MachineSetup setup = MachineSetup.of(machine);
            if (!setup.isEmpty()) stack.set(ModContent.MACHINE_SETUP.get(), setup);
        }
        return stack;
    }
```

In `block/VendingMachineBlock.java` replace the body of `playerWillDestroy`'s `if` with

```java
        if (!level.isClientSide() && !player.isCreative()) {
            BlockPos master = MachinePart.masterOf(pos, state);
            VendingMachineBlockEntity machine = level.getBlockEntity(master) instanceof VendingMachineBlockEntity found ? found : null;
            popResource(level, master, MachineItems.forMachine(state.getValue(COLOR), machine));
        }
```

(and "Survival breaks drop one machine item in the machine's color" in its comment becomes "… in the machine's color, carrying its setup"), and replace the last `if` of `setPlacedBy` with:

```java
        if (!(level.getBlockEntity(pos) instanceof VendingMachineBlockEntity machine)) return;
        Player player = placer instanceof Player p ? p : null;
        if (player != null) machine.setOwner(player.getUUID(), player.getName().getString());
        // Spec §5.4: an item that kept its setup puts it back; only an admin gets an infinite machine back.
        MachineSetup setup = stack.get(ModContent.MACHINE_SETUP.get());
        if (setup != null) setup.applyTo(machine, player != null && MachineAccess.isAdmin(player));
```

- [ ] **Step 5: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 91 required tests passed :)`.

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 91 / 90 / 90 pass.

- [ ] **Step 6: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: a broken machine keeps its setup on the item"
```

---

### Task 4: The setup menu (server side)

Deliverable: an owner or admin who sneak-right-clicks their machine with both hands empty gets the setup menu (anyone else is told "Only the owner can do that."). The menu has four tabs; its ghost slots, Stock, Cash Box and currency slot work through vanilla container clicks, and its buttons (amount, price, clear, withdraw, infinite, catalog) through vanilla menu-button clicks, every one re-checked on the server.

**Files:**
- Create: `src/main/java/diamondvending/core/SetupTab.java`, `core/SetupButtons.java`; tests `src/test/java/diamondvending/core/SetupTabTest.java`, `SetupButtonsTest.java`
- Modify: `shop/Selection.java` (MAX_PRICE from `SetupButtons`)
- Create: `src/main/java/diamondvending/menu/VendingSetupMenu.java`, `menu/MachineSlots.java`, `registry/MenuHandle.java`
- Modify: `registry/Registrar.java`, `registry/ModContent.java`, `platform/neoforge/NeoForgeRegistrar.java`, `platform/fabric/FabricRegistrar.java`
- Modify: `block/VendingMachineBlock.java`, `core/Texts.java`, `lang/en_us.json`
- Create: `src/gametest/java/diamondvending/gametest/RecordingServerPlayer.java`
- Test: `ShopTests.java`, `FabricShopTests.java`, `NeoForgeShopTests.java`

**Interfaces:**
- Consumes: Tasks 1–3 (`currencySlot`/`setCurrencySlot`, `catalogId`/`setCatalog`/`usesCatalog`, `getSelection`/`ownSelection`, `Catalogs.cycle`, `Catalogs.ids`); `MachineAccess`, `ItemSlots`, `PlayerItems.give`, `Messages.actionBar`.
- Produces:
  - `core.SetupTab` — `SELECTIONS, STOCK, CASH_BOX, ADMIN`; `boolean shownTo(boolean admin, boolean infinite)`; `static SetupTab fixing(Problem)` (null for none); `static Set<SetupTab> needingAttention(List<Problem>)`.
  - `core.SetupButtons` — `MAX_PRICE = 999`; encoders `showTab(SetupTab)`, `withdraw()`, `toggleInfinite()`, `cycleCatalog(int step)`, `clear(int)`, `fewer(int)`, `more(int)`, `price(int index, int price)`; `static Press decode(int)` (null when unknown); `sealed interface Press` with records `ShowTab(SetupTab)`, `Withdraw()`, `ToggleInfinite()`, `CycleCatalog(int step)`, `Clear(int index)`, `ChangeQuantity(int index, int by)`, `SetPrice(int index, int price)`.
  - `menu.VendingSetupMenu(int id, Inventory, BlockPos)` — constants `WIDTH` 208, `HEIGHT` 224, `GHOSTS_X/Y` 12/56, `GRID_X/Y` 24/72, `CURRENCY_X/Y` 112/102, `INVENTORY_X/Y` 24/142, `HOTBAR_Y` 200; slot indexes `FIRST_GHOST` 0, `FIRST_STOCK` 12, `FIRST_CASH` 39, `CURRENCY` 66, `FIRST_PLAYER` 67; `machine()`, `tab()`, `selected()`, `viewerIsAdmin()`, `stockAndCashEmpty()`, `cashBoxEmpty()`.
  - `registry.MenuHandle<M>` — `MenuType<M> type()`, `void open(ServerPlayer, Component title, BlockPos)`; `Registrar.BlockMenuFactory<M>` and `Registrar.menu(String, BlockMenuFactory<M>)`; `ModContent.SETUP_MENU` (`MenuHandle<VendingSetupMenu>`, id `diamondvending:setup`).
  - `Texts.SETUP_TITLE`; `gametest.RecordingServerPlayer.create(GameTestHelper, GameType)`, `lastMessage()`; `ShopTests.setupMenu`, `clickSlot`, `shiftClickSlot`, `standInFront`, `appleMachineOwnedBy`.

- [ ] **Step 1: Write the failing unit tests**

`src/test/java/diamondvending/core/SetupTabTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec §4: who sees which setup tab, and which tab gets the red "!" for each problem. */
class SetupTabTest {
    @Test
    void ownersSeeTheirShopTabsAndOnlyAdminsSeeTheAdminTab() {
        assertTrue(SetupTab.SELECTIONS.shownTo(false, false));
        assertTrue(SetupTab.STOCK.shownTo(false, false));
        assertTrue(SetupTab.CASH_BOX.shownTo(false, false));
        assertFalse(SetupTab.ADMIN.shownTo(false, false));
        assertTrue(SetupTab.ADMIN.shownTo(true, false));
    }

    @Test
    void infiniteMachinesHaveNoStockOrCashBox() {
        assertTrue(SetupTab.SELECTIONS.shownTo(true, true));
        assertFalse(SetupTab.STOCK.shownTo(true, true));
        assertFalse(SetupTab.CASH_BOX.shownTo(true, true));
        assertTrue(SetupTab.ADMIN.shownTo(true, true));
    }

    @Test
    void eachProblemMarksTheTabThatFixesIt() {
        assertEquals(EnumSet.of(SetupTab.CASH_BOX, SetupTab.STOCK),
                SetupTab.needingAttention(List.of(Problem.CASH_BOX_FULL, Problem.SOLD_OUT, Problem.TRAY_FULL)));
        assertEquals(EnumSet.of(SetupTab.SELECTIONS), SetupTab.needingAttention(List.of(Problem.NOT_SET_UP)));
        assertEquals(EnumSet.of(SetupTab.ADMIN), SetupTab.needingAttention(List.of(Problem.CATALOG_MISSING)));
        assertTrue(SetupTab.needingAttention(List.of(Problem.TRAY_FULL)).isEmpty(), "a full tray is emptied at the machine, not in setup");
    }
}
```

`src/test/java/diamondvending/core/SetupButtonsTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The setup screen's button numbers: every button decodes to what it asks for, and nothing else decodes at all. */
class SetupButtonsTest {
    @Test
    void everyButtonDecodesToWhatItAsksFor() {
        for (SetupTab tab : SetupTab.values()) assertEquals(new SetupButtons.ShowTab(tab), SetupButtons.decode(SetupButtons.showTab(tab)));
        assertEquals(new SetupButtons.Withdraw(), SetupButtons.decode(SetupButtons.withdraw()));
        assertEquals(new SetupButtons.ToggleInfinite(), SetupButtons.decode(SetupButtons.toggleInfinite()));
        assertEquals(new SetupButtons.CycleCatalog(-1), SetupButtons.decode(SetupButtons.cycleCatalog(-1)));
        assertEquals(new SetupButtons.CycleCatalog(1), SetupButtons.decode(SetupButtons.cycleCatalog(1)));
        for (int index = 0; index < MachineLayout.SELECTIONS; index++) {
            assertEquals(new SetupButtons.Clear(index), SetupButtons.decode(SetupButtons.clear(index)));
            assertEquals(new SetupButtons.ChangeQuantity(index, -1), SetupButtons.decode(SetupButtons.fewer(index)));
            assertEquals(new SetupButtons.ChangeQuantity(index, 1), SetupButtons.decode(SetupButtons.more(index)));
            for (int price : new int[] {0, 1, 3, SetupButtons.MAX_PRICE}) {
                assertEquals(new SetupButtons.SetPrice(index, price), SetupButtons.decode(SetupButtons.price(index, price)));
            }
        }
    }

    @Test
    void numbersNoButtonSendsMeanNothing() {
        for (int id : new int[] {-1, 4, 9, 14, 99, 112, 212, 312, 9_999, 22_000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertNull(SetupButtons.decode(id), "button number " + id);
        }
    }

    @Test
    void askingForSomethingOutOfRangeIsABug() {
        assertThrows(IllegalArgumentException.class, () -> SetupButtons.price(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> SetupButtons.price(12, 5));
        assertThrows(IllegalArgumentException.class, () -> SetupButtons.clear(-1));
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew :26.1-neoforge:test`
Expected: FAIL — compilation errors: `SetupTab` and `SetupButtons` not found.

- [ ] **Step 3: Write the tabs and the button numbers**

`src/main/java/diamondvending/core/SetupTab.java`:

```java
package diamondvending.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The setup screen's tabs (spec §4). */
public enum SetupTab {
    SELECTIONS, STOCK, CASH_BOX, ADMIN;

    /** Whether this tab is there: Stock and Cash Box only on owned machines, Admin only for admins. */
    public boolean shownTo(boolean admin, boolean infinite) {
        return switch (this) {
            case SELECTIONS -> true;
            case STOCK, CASH_BOX -> !infinite;
            case ADMIN -> admin;
        };
    }

    /** The tab where a problem is fixed (its red "!"), or null: a full tray is emptied at the machine. */
    public static SetupTab fixing(Problem problem) {
        return switch (problem) {
            case CATALOG_MISSING -> ADMIN;
            case NOT_SET_UP -> SELECTIONS;
            case CASH_BOX_FULL -> CASH_BOX;
            case SOLD_OUT -> STOCK;
            case TRAY_FULL -> null;
        };
    }

    /** The tabs that get a red "!" for these problems. */
    public static Set<SetupTab> needingAttention(List<Problem> problems) {
        Set<SetupTab> tabs = EnumSet.noneOf(SetupTab.class);
        for (Problem problem : problems) {
            SetupTab tab = fixing(problem);
            if (tab != null) tabs.add(tab);
        }
        return tabs;
    }
}
```

`src/main/java/diamondvending/core/SetupButtons.java`:

```java
package diamondvending.core;

/**
 * The setup screen's buttons, as the numbers the client sends with vanilla's "menu button clicked" packet (spec §4:
 * every change is a small packet the server re-checks). The server decodes each number and decides; a number no
 * button sends decodes to null and is ignored.
 */
public final class SetupButtons {
    /** Prices run 0–999 (spec §3.1): three digits of a button number. */
    public static final int MAX_PRICE = 999;

    private static final int TAB = 0;              // + tab
    private static final int WITHDRAW = 10;
    private static final int INFINITE = 11;
    private static final int PREVIOUS_CATALOG = 12;
    private static final int NEXT_CATALOG = 13;
    private static final int CLEAR = 100;          // + button index
    private static final int FEWER = 200;          // + button index
    private static final int MORE = 300;           // + button index
    private static final int PRICE = 10_000;       // + button index × 1000 + price

    /** What a button asks for. */
    public sealed interface Press permits ShowTab, Withdraw, ToggleInfinite, CycleCatalog, Clear, ChangeQuantity, SetPrice {}

    public record ShowTab(SetupTab tab) implements Press {}

    /** Everything in the cash box to the player. */
    public record Withdraw() implements Press {}

    public record ToggleInfinite() implements Press {}

    /** The previous (−1) or next (+1) choice in the catalog picker. */
    public record CycleCatalog(int step) implements Press {}

    public record Clear(int index) implements Press {}

    /** One more or one fewer item per purchase. */
    public record ChangeQuantity(int index, int by) implements Press {}

    public record SetPrice(int index, int price) implements Press {}

    private SetupButtons() {}

    public static int showTab(SetupTab tab) {
        return TAB + tab.ordinal();
    }

    public static int withdraw() {
        return WITHDRAW;
    }

    public static int toggleInfinite() {
        return INFINITE;
    }

    public static int cycleCatalog(int step) {
        return step < 0 ? PREVIOUS_CATALOG : NEXT_CATALOG;
    }

    public static int clear(int index) {
        return CLEAR + checkIndex(index);
    }

    public static int fewer(int index) {
        return FEWER + checkIndex(index);
    }

    public static int more(int index) {
        return MORE + checkIndex(index);
    }

    public static int price(int index, int price) {
        if (price < 0 || price > MAX_PRICE) throw new IllegalArgumentException("price " + price + " is outside 0–" + MAX_PRICE);
        return PRICE + checkIndex(index) * 1000 + price;
    }

    /** What a button number asks for, or null for a number no button sends. */
    public static Press decode(int id) {
        int buttons = MachineLayout.SELECTIONS;
        if (id >= TAB && id < TAB + SetupTab.values().length) return new ShowTab(SetupTab.values()[id - TAB]);
        if (id == WITHDRAW) return new Withdraw();
        if (id == INFINITE) return new ToggleInfinite();
        if (id == PREVIOUS_CATALOG) return new CycleCatalog(-1);
        if (id == NEXT_CATALOG) return new CycleCatalog(1);
        if (id >= CLEAR && id < CLEAR + buttons) return new Clear(id - CLEAR);
        if (id >= FEWER && id < FEWER + buttons) return new ChangeQuantity(id - FEWER, -1);
        if (id >= MORE && id < MORE + buttons) return new ChangeQuantity(id - MORE, 1);
        if (id >= PRICE && id < PRICE + buttons * 1000) return new SetPrice((id - PRICE) / 1000, (id - PRICE) % 1000);
        return null;
    }

    private static int checkIndex(int index) {
        if (index < 0 || index >= MachineLayout.SELECTIONS) throw new IllegalArgumentException("there is no button " + (index + 1));
        return index;
    }
}
```

In `shop/Selection.java` change `public static final int MAX_PRICE = 999;` to `public static final int MAX_PRICE = SetupButtons.MAX_PRICE;` (import `diamondvending.core.SetupButtons`).

- [ ] **Step 4: Run to verify the unit tests pass**

Run: `./gradlew :26.1-neoforge:test`
Expected: PASS — `SetupTabTest` 3/3, `SetupButtonsTest` 3/3, all older unit tests green.

- [ ] **Step 5: Write the failing GameTests**

`src/gametest/java/diamondvending/gametest/RecordingServerPlayer.java`:

```java
package diamondvending.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A real server player — menus open for it, and its clicks can go through the game's own click handling, loader events
 * included — that also remembers its action-bar messages. Made like {@link GameTestHelper#makeMockServerPlayerInLevel},
 * but in any game mode, standing two blocks in front of the test machine and facing south like the mock players (so a
 * machine it places faces north).
 */
public final class RecordingServerPlayer extends ServerPlayer {
    private final List<Component> messages = new ArrayList<>();

    private RecordingServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation information) {
        super(server, level, profile, information);
    }

    public static RecordingServerPlayer create(GameTestHelper helper, GameType gameType) {
        ServerLevel level = helper.getLevel();
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
        RecordingServerPlayer player = new RecordingServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(gameType);
        BlockPos spot = helper.absolutePos(MachineTests.MASTER.north(2));
        player.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        player.setYRot(0.0F);
        return player;
    }

    /** The latest action-bar message, or null if there was none. */
    public Component lastMessage() {
        return messages.isEmpty() ? null : messages.getLast();
    }

    //? if >=26.1 {
    @Override
    public void sendOverlayMessage(Component message) {
        messages.add(message);
        super.sendOverlayMessage(message);
    }
    //?} else {
    /*@Override
    public void displayClientMessage(Component message, boolean actionBar) {
        if (actionBar) messages.add(message);
        super.displayClientMessage(message, actionBar);
    }
    *///?}
}
```

In `ShopTests.java` add imports:

```java
import diamondvending.core.SetupButtons;
import diamondvending.core.SetupTab;
import diamondvending.menu.VendingSetupMenu;
import diamondvending.shop.ItemSlots;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;
*///?}
```

Add to `ALL`:

```java
            Map.entry("ghost_slots_copy_without_taking", ShopTests::ghostSlotsCopyWithoutTaking),
            Map.entry("the_editor_buttons_change_amount_and_price", ShopTests::theEditorButtonsChangeAmountAndPrice),
            Map.entry("strangers_cannot_change_anything", ShopTests::strangersCannotChangeAnything),
            Map.entry("the_stock_tab_stocks_the_machine", ShopTests::theStockTabStocksTheMachine),
            Map.entry("the_cash_box_is_take_only", ShopTests::theCashBoxIsTakeOnly),
            Map.entry("withdraw_empties_the_cash_box_into_the_owners_inventory", ShopTests::withdrawEmptiesTheCashBoxIntoTheOwnersInventory),
            Map.entry("only_admins_see_and_use_the_admin_tab", ShopTests::onlyAdminsSeeAndUseTheAdminTab),
            Map.entry("infinite_needs_an_empty_stock_and_cash_box", ShopTests::infiniteNeedsAnEmptyStockAndCashBox),
            Map.entry("the_catalog_picker_cycles_through_every_catalog", ShopTests::theCatalogPickerCyclesThroughEveryCatalog),
            Map.entry("catalog_selections_are_read_only", ShopTests::catalogSelectionsAreReadOnly),
            Map.entry("the_currency_slot_is_for_admins", ShopTests::theCurrencySlotIsForAdmins),
            Map.entry("sneaking_with_empty_hands_opens_setup", ShopTests::sneakingWithEmptyHandsOpensSetup),
            Map.entry("two_open_screens_share_one_machine", ShopTests::twoOpenScreensShareOneMachine),
            Map.entry("breaking_the_machine_closes_its_setup", ShopTests::breakingTheMachineClosesItsSetup),
            Map.entry("shift_click_only_stocks_on_the_stock_tab", ShopTests::shiftClickOnlyStocksOnTheStockTab)
```

and the helpers and tests:

```java
    // ---- the setup menu (spec §4) -------------------------------------------------------------------------------

    /** The setup menu the server would make for this player at this machine. */
    static VendingSetupMenu setupMenu(Player player, VendingMachineBlockEntity machine) {
        return new VendingSetupMenu(0, player.getInventory(), machine.getBlockPos());
    }

    /** A left (0) or right (1) click on a menu slot, holding whatever is on the menu's cursor. */
    static void clickSlot(VendingSetupMenu menu, int slot, int button, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, button, ContainerInput.PICKUP, player);
        //?} else {
        /*menu.clicked(slot, button, ClickType.PICKUP, player);
        *///?}
    }

    static void shiftClickSlot(VendingSetupMenu menu, int slot, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, player);
        //?} else {
        /*menu.clicked(slot, 0, ClickType.QUICK_MOVE, player);
        *///?}
    }

    /** Moves a mock player two blocks in front of the test machine: close enough for its setup screen to stay open. */
    static void standInFront(GameTestHelper helper, Player player) {
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(MachineTests.MASTER.north(2))));
    }

    /** A machine owned by {@code owner} selling 2 apples for 3 on button 1, with nothing in stock. */
    static VendingMachineBlockEntity appleMachineOwnedBy(GameTestHelper helper, Player owner) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        return machine;
    }

    public static void ghostSlotsCopyWithoutTaking(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.BREAD, 3));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST + 4, 0, owner);
        BuyingTests.assertSelection(helper, machine, 4, Items.BREAD, 3, 0);
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST + 5, 1, owner);
        BuyingTests.assertSelection(helper, machine, 5, Items.BREAD, 1, 0);
        helper.assertTrue(menu.getCarried().getCount() == 3, "nothing is used up");
        helper.assertTrue(menu.selected() == 5, "the last clicked button is the one being edited");
        helper.succeed();
    }

    public static void theEditorButtonsChangeAmountAndPrice(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertTrue(menu.clickMenuButton(owner, SetupButtons.more(0)), "the owner may change the amount");
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 3, 3);
        for (int i = 0; i < 3; i++) menu.clickMenuButton(owner, SetupButtons.fewer(0));
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 1, 3);
        for (int i = 0; i < 70; i++) menu.clickMenuButton(owner, SetupButtons.more(0));
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 64, 3);
        menu.clickMenuButton(owner, SetupButtons.price(0, 999));
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 64, 999);
        menu.clickMenuButton(owner, SetupButtons.price(3, 5));
        helper.assertFalse(machine.getSelection(3).isSetUp(), "an empty button gets no price");
        menu.clickMenuButton(owner, SetupButtons.clear(0));
        helper.assertFalse(machine.getSelection(0).isSetUp(), "Clear empties the button");
        helper.succeed();
    }

    /** Spec §4: the server re-checks everything, even from a screen a stranger somehow has open. */
    public static void strangersCannotChangeAnything(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        standInFront(helper, stranger);
        VendingSetupMenu menu = setupMenu(stranger, machine);
        helper.assertFalse(menu.stillValid(stranger), "a stranger's setup screen closes at once");
        helper.assertFalse(menu.clickMenuButton(stranger, SetupButtons.price(0, 0)), "and every change is refused");
        menu.setCarried(new ItemStack(Items.DIRT));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST, 0, stranger);
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        helper.succeed();
    }

    public static void theStockTabStocksTheMachine(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        BuyingTests.assertProblems(helper, machine, Problem.SOLD_OUT);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.APPLE, 10));
        clickSlot(menu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(ItemSlots.isEmpty(machine.stock()), "stock slots do nothing while the Items tab is showing");
        helper.assertTrue(menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.STOCK)), "owners have a Stock tab");
        clickSlot(menu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(BuyingTests.countIn(machine.stock(), Items.APPLE) == 10 && menu.getCarried().isEmpty(),
                "the apples go from the cursor into the machine's stock");
        BuyingTests.assertProblems(helper, machine);
        helper.succeed();
    }

    public static void theCashBoxIsTakeOnly(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.CASH_BOX));
        menu.setCarried(new ItemStack(Items.DIRT));
        clickSlot(menu, VendingSetupMenu.FIRST_CASH, 0, owner);
        helper.assertTrue(ItemSlots.isEmpty(machine.cashBox()) && menu.getCarried().is(Items.DIRT), "nothing can be put in the cash box");
        machine.cashBox().set(0, new ItemStack(Items.DIAMOND, 5));
        menu.setCarried(ItemStack.EMPTY);
        clickSlot(menu, VendingSetupMenu.FIRST_CASH, 0, owner);
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 5 && ItemSlots.isEmpty(machine.cashBox()),
                "but the owner can take the money out");
        helper.succeed();
    }

    public static void withdrawEmptiesTheCashBoxIntoTheOwnersInventory(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.cashBox().set(3, new ItemStack(Items.DIAMOND, 5));
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertTrue(menu.clickMenuButton(owner, SetupButtons.withdraw()), "the owner may withdraw");
        helper.assertTrue(BuyingTests.countHeld(owner, Items.DIAMOND) == 5 && ItemSlots.isEmpty(machine.cashBox()), "the diamonds move to the owner");
        helper.succeed();
    }

    public static void onlyAdminsSeeAndUseTheAdminTab(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertFalse(menu.viewerIsAdmin(), "a survival owner isn't an admin");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.ADMIN)), "so there's no Admin tab");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.toggleInfinite()), "no infinite switch");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.cycleCatalog(1)), "and no catalog picker");
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        helper.assertTrue(setupMenu(admin, machine).clickMenuButton(admin, SetupButtons.showTab(SetupTab.ADMIN)), "creative players are admins");
        helper.succeed();
    }

    /** Spec §4: going infinite needs an empty Stock and Cash Box, so nobody's items silently vanish. */
    public static void infiniteNeedsAnEmptyStockAndCashBox(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        machine.stock().set(0, new ItemStack(Items.APPLE, 3));
        VendingSetupMenu menu = setupMenu(admin, machine);
        helper.assertFalse(menu.stockAndCashEmpty(), "the stock has apples");
        helper.assertFalse(menu.clickMenuButton(admin, SetupButtons.toggleInfinite()) || machine.isInfinite(), "so it can't go infinite yet");
        machine.stock().set(0, ItemStack.EMPTY);
        helper.assertTrue(menu.clickMenuButton(admin, SetupButtons.toggleInfinite()) && machine.isInfinite(), "with both empty it goes infinite");
        helper.assertFalse(menu.clickMenuButton(admin, SetupButtons.showTab(SetupTab.STOCK)), "infinite machines have no Stock tab");
        helper.assertTrue(menu.clickMenuButton(admin, SetupButtons.toggleInfinite()) && !machine.isInfinite(), "and it can always go back");
        helper.succeed();
    }

    public static void theCatalogPickerCyclesThroughEveryCatalog(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        VendingSetupMenu menu = setupMenu(admin, machine);
        List<Identifier> ids = Catalogs.ids();
        for (Identifier expected : ids) {
            menu.clickMenuButton(admin, SetupButtons.cycleCatalog(1));
            helper.assertTrue(expected.equals(machine.catalogId()), "expected catalog " + expected + ", got " + machine.catalogId());
        }
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(1));
        helper.assertTrue(machine.catalogId() == null, "after the last catalog comes None");
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(-1));
        helper.assertTrue(ids.get(ids.size() - 1).equals(machine.catalogId()), "and going back from None gives the last one");
        helper.succeed();
    }

    /** Spec §4: the Selections tab is read-only while a catalog is assigned. */
    public static void catalogSelectionsAreReadOnly(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.setCatalog(EMERALDS);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.DIRT));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST, 0, owner);
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.price(0, 1)), "prices come from the catalog");
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        helper.assertFalse(machine.ownSelection(0).isSetUp(), "and the ghost click changed nothing underneath");
        helper.succeed();
    }

    public static void theCurrencySlotIsForAdmins(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        VendingSetupMenu menu = setupMenu(admin, machine);
        menu.clickMenuButton(admin, SetupButtons.showTab(SetupTab.ADMIN));
        menu.setCarried(new ItemStack(Items.EMERALD, 7));
        clickSlot(menu, VendingSetupMenu.CURRENCY, 0, admin);
        helper.assertTrue(machine.currencySlot() == Items.EMERALD && menu.getCarried().getCount() == 7, "the admin sets emeralds without using any");
        menu.setCarried(ItemStack.EMPTY);
        clickSlot(menu, VendingSetupMenu.CURRENCY, 0, admin);
        helper.assertTrue(machine.currencySlot() == null, "an empty-handed click clears it back to the default");
        helper.succeed();
    }

    /** Spec §3.2 rule 1: sneak + right-click with both hands empty. */
    public static void sneakingWithEmptyHandsOpensSetup(GameTestHelper helper) {
        RecordingServerPlayer owner = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        BuyingTests.placeMachine(helper, owner);
        owner.setShiftKeyDown(true);
        BuyingTests.click(helper, owner, MachineLayout.WINDOW);
        helper.assertTrue(owner.containerMenu instanceof VendingSetupMenu, "the owner's sneak-click opens setup, got " + owner.containerMenu);
        owner.closeContainer();
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        stranger.setShiftKeyDown(true);
        BuyingTests.click(helper, stranger, MachineLayout.WINDOW);
        BuyingTests.lastMessage(helper, stranger, Texts.OWNER_ONLY);
        helper.succeed();
    }

    public static void twoOpenScreensShareOneMachine(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu ownerMenu = setupMenu(owner, machine);
        VendingSetupMenu adminMenu = setupMenu(admin, machine);
        ownerMenu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.STOCK));
        adminMenu.clickMenuButton(admin, SetupButtons.showTab(SetupTab.STOCK));
        ownerMenu.setCarried(new ItemStack(Items.APPLE, 10));
        clickSlot(ownerMenu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(adminMenu.getSlot(VendingSetupMenu.FIRST_STOCK).getItem().getCount() == 10, "the admin's screen shows the owner's apples");
        clickSlot(adminMenu, VendingSetupMenu.FIRST_STOCK, 0, admin);
        helper.assertTrue(adminMenu.getCarried().getCount() == 10 && ownerMenu.getSlot(VendingSetupMenu.FIRST_STOCK).getItem().isEmpty()
                && ItemSlots.isEmpty(machine.stock()), "once the admin takes them, they're nowhere else");
        ownerMenu.setCarried(new ItemStack(Items.BREAD));
        clickSlot(ownerMenu, VendingSetupMenu.FIRST_GHOST + 2, 0, owner);
        helper.assertTrue(adminMenu.getSlot(VendingSetupMenu.FIRST_GHOST + 2).getItem().is(Items.BREAD), "selections show on both screens");
        helper.succeed();
    }

    public static void breakingTheMachineClosesItsSetup(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        standInFront(helper, owner);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertTrue(menu.stillValid(owner), "the owner standing in front keeps setup open");
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, owner);
        helper.assertFalse(menu.stillValid(owner), "once the machine is gone, its setup closes");
        helper.assertTrue(menu.getSlot(VendingSetupMenu.FIRST_STOCK).getItem().isEmpty(), "the stock spilled, so the screen can't hand it out again");
        helper.assertTrue(BuyingTests.droppedNear(helper, MachineTests.MASTER, Items.APPLE) == 10, "the apples are on the ground, once");
        helper.succeed();
    }

    public static void shiftClickOnlyStocksOnTheStockTab(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        owner.getInventory().setItem(0, new ItemStack(Items.APPLE, 10));
        VendingSetupMenu menu = setupMenu(owner, machine);
        int hotbarFirst = VendingSetupMenu.FIRST_PLAYER + 27;
        shiftClickSlot(menu, hotbarFirst, owner);
        helper.assertTrue(owner.getInventory().getItem(0).getCount() == 10 && ItemSlots.isEmpty(machine.stock()),
                "on the Items tab shift-click moves nothing");
        menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.STOCK));
        shiftClickSlot(menu, hotbarFirst, owner);
        helper.assertTrue(owner.getInventory().getItem(0).isEmpty() && BuyingTests.countIn(machine.stock(), Items.APPLE) == 10,
                "on the Stock tab it stocks the machine");
        shiftClickSlot(menu, VendingSetupMenu.FIRST_STOCK, owner);
        helper.assertTrue(BuyingTests.countHeld(owner, Items.APPLE) == 10 && ItemSlots.isEmpty(machine.stock()), "and shift-clicking stock takes it back");
        helper.succeed();
    }
```

Add the fifteen adapter methods to `FabricShopTests.java` and to the 1.21.1 block of `NeoForgeShopTests.java` (templates under "Adding a shop test"): `ghostSlotsCopyWithoutTaking`, `theEditorButtonsChangeAmountAndPrice`, `strangersCannotChangeAnything`, `theStockTabStocksTheMachine`, `theCashBoxIsTakeOnly`, `withdrawEmptiesTheCashBoxIntoTheOwnersInventory`, `onlyAdminsSeeAndUseTheAdminTab`, `infiniteNeedsAnEmptyStockAndCashBox`, `theCatalogPickerCyclesThroughEveryCatalog`, `catalogSelectionsAreReadOnly`, `theCurrencySlotIsForAdmins`, `sneakingWithEmptyHandsOpensSetup`, `twoOpenScreensShareOneMachine`, `breakingTheMachineClosesItsSetup`, `shiftClickOnlyStocksOnTheStockTab`.

- [ ] **Step 6: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation errors: package `diamondvending.menu` does not exist.

- [ ] **Step 7: Register a menu that knows its block**

`src/main/java/diamondvending/registry/MenuHandle.java`:

```java
package diamondvending.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** A registered menu type for a block, and how to open it (each loader sends the block's position its own way). */
public interface MenuHandle<M extends AbstractContainerMenu> {
    MenuType<M> type();

    /** Opens the menu for {@code player}; the client's menu gets {@code pos} to find the block. */
    void open(ServerPlayer player, Component title, BlockPos pos);
}
```

In `registry/Registrar.java` add imports `net.minecraft.world.entity.player.Inventory`, `net.minecraft.world.inventory.AbstractContainerMenu`, and:

```java
    /** Makes a menu for a block: on the server when it opens, and on the client from the position the server sends. */
    @FunctionalInterface
    interface BlockMenuFactory<M extends AbstractContainerMenu> {
        M create(int id, Inventory inventory, BlockPos pos);
    }

    /** A menu type whose client side knows which block it's for. */
    <M extends AbstractContainerMenu> MenuHandle<M> menu(String name, BlockMenuFactory<M> factory);
```

In `platform/neoforge/NeoForgeRegistrar.java` add imports `diamondvending.registry.MenuHandle`, `net.minecraft.core.BlockPos`, `net.minecraft.network.chat.Component`, `net.minecraft.server.level.ServerPlayer`, `net.minecraft.world.SimpleMenuProvider`, `net.minecraft.world.inventory.AbstractContainerMenu`, `net.minecraft.world.inventory.MenuType`, `net.neoforged.neoforge.common.extensions.IMenuTypeExtension`; the field

```java
    private final DeferredRegister<MenuType<?>> menus = DeferredRegister.create(Registries.MENU, DiamondVending.MOD_ID);
```

`menus.register(modBus);` in `registerAll`, and:

```java
    @Override
    public <M extends AbstractContainerMenu> MenuHandle<M> menu(String name, BlockMenuFactory<M> factory) {
        Supplier<MenuType<M>> type = menus.register(name,
                () -> IMenuTypeExtension.<M>create((id, inventory, data) -> factory.create(id, inventory, data.readBlockPos())));
        return new MenuHandle<>() {
            @Override
            public MenuType<M> type() {
                return type.get();
            }

            @Override
            public void open(ServerPlayer player, Component title, BlockPos pos) {
                // The position rides along with the "open screen" packet, so the client's menu finds the machine.
                player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> factory.create(id, inventory, pos), title),
                        buffer -> buffer.writeBlockPos(pos));
            }
        };
    }
```

In `platform/fabric/FabricRegistrar.java` add imports

```java
import diamondvending.registry.MenuHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
//? if >=26.1 {
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
//?} else {
/*import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
*///?}
```

(merge the conditional lines into the existing `//? if >=26.1` import block) and:

```java
    /** Fabric API's "extended" menu types send the block's position with the "open screen" packet. */
    @Override
    public <M extends AbstractContainerMenu> MenuHandle<M> menu(String name, BlockMenuFactory<M> factory) {
        //? if >=26.1 {
        MenuType<M> type = Registry.register(BuiltInRegistries.MENU, DiamondVending.id(name),
                new ExtendedMenuType<M, BlockPos>(factory::create, BlockPos.STREAM_CODEC));
        //?} else {
        /*MenuType<M> type = Registry.register(BuiltInRegistries.MENU, DiamondVending.id(name),
                new ExtendedScreenHandlerType<M, BlockPos>(factory::create, BlockPos.STREAM_CODEC));
        *///?}
        return new MenuHandle<>() {
            @Override
            public MenuType<M> type() {
                return type;
            }

            @Override
            public void open(ServerPlayer player, Component title, BlockPos pos) {
                player.openMenu(new PositionedMenu<>(factory, title, pos));
            }
        };
    }

    //? if >=26.1 {
    private record PositionedMenu<M extends AbstractContainerMenu>(BlockMenuFactory<M> factory, Component title, BlockPos pos)
            implements ExtendedMenuProvider<BlockPos> {
    //?} else {
    /*private record PositionedMenu<M extends AbstractContainerMenu>(BlockMenuFactory<M> factory, Component title, BlockPos pos)
            implements ExtendedScreenHandlerFactory<BlockPos> {
    *///?}
        @Override
        public BlockPos getScreenOpeningData(ServerPlayer player) {
            return pos;
        }

        @Override
        public Component getDisplayName() {
            return title;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return factory.create(id, inventory, pos);
        }
    }
```

In `registry/ModContent.java` add imports `diamondvending.menu.VendingSetupMenu`, the field `public static MenuHandle<VendingSetupMenu> SETUP_MENU;` and at the end of `register`:

```java
        SETUP_MENU = registrar.menu("setup", VendingSetupMenu::new);
```

- [ ] **Step 8: Write the menu**

`src/main/java/diamondvending/menu/MachineSlots.java`:

```java
package diamondvending.menu;

import diamondvending.shop.ItemSlots;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One of the machine's slot lists (Stock or Cash Box) as a container for the setup screen; every change re-syncs the machine. */
final class MachineSlots implements Container {
    private final NonNullList<ItemStack> items;
    private final Runnable changed;

    MachineSlots(NonNullList<ItemStack> items, Runnable changed) {
        this.items = items;
        this.changed = changed;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return ItemSlots.isEmpty(items);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, count);
        if (!taken.isEmpty()) changed.run();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        changed.run();
    }

    @Override
    public void setChanged() {
        changed.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        ItemSlots.clear(items);
        changed.run();
    }
}
```

`src/main/java/diamondvending/menu/VendingSetupMenu.java`:

```java
package diamondvending.menu;

import diamondvending.block.MachineAccess;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.catalog.Catalogs;
import diamondvending.core.MachineLayout;
import diamondvending.core.SetupButtons;
import diamondvending.core.SetupTab;
import diamondvending.registry.ModContent;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.PlayerItems;
import diamondvending.shop.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;
*///?}

import java.util.function.UnaryOperator;

/**
 * The setup screen's menu (spec §4): tabs, 12 ghost slots for what the buttons sell, the Stock, the Cash Box and the
 * admin's currency slot, with the player's inventory below. The client only asks: ghost-slot clicks arrive as vanilla
 * container clicks and everything else as menu buttons ({@link SetupButtons}), and every one is re-checked here.
 */
public class VendingSetupMenu extends AbstractContainerMenu {
    // The screen's size and where the slots sit on it (the client screen draws around them).
    public static final int WIDTH = 208;
    public static final int HEIGHT = 224;
    public static final int GHOSTS_X = 12;
    public static final int GHOSTS_Y = 56;
    public static final int GRID_X = 24;
    public static final int GRID_Y = 72;
    public static final int CURRENCY_X = 112;
    public static final int CURRENCY_Y = 102;
    public static final int INVENTORY_X = 24;
    public static final int INVENTORY_Y = 142;
    public static final int HOTBAR_Y = 200;

    // Slot indexes.
    public static final int FIRST_GHOST = 0;
    public static final int FIRST_STOCK = FIRST_GHOST + MachineLayout.SELECTIONS;
    public static final int FIRST_CASH = FIRST_STOCK + VendingMachineBlockEntity.STOCK_SLOTS;
    public static final int CURRENCY = FIRST_CASH + VendingMachineBlockEntity.CASH_BOX_SLOTS;
    public static final int FIRST_PLAYER = CURRENCY + 1;
    private static final int END = FIRST_PLAYER + 36;

    private final VendingMachineBlockEntity machine;
    private final DataSlot admin = DataSlot.standalone();
    private SetupTab tab = SetupTab.SELECTIONS;
    private int selected;

    public VendingSetupMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModContent.SETUP_MENU.type(), id);
        Player player = inventory.player;
        Level level = player.level();
        // A machine that's already gone (the client can be a moment behind) gets a stand-in, and the screen closes.
        machine = level.getBlockEntity(pos) instanceof VendingMachineBlockEntity found ? found
                : new VendingMachineBlockEntity(pos, ModContent.VENDING_MACHINE.get().defaultBlockState());
        boolean server = !level.isClientSide();
        Container stock = server ? new MachineSlots(machine.stock(), machine::changed) : new SimpleContainer(VendingMachineBlockEntity.STOCK_SLOTS);
        Container cashBox = server ? new MachineSlots(machine.cashBox(), machine::changed) : new SimpleContainer(VendingMachineBlockEntity.CASH_BOX_SLOTS);
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            addSlot(new ShelfSlot(i, GHOSTS_X + i % 3 * 18, GHOSTS_Y + i / 3 * 18));
        }
        for (int i = 0; i < VendingMachineBlockEntity.STOCK_SLOTS; i++) addSlot(new TabSlot(stock, i, SetupTab.STOCK, true));
        for (int i = 0; i < VendingMachineBlockEntity.CASH_BOX_SLOTS; i++) addSlot(new TabSlot(cashBox, i, SetupTab.CASH_BOX, false));
        addSlot(new CurrencySlot());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        if (server) admin.set(MachineAccess.isAdmin(player) ? 1 : 0);
        addDataSlot(admin);
    }

    public VendingMachineBlockEntity machine() {
        return machine;
    }

    public SetupTab tab() {
        return tab;
    }

    /** The button (0–11) whose amount and price the Items tab is editing. */
    public int selected() {
        return selected;
    }

    /** Whether the player looking at this menu is an admin (decided by the server, synced to the client). */
    public boolean viewerIsAdmin() {
        return admin.get() == 1;
    }

    public boolean stockAndCashEmpty() {
        return rangeEmpty(FIRST_STOCK, CURRENCY);
    }

    public boolean cashBoxEmpty() {
        return rangeEmpty(FIRST_CASH, CURRENCY);
    }

    private boolean rangeEmpty(int from, int to) {
        for (int i = from; i < to; i++) {
            if (slots.get(i).hasItem()) return false;
        }
        return true;
    }

    // ---- clicks --------------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (!handledHere(slotIndex, button, input == ContainerInput.PICKUP, player)) super.clicked(slotIndex, button, input, player);
    }
    //?} else {
    /*@Override
    public void clicked(int slotIndex, int button, ClickType type, Player player) {
        if (!handledHere(slotIndex, button, type == ClickType.PICKUP, player)) super.clicked(slotIndex, button, type, player);
    }
    *///?}

    /** Ghost slots and slots on hidden tabs: true when the click is dealt with and vanilla must not move anything. */
    private boolean handledHere(int slotIndex, int button, boolean pickup, Player player) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;
        Slot slot = slots.get(slotIndex);
        if (!slot.isActive()) return true;
        if (slot instanceof ShelfSlot shelf) {
            if (pickup) copyToButton(shelf.index, button, player);
            return true;
        }
        if (slot instanceof CurrencySlot) {
            if (pickup) setCurrency(player);
            return true;
        }
        return false;
    }

    /** Ghost-slot click: the cursor's item (all of it, or one with a right-click) becomes what the button sells; nothing is used up. */
    private void copyToButton(int index, int button, Player player) {
        selected = index;
        if (player.level().isClientSide() || !MachineAccess.canManage(player, machine.getOwner()) || machine.usesCatalog()) return;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;
        ItemStack template = button == 1 ? carried.copyWithCount(1) : carried.copy();
        machine.setSelection(index, Selection.of(template, machine.getSelection(index).price()));
    }

    /** Currency ghost-slot click (admins): the cursor's item becomes the currency; an empty cursor clears it. */
    private void setCurrency(Player player) {
        if (player.level().isClientSide() || !MachineAccess.isAdmin(player)) return;
        ItemStack carried = getCarried();
        machine.setCurrencySlot(carried.isEmpty() ? null : carried.getItem());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        SetupButtons.Press press = SetupButtons.decode(id);
        if (press == null) return false;
        if (press instanceof SetupButtons.ShowTab show) return showTab(show.tab());
        // Everything else is the server's to decide; the client only asks (spec §4).
        if (player.level().isClientSide()) return true;
        if (!MachineAccess.canManage(player, machine.getOwner())) return false;
        return switch (press) {
            case SetupButtons.ShowTab show -> showTab(show.tab());
            case SetupButtons.Withdraw ignored -> withdraw(player);
            case SetupButtons.ToggleInfinite ignored -> toggleInfinite(player);
            case SetupButtons.CycleCatalog cycle -> cycleCatalog(player, cycle.step());
            case SetupButtons.Clear clear -> edit(clear.index(), selection -> Selection.EMPTY);
            case SetupButtons.ChangeQuantity change -> edit(change.index(), selection -> withQuantity(selection, change.by()));
            case SetupButtons.SetPrice price -> edit(price.index(), selection -> withPrice(selection, price.price()));
        };
    }

    private boolean showTab(SetupTab wanted) {
        if (!wanted.shownTo(viewerIsAdmin(), machine.isInfinite())) return false;
        tab = wanted;
        return true;
    }

    private boolean withdraw(Player player) {
        if (machine.isInfinite()) return false;
        ItemSlots.takeAll(machine.cashBox()).forEach(stack -> PlayerItems.give(player, stack));
        machine.changed();
        return true;
    }

    private boolean toggleInfinite(Player player) {
        if (!MachineAccess.isAdmin(player)) return false;
        // Spec §4: going infinite needs an empty Stock and Cash Box, so nobody's items silently vanish.
        if (!machine.isInfinite() && !stockAndCashEmpty()) return false;
        machine.setInfinite(!machine.isInfinite());
        if (machine.isInfinite() && (tab == SetupTab.STOCK || tab == SetupTab.CASH_BOX)) tab = SetupTab.SELECTIONS;
        return true;
    }

    private boolean cycleCatalog(Player player, int step) {
        if (!MachineAccess.isAdmin(player)) return false;
        machine.setCatalog(Catalogs.cycle(machine.catalogId(), step));
        return true;
    }

    /** Changes a button's selection; refused while a catalog decides what's sold (spec §4). */
    private boolean edit(int index, UnaryOperator<Selection> change) {
        if (machine.usesCatalog()) return false;
        Selection before = machine.getSelection(index);
        Selection after = change.apply(before);
        if (after != before) machine.setSelection(index, after);
        return true;
    }

    private static Selection withQuantity(Selection selection, int by) {
        if (!selection.isSetUp()) return selection;
        int quantity = Math.max(1, Math.min(selection.quantity() + by, selection.template().getMaxStackSize()));
        return Selection.of(selection.template().copyWithCount(quantity), selection.price());
    }

    private static Selection withPrice(Selection selection, int price) {
        return selection.isSetUp() ? Selection.of(selection.template(), price) : selection;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot instanceof ShowSlot || !slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = index >= FIRST_PLAYER
                // From the player: only into the Stock, and only while the Stock tab is showing.
                ? tab == SetupTab.STOCK && !machine.isInfinite() && moveItemStackTo(stack, FIRST_STOCK, FIRST_CASH, false)
                : moveItemStackTo(stack, FIRST_PLAYER, END, true);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(machine, player) && MachineAccess.canManage(player, machine.getOwner());
    }

    // ---- slots ---------------------------------------------------------------------------------------------------

    /** A slot that only shows an item; the menu handles its clicks, and it never takes or gives anything itself. */
    private abstract static class ShowSlot extends Slot {
        ShowSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public abstract ItemStack getItem();

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** Ghost slot: what button {@code index} sells, shown with the amount per purchase. */
    private final class ShelfSlot extends ShowSlot {
        final int index;

        ShelfSlot(int index, int x, int y) {
            super(x, y);
            this.index = index;
        }

        @Override
        public ItemStack getItem() {
            return machine.getSelection(index).template();
        }

        @Override
        public boolean isActive() {
            return tab == SetupTab.SELECTIONS;
        }
    }

    /** The admin's currency ghost slot: the currency item, or empty for the default currency. */
    private final class CurrencySlot extends ShowSlot {
        CurrencySlot() {
            super(CURRENCY_X, CURRENCY_Y);
        }

        @Override
        public ItemStack getItem() {
            Item item = machine.currencySlot();
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        @Override
        public boolean isActive() {
            return tab == SetupTab.ADMIN;
        }
    }

    /** A Stock or Cash Box slot: real items, there only while its tab shows on an owned machine. The Cash Box is take-only. */
    private final class TabSlot extends Slot {
        private final SetupTab home;
        private final boolean placeable;

        TabSlot(Container container, int index, SetupTab home, boolean placeable) {
            super(container, index, GRID_X + index % 9 * 18, GRID_Y + index / 9 * 18);
            this.home = home;
            this.placeable = placeable;
        }

        @Override
        public boolean isActive() {
            return tab == home && !machine.isInfinite();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return placeable && super.mayPlace(stack);
        }
    }
}
```

- [ ] **Step 9: Sneak-click opens it**

In `core/Texts.java` add after the hover keys:

```java
    // The setup screen (spec §4).
    public static final String SETUP_TITLE = "setup.diamondvending.title";
```

and `keys.add(SETUP_TITLE);` in `all()`. In `en_us.json` add `"setup.diamondvending.title": "Vending Machine Setup",` after the `hud.diamondvending.*` lines.

In `block/VendingMachineBlock.java` add the import `net.minecraft.server.level.ServerPlayer`; in `use(…)`, right after `machine.refreshOwnerName(player);`:

```java
        if (player.isSecondaryUseActive()) {
            // Spec §3.2 rule 1: the game only lets a sneak-click reach a block when both hands are empty.
            openSetup(machine, player);
            return;
        }
```

and after `use`:

```java
    /** Opens the setup screen for the owner or an admin (spec §4); anyone else is told it's not theirs. */
    private static void openSetup(VendingMachineBlockEntity machine, Player player) {
        if (!MachineAccess.canManage(player, machine.getOwner())) {
            Messages.actionBar(player, Component.translatable(Texts.OWNER_ONLY));
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ModContent.SETUP_MENU.open(serverPlayer, Component.translatable(Texts.SETUP_TITLE), machine.getBlockPos());
        }
    }
```

- [ ] **Step 10: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 106 required tests passed :)`; unit tests green.

Run, one at a time: `G26F`, `G121N`, `G121F`
Expected: 106 / 105 / 105 pass.

- [ ] **Step 11: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "feat: the setup menu, opened by a sneak-click with empty hands"
```

---

### Task 5: The setup screen

Deliverable: the setup menu has a screen on both loaders and versions: tab buttons (a red "!" on the tab that fixes a problem), a red banner listing the machine's problems, ghost slots with an editor for amount and price, the Stock and Cash Box grids with Withdraw all, and the Admin tab's infinite switch, catalog picker and currency slot. A client screenshot test opens it for real.

**Files:**
- Create: `src/main/java/diamondvending/client/Gui.java`, `client/VendingSetupScreen.java`
- Modify: `platform/fabric/DiamondVendingFabricClient.java`, `platform/neoforge/DiamondVendingNeoForgeClient.java`
- Modify: `core/Texts.java`, `lang/en_us.json`
- Modify: `src/gametest/java/diamondvending/gametest/fabric/FabricClientTests.java`, `docs/dev-setup.md`

**Interfaces:**
- Consumes: Task 4's `VendingSetupMenu` (constants, `machine()`, `tab()`, `selected()`, `viewerIsAdmin()`, `stockAndCashEmpty()`, `cashBoxEmpty()`, `clickMenuButton`), `SetupButtons`, `SetupTab`, `ModContent.SETUP_MENU`; Task 2's `usesCatalog()`, `catalogLabel()`; `syncedProblems()`.
- Produces: `client.VendingSetupScreen` (public `showTab(SetupTab)`), `client.Gui`; `Texts.SETUP_*` keys.

- [ ] **Step 1: Add the screen's words**

In `core/Texts.java`, under `SETUP_TITLE`:

```java
    public static final String SETUP_ALL_GOOD = "setup.diamondvending.all_good";
    public static final String SETUP_TAB_SELECTIONS = "setup.diamondvending.tab.selections";
    public static final String SETUP_TAB_STOCK = "setup.diamondvending.tab.stock";
    public static final String SETUP_TAB_CASH_BOX = "setup.diamondvending.tab.cash_box";
    public static final String SETUP_TAB_ADMIN = "setup.diamondvending.tab.admin";
    public static final String SETUP_BUTTON = "setup.diamondvending.button";
    public static final String SETUP_BUTTON_ITEM = "setup.diamondvending.button_item";
    public static final String SETUP_PICK_ITEM = "setup.diamondvending.pick_item";
    public static final String SETUP_QUANTITY = "setup.diamondvending.quantity";
    public static final String SETUP_PRICE = "setup.diamondvending.price";
    public static final String SETUP_CLEAR = "setup.diamondvending.clear";
    public static final String SETUP_FROM_CATALOG = "setup.diamondvending.from_catalog";
    public static final String SETUP_WITHDRAW = "setup.diamondvending.withdraw";
    public static final String SETUP_INFINITE_ON = "setup.diamondvending.infinite_on";
    public static final String SETUP_INFINITE_OFF = "setup.diamondvending.infinite_off";
    public static final String SETUP_INFINITE_EXPLAINED = "setup.diamondvending.infinite_explained";
    public static final String SETUP_EMPTY_FIRST = "setup.diamondvending.empty_first";
    public static final String SETUP_CATALOG = "setup.diamondvending.catalog";
    public static final String SETUP_NO_CATALOG = "setup.diamondvending.no_catalog";
    public static final String SETUP_CURRENCY = "setup.diamondvending.currency";
    public static final String SETUP_CURRENCY_DEFAULT = "setup.diamondvending.currency_default";
```

and in `all()` replace `keys.add(SETUP_TITLE);` with:

```java
        keys.addAll(List.of(SETUP_TITLE, SETUP_ALL_GOOD, SETUP_TAB_SELECTIONS, SETUP_TAB_STOCK, SETUP_TAB_CASH_BOX, SETUP_TAB_ADMIN,
                SETUP_BUTTON, SETUP_BUTTON_ITEM, SETUP_PICK_ITEM, SETUP_QUANTITY, SETUP_PRICE, SETUP_CLEAR, SETUP_FROM_CATALOG,
                SETUP_WITHDRAW, SETUP_INFINITE_ON, SETUP_INFINITE_OFF, SETUP_INFINITE_EXPLAINED, SETUP_EMPTY_FIRST, SETUP_CATALOG,
                SETUP_NO_CATALOG, SETUP_CURRENCY, SETUP_CURRENCY_DEFAULT));
```

Run: `./gradlew :26.1-neoforge:test --tests diamondvending.core.TextsTest`
Expected: FAIL — `en_us.json has no text for setup.diamondvending.all_good`.

In `en_us.json`, after the title line:

```json
  "setup.diamondvending.all_good": "No problems. Happy selling!",
  "setup.diamondvending.tab.selections": "Items",
  "setup.diamondvending.tab.stock": "Stock",
  "setup.diamondvending.tab.cash_box": "Cash Box",
  "setup.diamondvending.tab.admin": "Admin",
  "setup.diamondvending.button": "Button %s",
  "setup.diamondvending.button_item": "Button %s: %s",
  "setup.diamondvending.pick_item": "Hold an item and click a slot on the left to sell it on that button.",
  "setup.diamondvending.quantity": "Amount",
  "setup.diamondvending.price": "Price",
  "setup.diamondvending.clear": "Clear",
  "setup.diamondvending.from_catalog": "This machine sells the catalog \"%s\". An admin can change it in the Admin tab.",
  "setup.diamondvending.withdraw": "Withdraw all",
  "setup.diamondvending.infinite_on": "Infinite: ON",
  "setup.diamondvending.infinite_off": "Infinite: OFF",
  "setup.diamondvending.infinite_explained": "Infinite machines never sell out, and the money they take disappears.",
  "setup.diamondvending.empty_first": "Take everything out of Stock and Cash Box first.",
  "setup.diamondvending.catalog": "Catalog",
  "setup.diamondvending.no_catalog": "None",
  "setup.diamondvending.currency": "Currency",
  "setup.diamondvending.currency_default": "Empty: %s",
```

Run: `./gradlew :26.1-neoforge:test --tests diamondvending.core.TextsTest`
Expected: PASS.

- [ ] **Step 2: Add the setup screenshots to the client test (they fail first)**

In `fabric/FabricClientTests.java` add to the 26.1 import block:

```java
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.client.VendingSetupScreen;
import diamondvending.core.SetupTab;
import net.minecraft.core.BlockPos;

import java.util.Locale;
```

and insert after the `hover_tray` screenshot, before the `// Behind the machine` block:

```java
            // The setup screen: an admin (creative; the test machine has no owner) sneak-right-clicks with empty hands.
            server.runCommand("clear @a");
            server.runCommand("gamemode creative @a");
            server.runCommand("tp @a 1 -60 3.5 180 10");
            context.waitTicks(5);
            context.getInput().holdKey(options -> options.keyShift);
            context.waitTicks(2); // the server learns the player is sneaking on the next tick
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitForScreen(VendingSetupScreen.class);
            context.getInput().releaseKey(options -> options.keyShift);
            context.waitTicks(5);
            context.takeScreenshot("setup_items");
            // A real button press, through the network: button 1 sold 2 apples, "+" makes it 3.
            context.clickScreenButton("+");
            context.waitTicks(5);
            int apples = server.computeOnServer(s -> ((VendingMachineBlockEntity) s.overworld().getBlockEntity(new BlockPos(0, -60, 0)))
                    .getSelection(0).quantity());
            if (apples != 3) throw new AssertionError("\"+\" should make button 1 sell 3 apples, it sells " + apples);
            for (SetupTab tab : new SetupTab[] {SetupTab.STOCK, SetupTab.CASH_BOX, SetupTab.ADMIN}) {
                context.runOnClient(client -> ((VendingSetupScreen) client.screen).showTab(tab));
                context.waitTicks(3);
                context.takeScreenshot("setup_" + tab.name().toLowerCase(Locale.ROOT));
            }
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            context.waitTicks(3);
            server.runCommand("gamemode survival @a");
```

Run: `GCLIENT`
Expected: FAIL — compilation error: `diamondvending.client.VendingSetupScreen` not found.

- [ ] **Step 3: Write the drawing adapter and the screen**

`src/main/java/diamondvending/client/Gui.java`:

```java
package diamondvending.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/** The few drawing calls the setup screen makes, on either version's GUI graphics. */
final class Gui {
    //? if >=26.1 {
    private final GuiGraphicsExtractor graphics;

    Gui(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }
    //?} else {
    /*private final GuiGraphics graphics;

    Gui(GuiGraphics graphics) {
        this.graphics = graphics;
    }
    *///?}

    void fill(int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y1, color);
    }

    /** Text without a shadow, like vanilla's container labels. */
    void text(Font font, Component text, int x, int y, int color) {
        text(font, text.getVisualOrderText(), x, y, color);
    }

    void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, color, false);
        //?} else {
        /*graphics.drawString(font, text, x, y, color, false);
        *///?}
    }

    void item(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        graphics.item(stack, x, y);
        //?} else {
        /*graphics.renderItem(stack, x, y);
        *///?}
    }
}
```

`src/main/java/diamondvending/client/VendingSetupScreen.java`:

```java
package diamondvending.client;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Problem;
import diamondvending.core.SetupButtons;
import diamondvending.core.SetupTab;
import diamondvending.core.Texts;
import diamondvending.menu.VendingSetupMenu;
import diamondvending.shop.Selection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
*///?}

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The setup screen (spec §4): the panel, a red banner with the machine's problems, tab buttons (a red "!" on the tab
 * that fixes a problem) and each tab's controls around {@link VendingSetupMenu}'s slots. Every control only asks — it
 * sends a menu button number ({@link SetupButtons}) and the server decides.
 */
public final class VendingSetupScreen extends AbstractContainerScreen<VendingSetupMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int EDGE = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;
    private static final int GOOD = 0xFF2A7A2A;
    private static final int BANNER = 0xFFAA2222;
    private static final int BANNER_TEXT = 0xFFFFFFFF;
    private static final int WARNING = 0xFFAA2222;
    private static final int EDITING = 0xFFFFD700;
    private static final int BANNER_Y = 23;
    private static final int BANNER_LINES = 3;
    private static final int EDITOR_X = 72;

    private final Map<SetupTab, Button> tabs = new EnumMap<>(SetupTab.class);
    private Button fewer;
    private Button more;
    private Button clear;
    private Button withdraw;
    private Button infinite;
    private Button previousCatalog;
    private Button nextCatalog;
    private EditBox price;
    private boolean settingPrice;
    private int priceShownFor = -1;

    public VendingSetupScreen(VendingSetupMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, VendingSetupMenu.WIDTH, VendingSetupMenu.HEIGHT);
        //?} else {
        /*super(menu, inventory, title);
        imageWidth = VendingSetupMenu.WIDTH;
        imageHeight = VendingSetupMenu.HEIGHT;
        *///?}
        inventoryLabelX = VendingSetupMenu.INVENTORY_X;
        inventoryLabelY = VendingSetupMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        for (SetupTab tab : SetupTab.values()) {
            tabs.put(tab, addRenderableWidget(Button.builder(Component.empty(), button -> showTab(tab))
                    .bounds(x + 4 + tab.ordinal() * 50, y + 4, 48, 16).build()));
        }
        fewer = addRenderableWidget(Button.builder(Component.literal("-"), button -> press(SetupButtons.fewer(menu.selected())))
                .bounds(x + EDITOR_X + 44, y + 70, 16, 14).build());
        more = addRenderableWidget(Button.builder(Component.literal("+"), button -> press(SetupButtons.more(menu.selected())))
                .bounds(x + EDITOR_X + 80, y + 70, 16, 14).build());
        price = addRenderableWidget(new EditBox(font, x + EDITOR_X + 44, y + 89, 36, 14, Component.translatable(Texts.SETUP_PRICE)));
        price.setMaxLength(3);
        price.setFilter(text -> text.chars().allMatch(Character::isDigit));
        price.setResponder(this::priceTyped);
        clear = addRenderableWidget(Button.builder(Component.translatable(Texts.SETUP_CLEAR), button -> press(SetupButtons.clear(menu.selected())))
                .bounds(x + EDITOR_X, y + 107, 56, 14).build());
        withdraw = addRenderableWidget(Button.builder(Component.translatable(Texts.SETUP_WITHDRAW), button -> press(SetupButtons.withdraw()))
                .bounds(x + 120, y + 55, 80, 14).build());
        infinite = addRenderableWidget(Button.builder(Component.empty(), button -> press(SetupButtons.toggleInfinite()))
                .bounds(x + 8, y + 56, 96, 16).build());
        previousCatalog = addRenderableWidget(Button.builder(Component.literal("<"), button -> press(SetupButtons.cycleCatalog(-1)))
                .bounds(x + 112, y + 68, 14, 14).build());
        nextCatalog = addRenderableWidget(Button.builder(Component.literal(">"), button -> press(SetupButtons.cycleCatalog(1)))
                .bounds(x + 190, y + 68, 14, 14).build());
        refresh();
    }

    /** Shows a tab, as its tab button does. */
    public void showTab(SetupTab tab) {
        press(SetupButtons.showTab(tab));
        refresh();
    }

    /** Asks the menu, then the server (the client's menu only switches tabs itself). */
    private void press(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void priceTyped(String text) {
        if (settingPrice || text.isEmpty()) return;
        press(SetupButtons.price(menu.selected(), Math.min(Integer.parseInt(text), SetupButtons.MAX_PRICE)));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    /** Shows, hides and labels the controls for the current tab and the machine as it is now. */
    private void refresh() {
        VendingMachineBlockEntity machine = menu.machine();
        if (!menu.tab().shownTo(menu.viewerIsAdmin(), machine.isInfinite())) press(SetupButtons.showTab(SetupTab.SELECTIONS));
        SetupTab current = menu.tab();
        Set<SetupTab> attention = SetupTab.needingAttention(machine.syncedProblems());
        for (SetupTab tab : SetupTab.values()) {
            Button button = tabs.get(tab);
            button.visible = tab.shownTo(menu.viewerIsAdmin(), machine.isInfinite());
            button.active = tab != current;
            button.setMessage(tabLabel(tab, attention.contains(tab)));
        }
        Selection selection = machine.getSelection(menu.selected());
        boolean editing = current == SetupTab.SELECTIONS && !machine.usesCatalog() && selection.isSetUp();
        fewer.visible = more.visible = price.visible = clear.visible = editing;
        fewer.active = selection.quantity() > 1;
        more.active = selection.quantity() < selection.template().getMaxStackSize();
        if (!editing) {
            price.setFocused(false);
        } else if (!price.isFocused() || priceShownFor != menu.selected()) {
            settingPrice = true;
            price.setValue(Integer.toString(selection.price()));
            settingPrice = false;
            priceShownFor = menu.selected();
        }
        withdraw.visible = current == SetupTab.CASH_BOX;
        withdraw.active = !menu.cashBoxEmpty();
        infinite.visible = previousCatalog.visible = nextCatalog.visible = current == SetupTab.ADMIN;
        infinite.setMessage(Component.translatable(machine.isInfinite() ? Texts.SETUP_INFINITE_ON : Texts.SETUP_INFINITE_OFF));
        infinite.active = machine.isInfinite() || menu.stockAndCashEmpty();
    }

    private static Component tabLabel(SetupTab tab, boolean attention) {
        MutableComponent label = Component.translatable(switch (tab) {
            case SELECTIONS -> Texts.SETUP_TAB_SELECTIONS;
            case STOCK -> Texts.SETUP_TAB_STOCK;
            case CASH_BOX -> Texts.SETUP_TAB_CASH_BOX;
            case ADMIN -> Texts.SETUP_TAB_ADMIN;
        });
        return attention ? label.append(Component.literal(" !").withStyle(ChatFormatting.RED)) : label;
    }

    // ---- drawing (absolute coordinates in drawBackground, panel-relative in drawLabels) ---------------------------

    private void drawBackground(Gui gui) {
        int x = leftPos;
        int y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, EDGE);
        gui.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        if (!menu.machine().syncedProblems().isEmpty()) {
            gui.fill(x + 4, y + BANNER_Y, x + imageWidth - 4, y + BANNER_Y + BANNER_LINES * 10 + 2, BANNER);
        }
        for (Slot slot : menu.slots) {
            if (slot.isActive()) drawSlot(gui, x + slot.x, y + slot.y);
        }
        if (menu.tab() == SetupTab.SELECTIONS) {
            Slot editing = menu.getSlot(VendingSetupMenu.FIRST_GHOST + menu.selected());
            outline(gui, x + editing.x - 2, y + editing.y - 2, 20, EDITING);
        }
    }

    private static void drawSlot(Gui gui, int x, int y) {
        gui.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        gui.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        gui.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    private static void outline(Gui gui, int x, int y, int size, int color) {
        gui.fill(x, y, x + size, y + 1, color);
        gui.fill(x, y + size - 1, x + size, y + size, color);
        gui.fill(x, y, x + 1, y + size, color);
        gui.fill(x + size - 1, y, x + size, y + size, color);
    }

    private void drawLabels(Gui gui) {
        VendingMachineBlockEntity machine = menu.machine();
        List<Problem> problems = machine.syncedProblems();
        if (problems.isEmpty()) {
            gui.text(font, title, 8, BANNER_Y + 4, TEXT);
            gui.text(font, Component.translatable(Texts.SETUP_ALL_GOOD), 8, BANNER_Y + 16, GOOD);
        } else {
            // Spec §4: the red banner lists every problem, in the words the display scrolls (each names who fixes it).
            for (int i = 0; i < Math.min(BANNER_LINES, problems.size()); i++) {
                gui.text(font, Component.translatable(Texts.problemDisplay(problems.get(i))), 8, BANNER_Y + 3 + i * 10, BANNER_TEXT);
            }
        }
        gui.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
        switch (menu.tab()) {
            case SELECTIONS -> drawEditor(gui, machine);
            case STOCK, CASH_BOX -> gui.text(font, tabLabel(menu.tab(), false), VendingSetupMenu.GRID_X, VendingSetupMenu.GRID_Y - 12, TEXT);
            case ADMIN -> drawAdmin(gui, machine);
        }
    }

    private void drawEditor(Gui gui, VendingMachineBlockEntity machine) {
        int index = menu.selected();
        Selection selection = machine.getSelection(index);
        Component heading = selection.isSetUp()
                ? Component.translatable(Texts.SETUP_BUTTON_ITEM, index + 1, selection.template().getHoverName())
                : Component.translatable(Texts.SETUP_BUTTON, index + 1);
        gui.text(font, firstLine(heading, VendingSetupMenu.WIDTH - EDITOR_X - 4), EDITOR_X, VendingSetupMenu.GHOSTS_Y + 1, TEXT);
        if (machine.usesCatalog()) {
            wrap(gui, Component.translatable(Texts.SETUP_FROM_CATALOG, machine.catalogLabel()), EDITOR_X, VendingSetupMenu.GHOSTS_Y + 16, TEXT);
        } else if (!selection.isSetUp()) {
            wrap(gui, Component.translatable(Texts.SETUP_PICK_ITEM), EDITOR_X, VendingSetupMenu.GHOSTS_Y + 16, TEXT);
        } else {
            gui.text(font, Component.translatable(Texts.SETUP_QUANTITY), EDITOR_X, 73, TEXT);
            String count = Integer.toString(selection.quantity());
            gui.text(font, Component.literal(count), EDITOR_X + 70 - font.width(count) / 2, 73, TEXT);
            gui.text(font, Component.translatable(Texts.SETUP_PRICE), EDITOR_X, 92, TEXT);
            gui.item(new ItemStack(machine.currency().displayItem()), EDITOR_X + 84, 88);
        }
    }

    private void drawAdmin(Gui gui, VendingMachineBlockEntity machine) {
        boolean blocked = !machine.isInfinite() && !menu.stockAndCashEmpty();
        wrap(gui, Component.translatable(blocked ? Texts.SETUP_EMPTY_FIRST : Texts.SETUP_INFINITE_EXPLAINED), 8, 76, 96, blocked ? WARNING : TEXT);
        gui.text(font, Component.translatable(Texts.SETUP_CATALOG), 112, 57, TEXT);
        Component catalog = machine.usesCatalog() ? Component.literal(machine.catalogLabel()) : Component.translatable(Texts.SETUP_NO_CATALOG);
        FormattedCharSequence name = firstLine(catalog, 58);
        gui.text(font, name, 158 - font.width(name) / 2, 71, TEXT);
        gui.text(font, Component.translatable(Texts.SETUP_CURRENCY), 112, 90, TEXT);
        Component currency = machine.currencySlot() == null
                ? Component.translatable(Texts.SETUP_CURRENCY_DEFAULT, machine.currency().name())
                : new ItemStack(machine.currencySlot()).getHoverName();
        gui.text(font, firstLine(currency, 70), 132, 107, TEXT);
    }

    private FormattedCharSequence firstLine(Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    private void wrap(Gui gui, Component text, int x, int y, int color) {
        wrap(gui, text, x, y, VendingSetupMenu.WIDTH - x - 4, color);
    }

    private void wrap(Gui gui, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            gui.text(font, line, x, y, color);
            y += 10;
        }
    }

    // ---- the four methods that differ between versions -----------------------------------------------------------

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackground(new Gui(graphics));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabels(new Gui(graphics));
    }

    // While the price box is being typed in, letters and numbers are for it — not the inventory or hotbar keys.
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (price.isFocused() && !event.isEscape()) return price.keyPressed(event) || price.canConsumeInput() || super.keyPressed(event);
        return super.keyPressed(event);
    }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawBackground(new Gui(graphics));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabels(new Gui(graphics));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    // While the price box is being typed in, letters and numbers are for it — not the inventory or hotbar keys.
    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (price.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            return price.keyPressed(key, scanCode, modifiers) || price.canConsumeInput() || super.keyPressed(key, scanCode, modifiers);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    *///?}
}
```

- [ ] **Step 4: Register the screen on both loaders**

`platform/fabric/DiamondVendingFabricClient.java` — add imports `diamondvending.client.VendingSetupScreen`, `net.minecraft.client.gui.screens.MenuScreens`, and at the end of `onInitializeClient()`:

```java
        // Vanilla's registration, opened up by Fabric API's transitive access wideners on both versions.
        MenuScreens.register(ModContent.SETUP_MENU.type(), VendingSetupScreen::new);
```

`platform/neoforge/DiamondVendingNeoForgeClient.java` — add imports `diamondvending.client.VendingSetupScreen`, `net.neoforged.neoforge.client.event.RegisterMenuScreensEvent`, and in the constructor:

```java
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(ModContent.SETUP_MENU.type(), VendingSetupScreen::new));
```

- [ ] **Step 5: Look at it (visual GREEN)**

Run: `GCLIENT`, then Read every new screenshot (`setup_items`, `setup_stock`, `setup_cash_box`, `setup_admin`) and re-check the old ones.
Expected:
- the run passes (no `AssertionError` about the "+" button);
- `setup_items`: a gray panel with four tab buttons (Items greyed as the current tab; Stock, Cash Box, Admin), a red banner reading `TRAY FULL - TAKE YOUR ITEMS` in white, the 12 ghost slots as a grid of 3 columns × 4 rows with the items in their button positions — row 1: apple (2), oak log (4), diamond sword; row 2: bread in the middle; row 3: enchanted golden apple in the middle; row 4: cake on the right — a gold outline around button 1, and on the right "Button 1: Apple", "Amount  -  2  +", "Price [3]" with a diamond icon, and "Clear"; the player inventory below (empty — the test cleared it);
- `setup_stock`: the Stock label and a 3 × 9 grid whose first five slots hold the test machine's stock: 64 apples, 64 oak logs, a diamond sword, 64 bread, 3 enchanted golden apples;
- `setup_cash_box`: the Cash Box label, an empty 3 × 9 grid and a greyed-out "Withdraw all";
- `setup_admin`: "Infinite: OFF" with the explanation under it (the stock isn't empty, so it's greyed out and the red "Take everything out of Stock and Cash Box first."), "Catalog" with `<  None  >`, "Currency" with an empty slot and "Empty: diamonds";
- nothing overlaps or runs off the panel; the other screenshots are as in Plan 4 (the hover tooltip still shows, the item test machines' tags are unchanged).

If a control overlaps a slot or runs off the panel, move it (the coordinates are the constants and literals in `init()`/`drawLabels()`), re-run, and ledger the change.

- [ ] **Step 6: Build everything and document**

Run, one at a time: `G26N`, `G26F`, `G121N`, `G121F`
Expected: 106 / 106 / 105 / 105 pass; all four builds warning-free (the 1.21.1 screen compiles).

In `docs/dev-setup.md`, in the client game test section, change "It builds a stocked machine with commands, clicks it, and saves screenshots" to "It builds a stocked machine with commands, clicks it, opens its setup screen (pressing one of its buttons for real), and saves screenshots".

- [ ] **Step 7: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src docs/dev-setup.md
git -C /c/Users/benet/mcvending commit -m "feat: the setup screen"
```

---

### Task 6: "Empty both hands" — and wrap-up

Deliverable: an owner or admin who sneak-right-clicks their machine while holding something is told "Empty both hands, then sneak + right-click to open setup." and nothing is placed or used; anyone else sneaking with an item gets vanilla behaviour. Then the changelog, roadmap and PR.

**Files:**
- Create: `src/main/java/diamondvending/block/SneakHint.java`
- Modify: `platform/neoforge/DiamondVendingNeoForge.java`, `platform/fabric/DiamondVendingFabric.java`, `core/Texts.java`, `lang/en_us.json`
- Modify: `src/gametest/java/diamondvending/gametest/BuyingTests.java` (split `click`)
- Test: `ShopTests.java`, `FabricShopTests.java`, `NeoForgeShopTests.java`
- Modify: `CHANGELOG.md`, `docs/superpowers/plans/2026-09-23-roadmap.md`

**Interfaces:**
- Consumes: `MachineAccess.canManage`, `VendingMachineBlock.ownerOf`, `Messages.actionBar`, `RecordingServerPlayer` (Task 4).
- Produces: `block.SneakHint.cancels(Player, Level, BlockHitResult)`; `Texts.EMPTY_HANDS`; `BuyingTests.FrontHit` and `frontHit(GameTestHelper, double u, double v)`.

- [ ] **Step 1: Write the failing GameTests**

In `BuyingTests.java` split `click(helper, player, u, v)` in two:

```java
    /** Where a player looking at canvas point (u, v) on the front hits it: the part it's on (test-relative) and the hit. */
    record FrontHit(BlockPos part, BlockHitResult hit) {}

    static FrontHit frontHit(GameTestHelper helper, double u, double v) {
        boolean right = u >= 16;
        boolean upper = v < 16;
        BlockPos part = upper ? (right ? MachineTests.UPPER_RIGHT : MachineTests.UPPER_LEFT)
                : (right ? MachineTests.LOWER_RIGHT : MachineTests.MASTER);
        double faceU = (u - (right ? 16 : 0)) / 16;
        double faceV = (v - (upper ? 0 : 16)) / 16;
        BlockPos at = helper.absolutePos(part);
        // Facing north, the front is each block's z = 0 side, and u runs from east (x + 1) to west (x).
        Vec3 point = new Vec3(at.getX() + 1 - faceU, at.getY() + 1 - faceV, at.getZ());
        return new FrontHit(part, new BlockHitResult(point, Direction.NORTH, at, false));
    }

    /** Right-clicks canvas point (u, v) on the front, like a player looking at the machine. */
    static void click(GameTestHelper helper, Player player, double u, double v) {
        FrontHit at = frontHit(helper, u, v);
        helper.useBlock(at.part(), player, at.hit());
    }
```

In `ShopTests.java` add imports `diamondvending.core.Rect`, `net.minecraft.server.level.ServerPlayer`, `net.minecraft.world.level.block.Blocks`; add to `ALL`:

```java
            Map.entry("sneaking_with_an_item_shows_the_empty_hands_hint", ShopTests::sneakingWithAnItemShowsTheEmptyHandsHint),
            Map.entry("strangers_sneaking_with_blocks_place_them_as_usual", ShopTests::strangersSneakingWithBlocksPlaceThemAsUsual)
```

and:

```java
    // ---- the empty-hands hint (spec §3.2 rule 1) ----------------------------------------------------------------

    /** Right-clicks the front the way a real client's click arrives: through the server's click handling, loader events included. */
    static void useAsServer(GameTestHelper helper, ServerPlayer player, Rect region) {
        BuyingTests.FrontHit at = BuyingTests.frontHit(helper, region.centerU(), region.centerV());
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, at.hit());
    }

    public static void sneakingWithAnItemShowsTheEmptyHandsHint(GameTestHelper helper) {
        RecordingServerPlayer owner = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        BuyingTests.placeMachine(helper, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        owner.setShiftKeyDown(true);
        useAsServer(helper, owner, MachineLayout.TRAY);
        helper.assertTrue(owner.getMainHandItem().getCount() == 5, "nothing is used up");
        helper.assertTrue(helper.getBlockState(MachineTests.MASTER.north()).isAir(), "and nothing is placed against the machine");
        helper.assertTrue(owner.lastMessage() != null, "the owner should be told how to open setup");
        BuyingTests.translation(helper, owner.lastMessage(), Texts.EMPTY_HANDS);
        helper.succeed();
    }

    public static void strangersSneakingWithBlocksPlaceThemAsUsual(GameTestHelper helper) {
        BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingServerPlayer stranger = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        stranger.setShiftKeyDown(true);
        useAsServer(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(helper.getBlockState(MachineTests.MASTER.north()).is(Blocks.STONE), "a stranger's sneak-click places the block, like vanilla");
        helper.assertTrue(stranger.lastMessage() == null, "and tells them nothing");
        helper.succeed();
    }
```

Add both adapter methods to `FabricShopTests.java` and to the 1.21.1 block of `NeoForgeShopTests.java` (templates under "Adding a shop test"): `sneakingWithAnItemShowsTheEmptyHandsHint`, `strangersSneakingWithBlocksPlaceThemAsUsual`.

- [ ] **Step 2: Run to verify they fail**

Run: `G26N`
Expected: FAIL — compilation error: `Texts.EMPTY_HANDS` not found.

- [ ] **Step 3: Add the hint**

In `core/Texts.java` add after `NEED_MONEY`:

```java
    public static final String EMPTY_HANDS = "message.diamondvending.empty_hands";
```

and `EMPTY_HANDS` to the first `List.of(…)` in `all()`. In `en_us.json` after the `credit_full` message:

```json
  "message.diamondvending.empty_hands": "Empty both hands, then sneak + right-click to open setup.",
```

Run: `G26N`
Expected: FAIL — `sneaking_with_an_item_shows_the_empty_hands_hint`: "nothing is used up" (the stone is placed against the machine), and `strangers_sneaking_with_blocks_place_them_as_usual` passes.

`src/main/java/diamondvending/block/SneakHint.java`:

```java
package diamondvending.block;

import diamondvending.Messages;
import diamondvending.core.Texts;
import diamondvending.registry.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Spec §3.2 rule 1: the game skips the block when a player sneak-right-clicks it holding something, and places or uses
 * the item instead. From the machine's owner or an admin that's surely a try at opening setup, so the click is cancelled
 * and they're told how. Everyone else gets vanilla's behaviour. Called from each loader's "right-click a block" event,
 * on both sides (so the client doesn't place the block for a moment either).
 */
public final class SneakHint {
    private SneakHint() {}

    /** Whether to cancel this click; on the server, the player is told why. */
    public static boolean cancels(Player player, Level level, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return false;
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) return false;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.is(ModContent.VENDING_MACHINE.get())) return false;
        if (!MachineAccess.canManage(player, VendingMachineBlock.ownerOf(level, hit.getBlockPos(), state))) return false;
        if (!level.isClientSide()) Messages.actionBar(player, Component.translatable(Texts.EMPTY_HANDS));
        return true;
    }
}
```

`platform/neoforge/DiamondVendingNeoForge.java` — add imports `diamondvending.block.SneakHint`, `net.minecraft.world.InteractionResult`, `net.neoforged.neoforge.event.entity.player.PlayerInteractEvent`, and at the end of the constructor:

```java
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
            if (SneakHint.cancels(event.getEntity(), event.getLevel(), event.getHitVec())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
```

`platform/fabric/DiamondVendingFabric.java` — add imports `diamondvending.block.SneakHint`, `net.fabricmc.fabric.api.event.player.UseBlockCallback`, `net.minecraft.world.InteractionResult`, and at the end of `onInitialize()`:

```java
        // SUCCESS stops the click here; on the client it still goes to the server, which sends the hint.
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                SneakHint.cancels(player, level, hit) ? InteractionResult.SUCCESS : InteractionResult.PASS);
```

- [ ] **Step 4: Run to verify they pass**

Run: `G26N`
Expected: PASS — `All 108 required tests passed :)`.

Run, one at a time: `G26F`, `G121N`, `G121F`, then `GCLIENT`
Expected: 108 / 107 / 107 pass; the client run passes and its screenshots are as in Task 5.

- [ ] **Step 5: Changelog, roadmap, commit**

In `CHANGELOG.md`, under `## [Unreleased]` → `### Added`, append:

```markdown
- Owners and admins set up a machine on a setup screen — empty both hands, then sneak + right-click it. Items tab: click a slot while holding an item to sell it on that button, then set the amount and the price. Stock and Cash Box tabs (with Withdraw all), and an Admin tab for admins.
- The setup screen lists every problem in a red banner and puts a red "!" on the tab that fixes it.
- Admins can make a machine infinite (its Stock and Cash Box must be empty first), give it a datapack catalog, or set the currency it takes.
- Datapack catalogs, with an example (`diamondvending:example_snacks`) and a pack-maker guide in `docs/catalogs.md`.
- A broken machine keeps its setup on the item; it comes back infinite only when an admin places it.
- Sneaking at your own machine with something in your hand tells you to empty your hands first.
```

In `docs/superpowers/plans/2026-09-23-roadmap.md`, change Plan 5's row to start with `| **5 — Running a shop** ([plan](2026-09-23-plan-5-running-a-shop.md)) |` and its status cell to `Done`.

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src CHANGELOG.md docs
git -C /c/Users/benet/mcvending commit -m "feat: tell owners to empty their hands to open setup"
```

- [ ] **Step 6: Push and open the PR**

```bash
git -C /c/Users/benet/mcvending push -u origin plan-5/running-a-shop
```

Open a PR titled "Plan 5: Running a shop — setup screen, catalogs, currency, setup kept on the item" with the repo's PR template (the manual line: "rules this plan adds are in the spec §6.2 list the manual covers in Plan 6"), describe the Task 5 screenshots, and wait for CI to be green on all four targets.

