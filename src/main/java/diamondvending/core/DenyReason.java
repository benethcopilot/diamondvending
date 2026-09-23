package diamondvending.core;

/** Why a purchase was refused, in the order the checks run (spec §3.3). */
public enum DenyReason {
    CATALOG_MISSING, EMPTY, SOLD_OUT, TRAY_FULL, CASH_BOX_FULL, NOT_ENOUGH_MONEY
}
