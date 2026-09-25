# Diamond Vending — Plan 6: Manual, Polish & v1.0 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players can craft a "Diamond Vending Manual" that explains every rule in words a young reader can follow; the last known rough edges are fixed; all four jars are checked in a real client against a real dedicated server; and v1.0.0 is published as a GitHub release with its jars.

**Architecture:** The manual is data, not code (spec §6.2): a shapeless recipe whose result is a vanilla `written_book` with a `written_book_content` component, each page a text component made of two translation keys (a bold title and its text) that live in `en_us.json`. `core/Texts` names the pages, so `TextsTest` checks every key has English text and a new `ManualTest` checks the English covers every rule in spec §6.2's list. GameTests craft the book through the real recipe manager; the Fabric client test measures every page with the game's font and screenshots it. Release QA runs each jar's dev client against the same jar's dedicated server, staged over RCON by small scripts in `tools/qa/`, so packets really cross the wire.

**Tech Stack:** as Plans 1–5. New: vanilla `CraftingInput`/`RecipeType.CRAFTING`, `WrittenBookContent`, `CriteriaTriggers.INVENTORY_CHANGED`, `BookViewScreen`; Minecraft RCON; Python 3 and Windows PowerShell for the QA scripts; `gh release`.

**Spec:** [`docs/superpowers/specs/2026-09-23-diamond-vending-design.md`](../specs/2026-09-23-diamond-vending-design.md) (§6.1, §6.2, §10 manual checklist) · **Roadmap:** [`2026-09-23-roadmap.md`](2026-09-23-roadmap.md) · **Previous:** [Plan 5](2026-09-23-plan-5-running-a-shop.md)

## Global Constraints

- Everything in Plans 1–5's Global Constraints still holds (mod id and package `diamondvending`, nodes, vcsVersion `26.1-neoforge`, no runtime deps, `core/` has no Minecraft imports, one Gradle node at a time, one shell command per Bash call, branch → PR → squash, write `Identifier`, player-facing keys live in `core/Texts` and `TextsTest` checks en_us.json, `//` comments only inside Stonecutter `//? if` blocks).
- Branch: `plan-6/manual-and-release` (already created; this plan is its first commit).
- If `java` isn't on PATH in the Bash tool, prefix Gradle with `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot"`. Every Minecraft API in this plan was checked on 2026-09-25 with `javap` against the cached 26.1.2 and 1.21.1 jars and `fabric-client-gametest-api-v1` 5.1.0.
- **The manual** (spec §6.2): a vanilla written book titled **"Diamond Vending Manual"**, author **"Diamond Vending HQ"**, crafted **shapeless from a book and a gold nugget**, unlocked in the recipe book together with the machine (a player's first diamond). Pages are translation keys; tone "welcome to the franchise", short sentences and simple words (the main readers are young children).
- **Page format differs by version** (checked in the bytecode): 1.21.1's `written_book_content` stores each page as a JSON *string* (`ComponentSerialization.flatCodec`), 26.1 as a text-component *object* (`flatRestrictedCodec` decodes with the normal component codec). So the recipe has one file per version, in `src/main/resources-1.21.1/` and `src/main/resources-26.1/` like the machine recipe. Ingredients differ the same way (`{"item": …}` vs a plain id).
- **A book page holds 14 lines** 114 px wide (`BookViewScreen.TEXT_WIDTH` 114, `TEXT_HEIGHT` 128, 9 px lines). The pages below were fitted with an estimate of Minecraft's glyph widths (every page ≤ 13 lines); the client test in Task 2 measures them with the real font.
- **Release QA** runs a dev client against a dedicated server of the same jar (spec §10: "dev client + dedicated server"). The QA server is local, offline-mode, flat and peaceful, has RCON on (password `qa`, local only), and its `eula.txt` says `eula=true` — running any Minecraft server means agreeing to the Minecraft EULA; approving this plan is that agreement.
- **Publishing** (Task 7) happens only after the PR is merged and only when the user says so: it creates the tag `v1.0.0` and a GitHub release in the (private) repo.

## Decisions this plan makes (for the reviewer)

- **11 pages** instead of spec §6.2's "about 8–10": the admin rules need two pages to stay in short sentences. The spec's page list is kept in order; "How customers buy" became two pages (buttons and payment; credit and the tray) and "For the bosses" became "Just for Admins" + "More Admin Tools" (a one-line title leaves room for the text).
- **Bold title, then the text on the next line** — no blank line after the title, so every page fits with room to spare.
- The approved sample page's 🎉 is dropped (Minecraft's font can't be relied on to draw emoji) and "Turn the page to learn how!" is dropped so page 1 fits.
- The book's **title and author stay English** in every language: a written book's title is a plain string in the game's data, not a translation key. The page text is translatable.
- The Plan 5 review's remaining minors that are cheap and player-facing are fixed here (Task 3); the rest go to `docs/backlog.md` (Task 6).

## Review Focus

1. **A real dedicated server with a separate client** — packets are actually encoded (the machine's update tag, the setup menu's open data, the `machine_setup` item component on a dropped/given item), unlike singleplayer's in-memory connection. → Task 4/5 QA run on all four jars.
2. **The 1.21.1 clients**, which no one has looked at yet: machine front, tags, display, setup screen, the manual's pages (1.21.1 stores pages as JSON strings). → Task 5 QA, plus `the_manual_recipe_makes_the_manual` decoding the 1.21.1 pages in GameTests.
3. **Manual pages overflowing** the 14-line book page, which would hide the rule at the bottom. → Task 2 client test measures every page with the game's font and fails if one needs more than 14 lines.
4. **A player's first diamond** must show both the machine and the manual in the recipe book (otherwise a young player never finds either). → Task 2 `a_first_diamond_unlocks_the_machine_and_the_manual`.
5. **The released jars** must be the mod only (no GameTest classes, no QA tools), say version 1.0.0 in their metadata, and there must be exactly one per loader × version. → Task 7 Step 3 checks each jar before upload.

---

## File Structure

```
src/main/java/diamondvending/
  core/Texts.java                          + MANUAL_PAGES, manualTitle(page), manualText(page); in all()
  menu/VendingSetupMenu.java               + canTakeItemForPickAll: double-click gathering skips hidden slots
  block/SneakHint.java                     spectators get no hint
  catalog/Catalogs.java                    a missing catalog sits just before "None" in the picker
src/main/resources/
  assets/diamondvending/lang/en_us.json    + 22 manual keys
  data/diamondvending/advancement/recipes/misc/manual.json   unlocks the manual with the first diamond
src/main/resources-26.1/data/diamondvending/recipe/manual.json     book + gold nugget → the manual (26.1 format)
src/main/resources-1.21.1/data/diamondvending/recipe/manual.json   the same, 1.21.1 format
src/test/java/diamondvending/core/
  TextsTest.java                           english() becomes package-visible (ManualTest reuses it)
  ManualTest.java                          spec §6.2 coverage rule: every rule's words are in the manual
src/gametest/java/diamondvending/gametest/
  MachineTests.java                        + the_manual_recipe_makes_the_manual, a_first_diamond_unlocks_…
  ShopTests.java                           + hidden-slot, spectator and missing-catalog tests
  fabric/FabricGameTests.java, fabric/FabricShopTests.java, neoforge/NeoForgeGameTests.java,
  neoforge/NeoForgeShopTests.java          adapters
  fabric/FabricClientTests.java            + the manual: every page fits, screenshots
build.neoforge.gradle.kts, build.fabric.gradle.kts   QA runs: server in its own folder, -Pdiamondvending.join
tools/qa/                                  prepare_server.py, rcon.py, scene.py, capture.ps1, input.ps1
docs/qa-checklist.md                       how to run release QA, and what each check should show
docs/dev-setup.md, docs/backlog.md, README.md, CHANGELOG.md, roadmap, stonecutter.properties.toml (1.0.0)
```

**Adding a GameTest** (unchanged from Plan 5): a `public static void` method + `ALL` entry in the common class, an adapter in `fabric/Fabric*Tests.java` and one in the 1.21.1 block of `neoforge/NeoForge*Tests.java`:

```java
    // FabricGameTests / FabricShopTests:
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void testName(GameTestHelper helper) {
        MachineTests.testName(helper);
    }

    // NeoForgeGameTests / NeoForgeShopTests (inside the `//? if <26.1 {` … `*///?}` block, so no comment markers of its own):

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void testName(GameTestHelper helper) {
        MachineTests.testName(helper);
    }
```

(For shop tests the scratchpad helper `add_shop_adapters.py <methodNames…>` from Plan 5 writes both adapters; if it's gone, write them by hand from the template above with `ShopTests.` in the body.)

**Test counts** ("All N required tests passed"): today 109 on 26.1, 108 on 1.21.1. Task 2 +2, Task 3 +4 → 115 / 114 at the end.

**Gradle commands** (prefix `JAVA_HOME=…` if needed; one at a time; log to the workspace and read the tail):
- `G26N` = `./gradlew :26.1-neoforge:test :26.1-neoforge:runGameTestServer`
- `G26F` = `./gradlew :26.1-fabric:test :26.1-fabric:runGametest`
- `G121N` = `./gradlew :1.21.1-neoforge:test :1.21.1-neoforge:runGameTestServer`
- `G121F` = `./gradlew :1.21.1-fabric:test :1.21.1-fabric:runGametest`
- `GCLIENT` = `./gradlew :26.1-fabric:runClientGametest` (opens a game window for a minute or two; screenshots in `versions/26.1-fabric/build/clientgametest/screenshots/`)

---

### Task 1: The manual's words

Deliverable: the manual's eleven pages exist as English text under keys that `core/Texts` names, and a unit test proves every rule in spec §6.2's list is explained in them.

**Files:**
- Modify: `src/main/java/diamondvending/core/Texts.java`
- Modify: `src/main/resources/assets/diamondvending/lang/en_us.json`
- Modify: `src/test/java/diamondvending/core/TextsTest.java` (`english()` package-visible)
- Create: `src/test/java/diamondvending/core/ManualTest.java`

**Interfaces:**
- Produces: `Texts.MANUAL_PAGES` (`List<String>`: `welcome, build, buy, credit, setup, stock, warnings, safe, moving, infinite, tools`, in page order), `Texts.manualTitle(String page)` → `"book.diamondvending.manual.<page>.title"`, `Texts.manualText(String page)` → `"book.diamondvending.manual.<page>.text"`. Tasks 2's recipe files and tests use exactly these keys.

- [ ] **Step 1: Write the failing test**

In `TextsTest.java`, change `private static Map<String, String> english()` to `static Map<String, String> english()` (package-visible, same body).

`src/test/java/diamondvending/core/ManualTest.java`:

```java
package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec §6.2's coverage rule: every rule a player could trip on is explained in the manual. Each rule in the spec's list
 * is pinned to the words that explain it; if a page stops saying them, or a new rule joins the list without a line in
 * the book, this fails.
 */
class ManualTest {
    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("buttons are numbered 1-12 and the price tag's number is the button", List.of("1 to 12", "price tag"));
        RULES.put("payment uses credit first, then the inventory, not shulker boxes or bundles",
                List.of("credit first", "shulker boxes or bundles"));
        RULES.put("credit is only yours, coin return gives it back, and there is a limit",
                List.of("Only you can spend it", "Coin return gives it back", "9 stacks"));
        RULES.put("anyone can take from the tray", List.of("Anyone can take it"));
        RULES.put("what each warning means and who fixes it", List.of("NOT SET UP YET: the owner", "SOLD OUT: the owner",
                "TRAY FULL: anyone", "CASH BOX FULL: the owner", "CATALOG MISSING: ask an admin"));
        RULES.put("to open setup, empty both hands, then sneak + right-click", List.of("Empty BOTH hands", "sneak and right-click"));
        RULES.put("only the owner or an admin can set up, dye or break the machine",
                List.of("Only you or an admin can set up, dye or break"));
        RULES.put("stock sells out; a full cash box stops sales until emptied", List.of("sold out", "stops selling", "Withdraw all"));
        RULES.put("breaking keeps the setup on the item, but contents pop out",
                List.of("keeps its items and prices", "Stock, Cash Box, tray and credit all pop out"));
        RULES.put("dye the machine by right-clicking it with a dye", List.of("Right-click it with any dye"));
        RULES.put("infinite machines never run out and the money vanishes", List.of("never run out", "money disappears"));
        RULES.put("only admins can change or break an infinite machine, even its owner can't",
                List.of("Only admins can change or break them", "Not even the owner"));
        RULES.put("switching to infinite needs empty Stock and Cash Box", List.of("empty Stock and Cash Box first"));
        RULES.put("an infinite machine stays infinite only when an admin places it", List.of("stays infinite only if an admin places it"));
        RULES.put("catalogs and the currency slot", List.of("pick a catalog", "Currency slot"));
    }

    @Test
    void everyRuleIsInTheManual() throws IOException {
        String manual = manual();
        RULES.forEach((rule, words) -> {
            for (String phrase : words) {
                assertTrue(manual.contains(phrase), "spec §6.2: the manual must explain that " + rule
                        + ", but no page says \"" + phrase + "\"");
            }
        });
    }

    /** Every page's English title and text, in page order. */
    private static String manual() throws IOException {
        Map<String, String> english = TextsTest.english();
        StringBuilder text = new StringBuilder();
        for (String page : Texts.MANUAL_PAGES) {
            text.append(unescape(english.getOrDefault(Texts.manualTitle(page), ""))).append('\n');
            text.append(unescape(english.getOrDefault(Texts.manualText(page), ""))).append('\n');
        }
        return text.toString();
    }

    /** en_us.json values as TextsTest reads them still hold JSON escapes such as \n; the manual's words don't. */
    private static String unescape(String json) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                out.append(next == 'n' ? '\n' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :26.1-neoforge:test`
Expected: FAIL — compilation error: `cannot find symbol … MANUAL_PAGES` (and `manualTitle`/`manualText`).

- [ ] **Step 3: Name the pages in Texts**

In `core/Texts.java`, after the `SETUP_CURRENCY_DEFAULT` constant:

```java
    // The manual (spec §6.2): one entry per page, in order; each page is a bold title and its text.
    public static final List<String> MANUAL_PAGES = List.of("welcome", "build", "buy", "credit", "setup", "stock", "warnings",
            "safe", "moving", "infinite", "tools");
```

after `flash(Flash)`:

```java
    /** A manual page's title, e.g. "Welcome, New Franchise Owner!". */
    public static String manualTitle(String page) {
        return "book.diamondvending.manual." + page + ".title";
    }

    /** A manual page's text. */
    public static String manualText(String page) {
        return "book.diamondvending.manual." + page + ".text";
    }
```

and in `all()`, before `return keys;`:

```java
        for (String page : MANUAL_PAGES) {
            keys.add(manualTitle(page));
            keys.add(manualText(page));
        }
```

Run: `./gradlew :26.1-neoforge:test`
Expected: FAIL — `TextsTest`: `en_us.json has no text for book.diamondvending.manual.welcome.title`; `ManualTest`: `spec §6.2: the manual must explain that buttons are numbered 1-12 … but no page says "1 to 12"`.

- [ ] **Step 4: Write the pages**

In `en_us.json`, after the last `setup.diamondvending.*` line (`"setup.diamondvending.currency_default": "Empty: %s",`), insert:

```json
  "book.diamondvending.manual.welcome.title": "Welcome, New Franchise Owner!",
  "book.diamondvending.manual.welcome.text": "You did it! You own a Diamond Vending machine!\n\nPeople will come to buy your snacks. Well... your stuff.\n\nYour job is easy: Fill it. Price it. Get diamonds!",
  "book.diamondvending.manual.build.title": "Build Your Machine",
  "book.diamondvending.manual.build.text": "Craft it from 6 iron blocks, a glass pane, redstone and a diamond.\n\nIt is big: 2 blocks wide and 2 blocks tall. Make sure there is room!\n\nYou are the owner now.",
  "book.diamondvending.manual.buy.title": "How People Buy",
  "book.diamondvending.manual.buy.text": "Each item has a button, 1 to 12. The number on the price tag is the button to press.\n\nThe machine takes your credit first, then diamonds from your inventory. Not from shulker boxes or bundles!",
  "book.diamondvending.manual.credit.title": "Credit and the Tray",
  "book.diamondvending.manual.credit.text": "Right-click the coin slot with diamonds. That is your credit. Only you can spend it. Up to 9 stacks fit. Coin return gives it back.\n\nYour item drops in the tray. Anyone can take it. Grab it fast!",
  "book.diamondvending.manual.setup.title": "Set Up Your Shop",
  "book.diamondvending.manual.setup.text": "Empty BOTH hands, then sneak and right-click your machine.\n\nItems tab: hold an item and click a slot on the left. That button sells it. You keep it!\n\nSet the amount and price. 0 is free.",
  "book.diamondvending.manual.stock.title": "Stock and Cash Box",
  "book.diamondvending.manual.stock.text": "Put what you sell in the Stock tab. When it runs out, that button is sold out.\n\nThe diamonds people pay go in the Cash Box. When it is full, the machine stops selling. Press Withdraw all!",
  "book.diamondvending.manual.warnings.title": "Uh-oh! Red Warnings",
  "book.diamondvending.manual.warnings.text": "NOT SET UP YET: the owner sets it up.\nSOLD OUT: the owner adds Stock.\nTRAY FULL: anyone can take the items.\nCASH BOX FULL: the owner empties it.\nCATALOG MISSING: ask an admin.",
  "book.diamondvending.manual.safe.title": "Keep It Safe and Pretty",
  "book.diamondvending.manual.safe.text": "Only you or an admin can set up, dye or break your machine. Everyone can still buy from it.\n\nWant a new color? Right-click it with any dye!",
  "book.diamondvending.manual.moving.title": "Moving Your Machine",
  "book.diamondvending.manual.moving.text": "Break it with a pickaxe. It keeps its items and prices, so it is ready when you place it again.\n\nBut the Stock, Cash Box, tray and credit all pop out. Pick them up!",
  "book.diamondvending.manual.infinite.title": "Just for Admins",
  "book.diamondvending.manual.infinite.text": "Infinite machines never run out. Their money disappears.\n\nOnly admins can change or break them. Not even the owner can!\n\nTo make one, empty Stock and Cash Box first.",
  "book.diamondvending.manual.tools.title": "More Admin Tools",
  "book.diamondvending.manual.tools.text": "An infinite machine stays infinite only if an admin places it.\n\nAdmins can pick a catalog: a ready-made shop from a datapack.\n\nOr put an item in the Currency slot. Then the machine takes that, not diamonds.",
```

Every sentence was checked against the code: the recipe (`BGB/BRB/BDB`), the 2×2 size, credit (`CoinSlot.insert` loads the whole held stack, up to 9 stacks), the problem display names (`problem.*.display`), the Items tab's ghost slots (they copy and never take), Withdraw all, the kept setup and the drops on break (spec §5.3–5.4), and the admin rules (spec §5.2, §5.4, §4).

- [ ] **Step 5: Run it to verify it passes**

Run: `./gradlew :26.1-neoforge:test`
Expected: PASS — `TextsTest` and `ManualTest` green, every other unit test still green, no compiler warnings.

- [ ] **Step 6: Commit**

```bash
git -C /c/Users/benet/mcvending add src/main/java/diamondvending/core/Texts.java src/main/resources/assets/diamondvending/lang/en_us.json src/test/java/diamondvending/core/TextsTest.java src/test/java/diamondvending/core/ManualTest.java
git -C /c/Users/benet/mcvending commit -m "feat: the manual's pages"
```

---

### Task 2: The manual book

Deliverable: a book and a gold nugget craft the manual on all four jars; a player's first diamond puts it (and the machine) in their recipe book; every page fits a book page and reads well in a real game window.

**Files:**
- Create: `src/main/resources-26.1/data/diamondvending/recipe/manual.json`
- Create: `src/main/resources-1.21.1/data/diamondvending/recipe/manual.json`
- Create: `src/main/resources/data/diamondvending/advancement/recipes/misc/manual.json`
- Modify: `src/gametest/java/diamondvending/gametest/MachineTests.java`, `fabric/FabricGameTests.java`, `neoforge/NeoForgeGameTests.java`
- Modify: `src/gametest/java/diamondvending/gametest/fabric/FabricClientTests.java`
- Modify: `docs/dev-setup.md`

**Interfaces:**
- Consumes: `Texts.MANUAL_PAGES`, `Texts.manualTitle`, `Texts.manualText` (Task 1); `RecordingServerPlayer.create(GameTestHelper, GameType)` (Plan 5).
- Produces: recipe `diamondvending:manual`; advancement `diamondvending:recipes/misc/manual`.

- [ ] **Step 1: Write the failing GameTests**

In `MachineTests.java` add to `ALL`, after the `machine_recipe_loads` entry:

```java
            Map.entry("the_manual_recipe_makes_the_manual", MachineTests::theManualRecipeMakesTheManual),
            Map.entry("a_first_diamond_unlocks_the_machine_and_the_manual", MachineTests::aFirstDiamondUnlocksTheMachineAndTheManual),
```

add imports (outside the `//? if` blocks; skip any already there): `diamondvending.core.Texts`, `net.minecraft.advancements.CriteriaTriggers`, `net.minecraft.core.component.DataComponents`, `net.minecraft.network.chat.Component`, `net.minecraft.network.chat.contents.TranslatableContents`, `net.minecraft.world.item.component.WrittenBookContent`, `net.minecraft.world.item.crafting.CraftingInput`, `net.minecraft.world.item.crafting.CraftingRecipe`, `net.minecraft.world.item.crafting.RecipeHolder`, `net.minecraft.world.item.crafting.RecipeType`, `java.util.ArrayList`, `java.util.Optional`. (`Registries` and `ResourceKey` are already imported inside the file's `//? if >=26.1` block.)

and after `machineRecipeLoads`:

```java
    /** Spec §6.2: a book and a gold nugget craft the manual — a written book whose pages are the manual's lang keys. */
    public static void theManualRecipeMakesTheManual(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_NUGGET)));
        //? if >=26.1 {
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        ItemStack book = recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        //?} else {
        /*Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        ItemStack book = recipe.map(holder -> holder.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        *///?}
        helper.assertTrue(book.is(Items.WRITTEN_BOOK), "a book and a gold nugget should craft the manual, got " + book);
        WrittenBookContent content = book.get(DataComponents.WRITTEN_BOOK_CONTENT);
        helper.assertTrue(content != null && content.title().raw().equals("Diamond Vending Manual")
                && content.author().equals("Diamond Vending HQ"), "the manual's title and author, got " + content);
        List<Component> pages = content.getPages(false);
        helper.assertTrue(pages.size() == Texts.MANUAL_PAGES.size(),
                "the manual should have " + Texts.MANUAL_PAGES.size() + " pages, it has " + pages.size());
        for (int i = 0; i < pages.size(); i++) {
            String page = Texts.MANUAL_PAGES.get(i);
            List<String> keys = translationKeys(pages.get(i));
            helper.assertTrue(keys.equals(List.of(Texts.manualTitle(page), Texts.manualText(page))),
                    "page " + (i + 1) + " should be the " + page + " page, it shows " + keys);
        }
        helper.succeed();
    }

    /** Every translation key in a text component, in reading order. */
    private static List<String> translationKeys(Component component) {
        List<String> keys = new ArrayList<>();
        if (component.getContents() instanceof TranslatableContents translatable) keys.add(translatable.getKey());
        for (Component sibling : component.getSiblings()) keys.addAll(translationKeys(sibling));
        return keys;
    }

    /** Spec §6.1–6.2: a player's first diamond puts both the machine and the manual in their recipe book. */
    public static void aFirstDiamondUnlocksTheMachineAndTheManual(GameTestHelper helper) {
        RecordingServerPlayer player = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        player.getInventory().add(diamond.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), diamond);
        for (String recipe : List.of("vending_machine", "manual")) {
            //? if >=26.1 {
            boolean unlocked = player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, DiamondVending.id(recipe)));
            //?} else {
            /*boolean unlocked = player.getRecipeBook().contains(DiamondVending.id(recipe));
            *///?}
            helper.assertTrue(unlocked, "a first diamond should unlock the " + recipe + " recipe");
        }
        helper.succeed();
    }
```

Add both adapter methods (`theManualRecipeMakesTheManual`, `aFirstDiamondUnlocksTheMachineAndTheManual`) to `FabricGameTests.java` and to the 1.21.1 block of `NeoForgeGameTests.java`, from the template above, calling `MachineTests.…`.

- [ ] **Step 2: Run them to verify they fail**

Run: `G26N`
Expected: FAIL — `the_manual_recipe_makes_the_manual`: "a book and a gold nugget should craft the manual, got 0 minecraft:air"; `a_first_diamond_unlocks_the_machine_and_the_manual`: "a first diamond should unlock the manual recipe" (the machine part passes — if it fails on `vending_machine`, the machine's unlock never worked: debug that first, it's the same advancement shape). The other 109 pass.

- [ ] **Step 3: Add the recipes and the unlock**

`src/main/resources-26.1/data/diamondvending/recipe/manual.json`:

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": ["minecraft:book", "minecraft:gold_nugget"],
  "result": {
    "id": "minecraft:written_book",
    "components": {
      "minecraft:written_book_content": {
        "title": "Diamond Vending Manual",
        "author": "Diamond Vending HQ",
        "resolved": true,
        "pages": [
          ["", {"translate": "book.diamondvending.manual.welcome.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.welcome.text"}],
          ["", {"translate": "book.diamondvending.manual.build.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.build.text"}],
          ["", {"translate": "book.diamondvending.manual.buy.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.buy.text"}],
          ["", {"translate": "book.diamondvending.manual.credit.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.credit.text"}],
          ["", {"translate": "book.diamondvending.manual.setup.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.setup.text"}],
          ["", {"translate": "book.diamondvending.manual.stock.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.stock.text"}],
          ["", {"translate": "book.diamondvending.manual.warnings.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.warnings.text"}],
          ["", {"translate": "book.diamondvending.manual.safe.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.safe.text"}],
          ["", {"translate": "book.diamondvending.manual.moving.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.moving.text"}],
          ["", {"translate": "book.diamondvending.manual.infinite.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.infinite.text"}],
          ["", {"translate": "book.diamondvending.manual.tools.title", "bold": true}, "\n", {"translate": "book.diamondvending.manual.tools.text"}]
        ]
      }
    }
  }
}
```

`src/main/resources-1.21.1/data/diamondvending/recipe/manual.json` (each page is the same component, written as a JSON string):

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [{ "item": "minecraft:book" }, { "item": "minecraft:gold_nugget" }],
  "result": {
    "id": "minecraft:written_book",
    "count": 1,
    "components": {
      "minecraft:written_book_content": {
        "title": "Diamond Vending Manual",
        "author": "Diamond Vending HQ",
        "resolved": true,
        "pages": [
          "[\"\",{\"translate\":\"book.diamondvending.manual.welcome.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.welcome.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.build.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.build.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.buy.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.buy.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.credit.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.credit.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.setup.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.setup.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.stock.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.stock.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.warnings.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.warnings.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.safe.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.safe.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.moving.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.moving.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.infinite.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.infinite.text\"}]",
          "[\"\",{\"translate\":\"book.diamondvending.manual.tools.title\",\"bold\":true},\"\\n\",{\"translate\":\"book.diamondvending.manual.tools.text\"}]"
        ]
      }
    }
  }
}
```

`src/main/resources/data/diamondvending/advancement/recipes/misc/manual.json` (the same shape as `vending_machine.json`, so both unlock with the first diamond):

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
        "recipe": "diamondvending:manual"
      },
      "trigger": "minecraft:recipe_unlocked"
    }
  },
  "requirements": [
    ["has_the_recipe", "has_diamond"]
  ],
  "rewards": {
    "recipes": ["diamondvending:manual"]
  }
}
```

- [ ] **Step 4: Run them to verify they pass, on every node**

Run, one at a time: `G26N`, `G26F`, `G121N`, `G121F`
Expected: 111 / 111 / 110 / 110 pass, no recipe or advancement parse errors in the logs (grep the log for `manual`), builds warning-free. The 1.21.1 runs are the ones that prove the JSON-string pages decode.

- [ ] **Step 5: Add the manual to the client test (RED)**

In `FabricClientTests.java` add imports inside the `//? if >=26.1 {` block (skip any already there): `diamondvending.core.Texts`, `net.minecraft.client.gui.screens.inventory.BookViewScreen`, `net.minecraft.core.component.DataComponents`, `net.minecraft.network.chat.Component`, `net.minecraft.world.InteractionHand`, `net.minecraft.world.item.ItemStack`, `net.minecraft.world.item.Items`, `net.minecraft.world.item.crafting.CraftingInput`, `net.minecraft.world.item.crafting.RecipeType`, `java.util.List`.

After the setup-screen part (right after `server.runCommand("gamemode survival @a");`) and before `// Behind the machine`, add:

```java
            // The manual (spec §6.2): crafted by its real recipe, every page fits a book page, and a person can read it.
            server.runOnServer(s -> {
                CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_NUGGET)));
                ItemStack manual = s.overworld().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, s.overworld())
                        .orElseThrow(() -> new AssertionError("a book and a gold nugget should craft the manual")).value().assemble(input);
                s.getPlayerList().getPlayers().getFirst().setItemInHand(InteractionHand.MAIN_HAND, manual);
            });
            server.runCommand("tp @a 1 -60 6 0 -45"); // facing away from the machine, at the sky, so right-click reads the book
            context.waitTicks(5);
            context.runOnClient(client -> {
                List<Component> pages = client.player.getMainHandItem().get(DataComponents.WRITTEN_BOOK_CONTENT).getPages(false);
                for (int i = 0; i < pages.size(); i++) {
                    // BookViewScreen: 114 px wide, 128 px tall = 14 lines of 9 px
                    int lines = client.font.split(pages.get(i), 114).size();
                    if (lines > 14) throw new AssertionError("manual page " + (i + 1) + " needs " + lines + " lines; a book page shows 14");
                }
            });
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitForScreen(BookViewScreen.class);
            for (int page = 1; page <= Texts.MANUAL_PAGES.size(); page++) {
                context.waitTicks(2);
                context.takeScreenshot("manual_" + page);
                context.getInput().pressKey(GLFW.GLFW_KEY_PAGE_DOWN);
            }
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            context.waitTicks(3);
            server.runCommand("clear @a");
```

To see it fail first, temporarily move `src/main/resources-26.1/data/diamondvending/recipe/manual.json` out of the tree (to the workspace), run `GCLIENT`, then move it back.
Expected (without the recipe): FAIL — `AssertionError: a book and a gold nugget should craft the manual`.

- [ ] **Step 6: Run the client test and read the manual**

Run: `GCLIENT`
Expected: PASS (no "needs N lines" error). Read every `manual_1` … `manual_11` screenshot:
- the book is open, page indicator "Page N of 11", a bold title on the first line, the text below it, nothing cut off at the bottom;
- page 1 "Welcome, New Franchise Owner!" … page 11 "More Admin Tools", in the order of `Texts.MANUAL_PAGES`;
- the old screenshots (`machine_front`, `setup_*`, `close_up`, …) are unchanged.

If a page is too long or reads badly, shorten its English text (keep every phrase `ManualTest` pins, or update the pin in the same change), re-run `./gradlew :26.1-neoforge:test` and `GCLIENT`, and ledger the wording change.

- [ ] **Step 7: Document**

In `docs/dev-setup.md`, client game test section, change "opens its setup screen (pressing one of its buttons for real), and saves screenshots" to "opens its setup screen (pressing one of its buttons for real), reads the manual (failing if a page is too long for the book), and saves screenshots", and add after that paragraph:

```markdown
The manual's pages are translation keys (`Texts.MANUAL_PAGES`) in `en_us.json`, and the recipe that makes the book is
per version (`src/main/resources-*/data/diamondvending/recipe/manual.json`: 1.21.1 writes each page as a JSON string).
After changing a page's text, run `./gradlew :26.1-neoforge:test` (`ManualTest` checks every rule in spec §6.2 is still
explained) and this client test (every page must fit the book's 14 lines).
```

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

- [ ] **Step 8: Commit**

```bash
git -C /c/Users/benet/mcvending add -A src docs/dev-setup.md
git -C /c/Users/benet/mcvending commit -m "feat: craft the Diamond Vending Manual"
```

---

### Task 3: Polish — hidden tabs, spectators, a missing catalog

Deliverable: the three cheap, player-facing minors left by Plan 5's review are fixed: double-click gathering can't pull items from a hidden tab's Stock or Cash Box; spectators get no empty-hands hint; and from a missing catalog, one press of `▶` picks "None". Dragging and number keys over hidden slots get tests too (they were already safe).

**Files:**
- Modify: `src/main/java/diamondvending/menu/VendingSetupMenu.java`
- Modify: `src/main/java/diamondvending/block/SneakHint.java`
- Modify: `src/main/java/diamondvending/catalog/Catalogs.java`
- Modify: `src/gametest/java/diamondvending/gametest/ShopTests.java`, `fabric/FabricShopTests.java`, `neoforge/NeoForgeShopTests.java`

**Interfaces:**
- Consumes: `ShopTests.setupMenu`, `appleMachineOwnedBy`, `useAsServer`, `BuyingTests.placeMachine`, `BuyingTests.countIn`, `RecordingServerPlayer` (Plan 5).
- Produces: nothing new for other tasks.

- [ ] **Step 1: Write the failing GameTests**

In `ShopTests.java` add the import `net.minecraft.world.inventory.AbstractContainerMenu`, add to `ALL`:

```java
            Map.entry("double_click_gathering_skips_hidden_slots", ShopTests::doubleClickGatheringSkipsHiddenSlots),
            Map.entry("dragging_and_number_keys_skip_hidden_slots", ShopTests::draggingAndNumberKeysSkipHiddenSlots),
            Map.entry("spectators_get_no_empty_hands_hint", ShopTests::spectatorsGetNoEmptyHandsHint),
            Map.entry("a_missing_catalog_is_one_press_from_none", ShopTests::aMissingCatalogIsOnePressFromNone)
```

(the last existing entry gains a comma; the new last entry ends `));`), and at the end of the class:

```java
    // ---- hidden tabs, spectators and a missing catalog (Plan 5 review, fixed in Plan 6) --------------------------

    /** A double-click on a menu slot while holding something: gathers matching items from the other slots. */
    static void pickAll(VendingSetupMenu menu, int slot, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, 0, ContainerInput.PICKUP_ALL, player);
        //?} else {
        /*menu.clicked(slot, 0, ClickType.PICKUP_ALL, player);
        *///?}
    }

    /** Number key {@code hotbar + 1} over a menu slot: swaps it with that hotbar slot. */
    static void swapWithHotbar(VendingSetupMenu menu, int slot, int hotbar, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, hotbar, ContainerInput.SWAP, player);
        //?} else {
        /*menu.clicked(slot, hotbar, ClickType.SWAP, player);
        *///?}
    }

    /** A left-button drag of the cursor's stack over these slots (spread evenly). */
    static void drag(VendingSetupMenu menu, Player player, int... slots) {
        //? if >=26.1 {
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ContainerInput.QUICK_CRAFT, player);
        for (int slot : slots) menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ContainerInput.QUICK_CRAFT, player);
        //?} else {
        /*menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, player);
        for (int slot : slots) menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, player);
        *///?}
    }

    /** Spec §4: slots on hidden tabs are inactive — double-click gathering on the Items tab must leave the Stock alone. */
    public static void doubleClickGatheringSkipsHiddenSlots(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        owner.getInventory().setItem(9, new ItemStack(Items.APPLE, 5)); // menu slot FIRST_PLAYER
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.APPLE));
        pickAll(menu, VendingSetupMenu.FIRST_PLAYER + 1, owner);
        int stocked = BuyingTests.countIn(machine.stock(), Items.APPLE);
        helper.assertTrue(stocked == 10, "the hidden Stock keeps its 10 apples, it has " + stocked);
        helper.assertTrue(menu.getCarried().getCount() == 6, "the cursor gathers the inventory's 5 apples, it holds " + menu.getCarried().getCount());
        helper.succeed();
    }

    /** The same for dragging a stack and for the number keys (already safe: the menu ignores clicks on hidden slots). */
    public static void draggingAndNumberKeysSkipHiddenSlots(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.APPLE, 4));
        drag(menu, owner, VendingSetupMenu.FIRST_STOCK, VendingSetupMenu.FIRST_PLAYER + 1);
        helper.assertTrue(ItemSlots.isEmpty(machine.stock()), "a drag across the hidden Stock puts nothing in it");
        helper.assertTrue(owner.getInventory().getItem(10).getCount() == 4, "the whole stack lands in the inventory slot");
        machine.stock().set(0, new ItemStack(Items.BREAD, 10));
        owner.getInventory().setItem(0, new ItemStack(Items.APPLE, 5));
        swapWithHotbar(menu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(machine.stock().get(0).is(Items.BREAD) && owner.getInventory().getItem(0).is(Items.APPLE),
                "number key 1 over the hidden Stock swaps nothing");
        helper.succeed();
    }

    /** Spec §3.2 rule 1's hint is for a sneak-click that would place or use something; a spectator's can't, so no hint. */
    public static void spectatorsGetNoEmptyHandsHint(GameTestHelper helper) {
        RecordingServerPlayer owner = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        BuyingTests.placeMachine(helper, owner);
        owner.setGameMode(GameType.SPECTATOR);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        owner.setShiftKeyDown(true);
        useAsServer(helper, owner, MachineLayout.TRAY);
        helper.assertTrue(owner.lastMessage() == null, "a spectator's sneak-click should say nothing, got " + owner.lastMessage());
        helper.succeed();
    }

    /** Spec §5.1: with CATALOG MISSING an admin picks another catalog or clears it — ▶ clears it in one press. */
    public static void aMissingCatalogIsOnePressFromNone(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        VendingSetupMenu menu = setupMenu(admin, machine);
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(1));
        helper.assertTrue(machine.catalogId() == null, "▶ from a missing catalog should pick None, got " + machine.catalogId());
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(-1));
        helper.assertTrue(Catalogs.ids().getLast().equals(machine.catalogId()),
                "◀ from a missing catalog should pick the last catalog, got " + machine.catalogId());
        helper.succeed();
    }
```

Add the four adapters to `FabricShopTests.java` and the 1.21.1 block of `NeoForgeShopTests.java` (`python <scratchpad>/add_shop_adapters.py doubleClickGatheringSkipsHiddenSlots draggingAndNumberKeysSkipHiddenSlots spectatorsGetNoEmptyHandsHint aMissingCatalogIsOnePressFromNone`, or by hand from the template).

- [ ] **Step 2: Run them to verify they fail**

Run: `G26N`
Expected: 3 failures —
- `double_click_gathering_skips_hidden_slots`: "the hidden Stock keeps its 10 apples, it has 0";
- `spectators_get_no_empty_hands_hint`: "a spectator's sneak-click should say nothing, got …empty_hands…";
- `a_missing_catalog_is_one_press_from_none`: "▶ from a missing catalog should pick None, got diamondvending:example_snacks".

`dragging_and_number_keys_skip_hidden_slots` passes already — it pins behaviour the menu has had since Plan 5 (`handledHere` ignores clicks on inactive slots), which the review found untested. If the spectator test passes on 26.1-neoforge, the loader doesn't fire its right-click event for spectators; keep the test (it guards the other loaders) and ledger it.

- [ ] **Step 3: Fix all three**

`menu/VendingSetupMenu.java`, after `quickMoveStack`:

```java
    /** Double-click gathering takes from shown slots only — never from a hidden tab's Stock or Cash Box (spec §4). */
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(carried, slot);
    }
```

`block/SneakHint.java`, first line of `cancels`:

```java
        if (player.isSpectator()) return false; // spectators can't place or use anything, so there's nothing to warn about
```

`catalog/Catalogs.java`, replace `cycle` and its Javadoc:

```java
    /**
     * The Admin tab's picker (spec §4): "None" (null), then every catalog in order, and round again. {@code step} is
     * +1 or −1. A catalog that isn't loaded any more sits just before "None", so ▶ clears it in one press (spec §5.1:
     * an admin picks another catalog or clears it) and ◀ picks the last catalog.
     */
    public static Identifier cycle(Identifier current, int step) {
        List<Identifier> choices = new ArrayList<>();
        choices.add(null);
        choices.addAll(ids());
        if (current != null && !choices.contains(current)) return step > 0 ? null : choices.getLast();
        return choices.get(Math.floorMod(choices.indexOf(current) + step, choices.size()));
    }
```

- [ ] **Step 4: Run to verify they pass, on every node**

Run, one at a time: `G26N`, `G26F`, `G121N`, `G121F`
Expected: 115 / 115 / 114 / 114 pass (including `the_catalog_picker_cycles_through_every_catalog` from Plan 5); builds warning-free.

- [ ] **Step 5: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add -A src
git -C /c/Users/benet/mcvending commit -m "fix: hidden tabs, spectators and a missing catalog in setup"
```

---

### Task 4: Release QA tooling

Deliverable: a documented, repeatable way to run any jar's dev client against the same jar's dedicated server, stage scenes over RCON and capture the game window — proven by a full QA pass on 26.1-neoforge.

**Files:**
- Modify: `build.neoforge.gradle.kts`, `build.fabric.gradle.kts` (runs)
- Create: `tools/qa/prepare_server.py`, `tools/qa/rcon.py`, `tools/qa/scene.py`, `tools/qa/capture.ps1`, `tools/qa/input.ps1`
- Create: `docs/qa-checklist.md`
- Modify: `docs/dev-setup.md`

**Interfaces:**
- Produces: `./gradlew :<node>:runServer` runs in `run/<node>-server/` (1 GB heap); `./gradlew :<node>:runClient -Pdiamondvending.join=localhost` joins that server as player `Dev` on every loader; the five scripts and the checklist, used by Task 5.

- [ ] **Step 1: Give the server its own folder and let the client join it**

`build.neoforge.gradle.kts`, in `neoForge { runs { … } }`, replace the `client` and `server` registrations:

```kotlin
        register("client") {
            gameDirectory = file("../../run/${sc.current.project}")
            client()
            // Release QA (docs/qa-checklist.md): -Pdiamondvending.join=localhost joins that server at once
            providers.gradleProperty("diamondvending.join").orNull?.let { programArguments.addAll("--quickPlayMultiplayer", it) }
        }
        register("server") {
            // Its own folder, so a client and a server can run at the same time without sharing logs
            gameDirectory = file("../../run/${sc.current.project}-server")
            server()
            programArgument("--nogui")
            jvmArgument("-Xmx1G")
        }
```

`build.fabric.gradle.kts`, first thing inside `loom { runs { … } }`:

```kotlin
        // Release QA (docs/qa-checklist.md): the dev player is "Dev" as on NeoForge, and
        // -Pdiamondvending.join=localhost joins that server at once
        named("client") {
            programArgs("--username", "Dev")
            providers.gradleProperty("diamondvending.join").orNull?.let { programArgs("--quickPlayMultiplayer", it) }
        }
        // Its own folder, so a client and a server can run at the same time without sharing logs
        named("server") {
            runDirectory = rootProject.file("run/${sc.current.project}-server")
            vmArg("-Xmx1G")
        }
```

Run: `./gradlew :26.1-neoforge:tasks --all` and `./gradlew :26.1-fabric:tasks --all` (one at a time)
Expected: both configure without errors and list `runServer` and `runClient`.

- [ ] **Step 2: Write the QA scripts**

`tools/qa/prepare_server.py`:

```python
"""Prepares run/<node>-server for release QA (docs/qa-checklist.md): a fresh flat, peaceful, offline-mode world with
RCON on for tools/qa/rcon.py. Writing eula=true means agreeing to the Minecraft EULA (https://aka.ms/MinecraftEULA).

Usage: python tools/qa/prepare_server.py 26.1-neoforge
"""
import pathlib
import shutil
import sys

PROPERTIES = {
    "online-mode": "false",  # the dev clients have no Microsoft account
    "enable-rcon": "true",
    "rcon.port": "25575",
    "rcon.password": "qa",
    "level-type": "minecraft\\:flat",
    "generate-structures": "false",
    "difficulty": "peaceful",
    "spawn-protection": "0",
    "view-distance": "6",
    "simulation-distance": "6",
    "motd": "Diamond Vending QA",
}

node = sys.argv[1]
server = pathlib.Path(__file__).resolve().parents[2] / "run" / f"{node}-server"
server.mkdir(parents=True, exist_ok=True)
shutil.rmtree(server / "world", ignore_errors=True)  # every QA run starts from a new world
(server / "eula.txt").write_text("eula=true\n", encoding="utf-8")
(server / "server.properties").write_text("".join(f"{key}={value}\n" for key, value in PROPERTIES.items()), encoding="utf-8")
print(f"prepared {server}")
```

`tools/qa/rcon.py`:

```python
"""Sends commands to the release-QA server over RCON and prints the replies (docs/qa-checklist.md).

Usage: python tools/qa/rcon.py "time set noon" "give Dev minecraft:diamond 10"
"""
import socket
import struct
import sys

HOST, PORT, PASSWORD = "localhost", 25575, "qa"  # as tools/qa/prepare_server.py sets them


def _packet(request_id, kind, body):
    payload = struct.pack("<ii", request_id, kind) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(payload)) + payload


def _read_exactly(sock, size):
    data = b""
    while len(data) < size:
        chunk = sock.recv(size - len(data))
        if not chunk:
            raise ConnectionError("the server closed the RCON connection")
        data += chunk
    return data


def _reply(sock):
    size = struct.unpack("<i", _read_exactly(sock, 4))[0]
    payload = _read_exactly(sock, size)
    request_id = struct.unpack("<i", payload[:4])[0]
    return request_id, payload[8:-2].decode("utf-8", "replace")


def send(*commands):
    """Runs each command as the server console, in order, and returns the replies."""
    replies = []
    with socket.create_connection((HOST, PORT), timeout=10) as sock:
        sock.sendall(_packet(1, 3, PASSWORD))
        if _reply(sock)[0] == -1:
            raise SystemExit("RCON login failed - is the server from prepare_server.py running?")
        for number, command in enumerate(commands, start=2):
            sock.sendall(_packet(number, 2, command))
            replies.append(_reply(sock)[1])
    return replies


if __name__ == "__main__":
    for reply in send(*sys.argv[1:]):
        print(reply)
```

`tools/qa/scene.py`:

```python
"""Stages one release-QA scene on the running QA server through RCON (docs/qa-checklist.md lists them in order).

Usage: python tools/qa/scene.py <scene> <node>
"""
import sys

from rcon import send

MACHINE = "diamondvending:vending_machine[facing=south,"
# The client test's machine (FabricClientTests): six buttons, a full cake-less stock, three cookies in the tray.
SHOP = ('{owner_name:"Tester",'
        'selections:[{slot:0,item:{id:"minecraft:apple",count:2},price:3},'
        '{slot:1,item:{id:"minecraft:oak_log",count:4},price:1},'
        '{slot:2,item:{id:"minecraft:diamond_sword",count:1},price:12},'
        '{slot:4,item:{id:"minecraft:bread",count:1},price:0},'
        '{slot:7,item:{id:"minecraft:enchanted_golden_apple",count:1},price:64},'
        '{slot:11,item:{id:"minecraft:cake",count:1},price:5}],'
        'stock:{Items:[{Slot:0b,id:"minecraft:apple",count:64},{Slot:1b,id:"minecraft:oak_log",count:64},'
        '{Slot:2b,id:"minecraft:diamond_sword",count:1},{Slot:3b,id:"minecraft:bread",count:64},'
        '{Slot:4b,id:"minecraft:enchanted_golden_apple",count:3}]},'
        'tray:{Items:[{Slot:0b,id:"minecraft:cookie",count:3}]}}')
# Dye colors in id order (0-15); 1.21.1 item models pick the color by custom_model_data = id + 1.
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
KEPT = 'diamondvending:machine_setup={selections:[{slot:0,item:{id:"minecraft:cake",count:1},price:5}]}'


def machine(x, z, color=None, contents=""):
    """The four setblocks of a south-facing machine whose lower-left part is at (x, -60, z); its right column is x + 1."""
    state = MACHINE + (f"color={color}," if color else "")
    return [f"setblock {x} -60 {z} {state}half=lower,side=left]{contents}",
            f"setblock {x + 1} -60 {z} {state}half=lower,side=right]",
            f"setblock {x} -59 {z} {state}half=upper,side=left]",
            f"setblock {x + 1} -59 {z} {state}half=upper,side=right]"]


def item(color, old):
    components = f"base_color={color}" + (f",custom_model_data={COLORS.index(color) + 1}" if old else "")
    return f"diamondvending:vending_machine[{components}]"


def commands(scene, node):
    old = node.startswith("1.21.1")
    return {
        "start": ["op Dev", "time set noon", "weather clear", "gamemode survival Dev"],
        "shop": ["fill -4 -61 -4 5 -61 6 minecraft:smooth_stone", *machine(0, 0, contents=SHOP),
                 "clear Dev", "give Dev minecraft:diamond 10", "tp Dev 1 -60 3.5 180 10"],
        "buy": ["tp Dev 1.625 -60 3.5 180 2.75"],  # crosshair on button 1
        "setup": ["clear Dev", "tp Dev 1 -60 3.5 180 10"],
        "hint": ["give Dev minecraft:stone 5"],
        "kept": ["clear Dev", f"give Dev diamondvending:vending_machine[{KEPT}]",
                 "fill 6 -61 -4 12 -61 6 minecraft:smooth_stone", "tp Dev 9 -60 3.5 180 50"],  # looking at the floor
        "kept_view": ["tp Dev 10 -60 5.5 180 10"],
        # West of everything else, so no other machine stands in the way of the view
        "colors": [c for i, color in enumerate(COLORS) for c in machine(-63 + 3 * i, -12, color)]
                  + ["tp Dev -40 -60 10 180 3"],
        "icons": ["clear Dev"] + [f"give Dev {item(color, old)}" for color in ("red", "blue", "lime", "black")],
        "manual": ["clear Dev", "tp Dev 20.5 -60 2.5 180 0",
                   'setblock 20 -60 0 minecraft:crafter[orientation=south_up]'
                   '{Items:[{Slot:0b,id:"minecraft:book",count:1},{Slot:1b,id:"minecraft:gold_nugget",count:1}]}',
                   "setblock 21 -60 0 minecraft:redstone_block"],  # the crafter crafts the manual and throws it south
        "read": ["tp Dev 20.5 -60 2.5 0 -60"],  # facing away from everything, at the sky
        "stop": ["stop"],
    }[scene]


if __name__ == "__main__":
    for reply in send(*commands(sys.argv[1], sys.argv[2])):
        if reply:
            print(reply)
```

`tools/qa/capture.ps1`:

```powershell
<# Saves the Minecraft window as a PNG for release QA (docs/qa-checklist.md). Brings the window to the front first if
   it isn't there (a minimize/restore plus a tap of Alt lets this process do that).
   Usage: powershell -File tools/qa/capture.ps1 build/qa/26.1-neoforge/front.png #>
param([Parameter(Mandatory)][string]$OutFile)

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class QaWindow {
    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int cmd);
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
}
"@

[QaWindow]::SetProcessDPIAware() | Out-Null
$game = Get-Process java, javaw -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowTitle -like "Minecraft*" } | Select-Object -First 1
if ($null -eq $game) { Write-Output "no Minecraft window"; exit 1 }
$hwnd = $game.MainWindowHandle
if ([QaWindow]::GetForegroundWindow() -ne $hwnd) {
    [QaWindow]::ShowWindow($hwnd, 6) | Out-Null
    [QaWindow]::ShowWindow($hwnd, 9) | Out-Null
    [QaWindow]::keybd_event(0x12, 0, 0, [UIntPtr]::Zero)
    [QaWindow]::keybd_event(0x12, 0, 2, [UIntPtr]::Zero)
    [QaWindow]::SetForegroundWindow($hwnd) | Out-Null
    Start-Sleep -Milliseconds 1500
}
$rect = New-Object QaWindow+RECT
[QaWindow]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
$bitmap = New-Object System.Drawing.Bitmap ($rect.Right - $rect.Left), ($rect.Bottom - $rect.Top)
[System.Drawing.Graphics]::FromImage($bitmap).CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)
New-Item -ItemType Directory -Force (Split-Path -Parent $OutFile) | Out-Null
$bitmap.Save($OutFile, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output "saved $OutFile"
```

`tools/qa/input.ps1`:

```powershell
<# Plays the game for release QA (docs/qa-checklist.md): picks a hotbar slot, sneaks, clicks, or presses keys, in that
   order. The game window must be in front (run capture.ps1 once first).
   Usage: powershell -File tools/qa/input.ps1 -Slot 1 -Sneak -Click right
          powershell -File tools/qa/input.ps1 -Keys "{PGDN}"      (SendKeys syntax: "e", "{ESC}", "{PGDN}") #>
param([int]$Slot = 0, [switch]$Sneak, [ValidateSet("", "left", "right")][string]$Click = "", [string]$Keys = "")

Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class QaInput {
    [DllImport("user32.dll")] public static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, int dx, int dy, uint data, UIntPtr extra);
}
"@

if ($Slot -ge 1) {
    [QaInput]::keybd_event([byte](0x30 + $Slot), 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 100
    [QaInput]::keybd_event([byte](0x30 + $Slot), 0, 2, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 300
}
if ($Sneak) {
    [QaInput]::keybd_event(0xA0, 0x2A, 0, [UIntPtr]::Zero)   # left shift down
    Start-Sleep -Milliseconds 400                             # the server learns about sneaking a tick later
}
if ($Click -ne "") {
    $down, $up = if ($Click -eq "right") { 0x0008, 0x0010 } else { 0x0002, 0x0004 }
    [QaInput]::mouse_event($down, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [QaInput]::mouse_event($up, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 300
}
if ($Sneak) { [QaInput]::keybd_event(0xA0, 0x2A, 2, [UIntPtr]::Zero) }
if ($Keys -ne "") {
    [System.Windows.Forms.SendKeys]::SendWait($Keys)
    Start-Sleep -Milliseconds 300
}
Write-Output "done"
```

- [ ] **Step 3: Write the checklist**

`docs/qa-checklist.md`:

````markdown
# Release QA checklist

Before a release, run this on **each of the four jars**: the dev client against a dedicated server of the same jar, so
every packet really crosses the network (singleplayer skips encoding). It takes about ten minutes per jar. Record each
result (✅ or what was wrong) in the release PR. Needs Windows (the window scripts), Python 3 and a display.

## Run one jar

Replace `<node>` with `26.1-neoforge`, `26.1-fabric`, `1.21.1-neoforge` or `1.21.1-fabric`. One jar at a time.

1. `python tools/qa/prepare_server.py <node>` — a fresh flat world in `run/<node>-server/`, RCON on (this accepts the
   [Minecraft EULA](https://aka.ms/MinecraftEULA) for that local test server).
2. Start the server and wait for `Done (` in `run/<node>-server/logs/latest.log`: `./gradlew :<node>:runServer`
3. Start the client and wait for `Dev joined the game` in the same log:
   `./gradlew :<node>:runClient -Pdiamondvending.join=localhost`
4. For each check below: `python tools/qa/scene.py <scene> <node>`, then the input, then
   `powershell -File tools/qa/capture.ps1 build/qa/<node>/<name>.png`, and compare with "Should show".
5. `python tools/qa/scene.py stop <node>`, close the game window, and look for `ERROR` or `Exception` lines that
   mention `diamondvending` in `run/<node>-server/logs/latest.log` and `run/<node>/logs/latest.log`.

| # | Scene(s) | Input (`tools/qa/input.ps1 …`) | Capture | Should show |
|---|---|---|---|---|
| 1 | `start`, `shop` | — | `front` | The machine with items on its shelves and white tags `1 · 3`, `2 · 1`, `3 · 12`, `5 · FREE`, `8 · 64`, a red `SOLD OUT` tag for the cake (12), cookies in the tray, the display lit green (`SELECT ITEM`). |
| 2 | `buy` | `-Click right` | `bought` | The hover tooltip for button 1 (apple × 2, 3 diamonds), the display flashing `THANK YOU` or the apples falling or in the tray. |
| 3 | `setup` | `-Sneak -Click right` | `setup` | The setup screen: tabs Items (current) / Stock / Cash Box / Admin, "No problems. Happy selling!", the 3 × 4 grid with the six items, "Button 1: Apple", Amount and Price, the player inventory. Then `-Keys "{ESC}"`. |
| 4 | `hint` | `-Slot 1 -Sneak -Click right` | `hint` (right away) | "Empty both hands, then sneak + right-click to open setup." above the hotbar, and no stone block against the machine. |
| 5 | `kept`, then `kept_view` | `-Slot 1 -Click right` before `kept_view` | `kept` | A second machine at x 9–10 with a cake on button 1's shelf and a red `SOLD OUT` tag (its setup came back; its stock didn't). |
| 6 | `colors` | — | `colors` | 16 machines in a row in white, orange, magenta, light blue, yellow, lime, pink, gray, light gray, cyan, purple, blue, brown, green, red, black — each clearly its own color, fronts drawn right. |
| 7 | `icons` | `-Keys "e"` | `icons` | The inventory with red, blue, lime and black machine icons in the hotbar. Then `-Keys "{ESC}"`. |
| 8 | `manual`, wait 2 s, `read` | `-Slot 1 -Click right` | `manual_1`; `-Keys "{PGDN}"` ten times, then `manual_11` | The crafter made the manual (it's in slot 1). The book opens on "Welcome, New Franchise Owner!" (bold) with its text; page 11 is "More Admin Tools". Nothing is cut off. Then `-Keys "{ESC}"`. |

JEI, EMI and REI aren't in the dev runs. If you play a pack with one of them, check that the machine and the manual
recipes show up there too.
````

In `docs/dev-setup.md`, after the client game test section, add:

```markdown
## Release QA
Before a release, every jar gets a short scripted play session against a real dedicated server: see
[docs/qa-checklist.md](qa-checklist.md). `./gradlew :<node>:runServer` uses its own folder, `run/<node>-server/`, and
`./gradlew :<node>:runClient -Pdiamondvending.join=localhost` joins it straight away as player `Dev`.
```

- [ ] **Step 4: Run the checklist on 26.1-neoforge**

Follow `docs/qa-checklist.md` for `26.1-neoforge`, with the server and the client as background commands (a `run_in_background` Bash call each, their output to the workspace), waiting on the log lines with an `until grep -q …; do sleep 2; done` background call. Read all nine captures (`front`, `bought`, `setup`, `hint`, `kept`, `colors`, `icons`, `manual_1`, `manual_11`).
Expected: each matches "Should show"; the two logs have no `ERROR`/`Exception` lines mentioning `diamondvending`; `scene.py stop` stops the server and the client, once closed, ends its Gradle task.

If a script or scene is wrong (a coordinate, a command the version rejects — `rcon.py` prints the server's reply), fix it, re-run that check, and ledger the fix. If running the server and the client at once fails (Gradle lock timeouts, or a process killed for memory), stop and ask: the dedicated-server check is the point of this task.

- [ ] **Step 5: Commit**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files (never anything under `run/` or `build/`).

```bash
git -C /c/Users/benet/mcvending add build.neoforge.gradle.kts build.fabric.gradle.kts tools docs/qa-checklist.md docs/dev-setup.md
git -C /c/Users/benet/mcvending commit -m "build: release QA tools, run against a dedicated server"
```

---

### Task 5: Release QA on the other three jars

Deliverable: `26.1-fabric`, `1.21.1-neoforge` and `1.21.1-fabric` each pass the whole checklist against their own dedicated server; anything found is fixed with a test where one can pin it.

**Files:**
- Modify: only what the QA run shows is wrong (art: `src/test/java/diamondvending/art/` + `./gradlew :26.1-neoforge:generateArt`, never the generated files; code: with a GameTest RED first).

**Interfaces:**
- Consumes: Task 4's scripts, runs and checklist.

- [ ] **Step 1: 26.1-fabric**

Follow `docs/qa-checklist.md` for `26.1-fabric`.
Expected: every check matches "Should show" (this is the first time the empty-hands hint is seen on a real Fabric client), logs clean.

- [ ] **Step 2: 1.21.1-neoforge**

Follow `docs/qa-checklist.md` for `1.21.1-neoforge`.
Expected: every check matches — the first real look at the 1.21.1 renderer, setup screen and manual (JSON-string pages) — logs clean.

- [ ] **Step 3: 1.21.1-fabric**

Follow `docs/qa-checklist.md` for `1.21.1-fabric`.
Expected: as Step 2.

- [ ] **Step 4: Fix what QA found**

For each difference from "Should show": if it's code, write the GameTest (or client-test step) that shows it, watch it fail, fix, and run all four nodes; if it's art, change the generator and regenerate; if it's the checklist or a script, fix that. Re-run the affected checks on the affected jars. Ledger every finding and its fix. If nothing was found, ledger "QA: all 8 checks passed on all 4 jars".

- [ ] **Step 5: Commit (only if Step 4 changed anything)**

Run: `./gradlew "Refresh active project"`; `git status --short`.

```bash
git -C /c/Users/benet/mcvending add -A src tools docs
git -C /c/Users/benet/mcvending commit -m "fix: what release QA found"
```

---

### Task 6: Version 1.0.0 and the release PR

Deliverable: the mod says 1.0.0; the changelog, README, docs and roadmap are ready for a release; the PR is open with the QA results and CI is green.

**Files:**
- Modify: `stonecutter.properties.toml`, `CHANGELOG.md`, `README.md`, `docs/dev-setup.md`, `docs/backlog.md`, `docs/superpowers/plans/2026-09-23-roadmap.md`

- [ ] **Step 1: Version**

In `stonecutter.properties.toml` change `mod.version = "0.1.0"` to `mod.version = "1.0.0"`.

Run: `./gradlew :26.1-neoforge:test`
Expected: PASS (`MetadataFloorsTest` reads the processed metadata; the version isn't pinned there).

- [ ] **Step 2: Changelog**

In `CHANGELOG.md`, under `## [Unreleased]` → `### Added`, append:

```markdown
- The Diamond Vending Manual: craft a book with a gold nugget (it unlocks with your first diamond). Eleven short pages explain everything a player could trip on.
```

then turn the heading `## [Unreleased]` into `## [1.0.0] - <today, YYYY-MM-DD>` and insert above it:

```markdown
## [Unreleased]

```

- [ ] **Step 3: README, docs, backlog, roadmap**

`README.md`: replace the status line (`> Status: in development — …`) with:

```markdown
> **1.0.0 is out.** Download the jar for your game from the [releases page](../../releases):
> `diamondvending-neoforge-…` for NeoForge or `diamondvending-fabric-…` for Fabric (Fabric also needs Fabric API), built
> for Minecraft 1.21.1 or 26.1.x. Put it in your `mods` folder, on the server and on every player's game. In game,
> craft a book with a gold nugget to get the manual. Building from source: [dev setup](docs/dev-setup.md).
```

and add to its `## Docs` list: `- [Datapack catalogs for pack makers](docs/catalogs.md)` and `- [Release QA checklist](docs/qa-checklist.md)`.

`docs/dev-setup.md`, section "Stocking a machine by command": replace "Until the setup screen exists (Plan 5), stock a machine with `/data`. Look at its lower-left part and run, for example:" with "In game, the setup screen does this. For a scripted world (tests, QA scenes), use `/data` on the machine's lower-left part, for example:".

`docs/backlog.md`: at the end add:

```markdown
## Small known issues (v1.0)

Found in Plan 5's review and left for later — none loses or duplicates items.

- A catalog entry whose `count` is above the item's stack size is quietly lowered to the stack size (e.g. 64 ender
  pearls sell 16). 26.1 can't check this while datapacks load; a warning when the catalog is first used would help pack
  makers.
- A machine placed from a creative player's machine item shares its button templates with that item (nothing changes
  templates today, so nothing shows). Copying them in `MachineSetup.applyTo` would remove the risk.
- "An item from a removed mod in a kept setup" is tested at the component level, not by placing such an item.
```

Roadmap: change Plan 6's row to start with `| **6 — Manual, polish & v1.0** ([plan](2026-09-25-plan-6-manual-and-release.md)) |` and its status cell to `Done — v1.0.0; release QA on all 4 jars against a dedicated server`; in Plan 4's status cell replace "and get their first look in Plan 6's QA run" with "and were seen in Plan 6's release QA".

- [ ] **Step 4: Build everything once more**

Run, one at a time: `G26N`, `G26F`, `G121N`, `G121F`, then `GCLIENT`
Expected: 115 / 115 / 114 / 114; the client test passes and its screenshots are as in Task 2.

- [ ] **Step 5: Commit, push, PR**

Run: `./gradlew "Refresh active project"`; `git status --short` lists only this task's files.

```bash
git -C /c/Users/benet/mcvending add stonecutter.properties.toml CHANGELOG.md README.md docs
git -C /c/Users/benet/mcvending commit -m "release: 1.0.0"
git -C /c/Users/benet/mcvending push -u origin plan-6/manual-and-release
```

Open a PR titled "Plan 6: Manual, polish & v1.0" with the repo's PR template. The manual line: "the manual now exists and covers every rule in spec §6.2 (`ManualTest`)". Include the QA results as a table (8 checks × 4 jars) and describe the manual screenshots. Wait for CI to be green on all four targets.

---

### Task 7: Publish v1.0.0 (after the PR is merged, and only when the user says so)

Deliverable: a `v1.0.0` tag and a GitHub release in the repo with the four mod jars, each checked before upload.

- [ ] **Step 1: Start from the merged main**

```bash
git -C /c/Users/benet/mcvending checkout main
git -C /c/Users/benet/mcvending pull --ff-only origin main
```

Expected: `git log -1` is the squash-merge of the Plan 6 PR; `git status` is clean.

- [ ] **Step 2: Build the four jars**

Run, one at a time: `./gradlew :26.1-neoforge:buildAndCollect`, `./gradlew :26.1-fabric:buildAndCollect`, `./gradlew :1.21.1-neoforge:buildAndCollect`, `./gradlew :1.21.1-fabric:buildAndCollect`
Expected: `build/libs/1.0.0/` holds four mod jars and four `-sources` jars, named `diamondvending-<loader>-1.0.0+<minecraft>.jar`.

- [ ] **Step 3: Check each jar before upload**

For each of the four non-sources jars:
- `unzip -l <jar> | grep -c gametest` → `0` (no test code ships), and no `tools/` or `qa` entries;
- NeoForge: `unzip -p <jar> META-INF/neoforge.mods.toml | grep -E "^version|modId"` → `version="1.0.0"`, `modId="diamondvending"`; Fabric: `unzip -p <jar> fabric.mod.json | grep -E "\"version\"|\"id\""` → `"version": "1.0.0"`, `"id": "diamondvending"`;
- `unzip -l <jar> | grep -c "recipe/manual.json"` → `1`.

- [ ] **Step 4: Tag and release**

Write the release notes to the workspace: the `## [1.0.0]` section of `CHANGELOG.md`, then an "Which jar do I need?" list (NeoForge 1.21.1, NeoForge 26.1.x, Fabric 1.21.1 + Fabric API, Fabric 26.1.x + Fabric API; install on the server and on every player's game).

```bash
git -C /c/Users/benet/mcvending tag -a v1.0.0 -m "Diamond Vending 1.0.0"
git -C /c/Users/benet/mcvending push origin v1.0.0
gh release create v1.0.0 --repo benethcopilot/diamondvending --title "Diamond Vending 1.0.0" --notes-file <workspace>/release-notes.md build/libs/1.0.0/diamondvending-neoforge-1.0.0+26.1.jar build/libs/1.0.0/diamondvending-fabric-1.0.0+26.1.jar build/libs/1.0.0/diamondvending-neoforge-1.0.0+1.21.1.jar build/libs/1.0.0/diamondvending-fabric-1.0.0+1.21.1.jar
```

(Use the exact jar names Step 2 produced.)
Expected: `gh release view v1.0.0 --repo benethcopilot/diamondvending` lists the four jars.
