package diamondvending.platform.neoforge;

import diamondvending.DiamondVending;
import diamondvending.client.VendingMachineRenderer;
import diamondvending.registry.ModContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** NeoForge client entrypoint: a second {@code @Mod} class that NeoForge only loads on a game client. */
@Mod(value = DiamondVending.MOD_ID, dist = Dist.CLIENT)
public final class DiamondVendingNeoForgeClient {
    public DiamondVendingNeoForgeClient(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new));
    }
}
