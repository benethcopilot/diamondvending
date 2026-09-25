"""Stages one release-QA scene on the running QA server through RCON (docs/qa-checklist.md lists them in order).

Usage: python tools/qa/scene.py <scene> <node>
"""
import sys

from rcon import send

MACHINE = "diamondvending:vending_machine[facing=south,"
# The client test's machine (FabricClientTests): six buttons, the cake sold out, three cookies in the tray.
SHOP = ('{owner_name:"Tester",'
        'selections:[{slot:0,item:{id:"minecraft:apple",count:2},price:3},'
        '{slot:1,item:{id:"minecraft:oak_log",count:4},price:1},'
        '{slot:2,item:{id:"minecraft:diamond_sword",count:1},price:12},'
        '{slot:4,item:{id:"minecraft:bread",count:1},price:0},'
        '{slot:7,item:{id:"minecraft:enchanted_golden_apple",count:1},price:64},'
        '{slot:11,item:{id:"minecraft:cake",count:1},price:5}],'
        'stock:{Items:[{Slot:0b,id:"minecraft:apple",count:64},{Slot:1b,id:"minecraft:oak_log",count:64},'
        '{Slot:2b,id:"minecraft:diamond_sword",count:1},{Slot:3b,id:"minecraft:bread",count:64},'
        '{Slot:4b,id:"minecraft:enchanted_golden_apple",count:3}]},'
        'tray:{Items:[{Slot:0b,id:"minecraft:cookie",count:3}]}}')
# Dye colors in id order (0-15); 1.21.1 item models pick the color by custom_model_data = id + 1.
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
KEPT = 'diamondvending:machine_setup={selections:[{slot:0,item:{id:"minecraft:cake",count:1},price:5}]}'


def machine(x, z, color=None, contents=""):
    """The four setblocks of a south-facing machine whose lower-left part is at (x, -60, z); its right column is x + 1."""
    state = MACHINE + (f"color={color}," if color else "")
    return [f"setblock {x} -60 {z} {state}half=lower,side=left]{contents}",
            f"setblock {x + 1} -60 {z} {state}half=lower,side=right]",
            f"setblock {x} -59 {z} {state}half=upper,side=left]",
            f"setblock {x + 1} -59 {z} {state}half=upper,side=right]"]


def item(color, old):
    components = f"base_color={color}" + (f",custom_model_data={COLORS.index(color) + 1}" if old else "")
    return f"diamondvending:vending_machine[{components}]"


def commands(scene, node):
    old = node.startswith("1.21.1")
    return {
        "start": ["op Dev", "time set noon", "weather clear", "gamemode survival Dev"],
        "shop": ["fill -4 -61 -4 5 -61 6 minecraft:smooth_stone", *machine(0, 0, contents=SHOP),
                 "clear Dev", "give Dev minecraft:diamond 10", "tp Dev 1 -60 3.5 180 10"],
        "buy": ["tp Dev 1.625 -60 3.5 180 2.75"],  # crosshair on button 1
        "setup": ["clear Dev", "tp Dev 1 -60 3.5 180 10"],
        "hint": ["give Dev minecraft:stone 5"],
        "kept": ["clear Dev", f"give Dev diamondvending:vending_machine[{KEPT}]",
                 "fill 6 -61 -4 12 -61 6 minecraft:smooth_stone", "tp Dev 9 -60 3.5 180 50"],  # looking at the floor
        "kept_view": ["tp Dev 10 -60 5.5 180 10"],
        # West of everything else, so no other machine stands in the way of the view
        "colors": [c for i, color in enumerate(COLORS) for c in machine(-63 + 3 * i, -12, color)]
                  + ["tp Dev -40 -60 10 180 3"],
        "icons": ["clear Dev"] + [f"give Dev {item(color, old)}" for color in ("red", "blue", "lime", "black")],
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
