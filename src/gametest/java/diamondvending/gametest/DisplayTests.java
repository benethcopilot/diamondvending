package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Flash;
import diamondvending.core.MachineLayout;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game tests for what machines show (Plan 4). Each test also needs a method in {@code fabric/FabricDisplayTests}
 * and (for 1.21.1) {@code neoforge/NeoForgeDisplayTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 */
public final class DisplayTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("a_purchase_says_thank_you_and_drops_the_item", DisplayTests::aPurchaseSaysThankYouAndDropsTheItem),
            Map.entry("a_refused_purchase_flashes_the_reason", DisplayTests::aRefusedPurchaseFlashesTheReason),
            Map.entry("the_coin_slot_flashes_wrong_coin", DisplayTests::theCoinSlotFlashesWrongCoin));

    private DisplayTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** What a client knows about the machine: its update tag, loaded into a fresh block entity (which has no level). */
    static VendingMachineBlockEntity clientView(GameTestHelper helper, VendingMachineBlockEntity machine) {
        CompoundTag update = machine.getUpdateTag(BuyingTests.registries(helper));
        update.putString("id", "diamondvending:vending_machine");
        return BuyingTests.reload(helper, machine, update);
    }

    // ---- block events --------------------------------------------------------------------------------------------

    public static void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.pressButton(helper, BuyingTests.buyerWith(helper, 5), 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.lastFlash() == Flash.THANK_YOU, "the display should say thank you, not " + machine.lastFlash());
            helper.assertTrue(machine.lastVendSelection() == 0, "button 1's item should drop, not " + machine.lastVendSelection());
        });
    }

    public static void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.pressButton(helper, BuyingTests.buyerWith(helper, 1), 0);
        helper.succeedWhen(() -> helper.assertTrue(machine.lastFlash() == Flash.NEED_MONEY && machine.lastFlashNumber() == 3,
                "the display should flash NEED 3, not " + machine.lastFlash() + " " + machine.lastFlashNumber()));
    }

    public static void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
        BuyingTests.click(helper, buyer, MachineLayout.COIN_SLOT);
        helper.succeedWhen(() -> helper.assertTrue(machine.lastFlash() == Flash.WRONG_COIN,
                "the display should flash WRONG COIN, not " + machine.lastFlash()));
    }
}
