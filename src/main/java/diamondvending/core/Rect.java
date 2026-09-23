package diamondvending.core;

/** A rectangle on the machine's 32×32 front canvas, in pixels. Min edges inclusive, max edges exclusive. */
public record Rect(double u0, double v0, double u1, double v1) {
    public boolean contains(double u, double v) {
        return u >= u0 && u < u1 && v >= v0 && v < v1;
    }

    public double centerU() {
        return (u0 + u1) / 2;
    }

    public double centerV() {
        return (v0 + v1) / 2;
    }
}
