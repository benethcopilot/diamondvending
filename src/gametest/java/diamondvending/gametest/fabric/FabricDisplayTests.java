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
}
