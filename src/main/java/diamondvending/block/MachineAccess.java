package diamondvending.block;

import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
import net.minecraft.server.permissions.Permissions;
//?}

import java.util.UUID;

/** Who may set up, dye and break a machine (spec §4, §5.3). */
public final class MachineAccess {
    private MachineAccess() {}

    /** Admin = creative mode or permission level 2 (op). */
    public static boolean isAdmin(Player player) {
        if (player.isCreative()) return true;
        //? if >=26.1 {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //?} else {
        /*return player.hasPermissions(2);
        *///?}
    }

    /** Owners and admins. Machines without an owner ({@code owner == null}) are admin-only. */
    public static boolean canManage(Player player, UUID owner) {
        return isAdmin(player) || (owner != null && owner.equals(player.getUUID()));
    }
}
