package diamondvending.core;

/**
 * The setup screen's buttons, as the numbers the client sends with vanilla's "menu button clicked" packet (spec §4:
 * every change is a small packet the server re-checks). The server decodes each number and decides; a number no
 * button sends decodes to null and is ignored.
 */
public final class SetupButtons {
    /** Prices run 0–999 (spec §3.1): three digits of a button number. */
    public static final int MAX_PRICE = 999;

    private static final int TAB = 0;              // + tab
    private static final int WITHDRAW = 10;
    private static final int INFINITE = 11;
    private static final int PREVIOUS_CATALOG = 12;
    private static final int NEXT_CATALOG = 13;
    private static final int CLEAR = 100;          // + button index
    private static final int FEWER = 200;          // + button index
    private static final int MORE = 300;           // + button index
    private static final int PRICE = 10_000;       // + button index × 1000 + price

    /** What a button asks for. */
    public sealed interface Press permits ShowTab, Withdraw, ToggleInfinite, CycleCatalog, Clear, ChangeQuantity, SetPrice {}

    public record ShowTab(SetupTab tab) implements Press {}

    /** Everything in the cash box to the player. */
    public record Withdraw() implements Press {}

    public record ToggleInfinite() implements Press {}

    /** The previous (−1) or next (+1) choice in the catalog picker. */
    public record CycleCatalog(int step) implements Press {}

    public record Clear(int index) implements Press {}

    /** One more or one fewer item per purchase. */
    public record ChangeQuantity(int index, int by) implements Press {}

    public record SetPrice(int index, int price) implements Press {}

    private SetupButtons() {}

    public static int showTab(SetupTab tab) {
        return TAB + tab.ordinal();
    }

    public static int withdraw() {
        return WITHDRAW;
    }

    public static int toggleInfinite() {
        return INFINITE;
    }

    public static int cycleCatalog(int step) {
        return step < 0 ? PREVIOUS_CATALOG : NEXT_CATALOG;
    }

    public static int clear(int index) {
        return CLEAR + checkIndex(index);
    }

    public static int fewer(int index) {
        return FEWER + checkIndex(index);
    }

    public static int more(int index) {
        return MORE + checkIndex(index);
    }

    public static int price(int index, int price) {
        if (price < 0 || price > MAX_PRICE) throw new IllegalArgumentException("price " + price + " is outside 0–" + MAX_PRICE);
        return PRICE + checkIndex(index) * 1000 + price;
    }

    /** What a button number asks for, or null for a number no button sends. */
    public static Press decode(int id) {
        int buttons = MachineLayout.SELECTIONS;
        if (id >= TAB && id < TAB + SetupTab.values().length) return new ShowTab(SetupTab.values()[id - TAB]);
        if (id == WITHDRAW) return new Withdraw();
        if (id == INFINITE) return new ToggleInfinite();
        if (id == PREVIOUS_CATALOG) return new CycleCatalog(-1);
        if (id == NEXT_CATALOG) return new CycleCatalog(1);
        if (id >= CLEAR && id < CLEAR + buttons) return new Clear(id - CLEAR);
        if (id >= FEWER && id < FEWER + buttons) return new ChangeQuantity(id - FEWER, -1);
        if (id >= MORE && id < MORE + buttons) return new ChangeQuantity(id - MORE, 1);
        if (id >= PRICE && id < PRICE + buttons * 1000) return new SetPrice((id - PRICE) / 1000, (id - PRICE) % 1000);
        return null;
    }

    private static int checkIndex(int index) {
        if (index < 0 || index >= MachineLayout.SELECTIONS) throw new IllegalArgumentException("there is no button " + (index + 1));
        return index;
    }
}
