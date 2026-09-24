package diamondvending.shop;

import diamondvending.DiamondVending;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a machine takes as money (spec §5.5). The default is the item tag {@code #diamondvending:currency} (just
 * diamonds unless a datapack changes it); a machine's currency slot or catalog can name one item instead. Items match
 * by type; names, enchantments and other components are ignored.
 */
public final class Currency {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, DiamondVending.id("currency"));
    /** The default: the tag, which ships with just diamonds and which packs may change by datapack. */
    public static final Currency DEFAULT = new Currency(null);

    /** One kind of item, or null for the tag. */
    private final Item item;

    private Currency(Item item) {
        this.item = item;
    }

    /** Exactly one kind of item: an admin's currency slot or a catalog's currency (spec §5.5). */
    public static Currency of(Item item) {
        return new Currency(item);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return item != null ? stack.is(item) : stack.is(TAG);
    }

    /** The item shown on price tags and in messages: the currency's item, else the tag's first item, else a diamond. */
    public Item displayItem() {
        if (item != null) return item;
        for (Holder<Item> tagged : BuiltInRegistries.ITEM.getTagOrEmpty(TAG)) {
            return tagged.value();
        }
        return Items.DIAMOND;
    }

    /**
     * An amount of money, e.g. "3 diamonds". Lang files can name any currency with
     * {@code currency.diamondvending.<namespace>.<path>.one} / {@code .many}; without them it reads "3 × Emerald".
     */
    public Component money(int count) {
        String key = langKey() + (count == 1 ? ".one" : ".many");
        return Component.translatableWithFallback(key, "%s × %s", count, itemName());
    }

    /** The currency's plural name, e.g. "diamonds" ({@code currency.diamondvending.<namespace>.<path>.name}). */
    public Component name() {
        return Component.translatableWithFallback(langKey() + ".name", "%s", itemName());
    }

    private String langKey() {
        return "currency." + DiamondVending.MOD_ID + "." + BuiltInRegistries.ITEM.getKey(displayItem()).toLanguageKey();
    }

    private Component itemName() {
        return new ItemStack(displayItem()).getHoverName();
    }
}
