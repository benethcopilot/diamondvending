package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.registry.ModContent;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.CreativeModeTabs;
//? if >=26.1 {
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
//?} else {
/*import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
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
        //?} else {
        /*ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(ModContent.VENDING_MACHINE_ITEM.get()));
        *///?}
    }
}
