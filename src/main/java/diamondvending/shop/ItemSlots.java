package diamondvending.shop;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Helpers for item lists: the machine's stock, cash box, tray and credit, and a player's pay slots. Empty stacks are free slots. */
public final class ItemSlots {
    private ItemSlots() {}

    /** Total items in stacks that match. */
    public static int count(List<ItemStack> slots, Predicate<ItemStack> matching) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (!stack.isEmpty() && matching.test(stack)) total += stack.getCount();
        }
        return total;
    }

    public static boolean isEmpty(List<ItemStack> slots) {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    public static boolean hasEmptySlot(List<ItemStack> slots) {
        for (ItemStack stack : slots) {
            if (stack.isEmpty()) return true;
        }
        return false;
    }

    /**
     * Adds a copy of {@code stack}: first topping up stacks of the same item and components, then filling empty slots.
     * Returns what didn't fit (empty if it all did). {@code stack} itself is not changed.
     */
    public static ItemStack insert(List<ItemStack> slots, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
            ItemStack slot = slots.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, rest)) {
                int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (moved > 0) {
                    slot.grow(moved);
                    rest.shrink(moved);
                }
            }
        }
        for (int i = 0; i < slots.size() && !rest.isEmpty(); i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, rest.split(Math.min(rest.getCount(), rest.getMaxStackSize())));
            }
        }
        return rest;
    }

    /** Whether all of {@code stacks} would fit, without changing {@code slots}. */
    public static boolean fitsAll(List<ItemStack> slots, List<ItemStack> stacks) {
        List<ItemStack> trial = copyOf(slots);
        for (ItemStack stack : stacks) {
            if (!insert(trial, stack).isEmpty()) return false;
        }
        return true;
    }

    /**
     * Removes up to {@code amount} matching items in slot order and returns them, names and other components kept.
     * Works on live lists such as {@link PlayerItems#paySlots}: the stacks shrink where they are.
     */
    public static List<ItemStack> take(List<ItemStack> slots, Predicate<ItemStack> matching, int amount) {
        List<ItemStack> taken = new ArrayList<>();
        int left = amount;
        for (ItemStack stack : slots) {
            if (left <= 0) break;
            if (stack.isEmpty() || !matching.test(stack)) continue;
            ItemStack part = stack.split(Math.min(left, stack.getCount()));
            left -= part.getCount();
            taken.add(part);
        }
        return taken;
    }

    /** Empties every slot and returns what was in them. */
    public static List<ItemStack> takeAll(List<ItemStack> slots) {
        List<ItemStack> taken = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            if (!slots.get(i).isEmpty()) taken.add(slots.get(i));
            slots.set(i, ItemStack.EMPTY);
        }
        return taken;
    }

    /** A deep copy, for trying changes out. */
    public static List<ItemStack> copyOf(List<ItemStack> slots) {
        List<ItemStack> copy = new ArrayList<>(slots.size());
        for (ItemStack stack : slots) copy.add(stack.copy());
        return copy;
    }

    public static void clear(List<ItemStack> slots) {
        Collections.fill(slots, ItemStack.EMPTY);
    }
}
