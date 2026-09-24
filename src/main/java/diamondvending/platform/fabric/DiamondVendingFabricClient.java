package diamondvending.platform.fabric;

import diamondvending.client.VendingMachineRenderer;
import diamondvending.registry.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/** Fabric client entrypoint (the {@code client} entrypoint in fabric.mod.json): only ever loaded on a game client. */
public final class DiamondVendingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Vanilla's registration, opened up by Fabric API's transitive access wideners (its own registry is deprecated).
        BlockEntityRenderers.register(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new);
    }
}
