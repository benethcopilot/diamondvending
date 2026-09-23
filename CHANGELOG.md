# Changelog

All notable changes to Diamond Vending are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Project skeleton building for Minecraft 1.21.1 and 26.1.2 on NeoForge and Fabric.
- Core rules (not yet wired into the game): front-face click regions, purchase decisions with credit-first payment, and machine problem detection — all unit-tested.
- CI that builds and tests all four jars on every pull request.
- The Vending Machine block: a 2×2 machine that faces you when placed, glows softly, and can't be moved by pistons or blown up.
- Only the owner (or an admin) can break or dye a machine; breaking keeps its color on the item.
- Dye it in any of the 16 colors.
- Crafting recipe (iron blocks, glass pane, redstone, diamond), unlocked when you get a diamond.
- In-game automated tests on all four targets.
