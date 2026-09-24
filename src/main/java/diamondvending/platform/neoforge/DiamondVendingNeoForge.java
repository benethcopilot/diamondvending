package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.block.SneakHint;
import diamondvending.catalog.CatalogLoader;
import diamondvending.registry.ModContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//? if >=26.1 {
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
//?} else {
/*import net.neoforged.neoforge.event.AddReloadListenerEvent;
*///?}

/** NeoForge entrypoint. Excluded from Fabric builds. */
@Mod(DiamondVending.MOD_ID)
public final class DiamondVendingNeoForge {
    public DiamondVendingNeoForge(IEventBus modBus, ModContainer container) {
        DiamondVending.init();
        NeoForgeRegistrar registrar = new NeoForgeRegistrar();
        ModContent.register(registrar);
        registrar.registerAll(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                event.accept(ModContent.VENDING_MACHINE_ITEM.get());
            }
        });
        //? if >=26.1 {
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class,
                event -> event.addListener(CatalogLoader.ID, new NeoForgeCatalogLoader()));
        //?} else {
        /*NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, event -> event.addListener(new NeoForgeCatalogLoader()));
        *///?}
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
            if (SneakHint.cancels(event.getEntity(), event.getLevel(), event.getHitVec())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
    }

    // NeoForge hands each reload listener the registries, tags included, just before it runs.
    private static final class NeoForgeCatalogLoader extends CatalogLoader {
        NeoForgeCatalogLoader() {
            super(null);
        }

        @Override
        protected HolderLookup.Provider registries() {
            return getRegistryLookup();
        }
    }
}
