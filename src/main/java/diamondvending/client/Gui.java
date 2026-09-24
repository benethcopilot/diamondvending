package diamondvending.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

/** The few drawing calls the setup screen makes, on either version's GUI graphics. */
final class Gui {
    //? if >=26.1 {
    private final GuiGraphicsExtractor graphics;

    Gui(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }
    //?} else {
    /*private final GuiGraphics graphics;

    Gui(GuiGraphics graphics) {
        this.graphics = graphics;
    }
    *///?}

    void fill(int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0, y0, x1, y1, color);
    }

    /** Text without a shadow, like vanilla's container labels. */
    void text(Font font, Component text, int x, int y, int color) {
        text(font, text.getVisualOrderText(), x, y, color);
    }

    void text(Font font, FormattedCharSequence text, int x, int y, int color) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, color, false);
        //?} else {
        /*graphics.drawString(font, text, x, y, color, false);
        *///?}
    }

    void item(ItemStack stack, int x, int y) {
        //? if >=26.1 {
        graphics.item(stack, x, y);
        //?} else {
        /*graphics.renderItem(stack, x, y);
        *///?}
    }
}
