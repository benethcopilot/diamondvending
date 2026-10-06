package diamondvending.platform.forge;

import diamondvending.client.HoverHud;
import diamondvending.client.VendingMachineRenderer;
import diamondvending.client.VendingSetupScreen;
import diamondvending.registry.ModContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Forge client setup, registered by {@link DiamondVendingForge} only on a game client. */
final class DiamondVendingForgeClient {
    private DiamondVendingForgeClient() {}

    static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineRenderer::new));
        modBus.addListener((RegisterGuiOverlaysEvent event) -> event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "hover",
                (gui, graphics, partialTick, width, height) -> HoverHud.render(graphics)));
        // Vanilla's screen registry isn't thread-safe, so the screen is registered on the main thread.
        modBus.addListener((FMLClientSetupEvent event) ->
                event.enqueueWork(() -> MenuScreens.register(ModContent.SETUP_MENU.type(), VendingSetupScreen::new)));
    }
}
