package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.BuyingTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * {@link BuyingTests} for NeoForge 1.21.1, found through {@code @GameTestHolder} like {@link NeoForgeGameTests}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeBuyingTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        BuyingTests.diamondsAreTheDefaultCurrency(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void moneyReadsNaturally(GameTestHelper helper) {
        BuyingTests.moneyReadsNaturally(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void contentsSurviveSaveAndLoad(GameTestHelper helper) {
        BuyingTests.contentsSurviveSaveAndLoad(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void unknownItemsLoadAsEmptySelections(GameTestHelper helper) {
        BuyingTests.unknownItemsLoadAsEmptySelections(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void clientsGetOnlyWhatTheyNeed(GameTestHelper helper) {
        BuyingTests.clientsGetOnlyWhatTheyNeed(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void problemsFollowTheMachine(GameTestHelper helper) {
        BuyingTests.problemsFollowTheMachine(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void infiniteMachinesHaveNoOwnerProblems(GameTestHelper helper) {
        BuyingTests.infiniteMachinesHaveNoOwnerProblems(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void selectionsAreKeptInRange(GameTestHelper helper) {
        BuyingTests.selectionsAreKeptInRange(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void trayGoesToWhoeverClicksIt(GameTestHelper helper) {
        BuyingTests.trayGoesToWhoeverClicksIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aFullInventoryDropsTheRestAtYourFeet(GameTestHelper helper) {
        BuyingTests.aFullInventoryDropsTheRestAtYourFeet(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void onlyTheFrontDoesAnything(GameTestHelper helper) {
        BuyingTests.onlyTheFrontDoesAnything(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void heldBlocksAreNeverPlaced(GameTestHelper helper) {
        BuyingTests.heldBlocksAreNeverPlaced(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ownerNameFollowsRenames(GameTestHelper helper) {
        BuyingTests.ownerNameFollowsRenames(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void strangersHoldingDyeCanStillUseTheTray(GameTestHelper helper) {
        BuyingTests.strangersHoldingDyeCanStillUseTheTray(helper);
    }
    *///?}
}
