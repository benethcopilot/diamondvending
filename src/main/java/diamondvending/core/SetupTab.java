package diamondvending.core;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The setup screen's tabs (spec §4). */
public enum SetupTab {
    SELECTIONS, STOCK, CASH_BOX, ADMIN;

    /** Whether this tab is there: Stock and Cash Box only on owned machines, Admin only for admins. */
    public boolean shownTo(boolean admin, boolean infinite) {
        return switch (this) {
            case SELECTIONS -> true;
            case STOCK, CASH_BOX -> !infinite;
            case ADMIN -> admin;
        };
    }

    /** The tab where a problem is fixed (its red "!"), or null: a full tray is emptied at the machine. */
    public static SetupTab fixing(Problem problem) {
        return switch (problem) {
            case CATALOG_MISSING -> ADMIN;
            case NOT_SET_UP -> SELECTIONS;
            case CASH_BOX_FULL -> CASH_BOX;
            case SOLD_OUT -> STOCK;
            case TRAY_FULL -> null;
        };
    }

    /** The tabs that get a red "!" for these problems. */
    public static Set<SetupTab> needingAttention(List<Problem> problems) {
        Set<SetupTab> tabs = EnumSet.noneOf(SetupTab.class);
        for (Problem problem : problems) {
            SetupTab tab = fixing(problem);
            if (tab != null) tabs.add(tab);
        }
        return tabs;
    }
}
