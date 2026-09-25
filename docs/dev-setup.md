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

## In-game tests (GameTests)
Game tests live in `src/gametest/` and build a separate test-only mod, so they never ship. Run one node at a time:

```bash
./gradlew :26.1-neoforge:runGameTestServer
./gradlew :26.1-fabric:runGametest
```

Add a test in three places: a `public static void` method in `gametest/MachineTests.java`, `BuyingTests.java`,
`DisplayTests.java` or `ShopTests.java` (+ its `ALL` entry), a method in the matching `gametest/fabric/Fabric*Tests.java`, and one in the 1.21.1 block of the
matching `gametest/neoforge/NeoForge*Tests.java`. A new test class also goes into `AllTests` and the test mod's
`fabric.mod.json` entrypoints. Use `MachineTests.platform(x, y, z)` for fixed positions: 1.21.1 and 26.1 measure test
coordinates from different origins. Put items in the mock player's hand before `placeAt`/`useBlock` — placement reads
the item in hand. `RecordingPlayer` is a mock player that remembers its action-bar messages.

## Client game test (26.1 Fabric)
Rendering can't be checked by a server, so one test runs in a real game window and takes screenshots:

```bash
./gradlew :26.1-fabric:runClientGametest
```

It builds a stocked machine with commands, clicks it, opens its setup screen (pressing one of its buttons for real),
reads the manual (failing if a page is too long for the book), and saves screenshots to
`versions/26.1-fabric/build/clientgametest/screenshots/`. Look at them after any change to `client/` or `scene/`. It
needs a display, so it only runs locally (CI runners have none), and only on 26.1 — 1.21.1's Fabric API has no client
tests. The 1.21.1 renderer shares `FrontCanvas` and `MachineScene` with 26.1; only the draw calls differ.

The manual's pages are translation keys (`Texts.MANUAL_PAGES`) in `en_us.json`, and the recipe that makes the book is
per version (`src/main/resources-*/data/diamondvending/recipe/manual.json`: 1.21.1 writes each page as a JSON string).
After changing a page's text, run `./gradlew :26.1-neoforge:test` (`ManualTest` checks every rule in spec §6.2 is still
explained) and this client test (every page must fit the book's 14 lines).

The test leaves its world in `versions/26.1-fabric/build/clientgametest/saves/`, so the same scene can be opened in the
NeoForge client, which culls differently: copy the newest world to `run/26.1-neoforge/saves/<name>`, set `allowCommands`
to 1 in its `level.dat` (to use `/tp`), and start straight into it with
`./gradlew :26.1-neoforge:runClient "--args=@<repo>/versions/26.1-neoforge/build/moddev/clientRunProgramArgs.txt --quickPlaySingleplayer <name>"`
(`--args` replaces the run's own arguments, so pass its argument file first).

## Release QA
Before a release, every jar gets a short scripted play session against a real dedicated server: see
[docs/qa-checklist.md](qa-checklist.md). `./gradlew :<node>:runServer` uses its own folder, `run/<node>-server/`, and
`./gradlew :<node>:runClient -Pdiamondvending.join=127.0.0.1` joins it straight away as player `Dev`.

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

## Loader versions players need
`stonecutter.properties.toml` has two kinds of dependency versions. `deps.fabric_api`, `deps.fabric_loader` and
`deps.neo_loader` are what we build against — bump them freely. `deps.*_min` are the floors written into
`fabric.mod.json` / `neoforge.mods.toml`: the oldest release that has every API we call. Raise a floor only when new
code needs a newer API, so packs on older loaders keep working. `MetadataFloorsTest` checks both.

## Stocking a machine by command
Until the setup screen exists (Plan 5), stock a machine with `/data`. Look at its lower-left part and run, for example:

```
/data merge block <x> <y> <z> {selections:[{slot:0,item:{id:"minecraft:apple",count:2},price:3}],stock:{Items:[{Slot:0b,id:"minecraft:apple",count:64}]}}
```

`slot` 0–11 is button 1–12; `count` is how many one purchase gives; `price` is 0–999 diamonds. Add `infinite:1b` for a
machine that never runs out and destroys what it's paid. The other saved fields are `owner`, `owner_name`, `cash_box`,
`tray` and `credits` (see `VendingMachineBlockEntity`).

## Vanilla vs NeoForge sources
NeoForge's patched Minecraft sources widen some access (e.g. `BlockEntityType`'s constructor is public there but
private in vanilla 26.1). When checking an API, confirm it in vanilla too — Fabric builds see vanilla.

## Workflow
Branch → PR (fill in the checklist) → CI green → squash-merge.
