package diamondvending.platform.fabric;

import diamondvending.DiamondVending;
import diamondvending.client.HoverHud;
import diamondvending.client.VendingSetupScreen;
import diamondvending.client.VendingMachineRenderer;
import diamondvending.registry.ModContent;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
*///?}

/** Fabric client entrypoint (the {@code client} entrypoint in fabric.mod.json): only ever loaded on a game client. */
public final class DiamondVendingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Vanilla's registration, opened up by Fabric API's transitive access wideners (its own registry is deprecated).
        BlockEntityRenderers.register(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new);
        //? if >=26.1 {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, DiamondVending.id("hover"), HoverHud::render);
        //?} else {
        /*HudRenderCallback.EVENT.register(HoverHud::render);
        *///?}
        // Vanilla's registration, opened up by Fabric API's transitive access wideners on both versions.
        MenuScreens.register(ModContent.SETUP_MENU.type(), VendingSetupScreen::new);
    }
}
