package diamondvending.catalog;

import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The catalogs the server has loaded (spec §5.1), replaced whenever datapacks load or /reload runs. Server side only. */
public final class Catalogs {
    private static volatile Map<Identifier, Catalog> loaded = Map.of();
    private static volatile int generation;

    private Catalogs() {}

    /** The catalog with this id, or null if none is loaded under it. */
    public static Catalog get(Identifier id) {
        return loaded.get(id);
    }

    /** Every loaded catalog's id, in order. */
    public static List<Identifier> ids() {
        return loaded.keySet().stream().sorted().toList();
    }

    /** Goes up on every load, so machines can tell that their catalog may have changed. */
    public static int generation() {
        return generation;
    }

    /**
     * The Admin tab's picker (spec §4): "None" (null), then every catalog in order, and round again. {@code step} is
     * +1 or −1. A catalog that isn't loaded any more sits just before "None", so ▶ clears it in one press (spec §5.1:
     * an admin picks another catalog or clears it) and ◀ picks the last catalog.
     */
    public static Identifier cycle(Identifier current, int step) {
        List<Identifier> choices = new ArrayList<>();
        choices.add(null);
        choices.addAll(ids());
        if (current != null && !choices.contains(current)) return step > 0 ? null : choices.get(choices.size() - 1);
        return choices.get(Math.floorMod(choices.indexOf(current) + step, choices.size()));
    }

    static void replace(Map<Identifier, Catalog> catalogs) {
        loaded = Map.copyOf(catalogs);
        generation++;
    }
}
