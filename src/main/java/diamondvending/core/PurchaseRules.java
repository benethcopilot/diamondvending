package diamondvending.core;

/** Decides whether a purchase can happen and how it is paid for. All checks run before anything changes (spec §3.3). */
public final class PurchaseRules {
    private PurchaseRules() {}

    public static PurchaseDecision decide(PurchaseInput in) {
        if (in.catalogMissing()) return deny(DenyReason.CATALOG_MISSING);
        if (!in.selectionSetUp()) return deny(DenyReason.EMPTY);
        if (!in.infinite() && in.stockCount() < in.quantity()) return deny(DenyReason.SOLD_OUT);
        if (!in.trayHasRoom()) return deny(DenyReason.TRAY_FULL);
        if (!in.infinite() && !in.cashBoxHasRoom()) return deny(DenyReason.CASH_BOX_FULL);
        long funds = (long) in.credit() + in.inventoryCurrency();
        if (funds < in.price()) return deny(DenyReason.NOT_ENOUGH_MONEY);

        int fromCredit = Math.min(in.credit(), in.price());
        return new PurchaseDecision.Approved(fromCredit, in.price() - fromCredit);
    }

    private static PurchaseDecision deny(DenyReason reason) {
        return new PurchaseDecision.Denied(reason);
    }
}
