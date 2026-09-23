package diamondvending;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Player-facing messages. Every failure names the reason and who can fix it (spec §3.5). */
public final class Messages {
    public static final String OWNER_ONLY = "message.diamondvending.owner_only";

    private Messages() {}

    /** Shows a short message above the hotbar, only to this player. */
    public static void actionBar(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }
}
