package diamondvending.shop;

import diamondvending.block.VendingMachineBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The pickup tray (spec §3.6). Anyone may take what's in it, like a real machine. */
public final class PickupTray {
    private PickupTray() {}

    /** Moves everything in the tray to the player; whatever doesn't fit drops at their feet. */
    public static void collect(VendingMachineBlockEntity machine, Player player) {
        List<ItemStack> items = ItemSlots.takeAll(machine.tray());
        if (items.isEmpty()) return;
        items.forEach(stack -> PlayerItems.give(player, stack));
        MachineSounds.pickup(machine.getLevel(), machine.getBlockPos());
        machine.changed();
    }
}
