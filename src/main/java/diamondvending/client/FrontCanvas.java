package diamondvending.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Facing;
import diamondvending.scene.MachineScene;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Where things go on a machine's front: turns the canvas pixels of a {@link MachineScene} into places for the renderer. */
final class FrontCanvas {
    /** Spec §2.3: nothing is drawn beyond 32 blocks. */
    static final int VIEW_DISTANCE = 32;
    /** Block light 15 and sky light 15: the display, lamp and labels glow in the dark. */
    static final int FULL_BRIGHT = 0xF000F0;
    /** Text is drawn this far up from its v, so the middle of the 8-unit-tall line sits on v. */
    static final float TEXT_Y = -4.0F;
    private static final float GLOW_DEPTH = -0.02F;
    private static final float TEXT_DEPTH = -0.05F;
    private static final float ITEM_DEPTH = -0.7F;
    /** Items are squashed flat against the glass so block items don't poke into the machine. */
    private static final float ITEM_THICKNESS = 0.2F;

    private FrontCanvas() {}

    /** Whether the camera is in front of the machine (spec §2.3: nothing is drawn when the front isn't visible). */
    static boolean visibleFrom(VendingMachineBlockEntity machine, double cameraX, double cameraZ) {
        Direction facing = machine.getBlockState().getValue(VendingMachineBlock.FACING);
        BlockPos pos = machine.getBlockPos();
        return Facing.valueOf(facing.name()).frontVisible(pos.getX(), pos.getZ(), cameraX, cameraZ);
    }

    /** The scene for this client's player, now. */
    static MachineScene scene(VendingMachineBlockEntity machine, float partialTicks) {
        Player player = Minecraft.getInstance().player;
        UUID viewer = player != null ? player.getUUID() : new UUID(0, 0);
        return MachineScene.of(machine, viewer, machine.getLevel().getGameTime() + (double) partialTicks, Component::getString);
    }

    /**
     * Moves the pose from the master block's corner to the front canvas: origin at the canvas's top-left, u to the right,
     * v down, one unit per canvas pixel, depth pointing into the machine.
     */
    static void enter(PoseStack pose, Direction facing) {
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5F, 2.0F, 0.5F);
        pose.scale(1 / 16F, -1 / 16F, -1 / 16F);
    }

    static void placeItem(PoseStack pose, MachineScene.Item item) {
        pose.translate(item.u(), item.v(), ITEM_DEPTH);
        // The canvas is y-down: turn items upright. Around z, like an item frame does — around x, they showed their backs
        // (swords pointed the wrong way).
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.scale(item.size(), item.size(), item.size() * ITEM_THICKNESS);
    }

    static void placeText(PoseStack pose, MachineScene.Text text) {
        pose.translate(text.u(), text.v(), TEXT_DEPTH);
        float scale = text.height() / 8F;
        pose.scale(scale, scale, scale);
    }

    /** Where a line of {@code width} font units starts, for its alignment. */
    static float textX(MachineScene.Text text, int width) {
        return switch (text.align()) {
            case LEFT -> 0;
            case CENTER -> -width / 2F;
            case RIGHT -> -width;
        };
    }

    /** A flat glowing rectangle, drawn both ways round so it shows whichever side the render type culls. */
    static void glow(PoseStack.Pose at, VertexConsumer out, MachineScene.Glow glow) {
        vertex(out, at, glow.u0(), glow.v0(), glow.color());
        vertex(out, at, glow.u0(), glow.v1(), glow.color());
        vertex(out, at, glow.u1(), glow.v1(), glow.color());
        vertex(out, at, glow.u1(), glow.v0(), glow.color());
        vertex(out, at, glow.u1(), glow.v0(), glow.color());
        vertex(out, at, glow.u1(), glow.v1(), glow.color());
        vertex(out, at, glow.u0(), glow.v1(), glow.color());
        vertex(out, at, glow.u0(), glow.v0(), glow.color());
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose at, float u, float v, int color) {
        out.addVertex(at, u, v, GLOW_DEPTH).setColor(color).setLight(FULL_BRIGHT);
    }
}
