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
import diamondvending.core.Rect;
import diamondvending.core.SetupButtons;
import diamondvending.core.SetupTab;
import diamondvending.core.Texts;
import diamondvending.menu.VendingSetupMenu;
import diamondvending.registry.ModContent;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.Selection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;
*///?}

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
            Map.entry("unknown_items_in_a_kept_setup_leave_that_button_empty", ShopTests::unknownItemsInAKeptSetupLeaveThatButtonEmpty),
            Map.entry("ghost_slots_copy_without_taking", ShopTests::ghostSlotsCopyWithoutTaking),
            Map.entry("the_editor_buttons_change_amount_and_price", ShopTests::theEditorButtonsChangeAmountAndPrice),
            Map.entry("strangers_cannot_change_anything", ShopTests::strangersCannotChangeAnything),
            Map.entry("the_stock_tab_stocks_the_machine", ShopTests::theStockTabStocksTheMachine),
            Map.entry("the_cash_box_is_take_only", ShopTests::theCashBoxIsTakeOnly),
            Map.entry("withdraw_empties_the_cash_box_into_the_owners_inventory", ShopTests::withdrawEmptiesTheCashBoxIntoTheOwnersInventory),
            Map.entry("only_admins_see_and_use_the_admin_tab", ShopTests::onlyAdminsSeeAndUseTheAdminTab),
            Map.entry("infinite_needs_an_empty_stock_and_cash_box", ShopTests::infiniteNeedsAnEmptyStockAndCashBox),
            Map.entry("the_catalog_picker_cycles_through_every_catalog", ShopTests::theCatalogPickerCyclesThroughEveryCatalog),
            Map.entry("catalog_selections_are_read_only", ShopTests::catalogSelectionsAreReadOnly),
            Map.entry("the_currency_slot_is_for_admins", ShopTests::theCurrencySlotIsForAdmins),
            Map.entry("sneaking_with_empty_hands_opens_setup", ShopTests::sneakingWithEmptyHandsOpensSetup),
            Map.entry("two_open_screens_share_one_machine", ShopTests::twoOpenScreensShareOneMachine),
            Map.entry("breaking_the_machine_closes_its_setup", ShopTests::breakingTheMachineClosesItsSetup),
            Map.entry("shift_click_only_stocks_on_the_stock_tab", ShopTests::shiftClickOnlyStocksOnTheStockTab),
            Map.entry("sneaking_with_an_item_shows_the_empty_hands_hint", ShopTests::sneakingWithAnItemShowsTheEmptyHandsHint),
            Map.entry("strangers_sneaking_with_blocks_place_them_as_usual", ShopTests::strangersSneakingWithBlocksPlaceThemAsUsual),
            Map.entry("an_infinite_machine_is_admin_only_even_for_its_owner", ShopTests::anInfiniteMachineIsAdminOnlyEvenForItsOwner),
            Map.entry("double_click_gathering_skips_hidden_slots", ShopTests::doubleClickGatheringSkipsHiddenSlots),
            Map.entry("dragging_and_number_keys_skip_hidden_slots", ShopTests::draggingAndNumberKeysSkipHiddenSlots),
            Map.entry("spectators_get_no_empty_hands_hint", ShopTests::spectatorsGetNoEmptyHandsHint),
            Map.entry("a_missing_catalog_is_one_press_from_none", ShopTests::aMissingCatalogIsOnePressFromNone));

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

    // ---- the setup menu (spec §4) -------------------------------------------------------------------------------

    /** The setup menu the server would make for this player at this machine. */
    static VendingSetupMenu setupMenu(Player player, VendingMachineBlockEntity machine) {
        return new VendingSetupMenu(0, player.getInventory(), machine.getBlockPos());
    }

    /** A left (0) or right (1) click on a menu slot, holding whatever is on the menu's cursor. */
    static void clickSlot(VendingSetupMenu menu, int slot, int button, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, button, ContainerInput.PICKUP, player);
        //?} else {
        /*menu.clicked(slot, button, ClickType.PICKUP, player);
        *///?}
    }

    static void shiftClickSlot(VendingSetupMenu menu, int slot, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, 0, ContainerInput.QUICK_MOVE, player);
        //?} else {
        /*menu.clicked(slot, 0, ClickType.QUICK_MOVE, player);
        *///?}
    }

    /** Moves a mock player two blocks in front of the test machine: close enough for its setup screen to stay open. */
    static void standInFront(GameTestHelper helper, Player player) {
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(MachineTests.MASTER.north(2))));
    }

    /** A machine owned by {@code owner} selling 2 apples for 3 on button 1, with nothing in stock. */
    static VendingMachineBlockEntity appleMachineOwnedBy(GameTestHelper helper, Player owner) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.setSelection(0, Selection.of(new ItemStack(Items.APPLE, 2), 3));
        return machine;
    }

    public static void ghostSlotsCopyWithoutTaking(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.BREAD, 3));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST + 4, 0, owner);
        BuyingTests.assertSelection(helper, machine, 4, Items.BREAD, 3, 0);
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST + 5, 1, owner);
        BuyingTests.assertSelection(helper, machine, 5, Items.BREAD, 1, 0);
        helper.assertTrue(menu.getCarried().getCount() == 3, "nothing is used up");
        helper.assertTrue(menu.selected() == 5, "the last clicked button is the one being edited");
        helper.succeed();
    }

    public static void theEditorButtonsChangeAmountAndPrice(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertTrue(menu.clickMenuButton(owner, SetupButtons.more(0)), "the owner may change the amount");
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 3, 3);
        for (int i = 0; i < 3; i++) menu.clickMenuButton(owner, SetupButtons.fewer(0));
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 1, 3);
        for (int i = 0; i < 70; i++) menu.clickMenuButton(owner, SetupButtons.more(0));
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 64, 3);
        menu.clickMenuButton(owner, SetupButtons.price(0, 999));
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 64, 999);
        menu.clickMenuButton(owner, SetupButtons.price(3, 5));
        helper.assertFalse(machine.getSelection(3).isSetUp(), "an empty button gets no price");
        menu.clickMenuButton(owner, SetupButtons.clear(0));
        helper.assertFalse(machine.getSelection(0).isSetUp(), "Clear empties the button");
        helper.succeed();
    }

    /** Spec §4: the server re-checks everything, even from a screen a stranger somehow has open. */
    public static void strangersCannotChangeAnything(GameTestHelper helper) {
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        standInFront(helper, stranger);
        VendingSetupMenu menu = setupMenu(stranger, machine);
        helper.assertFalse(menu.stillValid(stranger), "a stranger's setup screen closes at once");
        helper.assertFalse(menu.clickMenuButton(stranger, SetupButtons.price(0, 0)), "and every change is refused");
        menu.setCarried(new ItemStack(Items.DIRT));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST, 0, stranger);
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        helper.succeed();
    }

    public static void theStockTabStocksTheMachine(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        BuyingTests.assertProblems(helper, machine, Problem.SOLD_OUT);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.APPLE, 10));
        clickSlot(menu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(ItemSlots.isEmpty(machine.stock()), "stock slots do nothing while the Items tab is showing");
        helper.assertTrue(menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.STOCK)), "owners have a Stock tab");
        clickSlot(menu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(BuyingTests.countIn(machine.stock(), Items.APPLE) == 10 && menu.getCarried().isEmpty(),
                "the apples go from the cursor into the machine's stock");
        BuyingTests.assertProblems(helper, machine);
        helper.succeed();
    }

    public static void theCashBoxIsTakeOnly(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.CASH_BOX));
        menu.setCarried(new ItemStack(Items.DIRT));
        clickSlot(menu, VendingSetupMenu.FIRST_CASH, 0, owner);
        helper.assertTrue(ItemSlots.isEmpty(machine.cashBox()) && menu.getCarried().is(Items.DIRT), "nothing can be put in the cash box");
        machine.cashBox().set(0, new ItemStack(Items.DIAMOND, 5));
        menu.setCarried(ItemStack.EMPTY);
        clickSlot(menu, VendingSetupMenu.FIRST_CASH, 0, owner);
        helper.assertTrue(menu.getCarried().is(Items.DIAMOND) && menu.getCarried().getCount() == 5 && ItemSlots.isEmpty(machine.cashBox()),
                "but the owner can take the money out");
        helper.succeed();
    }

    public static void withdrawEmptiesTheCashBoxIntoTheOwnersInventory(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.cashBox().set(3, new ItemStack(Items.DIAMOND, 5));
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertTrue(menu.clickMenuButton(owner, SetupButtons.withdraw()), "the owner may withdraw");
        helper.assertTrue(BuyingTests.countHeld(owner, Items.DIAMOND) == 5 && ItemSlots.isEmpty(machine.cashBox()), "the diamonds move to the owner");
        helper.succeed();
    }

    public static void onlyAdminsSeeAndUseTheAdminTab(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertFalse(menu.viewerIsAdmin(), "a survival owner isn't an admin");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.ADMIN)), "so there's no Admin tab");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.toggleInfinite()), "no infinite switch");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.cycleCatalog(1)), "and no catalog picker");
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        helper.assertTrue(setupMenu(admin, machine).clickMenuButton(admin, SetupButtons.showTab(SetupTab.ADMIN)), "creative players are admins");
        helper.succeed();
    }

    /** Spec §4: going infinite needs an empty Stock and Cash Box, so nobody's items silently vanish. */
    public static void infiniteNeedsAnEmptyStockAndCashBox(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        machine.stock().set(0, new ItemStack(Items.APPLE, 3));
        VendingSetupMenu menu = setupMenu(admin, machine);
        helper.assertFalse(menu.stockAndCashEmpty(), "the stock has apples");
        helper.assertFalse(menu.clickMenuButton(admin, SetupButtons.toggleInfinite()) || machine.isInfinite(), "so it can't go infinite yet");
        machine.stock().set(0, ItemStack.EMPTY);
        helper.assertTrue(menu.clickMenuButton(admin, SetupButtons.toggleInfinite()) && machine.isInfinite(), "with both empty it goes infinite");
        helper.assertFalse(menu.clickMenuButton(admin, SetupButtons.showTab(SetupTab.STOCK)), "infinite machines have no Stock tab");
        helper.assertTrue(menu.clickMenuButton(admin, SetupButtons.toggleInfinite()) && !machine.isInfinite(), "and it can always go back");
        helper.succeed();
    }

    public static void theCatalogPickerCyclesThroughEveryCatalog(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        VendingSetupMenu menu = setupMenu(admin, machine);
        List<Identifier> ids = Catalogs.ids();
        for (Identifier expected : ids) {
            menu.clickMenuButton(admin, SetupButtons.cycleCatalog(1));
            helper.assertTrue(expected.equals(machine.catalogId()), "expected catalog " + expected + ", got " + machine.catalogId());
        }
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(1));
        helper.assertTrue(machine.catalogId() == null, "after the last catalog comes None");
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(-1));
        helper.assertTrue(ids.get(ids.size() - 1).equals(machine.catalogId()), "and going back from None gives the last one");
        helper.succeed();
    }

    /** Spec §4: the Selections tab is read-only while a catalog is assigned. */
    public static void catalogSelectionsAreReadOnly(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        machine.setCatalog(EMERALDS);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.DIRT));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST, 0, owner);
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.price(0, 1)), "prices come from the catalog");
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        helper.assertFalse(machine.ownSelection(0).isSetUp(), "and the ghost click changed nothing underneath");
        helper.succeed();
    }

    public static void theCurrencySlotIsForAdmins(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        VendingSetupMenu menu = setupMenu(admin, machine);
        menu.clickMenuButton(admin, SetupButtons.showTab(SetupTab.ADMIN));
        menu.setCarried(new ItemStack(Items.EMERALD, 7));
        clickSlot(menu, VendingSetupMenu.CURRENCY, 0, admin);
        helper.assertTrue(machine.currencySlot() == Items.EMERALD && menu.getCarried().getCount() == 7, "the admin sets emeralds without using any");
        menu.setCarried(ItemStack.EMPTY);
        clickSlot(menu, VendingSetupMenu.CURRENCY, 0, admin);
        helper.assertTrue(machine.currencySlot() == null, "an empty-handed click clears it back to the default");
        helper.succeed();
    }

    /** Spec §3.2 rule 1: sneak + right-click with both hands empty. */
    public static void sneakingWithEmptyHandsOpensSetup(GameTestHelper helper) {
        RecordingServerPlayer owner = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        BuyingTests.placeMachine(helper, owner);
        owner.setShiftKeyDown(true);
        BuyingTests.click(helper, owner, MachineLayout.WINDOW);
        helper.assertTrue(owner.containerMenu instanceof VendingSetupMenu, "the owner's sneak-click opens setup, got " + owner.containerMenu);
        owner.closeContainer();
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        stranger.setShiftKeyDown(true);
        BuyingTests.click(helper, stranger, MachineLayout.WINDOW);
        BuyingTests.lastMessage(helper, stranger, Texts.OWNER_ONLY);
        helper.succeed();
    }

    /** Spec §5.2: only admins set up or break an infinite machine — its owner too, or they could sell from nothing. */
    public static void anInfiniteMachineIsAdminOnlyEvenForItsOwner(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        standInFront(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        machine.setInfinite(true); // an admin made it infinite while the owner had setup open
        helper.assertFalse(menu.stillValid(owner), "the owner's open setup closes");
        helper.assertFalse(menu.clickMenuButton(owner, SetupButtons.price(0, 0)), "and every change is refused");
        menu.setCarried(new ItemStack(Items.NETHERITE_BLOCK, 64));
        clickSlot(menu, VendingSetupMenu.FIRST_GHOST, 0, owner);
        BuyingTests.assertSelection(helper, machine, 0, Items.APPLE, 2, 3);
        owner.setShiftKeyDown(true);
        BuyingTests.click(helper, owner, MachineLayout.WINDOW);
        BuyingTests.lastMessage(helper, owner, Texts.INFINITE_ADMIN_ONLY);
        helper.assertTrue(helper.getBlockState(MachineTests.MASTER).getDestroyProgress(owner, helper.getLevel(),
                helper.absolutePos(MachineTests.MASTER)) == 0, "the owner can't break it");
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        standInFront(helper, admin);
        helper.assertTrue(setupMenu(admin, machine).stillValid(admin), "admins still can");
        helper.succeed();
    }

    public static void twoOpenScreensShareOneMachine(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu ownerMenu = setupMenu(owner, machine);
        VendingSetupMenu adminMenu = setupMenu(admin, machine);
        ownerMenu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.STOCK));
        adminMenu.clickMenuButton(admin, SetupButtons.showTab(SetupTab.STOCK));
        ownerMenu.setCarried(new ItemStack(Items.APPLE, 10));
        clickSlot(ownerMenu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(adminMenu.getSlot(VendingSetupMenu.FIRST_STOCK).getItem().getCount() == 10, "the admin's screen shows the owner's apples");
        clickSlot(adminMenu, VendingSetupMenu.FIRST_STOCK, 0, admin);
        helper.assertTrue(adminMenu.getCarried().getCount() == 10 && ownerMenu.getSlot(VendingSetupMenu.FIRST_STOCK).getItem().isEmpty()
                && ItemSlots.isEmpty(machine.stock()), "once the admin takes them, they're nowhere else");
        ownerMenu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.SELECTIONS));
        ownerMenu.setCarried(new ItemStack(Items.BREAD));
        clickSlot(ownerMenu, VendingSetupMenu.FIRST_GHOST + 2, 0, owner);
        helper.assertTrue(adminMenu.getSlot(VendingSetupMenu.FIRST_GHOST + 2).getItem().is(Items.BREAD), "selections show on both screens");
        helper.succeed();
    }

    public static void breakingTheMachineClosesItsSetup(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        standInFront(helper, owner);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        VendingSetupMenu menu = setupMenu(owner, machine);
        helper.assertTrue(menu.stillValid(owner), "the owner standing in front keeps setup open");
        MachineTests.breakAsPlayer(helper, MachineTests.MASTER, owner);
        helper.assertFalse(menu.stillValid(owner), "once the machine is gone, its setup closes");
        helper.assertTrue(menu.getSlot(VendingSetupMenu.FIRST_STOCK).getItem().isEmpty(), "the stock spilled, so the screen can't hand it out again");
        helper.assertTrue(BuyingTests.droppedNear(helper, MachineTests.MASTER, Items.APPLE) == 10, "the apples are on the ground, once");
        helper.succeed();
    }

    public static void shiftClickOnlyStocksOnTheStockTab(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        owner.getInventory().setItem(0, new ItemStack(Items.APPLE, 10));
        VendingSetupMenu menu = setupMenu(owner, machine);
        int hotbarFirst = VendingSetupMenu.FIRST_PLAYER + 27;
        shiftClickSlot(menu, hotbarFirst, owner);
        helper.assertTrue(owner.getInventory().getItem(0).getCount() == 10 && ItemSlots.isEmpty(machine.stock()),
                "on the Items tab shift-click moves nothing");
        menu.clickMenuButton(owner, SetupButtons.showTab(SetupTab.STOCK));
        shiftClickSlot(menu, hotbarFirst, owner);
        helper.assertTrue(owner.getInventory().getItem(0).isEmpty() && BuyingTests.countIn(machine.stock(), Items.APPLE) == 10,
                "on the Stock tab it stocks the machine");
        shiftClickSlot(menu, VendingSetupMenu.FIRST_STOCK, owner);
        helper.assertTrue(BuyingTests.countHeld(owner, Items.APPLE) == 10 && ItemSlots.isEmpty(machine.stock()), "and shift-clicking stock takes it back");
        helper.succeed();
    }

    // ---- the empty-hands hint (spec §3.2 rule 1) ----------------------------------------------------------------

    /** Right-clicks the front the way a real client's click arrives: through the server's click handling, loader events included. */
    static void useAsServer(GameTestHelper helper, ServerPlayer player, Rect region) {
        BuyingTests.FrontHit at = BuyingTests.frontHit(helper, region.centerU(), region.centerV());
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, at.hit());
    }

    public static void sneakingWithAnItemShowsTheEmptyHandsHint(GameTestHelper helper) {
        RecordingServerPlayer owner = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        BuyingTests.placeMachine(helper, owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        owner.setShiftKeyDown(true);
        useAsServer(helper, owner, MachineLayout.TRAY);
        helper.assertTrue(owner.getMainHandItem().getCount() == 5, "nothing is used up");
        helper.assertTrue(helper.getBlockState(MachineTests.MASTER.north()).isAir(), "and nothing is placed against the machine");
        helper.assertTrue(owner.lastMessage() != null, "the owner should be told how to open setup");
        BuyingTests.translation(helper, owner.lastMessage(), Texts.EMPTY_HANDS);
        helper.succeed();
    }

    public static void strangersSneakingWithBlocksPlaceThemAsUsual(GameTestHelper helper) {
        BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingServerPlayer stranger = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        stranger.setShiftKeyDown(true);
        useAsServer(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(helper.getBlockState(MachineTests.MASTER.north()).is(Blocks.STONE), "a stranger's sneak-click places the block, like vanilla");
        helper.assertTrue(stranger.lastMessage() == null, "and tells them nothing");
        helper.succeed();
    }

    // ---- hidden tabs, spectators and a missing catalog (Plan 5 review, fixed in Plan 6) --------------------------

    /** A double-click on a menu slot while holding something: gathers matching items from the other slots. */
    static void pickAll(VendingSetupMenu menu, int slot, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, 0, ContainerInput.PICKUP_ALL, player);
        //?} else {
        /*menu.clicked(slot, 0, ClickType.PICKUP_ALL, player);
        *///?}
    }

    /** Number key {@code hotbar + 1} over a menu slot: swaps it with that hotbar slot. */
    static void swapWithHotbar(VendingSetupMenu menu, int slot, int hotbar, Player player) {
        //? if >=26.1 {
        menu.clicked(slot, hotbar, ContainerInput.SWAP, player);
        //?} else {
        /*menu.clicked(slot, hotbar, ClickType.SWAP, player);
        *///?}
    }

    /** A left-button drag of the cursor's stack over these slots (spread evenly). */
    static void drag(VendingSetupMenu menu, Player player, int... slots) {
        //? if >=26.1 {
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ContainerInput.QUICK_CRAFT, player);
        for (int slot : slots) menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ContainerInput.QUICK_CRAFT, player);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ContainerInput.QUICK_CRAFT, player);
        //?} else {
        /*menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(0, 0), ClickType.QUICK_CRAFT, player);
        for (int slot : slots) menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(1, 0), ClickType.QUICK_CRAFT, player);
        menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(2, 0), ClickType.QUICK_CRAFT, player);
        *///?}
    }

    /** Spec §4: slots on hidden tabs are inactive — double-click gathering on the Items tab must leave the Stock alone. */
    public static void doubleClickGatheringSkipsHiddenSlots(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = appleMachineOwnedBy(helper, owner);
        machine.stock().set(0, new ItemStack(Items.APPLE, 10));
        owner.getInventory().setItem(9, new ItemStack(Items.APPLE, 5)); // menu slot FIRST_PLAYER
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.APPLE));
        pickAll(menu, VendingSetupMenu.FIRST_PLAYER + 1, owner);
        int stocked = BuyingTests.countIn(machine.stock(), Items.APPLE);
        helper.assertTrue(stocked == 10, "the hidden Stock keeps its 10 apples, it has " + stocked);
        helper.assertTrue(menu.getCarried().getCount() == 6, "the cursor gathers the inventory's 5 apples, it holds " + menu.getCarried().getCount());
        helper.succeed();
    }

    /** The same for dragging a stack and for the number keys (already safe: the menu ignores clicks on hidden slots). */
    public static void draggingAndNumberKeysSkipHiddenSlots(GameTestHelper helper) {
        RecordingPlayer owner = new RecordingPlayer(helper, GameType.SURVIVAL);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, owner);
        VendingSetupMenu menu = setupMenu(owner, machine);
        menu.setCarried(new ItemStack(Items.APPLE, 4));
        drag(menu, owner, VendingSetupMenu.FIRST_STOCK, VendingSetupMenu.FIRST_PLAYER + 1);
        helper.assertTrue(ItemSlots.isEmpty(machine.stock()), "a drag across the hidden Stock puts nothing in it");
        helper.assertTrue(owner.getInventory().getItem(10).getCount() == 4, "the whole stack lands in the inventory slot");
        machine.stock().set(0, new ItemStack(Items.BREAD, 10));
        owner.getInventory().setItem(0, new ItemStack(Items.APPLE, 5));
        swapWithHotbar(menu, VendingSetupMenu.FIRST_STOCK, 0, owner);
        helper.assertTrue(machine.stock().get(0).is(Items.BREAD) && owner.getInventory().getItem(0).is(Items.APPLE),
                "number key 1 over the hidden Stock swaps nothing");
        helper.succeed();
    }

    /** Spec §3.2 rule 1's hint is for a sneak-click that would place or use something; a spectator's can't, so no hint. */
    public static void spectatorsGetNoEmptyHandsHint(GameTestHelper helper) {
        RecordingServerPlayer owner = RecordingServerPlayer.create(helper, GameType.SURVIVAL);
        BuyingTests.placeMachine(helper, owner);
        owner.setGameMode(GameType.SPECTATOR);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 5));
        owner.setShiftKeyDown(true);
        useAsServer(helper, owner, MachineLayout.TRAY);
        helper.assertTrue(owner.lastMessage() == null, "a spectator's sneak-click should say nothing, got " + owner.lastMessage());
        helper.succeed();
    }

    /** Spec §5.1: with CATALOG MISSING an admin picks another catalog or clears it — ▶ clears it in one press. */
    public static void aMissingCatalogIsOnePressFromNone(GameTestHelper helper) {
        RecordingPlayer admin = new RecordingPlayer(helper, GameType.CREATIVE);
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, admin);
        VendingSetupMenu menu = setupMenu(admin, machine);
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(1));
        helper.assertTrue(machine.catalogId() == null, "▶ from a missing catalog should pick None, got " + machine.catalogId());
        machine.setCatalog(DiamondVending.id("no_such_catalog"));
        menu.clickMenuButton(admin, SetupButtons.cycleCatalog(-1));
        helper.assertTrue(Catalogs.ids().get(Catalogs.ids().size() - 1).equals(machine.catalogId()),
                "◀ from a missing catalog should pick the last catalog, got " + machine.catalogId());
        helper.succeed();
    }
}
