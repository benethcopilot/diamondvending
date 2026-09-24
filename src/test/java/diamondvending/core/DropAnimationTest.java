package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropAnimationTest {
    @Test
    void startsOnItsShelf() {
        double[] at = DropAnimation.position(4, 0);
        assertEquals(MachineLayout.shelfSlot(4).centerU(), at[0], 1e-9);
        assertEquals(MachineLayout.shelfSlot(4).centerV(), at[1], 1e-9);
    }

    @Test
    void landsInTheTrayThenDisappears() {
        double[] almost = DropAnimation.position(4, DropAnimation.TICKS - 0.001);
        assertEquals(MachineLayout.TRAY.centerV(), almost[1], 0.01);
        assertNull(DropAnimation.position(4, DropAnimation.TICKS));
    }

    @Test
    void fallsFasterAndFaster() {
        double v0 = DropAnimation.position(0, 0)[1];
        double v1 = DropAnimation.position(0, 1)[1];
        double v2 = DropAnimation.position(0, 2)[1];
        assertTrue(v2 - v1 > v1 - v0);
    }
}
