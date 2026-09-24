package diamondvending.client;

import com.mojang.blaze3d.vertex.PoseStack;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.scene.MachineScene;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
*///?}

/**
 * Draws a machine's front — shelf items, price labels, the display, the lamp, the tray, the falling item — from its
 * {@link MachineScene}. 26.1 gathers everything first and then submits draw calls; 1.21.1 draws straight away.
 */
//? if >=26.1 {
public final class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity, VendingMachineRenderer.State> {
    private final ItemModelResolver itemModels;
    private final Font font;

    public VendingMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
        this.font = context.font();
    }

    // What one frame needs, gathered from the machine before drawing.
    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        MachineScene scene = MachineScene.EMPTY;
        final List<ItemStackRenderState> items = new ArrayList<>();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VendingMachineBlockEntity machine, State state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(machine, state, partialTicks, camera, breakProgress);
        state.facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        state.items.clear();
        if (machine.getLevel() == null || !FrontCanvas.visibleFrom(machine, camera.x, camera.z)) {
            state.scene = MachineScene.EMPTY;
            return;
        }
        // Light from the air in front of the machine: the machine's own block is solid, so its light is dark.
        state.lightCoords = LevelRenderer.getLightCoords(machine.getLevel(), machine.getBlockPos().relative(state.facing));
        state.scene = FrontCanvas.scene(machine, partialTicks);
        for (MachineScene.Item item : state.scene.items()) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            itemModels.updateForTopItem(itemState, item.stack(), ItemDisplayContext.FIXED, machine.getLevel(), null, 0);
            state.items.add(itemState);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        FrontCanvas.enter(pose, state.facing);
        for (MachineScene.Glow glow : state.scene.glows()) {
            out.submitCustomGeometry(pose, RenderTypes.textBackground(), (at, vertices) -> FrontCanvas.glow(at, vertices, glow));
        }
        for (int i = 0; i < state.items.size(); i++) {
            pose.pushPose();
            FrontCanvas.placeItem(pose, state.scene.items().get(i));
            state.items.get(i).submit(pose, out, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        for (MachineScene.Text text : state.scene.texts()) {
            FormattedCharSequence line = Component.literal(text.text()).getVisualOrderText();
            pose.pushPose();
            FrontCanvas.placeText(pose, text);
            out.submitText(pose, FrontCanvas.textX(text, font.width(line)), FrontCanvas.TEXT_Y, line, false,
                    Font.DisplayMode.POLYGON_OFFSET, FrontCanvas.FULL_BRIGHT, text.color(), 0, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    // The machine is 2 x 2 but its block entity sits in one block: culling by that block would hide it too early.
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return FrontCanvas.VIEW_DISTANCE;
    }
}
//?} else {
/*public final class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity> {
    private final ItemRenderer itemRenderer;
    private final Font font;

    public VendingMachineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(VendingMachineBlockEntity machine, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (machine.getLevel() == null || !FrontCanvas.visibleFrom(machine, camera.x, camera.z)) return;
        Direction facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        // Light from the air in front of the machine: the machine's own block is solid, so its light is dark.
        int frontLight = LevelRenderer.getLightColor(machine.getLevel(), machine.getBlockPos().relative(facing));
        MachineScene scene = FrontCanvas.scene(machine, partialTicks);
        pose.pushPose();
        FrontCanvas.enter(pose, facing);
        VertexConsumer glows = buffers.getBuffer(RenderType.textBackground());
        for (MachineScene.Glow glow : scene.glows()) FrontCanvas.glow(pose.last(), glows, glow);
        for (MachineScene.Item item : scene.items()) {
            pose.pushPose();
            FrontCanvas.placeItem(pose, item);
            itemRenderer.renderStatic(item.stack(), ItemDisplayContext.FIXED, frontLight, OverlayTexture.NO_OVERLAY, pose, buffers, machine.getLevel(), 0);
            pose.popPose();
        }
        for (MachineScene.Text text : scene.texts()) {
            pose.pushPose();
            FrontCanvas.placeText(pose, text);
            font.drawInBatch(text.text(), FrontCanvas.textX(text, font.width(text.text())), FrontCanvas.TEXT_Y, text.color(), false,
                    pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, FrontCanvas.FULL_BRIGHT);
            pose.popPose();
        }
        pose.popPose();
    }

    // The machine is 2 x 2 but its block entity sits in one block: culling by that block would hide it too early.
    @Override
    public boolean shouldRenderOffScreen(VendingMachineBlockEntity machine) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return FrontCanvas.VIEW_DISTANCE;
    }
}
*///?}
