package diamondvending.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Every game test by name, for loaders that register tests from a list (NeoForge 26.1). */
public final class AllTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = combine(List.of(MachineTests.ALL, BuyingTests.ALL, DisplayTests.ALL));

    private AllTests() {}

    private static Map<String, Consumer<GameTestHelper>> combine(List<Map<String, Consumer<GameTestHelper>>> groups) {
        Map<String, Consumer<GameTestHelper>> all = new LinkedHashMap<>();
        for (Map<String, Consumer<GameTestHelper>> group : groups) {
            group.forEach((name, test) -> {
                if (all.putIfAbsent(name, test) != null) throw new IllegalStateException("two game tests are called " + name);
            });
        }
        return Map.copyOf(all);
    }
}
