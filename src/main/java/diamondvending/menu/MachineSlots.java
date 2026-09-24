package diamondvending.menu;

import diamondvending.shop.ItemSlots;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One of the machine's slot lists (Stock or Cash Box) as a container for the setup screen; every change re-syncs the machine. */
final class MachineSlots implements Container {
    private final NonNullList<ItemStack> items;
    private final Runnable changed;

    MachineSlots(NonNullList<ItemStack> items, Runnable changed) {
        this.items = items;
        this.changed = changed;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return ItemSlots.isEmpty(items);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, count);
        if (!taken.isEmpty()) changed.run();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        changed.run();
    }

    @Override
    public void setChanged() {
        changed.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        ItemSlots.clear(items);
        changed.run();
    }
}
