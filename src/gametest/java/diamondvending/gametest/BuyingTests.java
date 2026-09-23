package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.MachineLayout;
import diamondvending.core.Problem;
import diamondvending.core.Rect;
import diamondvending.shop.Currency;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
            Map.entry("selections_are_kept_in_range", BuyingTests::selectionsAreKeptInRange),
            Map.entry("tray_goes_to_whoever_clicks_it", BuyingTests::trayGoesToWhoeverClicksIt),
            Map.entry("a_full_inventory_drops_the_rest_at_your_feet", BuyingTests::aFullInventoryDropsTheRestAtYourFeet),
            Map.entry("only_the_front_does_anything", BuyingTests::onlyTheFrontDoesAnything),
            Map.entry("held_blocks_are_never_placed", BuyingTests::heldBlocksAreNeverPlaced),
            Map.entry("owner_name_follows_renames", BuyingTests::ownerNameFollowsRenames),
            Map.entry("strangers_holding_dye_can_still_use_the_tray", BuyingTests::strangersHoldingDyeCanStillUseTheTray));

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

    /** Right-clicks canvas point (u, v) on the front, like a player looking at the machine. */
    static void click(GameTestHelper helper, Player player, double u, double v) {
        boolean right = u >= 16;
        boolean upper = v < 16;
        BlockPos part = upper ? (right ? MachineTests.UPPER_RIGHT : MachineTests.UPPER_LEFT)
                : (right ? MachineTests.LOWER_RIGHT : MachineTests.MASTER);
        double faceU = (u - (right ? 16 : 0)) / 16;
        double faceV = (v - (upper ? 0 : 16)) / 16;
        BlockPos at = helper.absolutePos(part);
        // Facing north, the front is each block's z = 0 side, and u runs from east (x + 1) to west (x).
        Vec3 point = new Vec3(at.getX() + 1 - faceU, at.getY() + 1 - faceV, at.getZ());
        helper.useBlock(part, player, new BlockHitResult(point, Direction.NORTH, at, false));
    }

    static void click(GameTestHelper helper, Player player, Rect region) {
        click(helper, player, region.centerU(), region.centerV());
    }

    /** Presses button {@code index} (0–11; the player sees 1–12). */
    static void pressButton(GameTestHelper helper, Player player, int index) {
        click(helper, player, MachineLayout.button(index));
    }

    /** Items of this kind anywhere in the player's inventory. */
    static int countHeld(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** Dropped items of this kind within 3 blocks of a test position. */
    static int droppedNear(GameTestHelper helper, BlockPos relative, Item item) {
        AABB area = new AABB(helper.absolutePos(relative)).inflate(3);
        int total = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(item))) {
            total += entity.getItem().getCount();
        }
        return total;
    }

    /** The player's latest action-bar message, after checking its key. */
    static TranslatableContents lastMessage(GameTestHelper helper, RecordingPlayer player, String key) {
        Component message = player.lastMessage();
        helper.assertTrue(message != null, "expected the player to be told " + key + ", but they were told nothing");
        return translation(helper, message, key);
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

    // ---- clicks and the tray -------------------------------------------------------------------------------------

    public static void trayGoesToWhoeverClicksIt(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 3));
        machine.tray().set(4, new ItemStack(Items.APPLE, 2));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 3 && countHeld(stranger, Items.APPLE) == 2,
                "anyone may take what's in the tray");
        helper.assertTrue(ItemSlots.isEmpty(machine.tray()), "the tray should be empty afterwards");
        helper.succeed();
    }

    public static void aFullInventoryDropsTheRestAtYourFeet(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 5));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            stranger.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        }
        BlockPos feet = MachineTests.platform(3, 1, 1);
        stranger.setPos(Vec3.atBottomCenterOf(helper.absolutePos(feet)));
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(droppedNear(helper, feet, Items.BREAD) == 5, "bread that doesn't fit should drop at the player's feet");
        helper.assertTrue(ItemSlots.isEmpty(machine.tray()), "nothing should stay behind or vanish");
        helper.succeed();
    }

    public static void onlyTheFrontDoesAnything(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 1));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        // The left column's outer side faces east. A click low on it is level with the tray, but it isn't the front.
        BlockPos master = helper.absolutePos(MachineTests.MASTER);
        helper.useBlock(MachineTests.MASTER, stranger, new BlockHitResult(
                new Vec3(master.getX() + 1, master.getY() + 0.2, master.getZ() + 0.5), Direction.EAST, master, false));
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 0, "clicking the side must not empty the tray");
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 1, "the tray on the front still works");
        helper.succeed();
    }

    /** Spec §3.2: clicks are always used up by the machine, so a block in hand is never placed against it. */
    public static void heldBlocksAreNeverPlaced(GameTestHelper helper) {
        placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 5);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, cobblestone);
        click(helper, stranger, MachineLayout.WINDOW);
        helper.assertTrue(cobblestone.getCount() == 5, "clicking the machine must never place the block in your hand");
        MachineTests.assertAir(helper, MachineTests.UPPER_LEFT.north());
        helper.succeed();
    }

    /** Spec §5.3: the owner's last-known name is kept current. */
    public static void ownerNameFollowsRenames(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL, id, "Alex"));
        helper.assertTrue(machine.getOwnerName().equals("Alex"), "the placer's name should be saved");
        click(helper, new RecordingPlayer(helper, GameType.SURVIVAL, id, "Sam"), MachineLayout.WINDOW);
        helper.assertTrue(machine.getOwnerName().equals("Sam"), "the owner's new name should be picked up, was " + machine.getOwnerName());
        helper.succeed();
    }

    /** Dyeing is for owners and admins; a customer who happens to hold dye still gets to use the machine. */
    public static void strangersHoldingDyeCanStillUseTheTray(GameTestHelper helper) {
        VendingMachineBlockEntity machine = placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        machine.tray().set(0, new ItemStack(Items.BREAD, 1));
        RecordingPlayer stranger = new RecordingPlayer(helper, GameType.SURVIVAL);
        ItemStack dye = new ItemStack(Items.BLUE_DYE, 2);
        stranger.setItemInHand(InteractionHand.MAIN_HAND, dye);
        click(helper, stranger, MachineLayout.TRAY);
        helper.assertTrue(countHeld(stranger, Items.BREAD) == 1, "a dye in hand shouldn't stop a customer using the tray");
        helper.assertTrue(dye.getCount() == 2 && stranger.messages().isEmpty(), "and it isn't treated as an attempt to dye");
        MachineTests.assertWholeMachine(helper, DyeColor.RED);
        helper.succeed();
    }
}
