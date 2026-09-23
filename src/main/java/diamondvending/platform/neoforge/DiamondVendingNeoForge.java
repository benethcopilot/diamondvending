package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.registry.ModContent;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

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
    }
}
