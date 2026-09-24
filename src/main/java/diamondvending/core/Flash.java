package diamondvending.core;

/** Short messages the display flashes for about 2 seconds, for everyone nearby (spec §3.5 c). */
public enum Flash {
    THANK_YOU("thank_you", false),
    NOTHING_HERE("nothing_here", true),
    SOLD_OUT("sold_out", true),
    TRAY_FULL("tray_full", true),
    CASH_BOX_FULL("cash_box_full", true),
    CATALOG_MISSING("catalog_missing", true),
    NEED_MONEY("need_money", true),
    WRONG_COIN("wrong_coin", true),
    CREDIT_FULL("credit_full", true);

    private final String key;
    private final boolean alarm;

    Flash(String key, boolean alarm) {
        this.key = key;
        this.alarm = alarm;
    }

    /** Stable snake_case id, used to build translation keys. */
    public String key() {
        return key;
    }

    /** Red for trouble, green for good news. */
    public boolean alarm() {
        return alarm;
    }

    /** What the display flashes when a purchase is refused. */
    public static Flash of(DenyReason reason) {
        return switch (reason) {
            case CATALOG_MISSING -> CATALOG_MISSING;
            case EMPTY -> NOTHING_HERE;
            case SOLD_OUT -> SOLD_OUT;
            case TRAY_FULL -> TRAY_FULL;
            case CASH_BOX_FULL -> CASH_BOX_FULL;
            case NOT_ENOUGH_MONEY -> NEED_MONEY;
        };
    }
}
