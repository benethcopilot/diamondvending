package diamondvending.art;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NbtWriterTest {

    @Test
    void writesACompoundWithAnInt() {
        Map<String, Object> root = NbtWriter.compound();
        root.put("a", 1);
        assertArrayEquals(new byte[] {10, 0, 0, 3, 0, 1, 'a', 0, 0, 0, 1, 0}, NbtWriter.toBytes(root));
    }

    @Test
    void writesListsOfIntsAndEmptyLists() {
        Map<String, Object> root = NbtWriter.compound();
        root.put("l", List.of(7));
        root.put("e", List.of());
        assertArrayEquals(new byte[] {
                10, 0, 0,
                9, 0, 1, 'l', 3, 0, 0, 0, 1, 0, 0, 0, 7,
                9, 0, 1, 'e', 0, 0, 0, 0, 0,
                0}, NbtWriter.toBytes(root));
    }

    @Test
    void writesStringsAndNestedCompounds() {
        Map<String, Object> inner = NbtWriter.compound();
        inner.put("s", "hi");
        Map<String, Object> root = NbtWriter.compound();
        root.put("c", inner);
        assertArrayEquals(new byte[] {10, 0, 0, 10, 0, 1, 'c', 8, 0, 1, 's', 0, 2, 'h', 'i', 0, 0}, NbtWriter.toBytes(root));
    }

    @Test
    void rejectsUnsupportedValues() {
        Map<String, Object> root = NbtWriter.compound();
        root.put("d", 1.5);
        assertThrows(IllegalArgumentException.class, () -> NbtWriter.toBytes(root));
    }
}
