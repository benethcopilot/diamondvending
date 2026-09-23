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

    private Texts() {}

    /** A problem's plain-language explanation (spec §3.5 b), e.g. "The tray is full! …". */
    public static String explanation(Problem problem) {
        return "problem.diamondvending." + problem.key() + ".explanation";
    }

    /** Every key a player can see. */
    public static List<String> all() {
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY, WRONG_CURRENCY, CREDIT_FULL, BUTTON_EMPTY, SOLD_OUT, NEED_MONEY));
        for (Problem problem : Problem.values()) keys.add(explanation(problem));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
}
