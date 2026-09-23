package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Problem;
import diamondvending.shop.Currency;
import diamondvending.shop.Selection;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
//? if <26.1 {
/*import net.minecraft.nbt.Tag;
*///?}

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * In-game tests for buying (Plan 3). Each test also needs a method in {@code fabric/FabricBuyingTests} and (for 1.21.1)
 * {@code neoforge/NeoForgeBuyingTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 *
 * <p>Machines here are placed by mock players facing south, so they face north: the front is the machine's north side
 * and its right-hand column is at x − 1 (see {@link MachineTests}).
 */
public final class BuyingTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("diamonds_are_the_default_currency", BuyingTests::diamondsAreTheDefaultCurrency),
            Map.entry("money_reads_naturally", BuyingTests::moneyReadsNaturally),
            Map.entry("contents_survive_save_and_load", BuyingTests::contentsSurviveSaveAndLoad),
            Map.entry("unknown_items_load_as_empty_selections", BuyingTests::unknownItemsLoadAsEmptySelections),
            Map.entry("clients_get_only_what_they_need", BuyingTests::clientsGetOnlyWhatTheyNeed),
            Map.entry("problems_follow_the_machine", BuyingTests::problemsFollowTheMachine),
            Map.entry("infinite_machines_have_no_owner_problems", BuyingTests::infiniteMachinesHaveNoOwnerProblems),
            Map.entry("selections_are_kept_in_range", BuyingTests::selectionsAreKeptInRange));

    private BuyingTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** The component's translation, after checking its key. */
    static TranslatableContents translation(GameTestHelper helper, Component component, String key) {
        helper.assertTrue(component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected the text " + key + " but got " + component);
        return (TranslatableContents) component.getContents();
    }

    static VendingMachineBlockEntity placeMachine(GameTestHelper helper, Player owner) {
        MachineTests.placeOn(helper, owner, MachineTests.machineItem(1), MachineTests.FLOOR);
        VendingMachineBlockEntity machine = MachineTests.machineAt(helper, MachineTests.MASTER);
        helper.assertTrue(machine != null, "the machine should have been placed");
        return machine;
    }

    static HolderLookup.Provider registries(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    /** Loads {@code tag} into a new block entity at the machine's spot, the way the game loads a chunk or a client does. */
    static VendingMachineBlockEntity reload(GameTestHelper helper, VendingMachineBlockEntity machine, CompoundTag tag) {
        BlockEntity loaded = BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), tag, registries(helper));
        helper.assertTrue(loaded instanceof VendingMachineBlockEntity, "the saved machine should load, got " + loaded);
        return (VendingMachineBlockEntity) loaded;
    }

    static void assertSelection(GameTestHelper helper, VendingMachineBlockEntity machine, int index, Item item, int quantity, int price) {
        Selection selection = machine.getSelection(index);
        helper.assertTrue(selection.template().is(item) && selection.quantity() == quantity && selection.price() == price,
                "button " + (index + 1) + " should sell " + quantity + " × " + item + " for " + price + ", but has " + selection);
    }

    static int countIn(List<ItemStack> slots, Item item) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    static void fill(List<ItemStack> slots, Item item) {
        for (int i = 0; i < slots.size(); i++) slots.set(i, new ItemStack(item, 64));
    }

    static void assertProblems(GameTestHelper helper, VendingMachineBlockEntity machine, Problem... expected) {
        helper.assertTrue(machine.problems().equals(List.of(expected)),
                "expected problems " + List.of(expected) + " but got " + machine.problems());
    }

    // ---- currency ------------------------------------------------------------------------------------------------

    public static void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        Currency currency = Currency.DEFAULT;
        ItemStack renamed = new ItemStack(Items.DIAMOND);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Lucky"));
        helper.assertTrue(currency.matches(new ItemStack(Items.DIAMOND, 5)), "diamonds should be money");
        helper.assertTrue(currency.matches(renamed), "a renamed diamond is still a diamond");
        helper.assertFalse(currency.matches(new ItemStack(Items.EMERALD)), "emeralds are not money unless a datapack says so");
        helper.assertFalse(currency.matches(ItemStack.EMPTY), "an empty hand is not money");
        helper.assertTrue(currency.displayItem() == Items.DIAMOND, "prices should show a diamond");
        helper.succeed();
    }

    public static void moneyReadsNaturally(GameTestHelper helper) {
        TranslatableContents one = translation(helper, Currency.DEFAULT.money(1), "currency.diamondvending.minecraft.diamond.one");
        TranslatableContents three = translation(helper, Currency.DEFAULT.money(3), "currency.diamondvending.minecraft.diamond.many");
        helper.assertTrue(one.getArgs()[0].equals(1) && three.getArgs()[0].equals(3), "the amount should be the first argument");
        helper.assertTrue("%s × %s".equals(three.getFallback()), "unnamed currencies should read like \"3 × Emerald\"");
        translation(helper, Currency.DEFAULT.name(), "currency.diamondvending.minecraft.diamond.name");
        helper.succeed();
    }

    // ---- contents ------------------------------------------------------------------------------------------------

    public static void contentsSurviveSaveAndLoad(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        UUID buyer = UUID.randomUUID();
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.setSelection(11, Selection.of(new ItemStack(Items.ARROW, 16), 0));
        machine.setInfinite(true);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        machine.cashBox().set(26, new ItemStack(Items.DIAMOND, 7));
        machine.tray().set(8, new ItemStack(Items.BREAD, 1));
        machine.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));

        VendingMachineBlockEntity loaded = reload(helper, machine, machine.saveWithFullMetadata(registries(helper)));
        assertSelection(helper, loaded, 0, Items.APPLE, 2, 3);
        assertSelection(helper, loaded, 11, Items.ARROW, 16, 0);
        helper.assertFalse(loaded.getSelection(5).isSetUp(), "button 6 was never set up");
        helper.assertTrue(loaded.isInfinite(), "the infinite flag should be saved");
        helper.assertTrue(countIn(loaded.stock(), Items.APPLE) == 10, "stock should be saved");
        helper.assertTrue(loaded.cashBox().get(26).getCount() == 7, "the cash box should be saved slot for slot");
        helper.assertTrue(loaded.tray().get(8).is(Items.BREAD), "the tray should be saved");
        helper.assertTrue(countIn(loaded.credit(buyer), Items.DIAMOND) == 4, "credit should be saved per player");
        helper.assertTrue(machine.getOwner().equals(loaded.getOwner()), "the owner should be saved");
        helper.succeed();
    }

    /** Spec §9: an item from a removed mod loads as an empty button; the rest of the machine keeps working. */
    public static void unknownItemsLoadAsEmptySelections(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE), 1));
        machine.setSelection(1, Selection.of(new ItemStack(Items.BREAD), 2));
        CompoundTag saved = machine.saveWithFullMetadata(registries(helper));
        // Pretend button 1's item came from a mod that has since been removed.
        //? if >=26.1 {
        saved.getListOrEmpty("selections").getCompoundOrEmpty(0).getCompoundOrEmpty("item").putString("id", "notamod:gadget");
        //?} else {
        /*saved.getList("selections", Tag.TAG_COMPOUND).getCompound(0).getCompound("item").putString("id", "notamod:gadget");
        *///?}
        VendingMachineBlockEntity loaded = reload(helper, machine, saved);
        helper.assertFalse(loaded.getSelection(0).isSetUp(), "an unknown item should load as an empty button");
        assertSelection(helper, loaded, 1, Items.BREAD, 1, 2);
        helper.succeed();
    }

    /** Spec §8.3: clients get what they draw and a few totals, never the stock, cash box or anyone's credit items. */
    public static void clientsGetOnlyWhatTheyNeed(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        UUID buyer = UUID.randomUUID();
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        machine.cashBox().set(0, new ItemStack(Items.DIAMOND, 5));
        fill(machine.tray(), Items.BREAD);
        machine.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));

        CompoundTag update = machine.getUpdateTag(registries(helper));
        for (String secret : List.of("stock", "cash_box", "credits")) {
            helper.assertFalse(update.contains(secret), "clients must not be sent the machine's " + secret);
        }
        update.putString("id", "diamondvending:vending_machine");
        VendingMachineBlockEntity client = reload(helper, machine, update);
        assertSelection(helper, client, 0, Items.APPLE, 2, 3);
        helper.assertTrue(client.tray().get(0).is(Items.BREAD), "clients draw the tray, so they need its items");
        helper.assertTrue(client.syncedStockCount(0) == 10, "clients need how much of each item is in stock");
        helper.assertTrue(client.syncedCredit(buyer) == 4, "each player's display shows their own credit");
        helper.assertTrue(client.syncedCredit(UUID.randomUUID()) == 0, "players without credit have none");
        helper.assertTrue(client.syncedProblems().equals(List.of(Problem.TRAY_FULL)), "clients show the machine's problems");
        helper.succeed();
    }

    public static void problemsFollowTheMachine(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        assertProblems(helper, machine, Problem.NOT_SET_UP);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 1));
        assertProblems(helper, machine, Problem.SOLD_OUT);
        machine.stock().set(0, new ItemStack(Items.APPLE, 2));
        assertProblems(helper, machine);
        fill(machine.cashBox(), Items.COBBLESTONE);
        assertProblems(helper, machine, Problem.CASH_BOX_FULL);
        fill(machine.tray(), Items.COBBLESTONE);
        assertProblems(helper, machine, Problem.CASH_BOX_FULL, Problem.TRAY_FULL);
        machine.cashBox().clear(); // NonNullList.clear() empties every slot
        assertProblems(helper, machine, Problem.TRAY_FULL);
        helper.succeed();
    }

    public static void infiniteMachinesHaveNoOwnerProblems(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        machine.setInfinite(true);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE), 1));
        fill(machine.cashBox(), Items.COBBLESTONE);
        assertProblems(helper, machine);
        fill(machine.tray(), Items.COBBLESTONE);
        assertProblems(helper, machine, Problem.TRAY_FULL);
        helper.succeed();
    }

    public static void selectionsAreKeptInRange(GameTestHelper helper) {
        Selection pearls = Selection.of(new ItemStack(Items.ENDER_PEARL, 40), 5000);
        helper.assertTrue(pearls.quantity() == 16, "quantity can't be more than one stack (16 pearls), was " + pearls.quantity());
        helper.assertTrue(pearls.price() == Selection.MAX_PRICE, "prices stop at 999, was " + pearls.price());
        helper.assertTrue(Selection.of(new ItemStack(Items.APPLE), -3).price() == 0, "prices can't be negative");
        helper.assertFalse(Selection.of(ItemStack.EMPTY, 3).isSetUp(), "no item means an empty button");
        helper.succeed();
    }
}
