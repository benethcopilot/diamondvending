package diamondvending.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import diamondvending.core.MachineLayout;
import diamondvending.shop.Selection;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * A datapack catalog (spec §5.1): what an admin can make a machine sell, from
 * {@code data/<namespace>/diamondvending/catalog/<name>.json}. Entries fill buttons 1…N in order; the format is in
 * docs/catalogs.md.
 */
public final class Catalog {
    private static final Codec<Holder<Item>> ITEM = BuiltInRegistries.ITEM.holderByNameCodec()
            .validate(item -> item.value() == Items.AIR ? DataResult.error(() -> "air can't be sold or used as money") : DataResult.success(item));

    /**
     * An entry's item, in the standard item-stack shape ({@code id}, {@code count}, {@code components}). It stays in
     * parts until it's needed: on 26.1 an item's default components are only bound once datapacks have loaded, so a
     * real stack can't be made while catalogs load (vanilla keeps recipe results the same way).
     */
    private record Stack(Holder<Item> item, int count, DataComponentPatch components) {
        static final Codec<Stack> CODEC = RecordCodecBuilder.create(stack -> stack.group(
                ITEM.fieldOf("id").forGetter(Stack::item),
                Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(Stack::count),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(Stack::components)
        ).apply(stack, Stack::new));
    }

    /** One entry: its item (the count is the amount per purchase) and a price. */
    private record Entry(Stack item, int price) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(entry -> entry.group(
                Stack.CODEC.fieldOf("item").forGetter(Entry::item),
                Codec.intRange(0, Selection.MAX_PRICE).fieldOf("price").forGetter(Entry::price)
        ).apply(entry, Entry::new));
    }

    /** {@code {"id": "minecraft:emerald"}} — shaped like an item stack, but a count is ignored. */
    private static final Codec<Item> CURRENCY = ITEM.xmap(Holder::value, BuiltInRegistries.ITEM::wrapAsHolder).fieldOf("id").codec();

    // Checked after the entries themselves, so a bad entry's own error reaches the log (a size-limited list hides it).
    private static final Codec<List<Entry>> ENTRIES = Entry.CODEC.listOf().validate(entries ->
            entries.isEmpty() || entries.size() > MachineLayout.SELECTIONS
                    ? DataResult.error(() -> "a catalog needs 1 to " + MachineLayout.SELECTIONS + " entries, this one has " + entries.size())
                    : DataResult.success(entries));

    public static final Codec<Catalog> CODEC = RecordCodecBuilder.create(catalog -> catalog.group(
            Codec.STRING.optionalFieldOf("display_name").forGetter(Catalog::displayName),
            CURRENCY.optionalFieldOf("currency").forGetter(Catalog::currency),
            ENTRIES.fieldOf("entries").forGetter(written -> written.entries)
    ).apply(catalog, Catalog::new));

    private final Optional<String> displayName;
    private final Optional<Item> currency;
    private final List<Entry> entries;
    private volatile List<Selection> selections;

    private Catalog(Optional<String> displayName, Optional<Item> currency, List<Entry> entries) {
        this.displayName = displayName;
        this.currency = currency;
        this.entries = List.copyOf(entries);
    }

    public Optional<String> displayName() {
        return displayName;
    }

    /** The catalog's own currency, if it names one. */
    public Optional<Item> currency() {
        return currency;
    }

    /** What buttons 1…N sell, made into real stacks the first time they're needed. */
    public List<Selection> selections() {
        List<Selection> made = selections;
        if (made == null) {
            made = entries.stream()
                    .map(entry -> Selection.of(new ItemStack(entry.item().item(), entry.item().count(), entry.item().components()), entry.price()))
                    .toList();
            selections = made;
        }
        return made;
    }

    /** Button {@code index}'s selection: the entry in that position, or empty past the last entry. */
    public Selection selection(int index) {
        List<Selection> made = selections();
        return index < made.size() ? made.get(index) : Selection.EMPTY;
    }

    /** The name shown in the Admin tab: its display name, or its id when it has none. */
    public String name(Identifier id) {
        return displayName.orElse(id.toString());
    }
}
