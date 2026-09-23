package diamondvending.gametest.fabric;

import diamondvending.gametest.BuyingTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/** Fabric entrypoint for {@link BuyingTests} (see {@link FabricGameTests} for why the annotations differ per version). */
public final class FabricBuyingTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        BuyingTests.diamondsAreTheDefaultCurrency(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void moneyReadsNaturally(GameTestHelper helper) {
        BuyingTests.moneyReadsNaturally(helper);
    }
}
