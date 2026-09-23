package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineProblemsTest {

    /** A healthy owned machine: 5 selections set up, all in stock, room everywhere. */
    private static final class Facts {
        boolean infinite = false;
        boolean catalogMissing = false;
        int setUp = 5;
        int inStock = 5;
        boolean cashSlot = true;
        boolean traySlot = true;

        List<Problem> problems() {
            return MachineProblems.of(new MachineFacts(infinite, catalogMissing, setUp, inStock, cashSlot, traySlot));
        }
    }

    @Test
    void healthyMachineHasNoProblems() {
        assertEquals(List.of(), new Facts().problems());
    }

    @Test
    void missingCatalog() {
        Facts f = new Facts();
        f.catalogMissing = true;
        assertEquals(List.of(Problem.CATALOG_MISSING), f.problems());
    }

    @Test
    void notSetUp() {
        Facts f = new Facts();
        f.setUp = 0;
        f.inStock = 0;
        assertEquals(List.of(Problem.NOT_SET_UP), f.problems());
    }

    @Test
    void cashBoxFull() {
        Facts f = new Facts();
        f.cashSlot = false;
        assertEquals(List.of(Problem.CASH_BOX_FULL), f.problems());
    }

    @Test
    void soldOutOnlyWhenEverySelectionIsOut() {
        Facts f = new Facts();
        f.inStock = 1;
        assertEquals(List.of(), f.problems());
        f.inStock = 0;
        assertEquals(List.of(Problem.SOLD_OUT), f.problems());
    }

    @Test
    void trayFull() {
        Facts f = new Facts();
        f.traySlot = false;
        assertEquals(List.of(Problem.TRAY_FULL), f.problems());
    }

    @Test
    void infiniteMachinesNeverReportCashBoxOrSoldOut() {
        Facts f = new Facts();
        f.infinite = true;
        f.cashSlot = false;
        f.inStock = 0;
        assertEquals(List.of(), f.problems());
    }

    @Test
    void infiniteMachinesStillReportTheRest() {
        Facts f = new Facts();
        f.infinite = true;
        f.traySlot = false;
        assertEquals(List.of(Problem.TRAY_FULL), f.problems());
        f.setUp = 0;
        f.inStock = 0;
        assertEquals(List.of(Problem.NOT_SET_UP, Problem.TRAY_FULL), f.problems());
    }

    @Test
    void missingCatalogHidesNotSetUpAndSoldOut() {
        Facts f = new Facts();
        f.catalogMissing = true;
        f.setUp = 0;
        f.inStock = 0;
        assertEquals(List.of(Problem.CATALOG_MISSING), f.problems());
    }

    @Test
    void severalProblemsComeInPriorityOrder() {
        Facts f = new Facts();
        f.cashSlot = false;
        f.inStock = 0;
        f.traySlot = false;
        assertEquals(List.of(Problem.CASH_BOX_FULL, Problem.SOLD_OUT, Problem.TRAY_FULL), f.problems());
    }

    @Test
    void keysAreUniqueSnakeCase() {
        Set<String> seen = new HashSet<>();
        for (Problem p : Problem.values()) {
            assertTrue(p.key().matches("[a-z]+(_[a-z]+)*"), p.key());
            assertTrue(seen.add(p.key()), "duplicate key " + p.key());
        }
    }

    @Test
    void impossibleFactsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MachineFacts(false, false, 2, 3, true, true));
        assertThrows(IllegalArgumentException.class, () -> new MachineFacts(false, false, 13, 0, true, true));
        assertThrows(IllegalArgumentException.class, () -> new MachineFacts(false, false, -1, 0, true, true));
    }
}
