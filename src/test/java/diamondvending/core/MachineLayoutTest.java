package diamondvending.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineLayoutTest {

    private static Hit centerOf(Rect r) {
        return MachineLayout.hitAt(r.centerU(), r.centerV());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void buttonCenterHitsThatButton(int i) {
        assertEquals(Hit.button(i), centerOf(MachineLayout.button(i)));
    }

    @Test
    void buttonsAreNumberedInReadingOrderTwoPerRow() {
        assertEquals(new Rect(25, 7, 27, 9), MachineLayout.button(0));
        assertEquals(new Rect(27.75, 7, 29.75, 9), MachineLayout.button(1));
        assertEquals(new Rect(25, 9.75, 27, 11.75), MachineLayout.button(2));
        assertEquals(new Rect(27.75, 20.75, 29.75, 22.75), MachineLayout.button(11));
    }

    @Test
    void gapsBetweenButtonsStillHitAButton() {
        // Horizontal gap between buttons 0 and 1 is u 27..27.75: left half → 0, right half → 1
        assertEquals(Hit.button(0), MachineLayout.hitAt(27.2, 8));
        assertEquals(Hit.button(1), MachineLayout.hitAt(27.5, 8));
        // Vertical gap between buttons 0 and 2 is v 9..9.75
        assertEquals(Hit.button(0), MachineLayout.hitAt(26, 9.2));
        assertEquals(Hit.button(2), MachineLayout.hitAt(26, 9.5));
    }

    @Test
    void panelAndBodyPartsResolve() {
        assertEquals(Hit.of(Region.COIN_SLOT), centerOf(MachineLayout.COIN_SLOT));
        assertEquals(Hit.of(Region.COIN_RETURN), centerOf(MachineLayout.COIN_RETURN));
        assertEquals(Hit.of(Region.TRAY), centerOf(MachineLayout.TRAY));
        assertEquals(Hit.of(Region.DISPLAY), centerOf(MachineLayout.DISPLAY));
        assertEquals(Hit.of(Region.LAMP), centerOf(MachineLayout.LAMP));
        assertEquals(Hit.of(Region.WINDOW), centerOf(MachineLayout.WINDOW));
    }

    @Test
    void plainBodyHitsNothing() {
        assertEquals(Hit.NONE, MachineLayout.hitAt(0.5, 0.5));   // top-left corner
        assertEquals(Hit.NONE, MachineLayout.hitAt(23.2, 12));   // strip between window and panel
        assertEquals(Hit.NONE, MachineLayout.hitAt(31.5, 31.5)); // bottom-right corner
    }

    @Test
    void seamBetweenPartsIsStillTheWindow() {
        assertEquals(Hit.of(Region.WINDOW), MachineLayout.hitAt(16, 12));
        assertEquals(Hit.of(Region.WINDOW), MachineLayout.hitAt(15.999, 16));
    }

    @Test
    void regionsNeverOverlap() {
        List<Rect> all = new ArrayList<>(List.of(
                MachineLayout.WINDOW, MachineLayout.LAMP, MachineLayout.DISPLAY,
                MachineLayout.COIN_SLOT, MachineLayout.COIN_RETURN, MachineLayout.TRAY));
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) all.add(MachineLayout.buttonCell(i));

        for (double u = 0; u < MachineLayout.CANVAS; u += 0.125) {
            for (double v = 0; v < MachineLayout.CANVAS; v += 0.125) {
                int owners = 0;
                for (Rect r : all) if (r.contains(u, v)) owners++;
                assertTrue(owners <= 1, "regions overlap at u=" + u + " v=" + v);
            }
        }
    }

    @Test
    void everyRegionFitsOnTheCanvas() {
        List<Rect> all = new ArrayList<>(List.of(
                MachineLayout.WINDOW, MachineLayout.LAMP, MachineLayout.DISPLAY,
                MachineLayout.COIN_SLOT, MachineLayout.COIN_RETURN, MachineLayout.TRAY));
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) all.add(MachineLayout.buttonCell(i));
        for (Rect r : all) {
            assertTrue(r.u0() >= 0 && r.u1() <= MachineLayout.CANVAS && r.u0() < r.u1(), r.toString());
            assertTrue(r.v0() >= 0 && r.v1() <= MachineLayout.CANVAS && r.v0() < r.v1(), r.toString());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void shelfSlotsSitInsideTheWindow(int i) {
        Rect s = MachineLayout.shelfSlot(i);
        Rect w = MachineLayout.WINDOW;
        assertTrue(s.u0() >= w.u0() && s.u1() <= w.u1() && s.v0() >= w.v0() && s.v1() <= w.v1(), s.toString());
    }

    @Test
    void shelvesHoldThreeItemsPerRowInReadingOrder() {
        assertEquals(new Rect(3, 3.5, 6.5, 7), MachineLayout.shelfSlot(0));
        assertEquals(new Rect(17.5, 3.5, 21, 7), MachineLayout.shelfSlot(2));
        assertEquals(new Rect(3, 9, 6.5, 12.5), MachineLayout.shelfSlot(3));
        assertEquals(new Rect(17.5, 20, 21, 23.5), MachineLayout.shelfSlot(11));
    }

    @Test
    void selectionIndexOutOfRangeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MachineLayout.button(12));
        assertThrows(IllegalArgumentException.class, () -> MachineLayout.buttonCell(-1));
        assertThrows(IllegalArgumentException.class, () -> MachineLayout.shelfSlot(12));
    }

    @Test
    void viewersRightFollowsFacing() {
        assertEquals(1, Facing.SOUTH.rightDx());
        assertEquals(0, Facing.SOUTH.rightDz());
        assertEquals(-1, Facing.NORTH.rightDx());
        assertEquals(0, Facing.NORTH.rightDz());
        assertEquals(0, Facing.EAST.rightDx());
        assertEquals(-1, Facing.EAST.rightDz());
        assertEquals(0, Facing.WEST.rightDx());
        assertEquals(1, Facing.WEST.rightDz());
    }

    @ParameterizedTest
    @EnumSource(Facing.class)
    void canvasUGrowsSmoothlyAcrossBothColumns(Facing facing) {
        boolean alongX = facing.rightDx() != 0;
        int dir = alongX ? facing.rightDx() : facing.rightDz(); // +1 or -1
        for (int step = 0; step < 20; step++) {
            double t = step / 10.0 + 0.05;            // blocks from the machine's left edge
            boolean right = t >= 1;
            double world = dir > 0 ? t : 1 - t;       // coordinate relative to the master block origin
            double partOrigin = right ? dir : 0;
            double frac = world - partOrigin;         // position inside the part's own block
            double fx = alongX ? frac : 0.5;
            double fz = alongX ? 0.5 : frac;
            double u = MachineLayout.toCanvas(facing, right, false, fx, 0.5, fz)[0];
            assertEquals(16 * t, u, 1e-9, facing + " t=" + t);
        }
    }

    @Test
    void canvasVIsMeasuredFromTheTop() {
        assertEquals(0, MachineLayout.toCanvas(Facing.SOUTH, false, true, 0.5, 1, 0.5)[1], 1e-9);
        assertEquals(16, MachineLayout.toCanvas(Facing.SOUTH, false, true, 0.5, 0, 0.5)[1], 1e-9);
        assertEquals(16, MachineLayout.toCanvas(Facing.SOUTH, false, false, 0.5, 1, 0.5)[1], 1e-9);
        assertEquals(32, MachineLayout.toCanvas(Facing.SOUTH, false, false, 0.5, 0, 0.5)[1], 1e-9);
    }

    @Test
    void slightlyOutOfRangeHitsAreClamped() {
        double[] uv = MachineLayout.toCanvas(Facing.SOUTH, true, false, 1.0000001, -0.0000001, 0.5);
        assertEquals(32, uv[0], 1e-9);
        assertEquals(32, uv[1], 1e-9);
        assertEquals(Hit.NONE, MachineLayout.hitOnFront(Facing.SOUTH, true, false, 1.0000001, -0.0000001, 0.5));
        assertEquals(Hit.NONE, MachineLayout.hitOnFront(Facing.WEST, false, true, 0.5, 1.0000001, -0.0000001));
    }

    @Test
    void hitOnFrontFindsButtonsThroughTheRightPart() {
        // Button 0's center is canvas (26, 8): right column (face u = 10/16 = 0.625), upper row (fy = 0.5)
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.SOUTH, true, true, 0.625, 0.5, 0.5));
        // Facing north mirrors x
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.NORTH, true, true, 0.375, 0.5, 0.5));
        // Facing east uses z, mirrored; facing west uses z directly
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.EAST, true, true, 0.5, 0.5, 0.375));
        assertEquals(Hit.button(0), MachineLayout.hitOnFront(Facing.WEST, true, true, 0.5, 0.5, 0.625));
    }
}
