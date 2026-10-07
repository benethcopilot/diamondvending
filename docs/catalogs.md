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

### Minecraft 1.20.1 (Forge)

1.20.1 items have no components; an entry's item carries its NBT as a string instead, written the way `/give` takes it:
`{ "id": ..., "count": ..., "nbt": "<SNBT>" }`. The mending book from the example above is

```json
{ "item": { "id": "minecraft:enchanted_book", "nbt": "{StoredEnchantments:[{id:\"minecraft:mending\",lvl:1s}]}" }, "price": 12 }
```

A catalog written for the other version is skipped rather than selling plain items, and the log says how to fix it:
`components` on 1.20.1 gives *item components need Minecraft 1.20.5 or newer; on 1.20.1 write the item's NBT as "nbt"*,
and `nbt` on 1.21.1 or 26.1 gives *nbt is for Minecraft 1.20.1; on this version use "components"*. 1.20.1 datapacks also
use the older plural folder names, e.g. `tags/items` instead of `tags/item`.

## Currency

A machine takes, first match wins:

1. its **currency slot** (Admin tab of the setup screen),
2. its catalog's `currency`,
3. the item tag **`#diamondvending:currency`**, which ships with just `minecraft:diamond`.

To change the default for a whole pack, override `data/diamondvending/tags/item/currency.json` in a datapack
(`tags/items/currency.json` on 1.20.1). The tag's
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
| `infinite` | byte, 0 or 1 | Never sells out; the money disappears. Only admins can set up or break an infinite machine, even if it has an owner. |
| `catalog` | string | A catalog id. |
| `currency` | string | An item id: the currency slot. |
| `selections` | list of `{slot: 0–11, item: {id, count, components}, price: 0–999}` | The machine's own buttons (`slot` 0 is button 1). |
| `stock`, `cash_box`, `tray` | `{Items: [{Slot: 0b, id, count}]}` | Contents. |

On 1.20.1, items in these fields are written the old way: `{id, Count: 1b, tag}` (a byte count, and NBT under `tag`).
