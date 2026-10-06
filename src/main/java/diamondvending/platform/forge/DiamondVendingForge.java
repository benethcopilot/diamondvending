package diamondvending.platform.forge;

import diamondvending.DiamondVending;
import diamondvending.block.SneakHint;
import diamondvending.catalog.CatalogLoader;
import diamondvending.registry.ModContent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Forge (1.20.1) entrypoint. Only compiled into the Forge jar. */
@Mod(DiamondVending.MOD_ID)
public final class DiamondVendingForge {
    public DiamondVendingForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DiamondVending.init();
        ForgeRegistrar registrar = new ForgeRegistrar();
        ModContent.register(registrar);
        registrar.registerAll(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                event.accept(ModContent.VENDING_MACHINE_ITEM.get());
            }
        });
        // Forge hands the reload its registries up front, tags included.
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(new CatalogLoader(event.getRegistryAccess())));
        MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
            if (SneakHint.cancels(event.getEntity(), event.getLevel(), event.getHitVec())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        // The client class is only loaded on a game client.
        if (FMLEnvironment.dist.isClient()) DiamondVendingForgeClient.register(modBus);
    }
}
