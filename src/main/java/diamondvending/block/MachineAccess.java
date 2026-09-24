package diamondvending.block;

import diamondvending.core.Texts;
import net.minecraft.world.entity.player.Player;
//? if >=26.1 {
import net.minecraft.server.permissions.Permissions;
//?}

/** Who may set up, dye and break a machine (spec §4, §5.2, §5.3). */
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

    /**
     * Owners and admins. A machine without an owner is admin-only, and so is an infinite one, even for its owner (spec
     * §5.2): it sells without using stock, so its owner could sell anything from nothing. A missing machine
     * ({@code null}) is admin-only too.
     */
    public static boolean canManage(Player player, VendingMachineBlockEntity machine) {
        if (isAdmin(player)) return true;
        return machine != null && !machine.isInfinite() && player.getUUID().equals(machine.getOwner());
    }

    /** Why {@link #canManage} said no: the owner of an infinite machine needs an admin; anyone else isn't the owner. */
    public static String refusal(Player player, VendingMachineBlockEntity machine) {
        boolean owner = machine != null && player.getUUID().equals(machine.getOwner());
        return owner && machine.isInfinite() ? Texts.INFINITE_ADMIN_ONLY : Texts.OWNER_ONLY;
    }
}
