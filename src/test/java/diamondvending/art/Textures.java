package diamondvending.art;

import diamondvending.core.MachineLayout;
import diamondvending.core.Rect;

import java.awt.image.BufferedImage;

/**
 * Paints the machine's textures. The front is the whole 32×32 canvas from {@link MachineLayout} (16 px per block);
 * each part's model shows its quarter. Because the painting reads MachineLayout, textures always match the click regions.
 */
final class Textures {
    static final int GLASS = 0xBFE3EF;
    static final int GLASS_SHINE = 0xE4F4FA;
    static final int WINDOW_FRAME = 0x3A3A3A;
    static final int SHELF = 0x5A5A5A;
    static final int PANEL = 0x2B2B2B;
    static final int BUTTON = 0xFFD23F;
    static final int BUTTON_SHADE = 0xC9A227;
    static final int DISPLAY_OFF = 0x0E2A12;
    static final int LAMP_OFF = 0x5A1414;
    static final int SLOT = 0x9A9A9A;
    static final int COIN_RETURN = 0xB0B0B0;
    static final int TRAY = 0x141414;
    static final int TRAY_LIP = 0x555555;

    /** Visual-only panel behind the display and buttons (not a click region). */
    static final Rect PANEL_AREA = new Rect(24, 2, 30.5, 24);

    private Textures() {}

    static BufferedImage front(Dye dye) {
        BufferedImage image = new BufferedImage(MachineLayout.CANVAS, MachineLayout.CANVAS, BufferedImage.TYPE_INT_ARGB);
        fill(image, dye.rgb);
        outline(image, shade(dye.rgb, 0.7));

        Rect window = MachineLayout.WINDOW;
        paint(image, window, WINDOW_FRAME);
        paint(image, inset(window, 1), GLASS);
        paint(image, new Rect(window.u0() + 1, window.v0() + 1, window.u1() - 1, window.v0() + 2), GLASS_SHINE);
        // A shelf line under the top three rows; the bottom row stands on the window frame
        for (int row = 0; row < 3; row++) {
            Rect slot = MachineLayout.shelfSlot(row * 3);
            paint(image, new Rect(window.u0() + 1, slot.v1() + 0.25, window.u1() - 1, slot.v1() + 1.25), SHELF);
        }

        paint(image, PANEL_AREA, PANEL);
        paint(image, MachineLayout.DISPLAY, DISPLAY_OFF);
        paint(image, MachineLayout.LAMP, LAMP_OFF);
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Rect button = MachineLayout.button(i);
            paint(image, button, BUTTON);
            paint(image, new Rect(button.u0(), button.v1() - 1, button.u1(), button.v1()), BUTTON_SHADE);
        }

        Rect coinSlot = MachineLayout.COIN_SLOT;
        paint(image, coinSlot, PANEL);
        paint(image, new Rect(coinSlot.centerU() - 0.5, coinSlot.v0() + 0.5, coinSlot.centerU() + 0.5, coinSlot.v1() - 0.5), SLOT);
        paint(image, MachineLayout.COIN_RETURN, COIN_RETURN);

        Rect tray = MachineLayout.TRAY;
        paint(image, tray, TRAY);
        paint(image, new Rect(tray.u0(), tray.v0(), tray.u1(), tray.v0() + 1), TRAY_LIP);
        return image;
    }

    /** Sides, top, bottom and back: the body color with a faint deterministic grain. */
    static BufferedImage side(Dye dye) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double grain = 0.94 + 0.06 * (((x * 7 + y * 13) % 5) / 4.0);
                image.setRGB(x, y, 0xFF000000 | shade(dye.rgb, grain));
            }
        }
        return image;
    }

    /** The inventory icon: the front scaled down to 16×16. */
    static BufferedImage item(Dye dye) {
        return downscale(front(dye), 2);
    }

    /** The mod icon: a red front scaled up to 128×128. */
    static BufferedImage icon() {
        return upscale(front(Dye.RED), 4);
    }

    /** All 16 colors side by side at 4× (for the README). */
    static BufferedImage preview() {
        int scale = 4;
        int cell = MachineLayout.CANVAS * scale;
        int gap = 8;
        BufferedImage image = new BufferedImage(8 * (cell + gap) - gap, 2 * (cell + gap) - gap, BufferedImage.TYPE_INT_ARGB);
        Dye[] dyes = Dye.values();
        for (int i = 0; i < dyes.length; i++) {
            BufferedImage big = upscale(front(dyes[i]), scale);
            int ox = (i % 8) * (cell + gap);
            int oy = (i / 8) * (cell + gap);
            for (int y = 0; y < cell; y++) {
                for (int x = 0; x < cell; x++) image.setRGB(ox + x, oy + y, big.getRGB(x, y));
            }
        }
        return image;
    }

    // ---- painting helpers ----------------------------------------------------------------------------------------

    /** Paints every pixel whose center lies inside {@code rect}. */
    static void paint(BufferedImage image, Rect rect, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (rect.contains(x + 0.5, y + 0.5)) image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
    }

    private static void fill(BufferedImage image, int rgb) {
        paint(image, new Rect(0, 0, image.getWidth(), image.getHeight()), rgb);
    }

    private static void outline(BufferedImage image, int rgb) {
        int w = image.getWidth();
        int h = image.getHeight();
        for (int i = 0; i < w; i++) {
            image.setRGB(i, 0, 0xFF000000 | rgb);
            image.setRGB(i, h - 1, 0xFF000000 | rgb);
        }
        for (int i = 0; i < h; i++) {
            image.setRGB(0, i, 0xFF000000 | rgb);
            image.setRGB(w - 1, i, 0xFF000000 | rgb);
        }
    }

    private static Rect inset(Rect rect, double by) {
        return new Rect(rect.u0() + by, rect.v0() + by, rect.u1() - by, rect.v1() - by);
    }

    static int shade(int rgb, double factor) {
        int r = (int) Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = (int) Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = (int) Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private static BufferedImage downscale(BufferedImage source, int factor) {
        int w = source.getWidth() / factor;
        int h = source.getHeight() / factor;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                for (int dy = 0; dy < factor; dy++) {
                    for (int dx = 0; dx < factor; dx++) {
                        int rgb = source.getRGB(x * factor + dx, y * factor + dy);
                        r += (rgb >> 16) & 0xFF;
                        g += (rgb >> 8) & 0xFF;
                        b += rgb & 0xFF;
                    }
                }
                int n = factor * factor;
                image.setRGB(x, y, 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n));
            }
        }
        return image;
    }

    private static BufferedImage upscale(BufferedImage source, int factor) {
        BufferedImage image = new BufferedImage(source.getWidth() * factor, source.getHeight() * factor, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) image.setRGB(x, y, source.getRGB(x / factor, y / factor));
        }
        return image;
    }
}
