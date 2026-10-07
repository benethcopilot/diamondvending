package diamondvending.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import diamondvending.DiamondVending;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads catalogs from datapacks, like recipes (spec §5.1). It needs the game's registries, because catalog items can
 * carry components that name registry entries (a book's enchantments): Fabric passes them in, NeoForge hands them to
 * the listener just before it runs (see {@link #registries()}). A file with a mistake is skipped, and the log says which
 * file and what's wrong.
 */
public class CatalogLoader extends SimplePreparableReloadListener<Map<Identifier, Catalog>> {
    public static final Identifier ID = DiamondVending.id("catalogs");
    private static final FileToIdConverter FILES = FileToIdConverter.json("diamondvending/catalog");

    private final HolderLookup.Provider registries;

    public CatalogLoader(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    /** The registries to decode with; NeoForge's listener overrides this with the ones it's given. */
    protected HolderLookup.Provider registries() {
        return registries;
    }

    @Override
    protected Map<Identifier, Catalog> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Catalog> catalogs = new HashMap<>();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries());
        FILES.listMatchingResources(manager).forEach((file, resource) -> {
            Identifier id = FILES.fileToId(file);
            try (Reader reader = resource.openAsReader()) {
                DataResult<Catalog> read = Catalog.CODEC.parse(ops, JsonParser.parseReader(reader));
                read.result().ifPresent(catalog -> catalogs.put(id, catalog));
                read.error().ifPresent(error -> DiamondVending.LOGGER.error("Skipping vending machine catalog {} ({}): {}", id, file, error.message()));
            } catch (IOException | RuntimeException e) {
                DiamondVending.LOGGER.error("Skipping vending machine catalog {} ({}): {}", id, file, e.getMessage());
            }
        });
        return catalogs;
    }

    @Override
    protected void apply(Map<Identifier, Catalog> catalogs, ResourceManager manager, ProfilerFiller profiler) {
        Catalogs.replace(catalogs);
        DiamondVending.LOGGER.info("Loaded {} vending machine catalog(s)", catalogs.size());
    }
}
