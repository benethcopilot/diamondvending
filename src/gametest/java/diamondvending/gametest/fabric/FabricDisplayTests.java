package diamondvending.gametest.fabric;

import diamondvending.gametest.DisplayTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/** Fabric entrypoint for {@link DisplayTests} (see {@link FabricGameTests} for why the annotations differ per version). */
public final class FabricDisplayTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        DisplayTests.aPurchaseSaysThankYouAndDropsTheItem(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        DisplayTests.aRefusedPurchaseFlashesTheReason(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        DisplayTests.theCoinSlotFlashesWrongCoin(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void shelvesShowWhatEachButtonSells(GameTestHelper helper) {
        DisplayTests.shelvesShowWhatEachButtonSells(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theTrayShowsWhatsWaiting(GameTestHelper helper) {
        DisplayTests.theTrayShowsWhatsWaiting(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theDisplaySaysSelectItemThenYourCredit(GameTestHelper helper) {
        DisplayTests.theDisplaySaysSelectItemThenYourCredit(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void problemsTurnTheDisplayRedAndLightTheLamp(GameTestHelper helper) {
        DisplayTests.problemsTurnTheDisplayRedAndLightTheLamp(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void flashesReachClientsWhole(GameTestHelper helper) {
        DisplayTests.flashesReachClientsWhole(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aFlashShowsForTwoSeconds(GameTestHelper helper) {
        DisplayTests.aFlashShowsForTwoSeconds(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aBoughtItemFallsIntoTheTray(GameTestHelper helper) {
        DisplayTests.aBoughtItemFallsIntoTheTray(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void hoveringAButtonShowsWhatItSells(GameTestHelper helper) {
        DisplayTests.hoveringAButtonShowsWhatItSells(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void soldOutAndEmptyButtonsSaySo(GameTestHelper helper) {
        DisplayTests.soldOutAndEmptyButtonsSaySo(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCoinSlotReturnAndTrayShowCounts(GameTestHelper helper) {
        DisplayTests.theCoinSlotReturnAndTrayShowCounts(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void problemsAreExplainedAnywhereOnTheFront(GameTestHelper helper) {
        DisplayTests.problemsAreExplainedAnywhereOnTheFront(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void shopMachinesSaySo(GameTestHelper helper) {
        DisplayTests.shopMachinesSaySo(helper);
    }
}
