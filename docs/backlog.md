# Backlog — ideas deferred from v1

Agreed during brainstorming (2026-09-23) as **out of scope for v1**, to follow up on later.
Each entry notes why it was deferred and what it would take.

| # | Idea | Why deferred / notes |
|---|---|---|
| 1 | **Hopper / pipe automation** — hoppers feed the stock storage; pull diamonds out of the cash box | Item-transfer APIs differ per loader (NeoForge capabilities vs Fabric Transfer API) and per version. Stock/cash box are already separate inventories, so this is additive. Decide which faces are input vs output. |
| 2 | **Forge 1.20.1 support** | Big long-tail install base (older Create / BMC4 packs), but a third API generation (pre-data-components) means many more code branches and tests. |
| 3 | **Minecraft 26.2 support** | Add one Stonecutter node once 26.1 builds are stable and packs start moving. |
| 4 | **Village / structure spawning** | e.g. an infinite machine with a catalog appearing in villages (MacTso's mod does this). Needs structure/jigsaw work per version. |
| 5 | **Patchouli guide book** | Richer illustrated manual, as an *optional* integration only (Patchouli must not become a required dependency). Check its 26.1 status first. |
| 6 | **Custom sound files** | v1 uses vanilla sounds. Custom "clunk", coin and beep sounds would add charm. |
| 7 | **Diamond blocks as payment / making change** | Pay a 12-diamond item with 2 diamond blocks and get 6 diamonds back. Needs change-making rules and cash box handling. |
| 8 | **Sales log & owner notifications** | "Steve bought 16 arrows for 1 diamond" in a log tab; optional chat ping to an online owner. |
| 9 | **Publishing to CurseForge / Modrinth** | The template includes the Mod Publish Plugin; needs project pages, icons, and API tokens. The Java package does not appear on these pages. |
| 10 | **Currency slot for non-admin owners** | Currently admin-only to keep one diamond economy. Opening it up is a one-line permission change plus a manual-page update. |
