# Diamond Vending — Design Spec

- **Date:** 2026-09-23
- **Status:** Draft — awaiting review
- **Mod ID:** `diamondvending` · **Display name:** Diamond Vending · **Java package / Gradle group:** `diamondvending`
- **License:** MIT · **Mod-list author:** "Diamond Vending Team"
- Research behind these decisions: [`docs/research.md`](../../research.md). Deferred ideas: [`docs/backlog.md`](../../backlog.md).

## 1. Goal

A Minecraft Java Edition mod that adds a **2×2 vending machine** that looks like a real
snack machine and sells items for **diamonds**. Each of its 12 items has its **own physical
button** on the machine's side panel. It must drop into the modpacks we play (NeoForge and
Fabric, Minecraft 1.21.1 and 26.1.x) with **no extra library dependencies**, and work on
dedicated servers.

### Success criteria

1. Four jars build from one codebase: {1.21.1, 26.1.2} × {NeoForge, Fabric}.
2. Each jar loads in a real modpack on its version/loader with no additional required mods
   (Fabric jars require only Fabric API, which every Fabric pack already has).
3. A player can place a machine, stock it, price it, and another player can buy from it on a
   dedicated server — with no GUI involved in buying.
4. An admin can turn a machine into an infinite "diamond sink" shop, either by hand or by
   assigning a datapack catalog.
5. All unit tests and GameTests pass on all four targets.

### Decisions made during brainstorming

| Topic | Decision |
|---|---|
| Shop model | **Both**: player-owned (stocked, priced, collects diamonds) and infinite/admin (unlimited stock, diamonds destroyed) |
| Targets | 1.21.1 + 26.1.x, NeoForge + Fabric; one codebase via **Stonecutter** |
| Form factor | **2 wide × 2 tall** snack machine, **12 selections**, 12 buttons on a right-hand side panel |
| Payment | **Both**: buttons pull from inventory; optional pre-loaded **credit** via coin slot, spent first |
| Dispensing | Item **drops into the pickup tray**; right-click the tray to collect |
| Catalogs | **Both**: admin sets up an infinite machine in-game, or assigns a **datapack catalog** |
| Color | **Dyeable**, all 16 dye colors |
| Currency | Diamonds by default; **admins** can set a per-machine currency item; catalogs may name a currency |
| Break & re-place | Setup is kept on the dropped item; contents spill out |
| Guide | A fun, simple **"New Franchise Owner" manual** as a vanilla written book |

## 2. The machine (block)

### 2.1 Structure

- One item, **Vending Machine**, places a **2×2 multiblock** (4 block positions: lower/upper ×
  left/right). The front faces the placing player. "Left" and "right" are as seen by someone
  standing in front of the machine: the clicked position becomes the **lower-left** part and the
  machine extends to that viewer's right and up.
- Placement fails (no item consumed) unless all 4 positions are replaceable and inside the world
  height limit.
- Block-state properties: `facing` (N/E/S/W), `half` (lower/upper), `side` (left/right),
  `color` (16 dye colors, default **red**).
- The **lower-left part is the master** and owns the block entity. The other three parts locate
  the master from their own state (no block entity of their own).
- Breaking **any** part removes all four (see §5.3 for drops and permissions).
- Each part is a full-cube collision/selection box.
- The machine is **lit** (light level 6) — real machines glow at night.
- Push reaction `BLOCK` (pistons can't move it). Blast resistance **1200** (obsidian-class).
  Tagged `minecraft:wither_immune` and `minecraft:dragon_immune`. Not flammable.

### 2.2 Front-face layout

The front is a 32 × 32 "pixel" canvas (16 px per block), `u` left→right, `v` top→bottom, as seen
from the front. Nominal regions (the single source of truth is `core/MachineLayout`, and the
textures must match it):

| Region | u (px) | v (px) | Notes |
|---|---|---|---|
| Glass window | 1.5 – 22.5 | 2 – 24 | 4 shelves × 3 items; no click action |
| Warning lamp | 26 – 28.5 | 0.5 – 2 | red, blinks while a problem is active (§3.5); no click action |
| Display | 24.75 – 29.75 | 3 – 5.5 | status text; turns red and scrolls for problems |
| Buttons 1–12 | 25 – 29.75 | 7 – 22.75 | 2 columns × 6 rows, each 2 × 2 px, 0.75 px gaps |
| Coin slot | 24 – 27.5 | 24.5 – 29 | insert credit |
| Coin-return button | 28 – 30.5 | 25 – 27.5 | return credit |
| Pickup tray | 3 – 21 | 26 – 30.5 | collect purchases |

- **Button hit areas** are padded to fill the gaps (each button owns a 2.75 × 2.75 px cell), so
  there are no dead spots between buttons.
- **Selection numbering is 1–12 in reading order** everywhere: shelf items left→right,
  top→bottom (3 per shelf); buttons left→right, top→bottom (2 per row). Price tags and buttons
  both show the number, so an early reader can match "7" to "7".
- Clicks are resolved **on the server** from the vanilla right-click hit location — the same
  technique as vanilla's chiseled bookshelf. Only clicks on the **front face** hit regions; a
  click on any other face does nothing (except the sneak and dye rules below).

### 2.3 Visuals

- **Models:** four part models (one per lower/upper × left/right) sharing a **grayscale body
  texture tinted by `color`**; glass, panel, buttons, display and tray are untinted overlays.
- **Item icon:** a small model of the whole machine, tinted with the item's stored color.
- **Block entity renderer** (on the master):
  - The 12 selection items rendered as **real item models on 4 shelves** behind the glass, each
    with a small **price tag** (`number · price · currency icon`, e.g. `7 · 3 ◆`).
  - The **display** shows short status text (see §3.5).
  - The **tray** renders the items waiting inside it.
  - On a successful purchase, the item **drops from its shelf into the tray** — a short
    client-side animation (~0.5 s) triggered by a block event, with a sound.
  - Rendering must be cheap: skip item rendering beyond 32 blocks and when the front face
    isn't visible.
- **Hover HUD** (client only): when the crosshair is on a front-face region, show a small
  tooltip near the crosshair:
  - Button: item name × quantity, price with currency icon, and `SOLD OUT` if applicable.
  - Coin slot: "Insert <currency>" and your current credit.
  - Coin return: "Return credit (<n>)".
  - Tray: "Take items (<n>)".
  - Always a last line: "Owned by <name>" or "Shop machine" (infinite / no owner).

## 3. Using a machine (buying)

### 3.1 Selections

Each of the 12 selections is either empty or has:
- an **item template** — an item stack whose count is the **quantity per purchase**
  (1 to the item's max stack size), components included (enchantments, potions…);
- a **price** — an integer 0–999 in currency items (0 = free).

### 3.2 Interaction rules

Server-side, in this priority order for a right-click on any machine part:

1. **Sneaking with both hands empty** and the player is the owner or an admin → open the
   **setup screen** (§4). (Vanilla only calls a block's use handler for sneak-clicks when both
   hands are empty, so this is the natural gesture.) A non-owner doing this gets
   "Only the owner can set up this machine."
   - **Sneaking while holding something** (owner/admin only): vanilla would skip the block and
     use the held item instead, which is confusing. The mod catches this click first (via each
     loader's block-interaction event), cancels it so nothing is placed, and tells the player:
     "Empty both hands, then sneak + right-click to open setup." For everyone else, sneaking
     with an item behaves like vanilla.
2. **Holding a dye** and the player is the owner or an admin → recolor the machine (consumes one
   dye unless in creative). Same color → no-op.
3. Otherwise, the front-face region decides: button → buy (§3.3); coin slot → insert credit
   (§3.4); coin return → return credit (§3.4); tray → collect (§3.6); anything else → nothing.

Clicks are always consumed by the machine, so held blocks are never placed against it
accidentally.

### 3.3 Purchase transaction

Runs on the server thread. **All checks happen before any change** (all-or-nothing):

1. Resolve the selection (own selections, or the catalog's — §5.1). Empty → `EMPTY`.
2. **Owned mode:** stock must hold at least *quantity* items matching the template
   (same item and same components) → else `SOLD OUT`.
3. The **tray** must have room for the purchase → else `TRAY FULL`.
4. **Owned mode:** the **cash box** must have room for *price* currency items → else
   `CASH BOX FULL`.
5. **Funds** = the player's credit on this machine + currency items in their inventory (main
   inventory, hotbar, offhand; **not** inside shulker boxes or bundles). If funds < price →
   `NEED <price>`.
6. Execute: take from **credit first**, then from inventory; move the currency items into the
   **cash box** (owned) or **destroy** them (infinite); remove the goods from stock (owned);
   insert the goods into the tray; fire the drop animation + sound; display `THANK YOU`.

Failures take nothing, play an error buzz, flash the short message on the display, and send
the buyer a plain-language explanation (§3.5 c). The owner may buy from their own machine
(they just pay themselves).

### 3.4 Credit

- Right-click the **coin slot** while holding currency items → the **whole held stack** is loaded
  as credit **for that player only**. Credit is stored as the **actual items inserted**, so the
  coin-return always gives back exactly what went in (even if an admin later changes the
  machine's currency).
- Credit cap: **9 stacks per player per machine**; anything over the cap stays in hand.
- Clicking the coin slot with an empty hand or a non-currency item → buzz; nothing changes
  (the hover HUD already says which currency the machine takes).
- **Coin return** → all of that player's credit goes to their inventory (overflow dropped at
  their feet).
- Other players can never spend or return someone else's credit.

### 3.5 Display, warnings & messages

**Principle: no silent failures.** Young players must always be able to tell *what's wrong* and
*who can fix it*. There is no generic "out of order" — every problem names its reason and fix.

**a) Normal display (green text)**

| State | Text |
|---|---|
| Idle, viewer has no credit | `SELECT ITEM` |
| Viewer has credit | `CREDIT <n>` (each client shows its own player's credit) |
| After a buy (~2 s) | `THANK YOU` |

**b) Problems — persistent and impossible to miss**

Computed on the server whenever the machine changes, and synced to clients. While any problem
is active:
- the display turns **red and scrolls** the full message, like an LED sign;
- a **red warning lamp** on top of the panel **blinks**;
- the hover HUD shows the plain-language explanation when looking at **any part of the front**,
  not just the buttons;
- the setup screen shows a **red banner** with the fix and a red **!** on the tab that needs
  attention (§4).

| Problem | Active when | Display (scrolling, red) | Explanation (HUD / action bar) | Who fixes it |
|---|---|---|---|---|
| Catalog missing | the assigned catalog isn't loaded | `CATALOG MISSING - ASK AN ADMIN` | "This machine's catalog is missing. An admin needs to pick a new one." | admin |
| Not set up | no selection has an item | `NOT SET UP YET` | "Nothing is for sale yet. The owner can empty both hands, then sneak + right-click to set it up." | owner |
| Cash box full | owned mode; the cash box has no empty slot | `CASH BOX FULL - OWNER MUST EMPTY IT` | "The cash box is full of diamonds! The owner needs to take them out." | owner |
| All sold out | owned mode; every set-up selection is out of stock | `SOLD OUT - OWNER MUST RESTOCK` | "Everything is sold out. The owner needs to put more items in the Stock." | owner |
| Tray full | all 9 tray slots are used | `TRAY FULL - TAKE YOUR ITEMS` | "The tray is full! Right-click the tray to take the items out." | anyone |

- If several problems are active, the display cycles through all of them; the lamp blinks until
  every one is fixed.
- "diamonds" in any message is replaced by the machine's actual currency name.

**c) Per-click messages** — every failed click tells the clicking player why, in their action
bar (only they see it). The display also flashes the short version (e.g. `NEED 3`,
`SOLD OUT`) for ~2 s for everyone nearby, with the error buzz.

| Situation | Message |
|---|---|
| Button with nothing on it | "Button 7 has nothing for sale." |
| That selection is sold out | "Button 7 is sold out." |
| Not enough money | "Button 7 costs 3 diamonds. You have 1." (credit + inventory) |
| Tray full / cash box full / catalog missing | the explanation from table (b) |
| Coin slot with the wrong item or empty hand | "This machine takes diamonds." |
| Credit cap reached | "You can't put in any more. Buy something or press coin return." |
| Non-owner tries setup, dye, or breaking | "Only the owner can do that." |
| Owner sneak-clicks while holding something | "Empty both hands, then sneak + right-click to open setup." |

Transient display messages reach nearby players via a block event; per-click messages are sent
only to the clicking player.

### 3.6 Tray

- **9 slots.** Anyone may collect (like a real machine).
- Right-click the tray → all tray contents go into the player's inventory; overflow drops at
  their feet.
- Nothing in the tray despawns; it persists with the machine.

## 4. Running a shop (setup screen)

Opened by the owner or an admin (§3.2 rule 1). A standard-width container screen with **tabs**
(slots on hidden tabs are inactive), the player inventory always shown below:

| Tab | Who | Contents |
|---|---|---|
| **Selections** | owner, admin | 12 "ghost" slots (4 × 3). Clicking one with an item copies it as the template (nothing is consumed); the count of the carried stack sets the quantity. The selected slot shows **quantity** (−/+) and **price** (number field). Clear button per slot. Read-only while a catalog is assigned. |
| **Stock** | owner, admin | 27 real slots (a chest's worth). Owned mode only. |
| **Cash Box** | owner, admin | 27 take-only slots + **Withdraw all** button. Owned mode only. |
| **Admin** | admin only | **Infinite** toggle · **Catalog** picker (cycles "None" + every loaded catalog, showing its display name) · **Currency** ghost slot (empty = default currency). |

- Every change is sent to the server as a small packet and **re-validated on the server**
  (permissions, value ranges, admin-only fields). The client never decides anything.
- The catalog list is sent to the client when the screen opens.
- A **red banner** at the top lists any active problems with their fix (§3.5 b), and the tab
  that needs attention shows a red **!** (e.g. Cash Box when it's full, Stock when sold out).
- Switching a machine **to infinite** requires stock and cash box to be empty first, so
  nobody's items silently vanish. Until then the toggle is disabled and the Admin tab says
  exactly what to do: "Take everything out of Stock and Cash Box first."

**Admin** means: permission level ≥ 2 (op) **or** creative mode.

## 5. Modes, catalogs, ownership, currency

### 5.1 Datapack catalogs

- Location: `data/<namespace>/diamondvending/catalog/<name>.json` → catalog id
  `<namespace>:<name>`.
- Loaded by a **data reload listener** (like recipes): validated with a codec at load, and
  **`/reload` picks up changes**. A catalog that fails validation is skipped with a clear log
  error naming the file and problem.
- Format:

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

  - `display_name` — optional, shown in the Admin tab picker (defaults to the id).
  - `currency` — optional item (count ignored).
  - `entries` — 1 to 12 entries, filling selections 1…N in order. `item` uses Minecraft's
    standard item-stack JSON (`id`, `count`, optional `components`). `price` 0–999.
- A machine stores only the catalog **id**. Its effective selections are the catalog's entries.
  Editing a catalog and running `/reload` updates **every machine** using it; loaded machines
  notice the reload and re-sync to clients.
- If a machine's catalog id no longer exists, it shows the **Catalog missing** problem (§3.5 b)
  and sells nothing until an admin picks another catalog or clears it.
- The mod ships **one example catalog** (`diamondvending:example_snacks`) so admins can see the
  format in action.
- Map/pack makers can pre-configure machines with `/setblock` block-entity NBT (catalog id,
  infinite flag).
- **`docs/catalogs.md`** is the pack-maker reference: catalog format, examples, the currency
  tag, and the block-entity NBT fields for `/setblock`.

### 5.2 Modes

| | Owned (default) | Infinite (admin only) |
|---|---|---|
| Selections | owner-set, or a catalog | admin-set, or a catalog |
| Stock | 27-slot stock storage; sells out | unlimited |
| Payment goes to | cash box (owner withdraws) | destroyed (diamond sink) |
| Who can configure / break | owner, admins | admins |

A machine placed by `/setblock` or a structure has **no owner**: only admins can configure or
break it.

### 5.3 Ownership & protection

- Placing a machine makes the placer its **owner** (UUID + last-known name for the HUD).
- **Only the owner or an admin** can: open setup, dye, break. For anyone else the block cannot
  be mined (zero break progress). Non-owners can always buy, load/return their own credit, and
  collect from the tray.
- Breaking goes through normal break events, so claim/protection mods (FTB Chunks, Open Parties
  and Claims, etc.) apply as usual.
- **On break:**
  - Always dropped: **tray** contents and **all players' credit** (as the original items).
  - Owned mode also drops: **stock** and **cash box** contents.
  - The **machine item** drops (not in creative mode) carrying its **setup** (§5.4).

### 5.4 Setup kept on the item

- The dropped item carries a `diamondvending:machine_setup` data component: the 12 selections
  (templates, quantities, prices), catalog id, currency, and infinite flag. Color is carried as
  the item's color.
- Placing it restores that setup; **the new placer becomes the owner**.
- The **infinite** flag is kept only if an **admin** places the item; otherwise the machine
  comes back as a normal owned machine with the same selections and prices.
- Contents (stock, cash, tray, credit) are **never** stored on the item.

### 5.5 Currency

Effective currency for a machine, first match wins:
1. The machine's **currency slot** (set by an admin in the Admin tab).
2. The assigned catalog's `currency` field.
3. The item tag **`#diamondvending:currency`** — ships containing only `minecraft:diamond`;
   packs may override it by datapack.

A currency item matches by item type (components ignored). Price tags, the HUD, the coin slot,
credit and the cash box all follow the effective currency. v1 has **no config file**.

## 6. Obtaining & guide

### 6.1 Recipe

Shaped, overridable by datapack like any recipe:

```
B G B      B = iron block
B R B      G = glass pane
B D B      R = redstone
           D = diamond
```

Unlocked in the recipe book when the player first obtains a diamond.

### 6.2 "New Franchise Owner" manual

- A **vanilla written book** titled **"Diamond Vending Manual"**, author **"Diamond Vending
  HQ"**, crafted shapeless from **book + gold nugget** (unlocked alongside the machine recipe).
  No code beyond a recipe and translations; visible in JEI/EMI/REI.
- Pages are translatable text in the lang file. **Tone:** fun "welcome to the franchise" voice.
  **Reading level:** short sentences and simple words an early reader can follow.
- **Coverage rule: every rule a player could trip on is explained in the manual.** If a rule
  can block, surprise, or cost a player something, it gets a line in the book. Any rule added or
  changed later must update the manual in the same change (enforced by the PR checklist). The
  v1 list:
  - Buttons are numbered 1–12; the number on the price tag matches the button.
  - Payment uses your credit first, then diamonds from your inventory (not inside shulker boxes
    or bundles).
  - Credit is only yours; press coin return to get it back. There's a limit on how much you can
    put in.
  - Your item drops into the tray — **anyone** can take from the tray, so grab your stuff!
  - What each warning means and who fixes it: not set up, sold out, tray full, cash box full,
    catalog missing (§3.5 b).
  - To open setup: **empty both hands**, then sneak + right-click.
  - Only the owner (or an admin) can set up, dye, or break the machine.
  - Stock sells out; the cash box can fill up and stop sales until you empty it.
  - Breaking your machine keeps its setup on the item, but stock, cash box, tray, and credit
    pop out.
  - Dye the machine by right-clicking it with a dye.
  - Admins: infinite machines never run out and the diamonds vanish; switching to infinite needs
    empty Stock and Cash Box; an infinite machine stays infinite only when an admin places it;
    catalogs and the currency slot.
- Pages (about 8–10, one idea per page): Welcome, new Franchise Owner! · Build your machine ·
  How customers buy (buttons, credit, tray) · Set up your shop (empty hands!) · Stock and the
  cash box · Uh-oh! What the red warnings mean · Keep it safe & make it pretty · Moving your
  machine · For the bosses (admins).
- Sample page 1 (approved tone):
  > **Welcome, New Franchise Owner!**
  > You did it! You own a Diamond Vending machine. 🎉
  > People will come from far away to buy your snacks. Well... your stuff.
  > Your job is easy: **Fill it. Price it. Get diamonds.**
  > Turn the page to learn how!

## 7. Sounds (vanilla for v1)

| Event | Sound |
|---|---|
| Button press | stone button click |
| Credit inserted | chain place |
| Vend (item drops) | dispenser dispense |
| Thank you | experience orb pickup (quiet) |
| Error | low note-block bass |
| Coin return | item pickup |

## 8. Architecture

### 8.1 Build

- **Stonecutter** single source tree, started from the `multicutter` template (already set up
  for 1.21.1 and 26.1.x on Fabric + NeoForge, including obfuscated vs. unobfuscated game
  versions). Strip the template's extra library dependencies (Fzzy Config, Mixson,
  MixinConstraints, Sodium) — the mod has **no runtime dependencies** besides the loader (and
  Fabric API on Fabric).
- Targets: `1.21.1-fabric`, `1.21.1-neoforge`, `26.1.2-fabric`, `26.1.2-neoforge`. Adding 26.2
  later is one new Stonecutter node.
- Toolchain: **JDK 25** to run Gradle (required by 26.1); Gradle toolchains auto-provision JDK 21
  for 1.21.1 compilation. Plugin and loader versions: latest stable at scaffold time, pinned in
  `stonecutter.properties.toml`.
- Version/loader differences are expressed with Stonecutter comments (`//? if neoforge`,
  `//? if >=26.1`) and kept inside the smallest possible spots, mostly `platform/`.
- Memory: cap Gradle's heap (`org.gradle.jvmargs`), and run one target's tasks at a time on the
  dev machine.

### 8.2 Source layout

```
src/main/java/diamondvending/
  DiamondVending.java      mod constants + common init
  core/                    plain Java, no Minecraft types — unit-testable
    MachineLayout            (u,v) → Region; part + facing + hit → (u,v)
    PurchaseRules            pure decision: inputs → OK(takeCredit, takeInventory) | failure reason
    MachineProblems          pure: machine facts → ordered list of active problems (§3.5 b)
  block/                   VendingMachineBlock, VendingMachineBlockEntity, MachinePart, placement/break
  catalog/                 Catalog record + codec, CatalogManager (reload listener, lookup, reload generation)
  menu/                    VendingSetupMenu (tabs, ghost slots, server-side validation)
  network/                 payloads: set selection, set price/qty, toggle infinite, set catalog, set currency, withdraw
  platform/                registration + reload listener + menu-opening + HUD hooks per loader
  client/                  VendingMachineRenderer (items, prices, display, tray, drop animation),
                           VendingSetupScreen, HoverHud, color tinting
src/main/resources/
  assets/diamondvending/   blockstates, models, textures, lang (incl. manual pages)
  data/diamondvending/     recipes, tags (currency, wither/dragon immune), loot table, example catalog
src/test/java/diamondvending/core/   JUnit tests for core/
```

### 8.3 Data held by the master block entity

| Field | Type | Synced to clients |
|---|---|---|
| owner | UUID + last-known name (optional) | name only |
| infinite | boolean | yes |
| catalogId | optional id | yes |
| currency | optional item | yes (icon) |
| selections | 12 × (template stack, price) | **effective** selections (after catalog resolution) |
| stock | 27 slots | per-selection *in-stock* counts only |
| cashBox | 27 slots | no |
| tray | 9 slots | yes (for rendering) |
| credits | map player UUID → item stacks | per-player totals |
| problems | derived (not saved) — recomputed on every change | yes |

Color lives in the block state. Transient display messages are block events, not saved data.

### 8.4 Networking

- **Buying needs no custom packets** — it rides on vanilla block interaction.
- Setup-screen edits use small custom payloads, each re-validated server-side.
- Block-entity sync uses the standard update tag/packet, sent on change.

## 9. Error handling

- Every server entry point (interaction, payload, catalog load, item placement with a setup
  component) validates its input and **fails closed**: invalid → no change + player-facing
  message or log line, never a crash.
- Corrupt or unknown saved data (e.g. an item from a removed mod in a selection) loads as an
  empty selection with a log warning; the machine keeps working.
- Bad catalog files are skipped with a precise log error; machines using them show the
  **Catalog missing** problem.
- Anything a *player* can cause gets a plain-language message (§3.5 c); log lines are for
  admins and pack makers.
- Multiblock integrity: if a part finds its master missing or mismatched (e.g. after a world
  edit), it removes itself cleanly, dropping nothing.

## 10. Testing

- **Unit tests (JUnit, no Minecraft):**
  - `MachineLayout`: every region's corners/centers map correctly; padded button cells have no
    gaps; all 4 facings × 4 parts convert hit points to the right (u,v).
  - `PurchaseRules`: credit-first ordering, exact-change and short-by-one, price 0, sold out,
    tray full, cash box full, infinite mode ignores stock and cash box.
  - `MachineProblems`: each problem's trigger, owned-only problems never appear on infinite
    machines, ordering when several are active.
  - Lang completeness: every problem and per-click message has a translation key with text.
- **GameTests** (in-game automated tests on all four targets):
  - Placement creates 4 correct parts; blocked placement consumes nothing.
  - Breaking any part removes all 4 with the right drops (owned vs infinite, creative vs survival).
  - Clicking button *n* moves the right goods into the tray and takes the right currency.
  - Credit insert → buy spends credit first; coin return refunds exact items.
  - Non-owner cannot break, dye, or open setup.
  - Break and re-place keeps setup; infinite kept only for an admin placer.
  - Catalog assignment sells the catalog's entries; missing catalog → Catalog missing problem.
  - Custom currency slot changes what payment is accepted.
  - Filling the cash box raises Cash box full; emptying it clears the problem.
  - Each failed click sends the right per-click message to the clicking player only.
  - Owner sneak-click while holding a block places nothing and shows the empty-hands hint.
- **Manual checklist** per jar (dev client + dedicated server): textures and tint, item/price
  rendering, drop animation, HUD, setup screen, manual book, recipe in JEI/EMI/REI where installed.

## 11. Out of scope for v1

Tracked with notes in [`docs/backlog.md`](../../backlog.md): hopper/pipe automation · Forge
1.20.1 · Minecraft 26.2 · village/structure spawning · Patchouli guide · custom sound files ·
diamond blocks as payment / making change · sales logs & owner notifications · publishing to
CurseForge/Modrinth · currency slot for non-admin owners.
