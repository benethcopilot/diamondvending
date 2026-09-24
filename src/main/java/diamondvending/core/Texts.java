package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation keys for everything the mod tells players (spec §3.5). Plain strings, so {@code TextsTest} can check
 * that en_us.json has text for every one of them.
 */
public final class Texts {
    public static final String OWNER_ONLY = "message.diamondvending.owner_only";
    public static final String INFINITE_ADMIN_ONLY = "message.diamondvending.infinite_admin_only";
    public static final String WRONG_CURRENCY = "message.diamondvending.wrong_currency";
    public static final String CREDIT_FULL = "message.diamondvending.credit_full";
    public static final String BUTTON_EMPTY = "message.diamondvending.button_empty";
    public static final String SOLD_OUT = "message.diamondvending.sold_out";
    public static final String NEED_MONEY = "message.diamondvending.need_money";
    public static final String EMPTY_HANDS = "message.diamondvending.empty_hands";
    /** Prefix of the keys that name the default currency: {@code .one}, {@code .many}, {@code .name} (see shop/Currency). */
    public static final String DIAMOND = "currency.diamondvending.minecraft.diamond";
    // The display (spec §3.5 a) and the price tags under the shelves.
    public static final String SELECT_ITEM = "display.diamondvending.select_item";
    public static final String CREDIT = "display.diamondvending.credit";
    public static final String TAG_FREE = "tag.diamondvending.free";
    public static final String TAG_SOLD_OUT = "tag.diamondvending.sold_out";
    // The hover tooltip (spec §2.3).
    public static final String HUD_ITEM = "hud.diamondvending.item";
    public static final String HUD_FREE = "hud.diamondvending.free";
    public static final String HUD_SOLD_OUT = "hud.diamondvending.sold_out";
    public static final String HUD_NOTHING = "hud.diamondvending.nothing";
    public static final String HUD_INSERT = "hud.diamondvending.insert";
    public static final String HUD_YOUR_CREDIT = "hud.diamondvending.your_credit";
    public static final String HUD_RETURN_CREDIT = "hud.diamondvending.return_credit";
    public static final String HUD_TAKE_ITEMS = "hud.diamondvending.take_items";
    public static final String HUD_OWNED_BY = "hud.diamondvending.owned_by";
    public static final String HUD_SHOP_MACHINE = "hud.diamondvending.shop_machine";
    // The setup screen (spec §4).
    public static final String SETUP_TITLE = "setup.diamondvending.title";
    public static final String SETUP_ALL_GOOD = "setup.diamondvending.all_good";
    public static final String SETUP_TAB_SELECTIONS = "setup.diamondvending.tab.selections";
    public static final String SETUP_TAB_STOCK = "setup.diamondvending.tab.stock";
    public static final String SETUP_TAB_CASH_BOX = "setup.diamondvending.tab.cash_box";
    public static final String SETUP_TAB_ADMIN = "setup.diamondvending.tab.admin";
    public static final String SETUP_BUTTON = "setup.diamondvending.button";
    public static final String SETUP_BUTTON_ITEM = "setup.diamondvending.button_item";
    public static final String SETUP_PICK_ITEM = "setup.diamondvending.pick_item";
    public static final String SETUP_QUANTITY = "setup.diamondvending.quantity";
    public static final String SETUP_PRICE = "setup.diamondvending.price";
    public static final String SETUP_CLEAR = "setup.diamondvending.clear";
    public static final String SETUP_FROM_CATALOG = "setup.diamondvending.from_catalog";
    public static final String SETUP_WITHDRAW = "setup.diamondvending.withdraw";
    public static final String SETUP_INFINITE_ON = "setup.diamondvending.infinite_on";
    public static final String SETUP_INFINITE_OFF = "setup.diamondvending.infinite_off";
    public static final String SETUP_INFINITE_EXPLAINED = "setup.diamondvending.infinite_explained";
    public static final String SETUP_EMPTY_FIRST = "setup.diamondvending.empty_first";
    public static final String SETUP_CATALOG = "setup.diamondvending.catalog";
    public static final String SETUP_NO_CATALOG = "setup.diamondvending.no_catalog";
    public static final String SETUP_CURRENCY = "setup.diamondvending.currency";
    public static final String SETUP_CURRENCY_DEFAULT = "setup.diamondvending.currency_default";

    private Texts() {}

    /** A problem's plain-language explanation (spec §3.5 b), e.g. "The tray is full! …". */
    public static String explanation(Problem problem) {
        return "problem.diamondvending." + problem.key() + ".explanation";
    }

    /** What the display scrolls for a problem (spec §3.5 b), e.g. "TRAY FULL - TAKE YOUR ITEMS". */
    public static String problemDisplay(Problem problem) {
        return "problem.diamondvending." + problem.key() + ".display";
    }

    /** What the display flashes, e.g. "NEED %s". */
    public static String flash(Flash flash) {
        return "display.diamondvending." + flash.key();
    }

    /** Every key a player can see. */
    public static List<String> all() {
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY, INFINITE_ADMIN_ONLY, WRONG_CURRENCY, CREDIT_FULL, BUTTON_EMPTY, SOLD_OUT, NEED_MONEY, EMPTY_HANDS,
                SELECT_ITEM, CREDIT, TAG_FREE, TAG_SOLD_OUT));
        keys.addAll(List.of(HUD_ITEM, HUD_FREE, HUD_SOLD_OUT, HUD_NOTHING, HUD_INSERT, HUD_YOUR_CREDIT, HUD_RETURN_CREDIT,
                HUD_TAKE_ITEMS, HUD_OWNED_BY, HUD_SHOP_MACHINE));
        keys.addAll(List.of(SETUP_TITLE, SETUP_ALL_GOOD, SETUP_TAB_SELECTIONS, SETUP_TAB_STOCK, SETUP_TAB_CASH_BOX, SETUP_TAB_ADMIN,
                SETUP_BUTTON, SETUP_BUTTON_ITEM, SETUP_PICK_ITEM, SETUP_QUANTITY, SETUP_PRICE, SETUP_CLEAR, SETUP_FROM_CATALOG,
                SETUP_WITHDRAW, SETUP_INFINITE_ON, SETUP_INFINITE_OFF, SETUP_INFINITE_EXPLAINED, SETUP_EMPTY_FIRST, SETUP_CATALOG,
                SETUP_NO_CATALOG, SETUP_CURRENCY, SETUP_CURRENCY_DEFAULT));
        for (Problem problem : Problem.values()) {
            keys.add(explanation(problem));
            keys.add(problemDisplay(problem));
        }
        for (Flash flash : Flash.values()) keys.add(flash(flash));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
}
