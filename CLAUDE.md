# Diamond Vending — rules for AI agents

- Spec: `docs/superpowers/specs/2026-09-23-diamond-vending-design.md`. Roadmap and plans: `docs/superpowers/plans/`.
- Build/run details: `docs/dev-setup.md`. Build **one Stonecutter node at a time**; never run all four in parallel on this machine.
- **Stonecutter:** git holds the `26.1-neoforge` state. If you switch the active version, reset it before committing. Write `Identifier`, not `ResourceLocation`.
- Loader-only code → `diamondvending/platform/<loader>/`. Version differences → `//? if >=26.1` comments, kept as small as possible.
- `diamondvending/core/` must never import `net.minecraft` or loader packages.
- Player-facing text: every failure names the reason and who can fix it. Any rule a player could trip on must be explained in the manual (spec §6.2) in the same change.
- No personal usernames in identifiers, metadata, or shipped URLs.
- Git: branch → PR → CI green → squash-merge. Don't push to `main` directly.
- Generated art/models/structures: change `src/test/java/diamondvending/art/`, run `./gradlew :26.1-neoforge:generateArt`, never hand-edit the outputs.
- GameTests: `./gradlew :<node>:runGameTestServer` (NeoForge) / `:<node>:runGametest` (Fabric). New tests go in three files (see docs/dev-setup.md).
- Verify Minecraft APIs against vanilla, not only NeoForge's patched sources — NeoForge widens access that Fabric builds don't get.
