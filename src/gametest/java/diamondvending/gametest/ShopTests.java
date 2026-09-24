package diamondvending.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import diamondvending.DiamondVending;
import diamondvending.block.MachineItems;
import diamondvending.block.MachineSetup;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.catalog.Catalog;
import diamondvending.catalog.Catalogs;
import diamondvending.core.MachineLayout;
import diamondvending.core.Problem;
import diamondvending.core.Texts;
import diamondvending.registry.ModContent;
import diamondvending.shop.Selection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game tests for running a shop (Plan 5): currency, catalogs, the setup kept on the item, the setup menu and the
 * empty-hands hint. Each test also needs a method in {@code fabric/FabricShopTests} and (for 1.21.1)
 * {@code neoforge/NeoForgeShopTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 */
public final class ShopTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("the_currency_slot_changes_what_the_machine_takes", ShopTests::theCurrencySlotChangesWhatTheMachineTakes),
            Map.entry("credit_comes_back_as_it_went_in_after_a_currency_change", ShopTests::creditComesBackAsItWentInAfterACurrencyChange),
            Map.entry("the_currency_slot_is_saved_and_synced", ShopTests::theCurrencySlotIsSavedAndSynced),
            Map.entry("an_unknown_currency_loads_as_the_default", ShopTests::anUnknownCurrencyLoadsAsTheDefault),
            Map.entry("the_example_catalog_loads", ShopTests::theExampleCatalogLoads),
            Map.entry("a_broken_catalog_is_skipped", ShopTests::aBrokenCatalogIsSkipped),
            Map.entry("catalog_files_are_checked", ShopTests::catalogFilesAreChecked),
            Map.entry("a_catalog_machine_sells_the_catalog", ShopTests::aCatalogMachineSellsTheCatalog),
            Map.entry("an_owned_catalog_machine_sells_from_its_stock", ShopTests::anOwnedCatalogMachineSellsFromItsStock),
            Map.entry("a_missing_catalog_stops_sales_and_says_why", ShopTests::aMissingCatalogStopsSalesAndSaysWhy),
            Map.entry("clearing_the_catalog_brings_back_own_selections", ShopTests::clearingTheCatalogBringsBackOwnSelections),
            Map.entry("clients_see_what_the_catalog_sells", ShopTests::clientsSeeWhatTheCatalogSells),
            Map.entry("breaking_keeps_the_setup_on_the_item", ShopTests::breakingKeepsTheSetupOnTheItem),
            Map.entry("placing_restores_the_setup_for_the_new_owner", ShopTests::placingRestoresTheSetupForTheNewOwner),
            Map.entry("infinite_stays_only_for_admin_placers", ShopTests::infiniteStaysOnlyForAdminPlacers),
            Map.entry("an_unset_machine_drops_a_plain_item", ShopTests::anUnsetMachineDropsAPlainItem),
            Map.entry("unknown_items_in_a_kept_setup_leave_that_button_empty", ShopTests::unknownItemsInAKeptSetupLeaveThatButtonEmpty));

    private ShopTests() {}

    // ---- currency (spec §5.5) -----------------------------------------------------------------------------------

    public static void theCurrencySlotChangesWhatTheMachineTakes(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // button 1: 2 apples for 3
        machine.setCurrencySlot(Items.EMERALD);
        RecordingPlayer diamonds = BuyingTests.buyerWith(helper, 5);
        BuyingTests.pressButton(helper, diamonds, 0);
        Object[] args = BuyingTests.lastMessage(helper, diamonds, Texts.NEED_MONEY).getArgs();
        BuyingTests.translation(helper, (Component) args[1], "currency.diamondvending.minecraft.emerald.many");
        helper.assertTrue(args[2].equals(0), "diamonds aren't money on a machine that takes emeralds, but the message counted " + args[2]);
        RecordingPlayer emeralds = new RecordingPlayer(helper, GameType.SURVIVAL);
        emeralds.getInventory().add(new ItemStack(Items.EMERALD, 3));
        BuyingTests.pressButton(helper, emeralds, 0);
        helper.assertTrue(BuyingTests.countIn(machine.tray(), Items.APPLE) == 2, "3 emeralds should buy the apples");
        helper.assertTrue(BuyingTests.countIn(machine.cashBox(), Items.EMERALD) == 3, "and go in the cash box");
        helper.assertTrue(BuyingTests.countHeld(diamonds, Items.DIAMOND) == 5, "nobody's diamonds were taken");
        helper.succeed();
    }

    /** Spec §3.4: credit is kept as the items that went in, so changing the currency never swallows it. */
    public static void creditComesBackAsItWentInAfterACurrencyChange(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND, 4));
        BuyingTests.click(helper, buyer, MachineLayout.COIN_SLOT);
        machine.setCurrencySlot(Items.EMERALD);
        BuyingTests.click(helper, buyer, MachineLayout.COIN_RETURN);
        int back = BuyingTests.countHeld(buyer, Items.DIAMOND);
        helper.assertTrue(back == 4, "the 4 diamonds should come back, got " + back);
        helper.succeed();
    }

    public static void theCurrencySlotIsSavedAndSynced(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setCurrencySlot(Items.EMERALD);
        VendingMachineBlockEntity loaded = BuyingTests.reload(helper, machine, machine.saveWithFullMetadata(BuyingTests.registries(helper)));
        helper.assertTrue(loaded.currencySlot() == Items.EMERALD, "the currency slot should be saved, got " + loaded.currencySlot());
        VendingMachineBlockEntity client = DisplayTests.clientView(helper, machine);
        helper.assertTrue(client.currency().displayItem() == Items.EMERALD, "clients should show emerald prices");
        helper.assertTrue(client.currency().matches(new ItemStack(Items.EMERALD)), "and count emerald credit");
        helper.succeed();
    }

    /** Spec §9: a currency item from a removed mod loads as the default currency. */
    public static void anUnknownCurrencyLoadsAsTheDefault(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        CompoundTag saved = machine.saveWithFullMetadata(BuyingTests.registries(helper));
        saved.putString("currency", "notamod:coin");
        VendingMachineBlockEntity loaded = BuyingTests.reload(helper, machine, saved);
        helper.assertTrue(loaded.currencySlot() == null, "an unknown currency should load as an empty slot");
        helper.assertTrue(loaded.currency().displayItem() == Items.DIAMOND, "and the machine takes diamonds again");
        helper.succeed();
    }

    // ---- catalogs (spec §5.1) -----------------------------------------------------------------------------------

    /** Test data (src/gametest/resources): button 1 = 2 apples for 3 emeralds, button 2 = free bread. */
    static final Identifier EMERALDS = DiamondVending.id("test_emeralds");

    public static void theExampleCatalogLoads(GameTestHelper helper) {
        Identifier id = DiamondVending.id("example_snacks");
        Catalog catalog = Catalogs.get(id);
        helper.assertTrue(catalog != null, "the example catalog should load; loaded: " + Catalogs.ids());
        helper.assertTrue(catalog.name(id).equals("Snack Shack"), "its name comes from display_name, got " + catalog.name(id));
        helper.assertTrue(catalog.selections().size() == 6, "it has 6 entries, got " + catalog.selections().size());
        ItemStack book = catalog.selection(5).template();
        helper.assertTrue(book.is(Items.ENCHANTED_BOOK)
                        && !book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                "button 6 sells a book with mending on it, got " + book);
        helper.assertTrue(catalog.currency().isEmpty(), "it takes the default currency");
        helper.succeed();
    }

    public static void aBrokenCatalogIsSkipped(GameTestHelper helper) {
        helper.assertTrue(Catalogs.get(DiamondVending.id("test_broken")) == null, "a catalog with a price of 5000 must not load");
        helper.assertTrue(Catalogs.get(EMERALDS) != null, "the good test catalog next to it still loads");
        helper.succeed();
    }

    /** Spec §5.1: 1 to 12 entries, prices 0–999, real items. */
    public static void catalogFilesAreChecked(GameTestHelper helper) {
        String apple = "{\"item\":{\"id\":\"minecraft:apple\"},\"price\":1}";
        helper.assertTrue(parses(helper, "{\"entries\":[" + apple + "]}"), "one apple is a fine catalog");
        helper.assertFalse(parses(helper, "{\"entries\":[" + String.join(",", Collections.nCopies(13, apple)) + "]}"), "13 entries are too many");
        helper.assertFalse(parses(helper, "{\"entries\":[]}"), "a catalog needs at least one entry");
        helper.assertFalse(parses(helper, "{\"entries\":[{\"item\":{\"id\":\"minecraft:apple\"},\"price\":1000}]}"), "prices stop at 999");
        helper.assertFalse(parses(helper, "{\"entries\":[{\"item\":{\"id\":\"notamod:gadget\"},\"price\":1}]}"), "unknown items are rejected");
        helper.assertFalse(parses(helper, "{\"currency\":{\"id\":\"minecraft:air\"},\"entries\":[" + apple + "]}"), "air is no currency");
        helper.succeed();
    }

    private static boolean parses(GameTestHelper helper, String json) {
        return Catalog.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, BuyingTests.registries(helper)), JsonParser.parseString(json))
                .result().isPresent();
    }

    public static void aCatalogMachineSellsTheCatalog(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.BREAD), 1)); // the machine's own button 1, hidden by the catalog
        machine.setInfinite(true);
        machine.setCatalog(EMERALDS);
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.getInventory().add(new ItemStack(Items.EMERALD, 3));
        BuyingTests.pressButton(helper, buyer, 0);
        helper.assertTrue(BuyingTests.countIn(machine.tray(), Items.APPLE) == 2, "button 1 sells the catalog's 2 apples");
        helper.assertTrue(BuyingTests.countHeld(buyer, Items.EMERALD) == 0, "for the catalog's price, in its currency");
        helper.assertTrue(BuyingTests.countIn(machine.cashBox(), Items.EMERALD) == 0, "an infinite machine destroys the money");
        helper.succeed();
    }

    /** Spec §5.2: owned machines with a catalog still sell from their stock. */
    public static void anOwnedCatalogMachineSellsFromItsStock(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setCatalog(EMERALDS);
        BuyingTests.assertProblems(helper, machine, Problem.SOLD_OUT);
        machine.stock().set(0, new ItemStack(Items.APPLE, 4));
        machine.changed();
        BuyingTests.assertProblems(helper, machine);
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.getInventory().add(new ItemStack(Items.EMERALD, 3));
        BuyingTests.pressButton(helper, buyer, 0);
        helper.assertTrue(BuyingTests.countIn(machine.stock(), Items.APPLE) == 2, "the apples come out of stock");
        helper.assertTrue(BuyingTests.countIn(machine.cashBox(), Items.EMERALD) == 3, "the emeralds go in the cash box");
        helper.succeed();
    }

    /** Spec §5.1: a machine whose catalog isn't loaded says so and sells nothing. */
    public static void aMissingCatalogStopsSalesAndSaysWhy(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        BuyingTests.assertProblems(helper, machine, Problem.CATALOG_MISSING);
        RecordingPlayer buyer = BuyingTests.buyerWith(helper, 5);
        BuyingTests.pressButton(helper, buyer, 0);
        BuyingTests.lastMessage(helper, buyer, Texts.explanation(Problem.CATALOG_MISSING));
        helper.assertTrue(BuyingTests.countHeld(buyer, Items.DIAMOND) == 5 && BuyingTests.countIn(machine.tray(), Items.APPLE) == 0,
                "nothing is sold");
        helper.succeed();
    }

    public static void clearingTheCatalogBringsBackOwnSelections(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // own button 1: 2 apples for 3, 10 in stock
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        machine.setCatalog(null);
        BuyingTests.assertProblems(helper, machine);
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        helper.succeed();
    }

    /** Spec §8.3: clients get the catalog's selections, name and currency; the save keeps the id and the own selections. */
    public static void clientsSeeWhatTheCatalogSells(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.BREAD), 1));
        machine.setCatalog(EMERALDS);
        VendingMachineBlockEntity client = DisplayTests.clientView(helper, machine);
        BuyingTests.assertSelection(helper, client, 0, Items.APPLE, 2, 3);
        helper.assertTrue(client.currency().displayItem() == Items.EMERALD, "clients show the catalog's currency");
        helper.assertTrue(client.usesCatalog() && client.catalogLabel().equals("Emerald Emporium"),
                "clients know the catalog's name, got \"" + client.catalogLabel() + "\"");
        VendingMachineBlockEntity loaded = BuyingTests.reload(helper, machine, machine.saveWithFullMetadata(BuyingTests.registries(helper)));
        helper.assertTrue(EMERALDS.equals(loaded.catalogId()), "the save keeps the catalog id");
        helper.assertTrue(loaded.ownSelection(0).template().is(Items.BREAD), "and the machine's own button 1");
        helper.succeed();
    }

    // ---- the setup kept on the item (spec §5.4) -----------------------------------------------------------------

    /** Breaks the test machine as {@code player} and picks up the machine item it dropped. */
    static ItemStack breakAndPickUp(GameTestHelper helper, Player player) {
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, player);
        List<ItemEntity> drops = MachineTests.droppedMachines(helper);
        helper.assertTrue(drops.size() == 1, "breaking should drop one machine item, dropped " + drops.size());
        ItemStack stack = drops.get(0).getItem().copy();
        drops.forEach(Entity::discard);
        return stack;
    }

    public static void breakingKeepsTheSetupOnTheItem(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.setSelection(11, Selection.of(new ItemStack(Items.ARROW, 16), 0));
        machine.setCurrencySlot(Items.EMERALD);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        MachineSetup setup = breakAndPickUp(helper, owner).get(ModContent.MACHINE_SETUP.get());
        helper.assertTrue(setup != null, "the dropped machine should carry its setup");
        Selection first = setup.selection(0);
        helper.assertTrue(first.template().is(Items.APPLE) && first.quantity() == 2 && first.price() == 3, "button 1 is kept, got " + first);
        helper.assertTrue(setup.selection(11).template().is(Items.ARROW) && setup.selection(11).quantity() == 16, "button 12 is kept");
        helper.assertTrue(setup.currency() == Items.EMERALD, "the currency slot is kept");
        helper.assertTrue(BuyingTests.droppedNear(helper, MachineTests.MASTER, Items.APPLE) == 10, "the stock spills instead of riding on the item");
        helper.succeed();
    }

    public static void placingRestoresTheSetupForTheNewOwner(GameTestHelper helper) {
        RecordingPlayer first = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, first);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        machine.setCatalog(EMERALDS);
        machine.setCurrencySlot(Items.GOLD_INGOT);
        ItemStack item = breakAndPickUp(helper, first);
        RecordingPlayer second = new RecordingPlayer(helper, GameType.SURVIVAL);
        MachineTests.placeOn(helper, second, item, MachineTests.FLOOR);
        VendingMachineBlockEntity placed = MachineTests.machineAt(helper, MachineTests.MASTER);
        helper.assertTrue(placed != null && second.getUUID().equals(placed.getOwner()), "whoever places it owns it");
        helper.assertTrue(placed.ownSelection(0).template().is(Items.APPLE) && placed.ownSelection(0).price() == 3, "button 1 comes back");
        helper.assertTrue(EMERALDS.equals(placed.catalogId()), "the catalog comes back");
        helper.assertTrue(placed.currencySlot() == Items.GOLD_INGOT, "the currency slot comes back");
        helper.succeed();
    }

    /** Spec §5.4: an infinite machine comes back infinite only for an admin. */
    public static void infiniteStaysOnlyForAdminPlacers(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setInfinite(true);
        ItemStack item = MachineItems.forMachine(DyeColor.RED, machine);
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, new RecordingPlayer(helper, GameType.CREATIVE));
        MachineTests.placeOn(helper, new RecordingPlayer(helper, GameType.SURVIVAL), item.copy(), MachineTests.FLOOR);
        helper.assertFalse(MachineTests.machineAt(helper, MachineTests.MASTER).isInfinite(), "a player who isn't an admin gets an owned machine");
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, new RecordingPlayer(helper, GameType.CREATIVE));
        MachineTests.placeOn(helper, new RecordingPlayer(helper, GameType.CREATIVE), item.copy(), MachineTests.FLOOR);
        helper.assertTrue(MachineTests.machineAt(helper, MachineTests.MASTER).isInfinite(), "an admin gets it back infinite");
        helper.succeed();
    }

    /** A machine nobody set up drops a plain item, so it stacks with new ones. */
    public static void anUnsetMachineDropsAPlainItem(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        ItemStack item = MachineItems.forMachine(DyeColor.RED, machine);
        helper.assertTrue(ItemStack.isSameItemSameComponents(item, MachineItems.forColor(DyeColor.RED)), "expected a plain machine item, got " + item);
        helper.succeed();
    }

    /** Spec §9: a kept setup naming an item from a removed mod still loads; only that button comes back empty. */
    public static void unknownItemsInAKeptSetupLeaveThatButtonEmpty(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE), 1));
        machine.setSelection(1, Selection.of(new ItemStack(Items.BREAD), 2));
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, BuyingTests.registries(helper));
        MachineSetup setup = MachineSetup.of(machine);
        helper.assertTrue(setup.equals(MachineSetup.CODEC.parse(ops, MachineSetup.CODEC.encodeStart(ops, setup).getOrThrow()).getOrThrow()),
                "a setup survives a save unchanged");
        CompoundTag saved = (CompoundTag) MachineSetup.CODEC.encodeStart(ops, setup).getOrThrow();
        // Pretend button 1's item came from a mod that has since been removed.
        //? if >=26.1 {
        saved.getListOrEmpty("selections").getCompoundOrEmpty(0).getCompoundOrEmpty("item").putString("id", "notamod:gadget");
        //?} else {
        /*saved.getList("selections", Tag.TAG_COMPOUND).getCompound(0).getCompound("item").putString("id", "notamod:gadget");
        *///?}
        MachineSetup loaded = MachineSetup.CODEC.parse(ops, saved).getOrThrow();
        helper.assertFalse(loaded.selection(0).isSetUp(), "the unknown item leaves button 1 empty");
        helper.assertTrue(loaded.selection(1).template().is(Items.BREAD) && loaded.selection(1).price() == 2, "button 2 is untouched");
        helper.succeed();
    }
}
