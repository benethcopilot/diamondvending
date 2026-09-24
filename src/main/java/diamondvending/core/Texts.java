package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation keys for everything the mod tells players (spec §3.5). Plain strings, so {@code TextsTest} can check
 * that en_us.json has text for every one of them.
 */
public final class Texts {
    public static final String OWNER_ONLY = "message.diamondvending.owner_only";
    public static final String WRONG_CURRENCY = "message.diamondvending.wrong_currency";
    public static final String CREDIT_FULL = "message.diamondvending.credit_full";
    public static final String BUTTON_EMPTY = "message.diamondvending.button_empty";
    public static final String SOLD_OUT = "message.diamondvending.sold_out";
    public static final String NEED_MONEY = "message.diamondvending.need_money";
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
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY, WRONG_CURRENCY, CREDIT_FULL, BUTTON_EMPTY, SOLD_OUT, NEED_MONEY,
                SELECT_ITEM, CREDIT, TAG_FREE, TAG_SOLD_OUT));
        keys.addAll(List.of(HUD_ITEM, HUD_FREE, HUD_SOLD_OUT, HUD_NOTHING, HUD_INSERT, HUD_YOUR_CREDIT, HUD_RETURN_CREDIT,
                HUD_TAKE_ITEMS, HUD_OWNED_BY, HUD_SHOP_MACHINE));
        keys.add(SETUP_TITLE);
        for (Problem problem : Problem.values()) {
            keys.add(explanation(problem));
            keys.add(problemDisplay(problem));
        }
        for (Flash flash : Flash.values()) keys.add(flash(flash));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
}
