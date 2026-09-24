package diamondvending.gametest.fabric;

import diamondvending.gametest.MachineTests;
import diamondvending.gametest.ShopTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/** Fabric entrypoint for {@link ShopTests} (see {@link FabricGameTests} for why the annotations differ per version). */
public final class FabricShopTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCurrencySlotChangesWhatTheMachineTakes(GameTestHelper helper) {
        ShopTests.theCurrencySlotChangesWhatTheMachineTakes(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void creditComesBackAsItWentInAfterACurrencyChange(GameTestHelper helper) {
        ShopTests.creditComesBackAsItWentInAfterACurrencyChange(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theCurrencySlotIsSavedAndSynced(GameTestHelper helper) {
        ShopTests.theCurrencySlotIsSavedAndSynced(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void anUnknownCurrencyLoadsAsTheDefault(GameTestHelper helper) {
        ShopTests.anUnknownCurrencyLoadsAsTheDefault(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void theExampleCatalogLoads(GameTestHelper helper) {
        ShopTests.theExampleCatalogLoads(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aBrokenCatalogIsSkipped(GameTestHelper helper) {
        ShopTests.aBrokenCatalogIsSkipped(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void catalogFilesAreChecked(GameTestHelper helper) {
        ShopTests.catalogFilesAreChecked(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aCatalogMachineSellsTheCatalog(GameTestHelper helper) {
        ShopTests.aCatalogMachineSellsTheCatalog(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void anOwnedCatalogMachineSellsFromItsStock(GameTestHelper helper) {
        ShopTests.anOwnedCatalogMachineSellsFromItsStock(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void aMissingCatalogStopsSalesAndSaysWhy(GameTestHelper helper) {
        ShopTests.aMissingCatalogStopsSalesAndSaysWhy(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void clearingTheCatalogBringsBackOwnSelections(GameTestHelper helper) {
        ShopTests.clearingTheCatalogBringsBackOwnSelections(helper);
    }

    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void clientsSeeWhatTheCatalogSells(GameTestHelper helper) {
        ShopTests.clientsSeeWhatTheCatalogSells(helper);
    }
}
