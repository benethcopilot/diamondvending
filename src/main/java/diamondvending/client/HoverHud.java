package diamondvending.client;

import diamondvending.block.FrontFace;
import diamondvending.block.MachinePart;
import diamondvending.block.VendingMachineBlock;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.registry.ModContent;
import diamondvending.scene.HoverText;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

import java.util.ArrayList;
import java.util.List;

/** Draws the {@link HoverText} tooltip to the right of the crosshair while it's on a machine's front (spec §2.3). */
public final class HoverHud {
    private static final int LINE_HEIGHT = 18;
    /** Between the parts of a line that wrapped: one line of text. */
    private static final int WRAP_HEIGHT = 10;
    private static final int ICON_WIDTH = 18;
    private static final int MIN_TEXT_WIDTH = 100;
    private static final int BACKGROUND = 0xA0000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int ALARM = 0xFFFF6060;

    private HoverHud() {}

    /** Each loader's HUD hook calls this with its GUI graphics (the hooks' other arguments differ, and none are needed). */
    //? if >=26.1 {
    public static void render(GuiGraphicsExtractor graphics) {
    //?} else {
    /*public static void render(GuiGraphics graphics) {
    *///?}
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null || mc.player == null) return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(ModContent.VENDING_MACHINE.get()) || hit.getDirection() != state.getValue(VendingMachineBlock.FACING)) return;
        if (!(mc.level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine)) return;

        List<HoverText.Line> lines = HoverText.lines(machine, FrontFace.hit(state, pos, hit), mc.player.getUUID());
        int x = graphics.guiWidth() / 2 + 12;
        // Long lines wrap to stay on screen: a problem's explanation says how to fix it in its second half.
        int maxTextWidth = Math.max(MIN_TEXT_WIDTH, graphics.guiWidth() - x - ICON_WIDTH - 6);
        List<Row> rows = new ArrayList<>();
        for (HoverText.Line line : lines) {
            int color = line.alarm() ? ALARM : TEXT;
            List<FormattedCharSequence> parts = mc.font.split(line.text(), maxTextWidth);
            for (int i = 0; i < parts.size(); i++) {
                boolean last = i == parts.size() - 1;
                rows.add(new Row(parts.get(i), i == 0 ? line.icon() : ItemStack.EMPTY, color, last ? LINE_HEIGHT : WRAP_HEIGHT));
            }
        }
        int width = 0;
        int height = 0;
        for (Row row : rows) {
            width = Math.max(width, ICON_WIDTH + mc.font.width(row.text()));
            height += row.height();
        }
        int y = Math.max(2, graphics.guiHeight() / 2 - height / 2);
        graphics.fill(x - 3, y - 2, x + width + 3, y + height, BACKGROUND);
        for (Row row : rows) {
            //? if >=26.1 {
            if (!row.icon().isEmpty()) graphics.item(row.icon(), x, y);
            graphics.text(mc.font, row.text(), x + ICON_WIDTH, y + 4, row.color());
            //?} else {
            /*if (!row.icon().isEmpty()) graphics.renderItem(row.icon(), x, y);
            graphics.drawString(mc.font, row.text(), x + ICON_WIDTH, y + 4, row.color());
            *///?}
            y += row.height();
        }
    }

    /** One row on screen: a tooltip line, or the next part of one that wrapped. */
    private record Row(FormattedCharSequence text, ItemStack icon, int color, int height) {}
}
