package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Spec §4: who sees which setup tab, and which tab gets the red "!" for each problem. */
class SetupTabTest {
    @Test
    void ownersSeeTheirShopTabsAndOnlyAdminsSeeTheAdminTab() {
        assertTrue(SetupTab.SELECTIONS.shownTo(false, false));
        assertTrue(SetupTab.STOCK.shownTo(false, false));
        assertTrue(SetupTab.CASH_BOX.shownTo(false, false));
        assertFalse(SetupTab.ADMIN.shownTo(false, false));
        assertTrue(SetupTab.ADMIN.shownTo(true, false));
    }

    @Test
    void infiniteMachinesHaveNoStockOrCashBox() {
        assertTrue(SetupTab.SELECTIONS.shownTo(true, true));
        assertFalse(SetupTab.STOCK.shownTo(true, true));
        assertFalse(SetupTab.CASH_BOX.shownTo(true, true));
        assertTrue(SetupTab.ADMIN.shownTo(true, true));
    }

    @Test
    void eachProblemMarksTheTabThatFixesIt() {
        assertEquals(EnumSet.of(SetupTab.CASH_BOX, SetupTab.STOCK),
                SetupTab.needingAttention(List.of(Problem.CASH_BOX_FULL, Problem.SOLD_OUT, Problem.TRAY_FULL)));
        assertEquals(EnumSet.of(SetupTab.SELECTIONS), SetupTab.needingAttention(List.of(Problem.NOT_SET_UP)));
        assertEquals(EnumSet.of(SetupTab.ADMIN), SetupTab.needingAttention(List.of(Problem.CATALOG_MISSING)));
        assertTrue(SetupTab.needingAttention(List.of(Problem.TRAY_FULL)).isEmpty(), "a full tray is emptied at the machine, not in setup");
    }
}
