package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation keys for everything the mod tells players (spec §3.5). Plain strings, so {@code TextsTest} can check
 * that en_us.json has text for every one of them.
 */
public final class Texts {
    public static final String OWNER_ONLY = "message.diamondvending.owner_only";
    /** Prefix of the keys that name the default currency: {@code .one}, {@code .many}, {@code .name} (see shop/Currency). */
    public static final String DIAMOND = "currency.diamondvending.minecraft.diamond";

    private Texts() {}

    /** Every key a player can see. */
    public static List<String> all() {
        List<String> keys = new ArrayList<>(List.of(OWNER_ONLY));
        for (String form : List.of(".one", ".many", ".name")) keys.add(DIAMOND + form);
        return keys;
    }
}
