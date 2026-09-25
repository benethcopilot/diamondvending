# Changelog

All notable changes to Diamond Vending are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [1.0.0] - 2026-09-25

### Added
- Project skeleton building for Minecraft 1.21.1 and 26.1.2 on NeoForge and Fabric.
- Core rules (not yet wired into the game): front-face click regions, purchase decisions with credit-first payment, and machine problem detection — all unit-tested.
- CI that builds and tests all four jars on every pull request.
- The Vending Machine block: a 2×2 machine that faces you when placed, glows softly, and can't be moved by pistons or blown up.
- Only the owner (or an admin) can break or dye a machine; breaking keeps its color on the item.
- Dye it in any of the 16 colors.
- Crafting recipe (iron blocks, glass pane, redstone, diamond), unlocked when you get a diamond.
- In-game automated tests on all four targets.
- Buying: press a numbered button to buy; pay with diamonds from your inventory or with credit loaded through the coin slot (credit is spent first and only you can use or return yours).
- Coin return gives back exactly what you put in; your item drops into the pickup tray, which anyone can empty.
- Every refused click tells you why (nothing for sale, sold out, not enough diamonds, tray full, cash box full), with vanilla sounds.
- Breaking a machine spills its tray, credit, stock and cash box.
- You can see what a machine sells: items on its shelves with price tags (FREE and SOLD OUT too), and what's waiting in the tray.
- The display shows SELECT ITEM or your own credit, flashes THANK YOU or what went wrong (NEED 3, SOLD OUT…), and scrolls every problem in red while the warning lamp blinks.
- Bought items drop from their shelf into the tray.
- Look at the front of a machine to see a tooltip: what a button sells and costs, your credit, what's in the tray, any problems, and who owns it.
- Owners and admins set up a machine on a setup screen — empty both hands, then sneak + right-click it. Items tab: click a slot while holding an item to sell it on that button, then set the amount and the price. Stock and Cash Box tabs (with Withdraw all), and an Admin tab for admins.
- The setup screen lists every problem in a red banner and puts a red "!" on the tab that fixes it.
- Admins can make a machine infinite (its Stock and Cash Box must be empty first), give it a datapack catalog, or set the currency it takes. Only admins can change or break an infinite machine, even its owner can't.
- Datapack catalogs, with an example (`diamondvending:example_snacks`) and a pack-maker guide in `docs/catalogs.md`.
- A broken machine keeps its setup on the item; it comes back infinite only when an admin places it.
- Sneaking at your own machine with something in your hand tells you to empty your hands first.
- The Diamond Vending Manual: craft a book with a gold nugget (it unlocks with your first diamond). Eleven short pages explain everything a player could trip on.
