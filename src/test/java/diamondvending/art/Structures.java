package diamondvending.art;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Structure templates used by the GameTests. */
final class Structures {
    /** Minecraft 1.20.1's data version, the oldest we target; newer versions upgrade the structure when loading it. */
    private static final int DATA_VERSION_1_20_1 = 3465;

    private Structures() {}

    /** The GameTest stage: a 6×4×6 box with a polished andesite floor at y = 0 and air above. */
    static Map<String, Object> gametestPlatform() {
        int sizeX = 6;
        int sizeY = 4;
        int sizeZ = 6;
        List<Object> blocks = new ArrayList<>();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    Map<String, Object> block = NbtWriter.compound();
                    block.put("pos", List.of(x, y, z));
                    block.put("state", y == 0 ? 0 : 1);
                    blocks.add(block);
                }
            }
        }
        Map<String, Object> floor = NbtWriter.compound();
        floor.put("Name", "minecraft:polished_andesite");
        Map<String, Object> air = NbtWriter.compound();
        air.put("Name", "minecraft:air");

        Map<String, Object> root = NbtWriter.compound();
        root.put("DataVersion", DATA_VERSION_1_20_1);
        root.put("size", List.of(sizeX, sizeY, sizeZ));
        root.put("palette", List.of(floor, air));
        root.put("blocks", blocks);
        root.put("entities", List.of());
        return root;
    }
}
