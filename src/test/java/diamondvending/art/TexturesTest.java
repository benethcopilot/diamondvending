package diamondvending.art;

import diamondvending.core.MachineLayout;
import diamondvending.core.Rect;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TexturesTest {
    private static final BufferedImage RED_FRONT = Textures.front(Dye.RED);

    private static int rgbAt(BufferedImage image, double u, double v) {
        return image.getRGB((int) Math.floor(u), (int) Math.floor(v)) & 0xFFFFFF;
    }

    private static int centerRgb(Rect rect) {
        return rgbAt(RED_FRONT, rect.centerU(), rect.centerV());
    }

    @Test
    void frontIsTheWholeCanvasAt16PixelsPerBlock() {
        assertEquals(MachineLayout.CANVAS, RED_FRONT.getWidth());
        assertEquals(MachineLayout.CANVAS, RED_FRONT.getHeight());
    }

    @Test
    void everyButtonIsPaintedWhereMachineLayoutPutsIt() {
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Rect button = MachineLayout.button(i);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    if (!button.contains(x + 0.5, y + 0.5)) continue;
                    int rgb = RED_FRONT.getRGB(x, y) & 0xFFFFFF;
                    assertTrue(rgb == Textures.BUTTON || rgb == Textures.BUTTON_SHADE,
                            "button " + (i + 1) + " pixel " + x + "," + y + " is " + Integer.toHexString(rgb));
                }
            }
        }
    }

    @Test
    void panelPartsHaveTheirColors() {
        // Glass just inside the frame, below the shine row and above the first shelf line
        assertEquals(Textures.GLASS, rgbAt(RED_FRONT, MachineLayout.WINDOW.u0() + 2, MachineLayout.WINDOW.v0() + 3));
        assertEquals(Textures.DISPLAY_OFF, centerRgb(MachineLayout.DISPLAY));
        assertEquals(Textures.LAMP_OFF, centerRgb(MachineLayout.LAMP));
        assertEquals(Textures.SLOT, centerRgb(MachineLayout.COIN_SLOT));
        assertEquals(Textures.COIN_RETURN, centerRgb(MachineLayout.COIN_RETURN));
        assertEquals(Textures.TRAY, centerRgb(MachineLayout.TRAY));
    }

    @Test
    void bodyTakesTheDyeColor() {
        // (23, 12) is plain body between the window and the panel
        assertEquals(Dye.RED.rgb, rgbAt(Textures.front(Dye.RED), 23, 12));
        assertEquals(Dye.BLUE.rgb, rgbAt(Textures.front(Dye.BLUE), 23, 12));
        assertNotEquals(rgbAt(Textures.side(Dye.RED), 8, 8), rgbAt(Textures.side(Dye.BLUE), 8, 8));
    }

    @Test
    void sizesAreRight() {
        assertEquals(16, Textures.side(Dye.RED).getWidth());
        assertEquals(16, Textures.item(Dye.RED).getWidth());
        assertEquals(128, Textures.icon().getWidth());
    }

    @Test
    void dyeColorsMatchMinecraft() {
        assertEquals(16, Dye.values().length);
        assertEquals(0xB02E26, Dye.RED.rgb);
        assertEquals(14, Dye.RED.ordinal(), "Dye order must match DyeColor ids");
        assertEquals(0x1D1D21, Dye.BLACK.rgb);
    }
}
