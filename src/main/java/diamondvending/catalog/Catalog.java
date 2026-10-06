package diamondvending.catalog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import diamondvending.core.MachineLayout;
import diamondvending.shop.Selection;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentPatch;
//?} else {
/*import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
*///?}

import java.util.List;
import java.util.Optional;

/**
 * A datapack catalog (spec §5.1): what an admin can make a machine sell, from
 * {@code data/<namespace>/diamondvending/catalog/<name>.json}. Entries fill buttons 1…N in order; the format is in
 * docs/catalogs.md.
 */
public final class Catalog {
    private static final Codec<Holder<Item>> ITEM = BuiltInRegistries.ITEM.holderByNameCodec().flatXmap(Catalog::notAir, Catalog::notAir);

    //? if >=1.20.5 {
    /**
     * An entry's item, in the standard item-stack shape ({@code id}, {@code count}, {@code components}). It stays in
     * parts until it's needed: on 26.1 an item's default components are only bound once datapacks have loaded, so a
     * real stack can't be made while catalogs load (vanilla keeps recipe results the same way).
     */
    private record Stack(Holder<Item> item, int count, DataComponentPatch components) {
        ItemStack make() {
            return new ItemStack(item, count, components);
        }
    }

    private static final MapCodec<Stack> STACK_FIELDS = RecordCodecBuilder.mapCodec(stack -> stack.group(
            ITEM.fieldOf("id").forGetter(Stack::item),
            optional(Codec.intRange(1, 99), "count", 1).forGetter(Stack::count),
            optional(DataComponentPatch.CODEC, "components", DataComponentPatch.EMPTY).forGetter(Stack::components)
    ).apply(stack, Stack::new));

    // A catalog written for Minecraft 1.20.1 must not quietly sell plain items here (Forge 1.20.1 spec §5.5).
    private static final String OTHER_VERSION_FIELD = "nbt";
    private static final String OTHER_VERSION_ERROR = "nbt is for Minecraft 1.20.1; on this version use \"components\"";
    //?} else {
    /*// An entry's item on 1.20.1: id, count and the item's NBT written as a string, the way /give takes it.
    private record Stack(Holder<Item> item, int count, Optional<CompoundTag> nbt) {
        ItemStack make() {
            ItemStack stack = new ItemStack(item, count);
            nbt.ifPresent(tag -> stack.setTag(tag.copy()));
            return stack;
        }
    }

    private static final Codec<CompoundTag> SNBT = Codec.STRING.comapFlatMap(Catalog::parseSnbt, CompoundTag::toString);

    private static final MapCodec<Stack> STACK_FIELDS = RecordCodecBuilder.mapCodec(stack -> stack.group(
            ITEM.fieldOf("id").forGetter(Stack::item),
            optional(Codec.intRange(1, 99), "count", 1).forGetter(Stack::count),
            optional(SNBT, "nbt").forGetter(Stack::nbt)
    ).apply(stack, Stack::new));

    // A catalog written for a newer Minecraft must not quietly sell plain items here (Forge 1.20.1 spec §5.5).
    private static final String OTHER_VERSION_FIELD = "components";
    private static final String OTHER_VERSION_ERROR = "item components need Minecraft 1.20.5 or newer; on 1.20.1 write the item's NBT as \"nbt\"";

    private static DataResult<CompoundTag> parseSnbt(String snbt) {
        try {
            return DataResult.success(TagParser.parseTag(snbt));
        } catch (CommandSyntaxException e) {
            return DataResult.error(() -> "nbt can't be read: " + e.getMessage());
        }
    }
    *///?}

    private static final Codec<Stack> STACK = Codec.PASSTHROUGH.flatXmap(Catalog::readStack,
            stack -> DataResult.error(() -> "catalogs are only read, never written"));

    /** One entry: its item (the count is the amount per purchase) and a price. */
    private record Entry(Stack item, int price) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(entry -> entry.group(
                STACK.fieldOf("item").forGetter(Entry::item),
                Codec.intRange(0, Selection.MAX_PRICE).fieldOf("price").forGetter(Entry::price)
        ).apply(entry, Entry::new));
    }

    /** {@code {"id": "minecraft:emerald"}} — shaped like an item stack, but a count is ignored. */
    private static final Codec<Item> CURRENCY = ITEM.xmap(Holder::value, BuiltInRegistries.ITEM::wrapAsHolder).fieldOf("id").codec();

    // Checked after the entries themselves, so a bad entry's own error reaches the log (a size-limited list hides it).
    private static final Codec<List<Entry>> ENTRIES = Entry.CODEC.listOf().flatXmap(Catalog::oneToTwelve, Catalog::oneToTwelve);

    public static final Codec<Catalog> CODEC = RecordCodecBuilder.create(catalog -> catalog.group(
            optional(Codec.STRING, "display_name").forGetter(Catalog::displayName),
            optional(CURRENCY, "currency").forGetter(Catalog::currency),
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

    private static DataResult<Holder<Item>> notAir(Holder<Item> item) {
        return item.value() == Items.AIR ? DataResult.error(() -> "air can't be sold or used as money") : DataResult.success(item);
    }

    private static DataResult<List<Entry>> oneToTwelve(List<Entry> entries) {
        return entries.isEmpty() || entries.size() > MachineLayout.SELECTIONS
                ? DataResult.error(() -> "a catalog needs 1 to " + MachineLayout.SELECTIONS + " entries, this one has " + entries.size())
                : DataResult.success(entries);
    }

    private static DataResult<Stack> readStack(Dynamic<?> item) {
        if (item.get(OTHER_VERSION_FIELD).result().isPresent()) return DataResult.error(() -> OTHER_VERSION_ERROR);
        return STACK_FIELDS.codec().parse(item);
    }

    /**
     * An optional field that must be right when it's there: a mistake fails the catalog instead of being skipped
     * (1.20.1's {@code optionalFieldOf} would skip it).
     */
    private static <T> MapCodec<Optional<T>> optional(Codec<T> codec, String name) {
        //? if >=1.20.5 {
        return codec.optionalFieldOf(name);
        //?} else {
        /*return Codec.PASSTHROUGH.optionalFieldOf(name).flatXmap(
                value -> value.isPresent() ? codec.parse(value.get()).map(Optional::of) : DataResult.success(Optional.<T>empty()),
                value -> DataResult.error(() -> "catalogs are only read, never written"));
        *///?}
    }

    private static <T> MapCodec<T> optional(Codec<T> codec, String name, T orElse) {
        return optional(codec, name).xmap(value -> value.orElse(orElse), Optional::of);
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
            made = entries.stream().map(entry -> Selection.of(entry.item().make(), entry.price())).toList();
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
