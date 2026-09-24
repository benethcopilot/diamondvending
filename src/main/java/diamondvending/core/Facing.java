package diamondvending.core;

/** The horizontal direction the machine's front faces, mirroring Minecraft's Direction without depending on it. */
public enum Facing {
    NORTH(0, -1), SOUTH(0, 1), WEST(-1, 0), EAST(1, 0);

    private final int dx;
    private final int dz;

    Facing(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** X component of the front face's outward normal. */
    public int dx() {
        return dx;
    }

    /** Z component of the front face's outward normal. */
    public int dz() {
        return dz;
    }

    /** X offset from the left column to the right column, as seen by someone facing the front. */
    public int rightDx() {
        return dz;
    }

    /** Z offset from the left column to the right column, as seen by someone facing the front. */
    public int rightDz() {
        return -dx;
    }

    /** Whether a camera at (x, z) is in front of the machine's front face; the master block is at (masterX, masterZ). */
    public boolean frontVisible(int masterX, int masterZ, double x, double z) {
        double faceX = masterX + 0.5 + dx * 0.5;
        double faceZ = masterZ + 0.5 + dz * 0.5;
        return (x - faceX) * dx + (z - faceZ) * dz > 0;
    }
}
