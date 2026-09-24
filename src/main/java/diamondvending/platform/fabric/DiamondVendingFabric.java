package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.block.SneakHint;
import diamondvending.catalog.CatalogLoader;
import diamondvending.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.resource.v1.DataResourceLoader;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
*///?}

/** Fabric entrypoint (declared in fabric.mod.json). Excluded from NeoForge builds. */
public final class DiamondVendingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DiamondVending.init();
        ModContent.register(new FabricRegistrar());
        //? if >=26.1 {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(output -> output.accept(ModContent.VENDING_MACHINE_ITEM.get()));
        DataResourceLoader.get().registerReloadListener(CatalogLoader.ID, CatalogLoader::new);
        //?} else {
        /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(ModContent.VENDING_MACHINE_ITEM.get()));
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(CatalogLoader.ID, FabricCatalogLoader::new);
        *///?}
        // SUCCESS stops the click here; on the client it still goes to the server, which sends the hint.
        UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                SneakHint.cancels(player, level, hit) ? InteractionResult.SUCCESS : InteractionResult.PASS);
    }

    //? if <26.1 {
    /*// Fabric 1.21.1 wants each reload listener to carry its id.
    private static final class FabricCatalogLoader extends CatalogLoader implements IdentifiableResourceReloadListener {
        FabricCatalogLoader(HolderLookup.Provider registries) {
            super(registries);
        }

        @Override
        public Identifier getFabricId() {
            return CatalogLoader.ID;
        }
    }
    *///?}
}
