package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.DisplayTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * {@link DisplayTests} for NeoForge 1.21.1, found through {@code @GameTestHolder} like {@link NeoForgeGameTests}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeDisplayTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        DisplayTests.aPurchaseSaysThankYouAndDropsTheItem(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        DisplayTests.aRefusedPurchaseFlashesTheReason(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        DisplayTests.theCoinSlotFlashesWrongCoin(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void shelvesShowWhatEachButtonSells(GameTestHelper helper) {
        DisplayTests.shelvesShowWhatEachButtonSells(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theTrayShowsWhatsWaiting(GameTestHelper helper) {
        DisplayTests.theTrayShowsWhatsWaiting(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theDisplaySaysSelectItemThenYourCredit(GameTestHelper helper) {
        DisplayTests.theDisplaySaysSelectItemThenYourCredit(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void problemsTurnTheDisplayRedAndLightTheLamp(GameTestHelper helper) {
        DisplayTests.problemsTurnTheDisplayRedAndLightTheLamp(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void flashesReachClientsWhole(GameTestHelper helper) {
        DisplayTests.flashesReachClientsWhole(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aFlashShowsForTwoSeconds(GameTestHelper helper) {
        DisplayTests.aFlashShowsForTwoSeconds(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aBoughtItemFallsIntoTheTray(GameTestHelper helper) {
        DisplayTests.aBoughtItemFallsIntoTheTray(helper);
    }
    *///?}
}
