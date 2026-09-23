package diamondvending.shop;

import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A player's own item slots, as far as the machine is concerned. */
public final class PlayerItems {
    private PlayerItems() {}

    /**
     * The stacks a player can pay from (spec §3.3): main inventory and hotbar (slots 0–35), then the offhand (slot 40) —
     * never armor, never anything inside a shulker box or bundle. The stacks are live: splitting one takes from the player.
     */
    public static List<ItemStack> paySlots(Player player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> slots = new ArrayList<>(Inventory.INVENTORY_SIZE + 1);
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) slots.add(inventory.getItem(i));
        slots.add(inventory.getItem(Inventory.SLOT_OFFHAND));
        return slots;
    }

    /** Puts a copy of the stack in the player's inventory; whatever doesn't fit drops at their feet (spec §3.4, §3.6). */
    public static void give(Player player, ItemStack stack) {
        ItemStack rest = stack.copy();
        player.getInventory().add(rest);
        if (!rest.isEmpty()) {
            Containers.dropItemStack(player.level(), player.getX(), player.getY(), player.getZ(), rest);
        }
    }
}
