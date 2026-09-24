package diamondvending.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import diamondvending.core.MachineLayout;
import diamondvending.shop.Selection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A machine's setup, carried by its item after it's broken (spec §5.4): what the 12 buttons sell, the catalog, the
 * currency slot and the infinite flag — never its contents. The {@code diamondvending:machine_setup} item component.
 */
public final class MachineSetup {
    /** One set-up button. An item from a mod that's gone leaves just that button empty (spec §9). */
    private record Entry(int slot, Optional<ItemStack> item, int price) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(entry -> entry.group(
                Codec.intRange(0, MachineLayout.SELECTIONS - 1).fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.lenientOptionalFieldOf("item").forGetter(Entry::item),
                Codec.intRange(0, Selection.MAX_PRICE).fieldOf("price").forGetter(Entry::price)
        ).apply(entry, Entry::new));
    }

    public static final Codec<MachineSetup> CODEC = RecordCodecBuilder.create(setup -> setup.group(
            Entry.CODEC.listOf().optionalFieldOf("selections", List.of()).forGetter(MachineSetup::entries),
            Identifier.CODEC.optionalFieldOf("catalog").forGetter(s -> Optional.ofNullable(s.catalog)),
            BuiltInRegistries.ITEM.byNameCodec().lenientOptionalFieldOf("currency").forGetter(s -> Optional.ofNullable(s.currency)),
            Codec.BOOL.optionalFieldOf("infinite", false).forGetter(s -> s.infinite)
    ).apply(setup, MachineSetup::new));

    private final Selection[] selections = new Selection[MachineLayout.SELECTIONS];
    private final Identifier catalog;
    private final Item currency;
    private final boolean infinite;

    private MachineSetup(List<Entry> entries, Optional<Identifier> catalog, Optional<Item> currency, boolean infinite) {
        Arrays.fill(selections, Selection.EMPTY);
        for (Entry entry : entries) {
            entry.item().ifPresent(item -> selections[entry.slot()] = Selection.of(item, entry.price()));
        }
        this.catalog = catalog.orElse(null);
        this.currency = currency.orElse(null);
        this.infinite = infinite;
    }

    /** What a machine is set up to do: its own selections (not a catalog's), catalog, currency slot and infinite flag. */
    public static MachineSetup of(VendingMachineBlockEntity machine) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Selection selection = machine.ownSelection(i);
            if (selection.isSetUp()) entries.add(new Entry(i, Optional.of(selection.template().copy()), selection.price()));
        }
        return new MachineSetup(entries, Optional.ofNullable(machine.catalogId()), Optional.ofNullable(machine.currencySlot()),
                machine.isInfinite());
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < selections.length; i++) {
            if (selections[i].isSetUp()) entries.add(new Entry(i, Optional.of(selections[i].template()), selections[i].price()));
        }
        return entries;
    }

    /** True for a machine nobody set up: its item then stays plain and stacks with new machines. */
    public boolean isEmpty() {
        return Arrays.stream(selections).noneMatch(Selection::isSetUp) && catalog == null && currency == null && !infinite;
    }

    public Selection selection(int index) {
        return selections[index];
    }

    /** The catalog id, or null. */
    public Identifier catalog() {
        return catalog;
    }

    /** The currency slot's item, or null. */
    public Item currency() {
        return currency;
    }

    public boolean infinite() {
        return infinite;
    }

    /** Puts this setup on a newly placed machine (spec §5.4); it stays infinite only when an admin placed it. */
    public void applyTo(VendingMachineBlockEntity machine, boolean placedByAdmin) {
        for (int i = 0; i < selections.length; i++) machine.setSelection(i, selections[i]);
        machine.setCatalog(catalog);
        machine.setCurrencySlot(currency);
        machine.setInfinite(infinite && placedByAdmin);
    }

    // Components are compared when items stack and when menus sync, so equal setups must be equal.
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MachineSetup that)) return false;
        for (int i = 0; i < selections.length; i++) {
            Selection mine = selections[i];
            Selection theirs = that.selections[i];
            if (mine.price() != theirs.price() || !ItemStack.matches(mine.template(), theirs.template())) return false;
        }
        return Objects.equals(catalog, that.catalog) && currency == that.currency && infinite == that.infinite;
    }

    @Override
    public int hashCode() {
        int hash = Objects.hash(catalog, currency, infinite);
        for (Selection selection : selections) {
            hash = 31 * hash + 31 * ItemStack.hashItemAndComponents(selection.template()) + 7 * selection.quantity() + selection.price();
        }
        return hash;
    }
}
