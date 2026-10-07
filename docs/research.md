# Research Notes

Collected 2026-09-22 while designing Diamond Vending. These notes explain *why* the design
targets what it does; re-check version facts before relying on them later.

## Minecraft version landscape (September 2026)

- Java Edition switched to **year-based version numbers** in 2026: 1.21.x was followed by
  **26.1** "Tiny Takeover" (March 2026) and **26.2** "Chaos Cubed" (June 2026). There is no
  "1.26".
- **26.1 requires Java 25**, and Mojang **removed code obfuscation** starting with 26.1, so mods
  see Mojang's real names (Parchment mappings become optional).
- Toolchain for 26.1: NeoForge **ModDevGradle 2.0.141+**, **Gradle 9.1+**; Fabric **Loom 1.15**,
  **Gradle 9.4**, Fabric Loader 0.18.4+.
- Notable 26.1 API changes that affect this mod: `GuiGraphics` → `GuiGraphicsExtractor`,
  `Screen#render` → `Screen#extractRenderState`; data files use `ItemStackTemplate` instead of
  `ItemStack`; `new ChunkPos(...)` → `ChunkPos.containing(...)`. GameTests became data-driven in
  1.21.5+, so GameTest code differs between 1.21.1 and 26.1.

## Where modpacks are

| Pack | Version | Loader |
|---|---|---|
| All the Mods 10 | 1.21.1 | NeoForge |
| FTB StoneBlock 4 | 1.21.1 | NeoForge |
| Better MC BMC5 | 1.21.1 | NeoForge |
| Cobblemon Official | 1.21.1 | Fabric |
| All the Mods 11 (beta) | **26.1.2** | NeoForge |
| Better MC BMC4 | 1.20.1 | Forge |
| Prominence II | 1.20.1 | Fabric |

- **1.21.1** is where most actively maintained packs live today.
- **26.1.x** is expected to become the next stable modding version (ATM11 is on it; Create is
  porting to it).
- 1.20.1 Forge still has a large long-tail install base (older Create / BMC4 packs) — supported from 1.1.0
  (Plan 7).
- Fabric remains favored for lighter / performance packs; NeoForge for big content packs.

## Existing vending machine mods (inspiration)

| Mod | Versions | Model | Takeaways |
|---|---|---|---|
| **Vending Block** | 1.10–1.12.2 (abandoned) | Player-owned barter shop; any item as price; "advanced" variant with button selection; storage attachment with hopper support | 3.6M downloads — player shops are popular. Owner-only removal. Creative-only wrench to edit. |
| **Vending Machine** (MacTso) | Forge 1.16.5–1.21.10 | Spawns in villages; 7 random items restocked daily; tiered prices; emeralds (configurable) | 1.1M downloads. GUI-based buying. Configurable currency is valued. |
| **Vending Machines** (screret) | Forge 1.16.5–1.19.2 | Player-controlled, every dye color, price-edit screen | Dye colors are a draw. |
| **Vending Machines Revamped** | Forge 1.7.10/1.8 | Soda/candy/coffee machines, coin currency | Themed flavor items. |
| **Wizard Vending Machine** | Forge 1.10 | Custom models; insert coin, click again to collect; snack GUI | Physical "insert coin → collect" feel. |
| **The Vending Machine mod** | NeoForge 1.21.8 | Sodas with joke effects | Tiny (49 downloads). |

**Gap we fill:** none of these offers a realistic, multi-block machine with **physical per-item
buttons and no buying GUI**, on current versions, on both loaders.

## Technique: clickable regions without a GUI

Vanilla's **chiseled bookshelf** (1.20+) maps where the player clicked on a block face to one of
six slots, without a GUI. Our buttons, coin slot and tray use the same approach: the server
reads the right-click hit location, converts it to front-face pixel coordinates, and looks up
the region.

## Multi-loader / multi-version tooling

| Option | Summary | Verdict |
|---|---|---|
| **Stonecutter** templates (`multicutter`, `rotgruengelb/stonecutter-mod-template`, `Mat0u5/MinecraftModTemplate`) | One source tree; comment-based `//?` conditionals per version/loader; builds every target from one command | **Chosen.** `multicutter` ships preconfigured for 1.21.1 + 26.1.2 + 26.2 on Fabric + NeoForge and handles obfuscated and unobfuscated versions. |
| MultiLoader-Template (common/fabric/neoforge modules) | Conventional; one git branch per MC version | Fixes must be ported between branches by hand. |
| Architectury | Common API over loaders | Adds a runtime dependency (Architectury API) every pack must install. |

## Local environment findings

- No JDK installed (need JDK 25; Gradle can provision 21).
- Git 2.47 available.
- The machine ran critically low on memory during brainstorming — keep Gradle heap capped and
  run one target at a time.

## Sources

- [Switchblade Gaming — "Minecraft hit 26.2…" best modpacks 2026](https://www.switchbladegaming.com/minecraft/best-modpacks-2026/)
- [CurseForge — Top 25 modpacks](https://blog.curseforge.com/top-minecraft-modpacks-on-curseforge/)
- [NeoForge for Minecraft 26.1](https://neoforged.net/news/26.1release/)
- [Fabric for Minecraft 26.1](https://fabricmc.net/2026/03/14/261.html)
- [ModReady — Java version for 26.1](https://modready.gg/news/minecraft-new-version-numbers-explained)
- [All the Mods 11 (CurseForge)](https://www.curseforge.com/minecraft/modpacks/all-the-mods-11)
- [Create — supported game versions](https://wiki.createmod.net/users/development-status)
- [Vending Block](https://www.curseforge.com/minecraft/mc-mods/vending-block)
- [Vending Machine (MacTso)](https://www.curseforge.com/minecraft/mc-mods/vending-machine)
- [Vending Machines (screret)](https://www.curseforge.com/minecraft/mc-mods/vending-machines)
- [Vending Machines Revamped](https://www.curseforge.com/minecraft/mc-mods/vending-machines-revamped)
- [Wizard Vending Machine Mod](https://www.curseforge.com/minecraft/mc-mods/wizard-vending-machine-mod)
- [The Vending Machine mod (Modrinth)](https://modrinth.com/mod/the-vending-machine-mod)
- [multicutter template](https://github.com/pajicadvance/multicutter)
- [rotgruengelb/stonecutter-mod-template](https://github.com/rotgruengelb/stonecutter-mod-template)
- [Chiseled Bookshelf — Minecraft Wiki](https://minecraft.wiki/w/Chiseled_Bookshelf)
