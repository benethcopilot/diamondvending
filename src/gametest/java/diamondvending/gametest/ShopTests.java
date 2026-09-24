package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.MachineLayout;
import diamondvending.core.Texts;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

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
            Map.entry("an_unknown_currency_loads_as_the_default", ShopTests::anUnknownCurrencyLoadsAsTheDefault));

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
}
