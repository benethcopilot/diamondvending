package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacingTest {
    @Test
    void theFrontIsOnlyVisibleFromInFront() {
        // A north-facing machine with its master at x 10, z 20: the front is the z = 20 plane, seen from z < 20.
        assertTrue(Facing.NORTH.frontVisible(10, 20, 10.5, 15));
        assertFalse(Facing.NORTH.frontVisible(10, 20, 10.5, 25));
        // East-facing: the front is the x = 11 plane, seen from x > 11.
        assertTrue(Facing.EAST.frontVisible(10, 20, 14, 20.5));
        assertFalse(Facing.EAST.frontVisible(10, 20, 9, 20.5));
    }
}
