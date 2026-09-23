package diamondvending.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static diamondvending.art.Json.obj;

/**
 * Block models, the blockstate, and item models. Part models face north; the blockstate rotates them.
 * Looking at a north face, texture u runs toward the viewer's right, matching the canvas.
 */
final class Models {
    /** The generator's own 4-part list (independent of the mod's MachinePart): right column? upper row? */
    enum Part {
        LOWER_LEFT(false, false), LOWER_RIGHT(true, false), UPPER_LEFT(false, true), UPPER_RIGHT(true, true);

        final boolean right;
        final boolean upper;

        Part(boolean right, boolean upper) {
            this.right = right;
            this.upper = upper;
        }

        String half() {
            return upper ? "upper" : "lower";
        }

        String side() {
            return right ? "right" : "left";
        }
    }

    private static final String[] FACINGS = {"east", "north", "south", "west"};
    private static final int[] FACING_Y = {90, 0, 180, 270};

    private Models() {}

    static String partModelName(Dye dye, Part part) {
        return "block/vending_machine/" + dye.id() + "_" + part.half() + "_" + part.side();
    }

    static Map<String, Object> partModel(Dye dye, Part part) {
        int u0 = part.right ? 8 : 0;
        int v0 = part.upper ? 0 : 8;
        String side = "diamondvending:block/vending_machine_side_" + dye.id();
        Map<String, Object> faces = obj(
                "north", obj("uv", List.of(u0, v0, u0 + 8, v0 + 8), "texture", "#front", "cullface", "north"),
                "south", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "south"),
                "east", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "east"),
                "west", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "west"),
                "up", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "up"),
                "down", obj("uv", List.of(0, 0, 16, 16), "texture", "#side", "cullface", "down"));
        return obj(
                "parent", "minecraft:block/block",
                "textures", obj(
                        "particle", side,
                        "front", "diamondvending:block/vending_machine_front_" + dye.id(),
                        "side", side),
                "elements", List.of(obj("from", List.of(0, 0, 0), "to", List.of(16, 16, 16), "faces", faces)));
    }

    static Map<String, Object> blockstate() {
        Map<String, Object> variants = obj();
        for (Dye dye : Dye.values()) {
            for (int f = 0; f < FACINGS.length; f++) {
                for (Part part : Part.values()) {
                    String key = "color=" + dye.id() + ",facing=" + FACINGS[f] + ",half=" + part.half() + ",side=" + part.side();
                    Map<String, Object> variant = obj("model", "diamondvending:" + partModelName(dye, part));
                    if (FACING_Y[f] != 0) variant.put("y", FACING_Y[f]);
                    variants.put(key, variant);
                }
            }
        }
        return obj("variants", variants);
    }

    static Map<String, Object> itemModel(Dye dye) {
        return obj("parent", "minecraft:item/generated",
                "textures", obj("layer0", "diamondvending:item/vending_machine_" + dye.id()));
    }

    /** 26.1: pick the model from the item's base_color component. */
    static Map<String, Object> itemDefinition26() {
        List<Object> cases = new ArrayList<>();
        for (Dye dye : Dye.values()) {
            cases.add(obj("when", dye.id(),
                    "model", obj("type", "minecraft:model", "model", "diamondvending:item/vending_machine_" + dye.id())));
        }
        return obj("model", obj(
                "type", "minecraft:select",
                "property", "minecraft:component",
                "component", "minecraft:base_color",
                "cases", cases,
                "fallback", obj("type", "minecraft:model", "model", "diamondvending:item/vending_machine_red")));
    }

    /** 1.21.1: overrides on custom_model_data (dye id + 1); no data means red. */
    static Map<String, Object> itemModel1211() {
        List<Object> overrides = new ArrayList<>();
        for (Dye dye : Dye.values()) {
            overrides.add(obj("predicate", obj("custom_model_data", dye.ordinal() + 1),
                    "model", "diamondvending:item/vending_machine_" + dye.id()));
        }
        return obj("parent", "minecraft:item/generated",
                "textures", obj("layer0", "diamondvending:item/vending_machine_red"),
                "overrides", overrides);
    }
}
