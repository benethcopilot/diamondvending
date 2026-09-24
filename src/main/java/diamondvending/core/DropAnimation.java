package diamondvending.core;

/** The bought item dropping from its shelf into the tray (spec §2.3): about half a second, speeding up as it falls. */
public final class DropAnimation {
    public static final int TICKS = 10;

    private DropAnimation() {}

    /** Where button {@code selection}'s item is, {@code {u, v}} on the canvas, {@code ticks} after the sale; null once it has landed. */
    public static double[] position(int selection, double ticks) {
        if (ticks < 0 || ticks >= TICKS) return null;
        Rect from = MachineLayout.shelfSlot(selection);
        Rect to = MachineLayout.TRAY;
        double t = ticks / TICKS;
        double u = from.centerU() + (to.centerU() - from.centerU()) * t;
        double v = from.centerV() + (to.centerV() - from.centerV()) * t * t;
        return new double[] {u, v};
    }
}
