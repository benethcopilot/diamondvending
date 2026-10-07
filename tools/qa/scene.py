"""Stages one release-QA scene on the running QA server through RCON (docs/qa-checklist.md lists them in order).

Usage: python tools/qa/scene.py <scene> <node>
"""
import json
import pathlib
import sys

from rcon import send

MACHINE = "diamondvending:vending_machine[facing=south,"
# The client test's machine (FabricClientTests): six buttons, the cake sold out, three cookies in the tray.
SELECTIONS = [(0, "minecraft:apple", 2, 3), (1, "minecraft:oak_log", 4, 1), (2, "minecraft:diamond_sword", 1, 12),
              (4, "minecraft:bread", 1, 0), (7, "minecraft:enchanted_golden_apple", 1, 64), (11, "minecraft:cake", 1, 5)]
STOCK = [(0, "minecraft:apple", 64), (1, "minecraft:oak_log", 64), (2, "minecraft:diamond_sword", 1),
         (3, "minecraft:bread", 64), (4, "minecraft:enchanted_golden_apple", 3)]
TRAY = [(0, "minecraft:cookie", 3)]
# Dye colors in id order (0-15); 1.20.1 and 1.21.1 item models pick the color by custom model data = id + 1.
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
ROOT = pathlib.Path(__file__).resolve().parents[2]


def stack(item, count, nbt):
    """An item stack as the version saves it: 1.20.1 {id, Count (byte)}, newer {id, count}."""
    return f'{{id:"{item}",Count:{count}b}}' if nbt else f'{{id:"{item}",count:{count}}}'


def slots(entries, nbt):
    return "{Items:[" + ",".join("{Slot:" + f"{slot}b," + stack(item, count, nbt)[1:] for slot, item, count in entries) + "]}"


def shop(nbt):
    selections = ",".join(f"{{slot:{slot},item:{stack(item, count, nbt)},price:{price}}}" for slot, item, count, price in SELECTIONS)
    return f'{{owner_name:"Tester",selections:[{selections}],stock:{slots(STOCK, nbt)},tray:{slots(TRAY, nbt)}}}'


def machine(x, z, color=None, contents=""):
    """The four setblocks of a south-facing machine whose lower-left part is at (x, -60, z); its right column is x + 1."""
    state = MACHINE + (f"color={color}," if color else "")
    return [f"setblock {x} -60 {z} {state}half=lower,side=left]{contents}",
            f"setblock {x + 1} -60 {z} {state}half=lower,side=right]",
            f"setblock {x} -59 {z} {state}half=upper,side=left]",
            f"setblock {x + 1} -59 {z} {state}half=upper,side=right]"]


def item(color, version):
    """A machine item in a color, as /give takes it."""
    if version == "1.20.1":
        return f'diamondvending:vending_machine{{"diamondvending:color":"{color}",CustomModelData:{COLORS.index(color) + 1}}}'
    components = f"base_color={color}" + (f",custom_model_data={COLORS.index(color) + 1}" if version == "1.21.1" else "")
    return f"diamondvending:vending_machine[{components}]"


def kept(version):
    """A machine item that kept a setup selling one cake for 5."""
    setup = f"{{selections:[{{slot:0,item:{stack('minecraft:cake', 1, version == '1.20.1')},price:5}}]}}"
    if version == "1.20.1":
        return f'diamondvending:vending_machine{{"diamondvending:machine_setup":{setup}}}'
    return f"diamondvending:vending_machine[diamondvending:machine_setup={setup}]"


def manual_1201(node):
    """1.20.1 has no crafter: a function in the QA world gives the manual with the recipe's own NBT (too long for RCON)."""
    recipe = json.loads((ROOT / "src/main/resources-1.20.1/data/diamondvending/recipes/manual.json").read_text(encoding="utf-8"))
    book = recipe["result"]["nbt"]
    pages = ",".join(json.dumps(page) for page in book["pages"])
    snbt = f'{{title:{json.dumps(book["title"])},author:{json.dumps(book["author"])},resolved:1b,pages:[{pages}]}}'
    pack = ROOT / "run" / f"{node}-server" / "qa-world" / "datapacks" / "diamondvending_qa"
    (pack / "data" / "diamondvending_qa" / "functions").mkdir(parents=True, exist_ok=True)
    (pack / "pack.mcmeta").write_text('{"pack":{"description":"Diamond Vending QA","pack_format":15}}\n', encoding="utf-8")
    (pack / "data" / "diamondvending_qa" / "functions" / "manual.mcfunction").write_text(
        f"give Dev minecraft:written_book{snbt}\n", encoding="utf-8")
    return ["reload", "function diamondvending_qa:manual"]


def commands(scene, node):
    version = node.split("-")[0]
    nbt = version == "1.20.1"  # 1.20.1 items carry NBT, not components
    if scene == "manual" and nbt:
        return ["clear Dev", "tp Dev 20.5 -60 2.5 180 0", *manual_1201(node)]
    return {
        "start": ["op Dev", "time set noon", "weather clear", "gamemode survival Dev"],
        "shop": ["fill -4 -61 -4 5 -61 6 minecraft:smooth_stone", *machine(0, 0, contents=shop(nbt)),
                 "clear Dev", "give Dev minecraft:diamond 10", "tp Dev 1 -60 3.5 180 10"],
        "buy": ["tp Dev 1.625 -60 3.5 180 2.75"],  # crosshair on button 1
        "setup": ["clear Dev", "tp Dev 1 -60 3.5 180 10"],
        "hint": ["give Dev minecraft:stone 5"],
        "kept": ["clear Dev", f"give Dev {kept(version)}",
                 "fill 6 -61 -4 12 -61 6 minecraft:smooth_stone", "tp Dev 9 -60 3.5 180 50"],  # looking at the floor
        "kept_view": ["tp Dev 10 -60 5.5 180 10"],
        # West of everything else, so no other machine stands in the way of the view
        "colors": [c for i, color in enumerate(COLORS) for c in machine(-63 + 3 * i, -12, color)]
                  + ["tp Dev -40 -60 10 180 3"],
        "icons": ["clear Dev"] + [f"give Dev {item(color, version)}" for color in ("red", "blue", "lime", "black")],
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
