package diamondvending.core;

/** Persistent machine problems, in display priority order (spec §3.5 b). */
public enum Problem {
    CATALOG_MISSING("catalog_missing"),
    NOT_SET_UP("not_set_up"),
    CASH_BOX_FULL("cash_box_full"),
    SOLD_OUT("sold_out"),
    TRAY_FULL("tray_full");

    private final String key;

    Problem(String key) {
        this.key = key;
    }

    /** Stable snake_case id, used to build translation keys. */
    public String key() {
        return key;
    }
}
