package diamondvending.core;

/**
 * Geometry of the machine's front face: a 32×32 pixel canvas (16 px per block), {@code u} left→right and
 * {@code v} top→bottom as seen from the front. The textures must match these numbers (spec §2.2).
 */
public final class MachineLayout {
    public static final int SELECTIONS = 12;
    public static final int CANVAS = 32;

    public static final Rect WINDOW = new Rect(1.5, 2, 22.5, 24);
    public static final Rect LAMP = new Rect(26, 0.5, 28.5, 2);
    public static final Rect DISPLAY = new Rect(24.75, 3, 29.75, 5.5);
    public static final Rect COIN_SLOT = new Rect(24, 24.5, 27.5, 29);
    public static final Rect COIN_RETURN = new Rect(28, 25, 30.5, 27.5);
    public static final Rect TRAY = new Rect(3, 26, 21, 30.5);

    private static final double BUTTON_U = 25;
    private static final double BUTTON_V = 7;
    private static final double BUTTON_SIZE = 2;
    private static final double BUTTON_PITCH = 2.75;
    private static final double BUTTON_PAD = (BUTTON_PITCH - BUTTON_SIZE) / 2;

    private static final double SHELF_U = 3;
    private static final double SHELF_V = 3.5;
    private static final double SHELF_SIZE = 3.5;
    private static final double SHELF_PITCH_U = 7.25;
    private static final double SHELF_PITCH_V = 5.5;

    private MachineLayout() {}

    /** The visible button for selection {@code i} (0–11): 2 columns × 6 rows in reading order. */
    public static Rect button(int i) {
        checkSelection(i);
        double u = BUTTON_U + (i % 2) * BUTTON_PITCH;
        double v = BUTTON_V + (i / 2) * BUTTON_PITCH;
        return new Rect(u, v, u + BUTTON_SIZE, v + BUTTON_SIZE);
    }

    /** The clickable cell for button {@code i}: the button padded by half the gap on every side, so gaps have no dead spots. */
    public static Rect buttonCell(int i) {
        Rect b = button(i);
        return new Rect(b.u0() - BUTTON_PAD, b.v0() - BUTTON_PAD, b.u1() + BUTTON_PAD, b.v1() + BUTTON_PAD);
    }

    /** Where selection {@code i}'s item sits behind the glass: 4 shelves × 3 items in reading order. */
    public static Rect shelfSlot(int i) {
        checkSelection(i);
        double u = SHELF_U + (i % 3) * SHELF_PITCH_U;
        double v = SHELF_V + (i / 3) * SHELF_PITCH_V;
        return new Rect(u, v, u + SHELF_SIZE, v + SHELF_SIZE);
    }

    /** What is at canvas point (u, v). */
    public static Hit hitAt(double u, double v) {
        for (int i = 0; i < SELECTIONS; i++) {
            if (buttonCell(i).contains(u, v)) return Hit.button(i);
        }
        if (COIN_SLOT.contains(u, v)) return Hit.of(Region.COIN_SLOT);
        if (COIN_RETURN.contains(u, v)) return Hit.of(Region.COIN_RETURN);
        if (TRAY.contains(u, v)) return Hit.of(Region.TRAY);
        if (DISPLAY.contains(u, v)) return Hit.of(Region.DISPLAY);
        if (LAMP.contains(u, v)) return Hit.of(Region.LAMP);
        if (WINDOW.contains(u, v)) return Hit.of(Region.WINDOW);
        return Hit.NONE;
    }

    /**
     * Converts a hit on one part's front face to canvas coordinates.
     *
     * @param right whether the part is in the right-hand column (as seen from the front)
     * @param upper whether the part is in the upper row
     * @param fx    hit x inside the part's block (hit location minus block position), nominally 0–1
     * @param fy    hit y inside the part's block, nominally 0–1
     * @param fz    hit z inside the part's block, nominally 0–1
     * @return {@code {u, v}} on the 32×32 canvas
     */
    public static double[] toCanvas(Facing facing, boolean right, boolean upper, double fx, double fy, double fz) {
        fx = clamp01(fx);
        fy = clamp01(fy);
        fz = clamp01(fz);
        double faceU = switch (facing) {
            case SOUTH -> fx;
            case NORTH -> 1 - fx;
            case EAST -> 1 - fz;
            case WEST -> fz;
        };
        double faceV = 1 - fy;
        return new double[] {(right ? 16 : 0) + faceU * 16, (upper ? 0 : 16) + faceV * 16};
    }

    /** {@link #toCanvas} followed by {@link #hitAt}. */
    public static Hit hitOnFront(Facing facing, boolean right, boolean upper, double fx, double fy, double fz) {
        double[] uv = toCanvas(facing, right, upper, fx, fy, fz);
        return hitAt(uv[0], uv[1]);
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static void checkSelection(int i) {
        if (i < 0 || i >= SELECTIONS) {
            throw new IllegalArgumentException("selection index must be 0-" + (SELECTIONS - 1) + ", was " + i);
        }
    }
}
