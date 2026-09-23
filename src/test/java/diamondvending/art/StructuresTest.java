package diamondvending.art;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuresTest {

    @Test
    @SuppressWarnings("unchecked")
    void platformIsA6x4x6BoxWithAFloor() {
        Map<String, Object> platform = Structures.gametestPlatform();
        assertEquals(List.of(6, 4, 6), platform.get("size"));
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) platform.get("blocks");
        assertEquals(6 * 4 * 6, blocks.size());
        for (Map<String, Object> block : blocks) {
            List<Integer> pos = (List<Integer>) block.get("pos");
            assertEquals(pos.get(1) == 0 ? 0 : 1, block.get("state"), "floor at y=0, air above: " + pos);
        }
        List<Map<String, Object>> palette = (List<Map<String, Object>>) platform.get("palette");
        assertEquals("minecraft:polished_andesite", palette.get(0).get("Name"));
        assertEquals("minecraft:air", palette.get(1).get("Name"));
    }
}
