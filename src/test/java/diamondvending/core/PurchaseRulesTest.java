package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseRulesTest {

    /** A healthy owned machine selling 1 item for 3 diamonds to a buyer with 10 diamonds and no credit. */
    private static final class In {
        boolean catalogMissing = false;
        boolean setUp = true;
        boolean infinite = false;
        int quantity = 1;
        int stock = 10;
        boolean trayRoom = true;
        boolean cashRoom = true;
        int price = 3;
        int credit = 0;
        int inventory = 10;

        PurchaseDecision decide() {
            return PurchaseRules.decide(new PurchaseInput(
                    catalogMissing, setUp, infinite, quantity, stock, trayRoom, cashRoom, price, credit, inventory));
        }
    }

    private static PurchaseDecision denied(DenyReason reason) {
        return new PurchaseDecision.Denied(reason);
    }

    @Test
    void paysFromInventoryWhenThereIsNoCredit() {
        assertEquals(new PurchaseDecision.Approved(0, 3), new In().decide());
    }

    @Test
    void spendsCreditFirst() {
        In in = new In();
        in.credit = 2;
        assertEquals(new PurchaseDecision.Approved(2, 1), in.decide());
    }

    @Test
    void creditCanCoverTheWholePrice() {
        In in = new In();
        in.credit = 5;
        assertEquals(new PurchaseDecision.Approved(3, 0), in.decide());
    }

    @Test
    void exactChangeIsEnough() {
        In in = new In();
        in.credit = 1;
        in.inventory = 2;
        assertEquals(new PurchaseDecision.Approved(1, 2), in.decide());
    }

    @Test
    void shortByOneIsRefused() {
        In in = new In();
        in.credit = 1;
        in.inventory = 1;
        assertEquals(denied(DenyReason.NOT_ENOUGH_MONEY), in.decide());
    }

    @Test
    void freeItemsNeedNoMoney() {
        In in = new In();
        in.price = 0;
        in.inventory = 0;
        assertEquals(new PurchaseDecision.Approved(0, 0), in.decide());
    }

    @Test
    void soldOutWhenStockIsBelowTheQuantity() {
        In in = new In();
        in.quantity = 16;
        in.stock = 15;
        assertEquals(denied(DenyReason.SOLD_OUT), in.decide());
        in.stock = 16;
        assertEquals(new PurchaseDecision.Approved(0, 3), in.decide());
    }

    @Test
    void emptySelectionIsRefused() {
        In in = new In();
        in.setUp = false;
        assertEquals(denied(DenyReason.EMPTY), in.decide());
    }

    @Test
    void fullTrayIsRefused() {
        In in = new In();
        in.trayRoom = false;
        assertEquals(denied(DenyReason.TRAY_FULL), in.decide());
    }

    @Test
    void fullCashBoxIsRefused() {
        In in = new In();
        in.cashRoom = false;
        assertEquals(denied(DenyReason.CASH_BOX_FULL), in.decide());
    }

    @Test
    void missingCatalogIsRefused() {
        In in = new In();
        in.catalogMissing = true;
        assertEquals(denied(DenyReason.CATALOG_MISSING), in.decide());
    }

    @Test
    void infiniteMachinesIgnoreStockAndCashBox() {
        In in = new In();
        in.infinite = true;
        in.stock = 0;
        in.cashRoom = false;
        assertEquals(new PurchaseDecision.Approved(0, 3), in.decide());
    }

    @Test
    void infiniteMachinesStillNeedTrayRoomAndMoney() {
        In in = new In();
        in.infinite = true;
        in.trayRoom = false;
        assertEquals(denied(DenyReason.TRAY_FULL), in.decide());
        in.trayRoom = true;
        in.inventory = 0;
        assertEquals(denied(DenyReason.NOT_ENOUGH_MONEY), in.decide());
    }

    @Test
    void checksHappenInSpecOrder() {
        In in = new In();
        in.catalogMissing = true;
        in.setUp = false;
        in.stock = 0;
        in.trayRoom = false;
        in.cashRoom = false;
        in.inventory = 0;
        assertEquals(denied(DenyReason.CATALOG_MISSING), in.decide());
        in.catalogMissing = false;
        assertEquals(denied(DenyReason.EMPTY), in.decide());
        in.setUp = true;
        assertEquals(denied(DenyReason.SOLD_OUT), in.decide());
        in.stock = 10;
        assertEquals(denied(DenyReason.TRAY_FULL), in.decide());
        in.trayRoom = true;
        assertEquals(denied(DenyReason.CASH_BOX_FULL), in.decide());
        in.cashRoom = true;
        assertEquals(denied(DenyReason.NOT_ENOUGH_MONEY), in.decide());
    }

    @Test
    void hugeFundsDoNotOverflow() {
        In in = new In();
        in.price = 999;
        in.credit = Integer.MAX_VALUE;
        in.inventory = Integer.MAX_VALUE;
        assertEquals(new PurchaseDecision.Approved(999, 0), in.decide());
    }

    @Test
    void negativeCountsAreRejected() {
        In in = new In();
        in.price = -1;
        assertThrows(IllegalArgumentException.class, in::decide);
        in.price = 3;
        in.credit = -5;
        assertThrows(IllegalArgumentException.class, in::decide);
    }
}
