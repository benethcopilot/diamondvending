package diamondvending.art;

import java.util.Locale;

/** Minecraft's 16 dye colors, in DyeColor id order, with their texture colors (DyeColor#getTextureDiffuseColor). */
enum Dye {
    WHITE(0xF9FFFE), ORANGE(0xF9801D), MAGENTA(0xC74EBD), LIGHT_BLUE(0x3AB3DA), YELLOW(0xFED83D), LIME(0x80C71F),
    PINK(0xF38BAA), GRAY(0x474F52), LIGHT_GRAY(0x9D9D97), CYAN(0x169C9C), PURPLE(0x8932B8), BLUE(0x3C44AA),
    BROWN(0x835432), GREEN(0x5E7C16), RED(0xB02E26), BLACK(0x1D1D21);

    final int rgb;

    Dye(int rgb) {
        this.rgb = rgb;
    }

    /** The block-state / component value, e.g. {@code light_blue}. */
    String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
