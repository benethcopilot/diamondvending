package diamondvending.shop;

import diamondvending.core.SetupButtons;
import net.minecraft.world.item.ItemStack;

/**
 * One of a machine's 12 buttons (spec §3.1): what it sells and for how much. The template's count is the quantity per
 * purchase and its components (names, enchantments…) are part of what's sold. Never modify the template — copy it.
 */
public record Selection(ItemStack template, int price) {
    public static final int MAX_PRICE = SetupButtons.MAX_PRICE;
    public static final Selection EMPTY = new Selection(ItemStack.EMPTY, 0);

    /** A valid selection: quantity kept to 1..max stack size and price to 0..999. An empty stack gives {@link #EMPTY}. */
    public static Selection of(ItemStack template, int price) {
        if (template.isEmpty()) return EMPTY;
        int quantity = Math.max(1, Math.min(template.getCount(), template.getMaxStackSize()));
        return new Selection(template.copyWithCount(quantity), Math.max(0, Math.min(price, MAX_PRICE)));
    }

    public boolean isSetUp() {
        return !template.isEmpty();
    }

    /** Items handed out per purchase. */
    public int quantity() {
        return template.getCount();
    }

    /** Whether a stack is what this button sells: same item and same components (spec §3.3). */
    public boolean sells(ItemStack stack) {
        return ItemSlots.sameItemAndData(stack, template);
    }
}
