package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint. Excluded from Fabric builds. */
@Mod(DiamondVending.MOD_ID)
public final class DiamondVendingNeoForge {
    public DiamondVendingNeoForge(IEventBus modBus, ModContainer container) {
        DiamondVending.init();
    }
}
