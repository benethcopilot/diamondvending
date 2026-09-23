package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import net.fabricmc.api.ModInitializer;

/** Fabric entrypoint (declared in fabric.mod.json). Excluded from NeoForge builds. */
public final class DiamondVendingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DiamondVending.init();
    }
}
