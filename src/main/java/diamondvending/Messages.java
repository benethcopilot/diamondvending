package diamondvending;

import diamondvending.core.Problem;
import diamondvending.core.Texts;
import diamondvending.shop.Currency;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Sends player-facing messages. Every failure names the reason and who can fix it (spec §3.5); keys live in core/Texts. */
public final class Messages {
    private Messages() {}

    /** Shows a short message above the hotbar, only to this player. */
    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    /** A problem's plain-language explanation (spec §3.5 b); "diamonds" becomes the machine's real currency. */
    public static Component explanation(Problem problem, Currency currency) {
        return Component.translatable(Texts.explanation(problem), currency.name());
    }
}
