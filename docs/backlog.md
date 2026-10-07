# Backlog — ideas deferred from v1

Agreed during brainstorming (2026-09-23) as **out of scope for v1**, to follow up on later.
Each entry notes why it was deferred and what it would take. Every entry is also a GitHub issue
(label `backlog`, milestone **Later**) — discuss and track progress there.

| Issue | Idea | Why deferred / notes |
|---|---|---|
| [#1](https://github.com/benethcopilot/diamondvending/issues/1) | **Hopper / pipe automation** — hoppers feed the stock storage; pull diamonds out of the cash box | Item-transfer APIs differ per loader (NeoForge capabilities vs Fabric Transfer API) and per version. Stock/cash box are already separate inventories, so this is additive. Decide which faces are input vs output. |
| [#2](https://github.com/benethcopilot/diamondvending/issues/2) | **Forge 1.20.1 support** | Done in 1.1.0 (Plan 7). |
| [#3](https://github.com/benethcopilot/diamondvending/issues/3) | **Minecraft 26.2 support** | Add one Stonecutter node once 26.1 builds are stable and packs start moving. |
| [#4](https://github.com/benethcopilot/diamondvending/issues/4) | **Village / structure spawning** | e.g. an infinite machine with a catalog appearing in villages (MacTso's mod does this). Needs structure/jigsaw work per version. |
| [#5](https://github.com/benethcopilot/diamondvending/issues/5) | **Patchouli guide book** | Richer illustrated manual, as an *optional* integration only (Patchouli must not become a required dependency). Check its 26.1 status first. |
| [#6](https://github.com/benethcopilot/diamondvending/issues/6) | **Custom sound files** | v1 uses vanilla sounds. Custom "clunk", coin and beep sounds would add charm. |
| [#7](https://github.com/benethcopilot/diamondvending/issues/7) | **Diamond blocks as payment / making change** | Pay a 12-diamond item with 2 diamond blocks and get 6 diamonds back. Needs change-making rules and cash box handling. |
| [#8](https://github.com/benethcopilot/diamondvending/issues/8) | **Sales log & owner notifications** | "Steve bought 16 arrows for 1 diamond" in a log tab; optional chat ping to an online owner. |
| [#9](https://github.com/benethcopilot/diamondvending/issues/9) | **Publishing to CurseForge / Modrinth** | The template includes the Mod Publish Plugin; needs project pages, icons, and API tokens. The Java package does not appear on these pages. |
| [#10](https://github.com/benethcopilot/diamondvending/issues/10) | **Currency slot for non-admin owners** | Currently admin-only to keep one diamond economy. Opening it up is a one-line permission change plus a manual-page update. |

## Small known issues (v1.0)

Found in Plan 5's review and left for later — none loses or duplicates items.

- A catalog entry whose `count` is above the item's stack size is quietly lowered to the stack size (e.g. 64 ender
  pearls sell 16). 26.1 can't check this while datapacks load; a warning when the catalog is first used would help pack
  makers.
- A machine placed from a creative player's machine item shares its button templates with that item (nothing changes
  templates today, so nothing shows). Copying them in `MachineSetup.applyTo` would remove the risk.
- "An item from a removed mod in a kept setup" is tested at the component level, not by placing such an item.
