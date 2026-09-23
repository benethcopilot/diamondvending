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
 * What a machine takes as money (spec §5.5). Plan 3 has only the default: the item tag {@code #diamondvending:currency},
 * which ships with just diamonds and which packs may change by datapack. Items match by type; names, enchantments and
 * other components are ignored.
 */
public final class Currency {
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, DiamondVending.id("currency"));
    public static final Currency DEFAULT = new Currency();

    private Currency() {}

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.is(TAG);
    }

    /** The item shown on price tags and in messages: the tag's first item, or a diamond if a datapack emptied the tag. */
    public Item displayItem() {
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TAG)) {
            return item.value();
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
