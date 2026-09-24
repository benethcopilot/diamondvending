package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import diamondvending.gametest.ShopTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * {@link ShopTests} for NeoForge 1.21.1, found through {@code @GameTestHolder} like {@link NeoForgeGameTests}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeShopTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCurrencySlotChangesWhatTheMachineTakes(GameTestHelper helper) {
        ShopTests.theCurrencySlotChangesWhatTheMachineTakes(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creditComesBackAsItWentInAfterACurrencyChange(GameTestHelper helper) {
        ShopTests.creditComesBackAsItWentInAfterACurrencyChange(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCurrencySlotIsSavedAndSynced(GameTestHelper helper) {
        ShopTests.theCurrencySlotIsSavedAndSynced(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void anUnknownCurrencyLoadsAsTheDefault(GameTestHelper helper) {
        ShopTests.anUnknownCurrencyLoadsAsTheDefault(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theExampleCatalogLoads(GameTestHelper helper) {
        ShopTests.theExampleCatalogLoads(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aBrokenCatalogIsSkipped(GameTestHelper helper) {
        ShopTests.aBrokenCatalogIsSkipped(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void catalogFilesAreChecked(GameTestHelper helper) {
        ShopTests.catalogFilesAreChecked(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aCatalogMachineSellsTheCatalog(GameTestHelper helper) {
        ShopTests.aCatalogMachineSellsTheCatalog(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void anOwnedCatalogMachineSellsFromItsStock(GameTestHelper helper) {
        ShopTests.anOwnedCatalogMachineSellsFromItsStock(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aMissingCatalogStopsSalesAndSaysWhy(GameTestHelper helper) {
        ShopTests.aMissingCatalogStopsSalesAndSaysWhy(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void clearingTheCatalogBringsBackOwnSelections(GameTestHelper helper) {
        ShopTests.clearingTheCatalogBringsBackOwnSelections(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void clientsSeeWhatTheCatalogSells(GameTestHelper helper) {
        ShopTests.clientsSeeWhatTheCatalogSells(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void breakingKeepsTheSetupOnTheItem(GameTestHelper helper) {
        ShopTests.breakingKeepsTheSetupOnTheItem(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placingRestoresTheSetupForTheNewOwner(GameTestHelper helper) {
        ShopTests.placingRestoresTheSetupForTheNewOwner(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void infiniteStaysOnlyForAdminPlacers(GameTestHelper helper) {
        ShopTests.infiniteStaysOnlyForAdminPlacers(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void anUnsetMachineDropsAPlainItem(GameTestHelper helper) {
        ShopTests.anUnsetMachineDropsAPlainItem(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void unknownItemsInAKeptSetupLeaveThatButtonEmpty(GameTestHelper helper) {
        ShopTests.unknownItemsInAKeptSetupLeaveThatButtonEmpty(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ghostSlotsCopyWithoutTaking(GameTestHelper helper) {
        ShopTests.ghostSlotsCopyWithoutTaking(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theEditorButtonsChangeAmountAndPrice(GameTestHelper helper) {
        ShopTests.theEditorButtonsChangeAmountAndPrice(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void strangersCannotChangeAnything(GameTestHelper helper) {
        ShopTests.strangersCannotChangeAnything(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theStockTabStocksTheMachine(GameTestHelper helper) {
        ShopTests.theStockTabStocksTheMachine(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCashBoxIsTakeOnly(GameTestHelper helper) {
        ShopTests.theCashBoxIsTakeOnly(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void withdrawEmptiesTheCashBoxIntoTheOwnersInventory(GameTestHelper helper) {
        ShopTests.withdrawEmptiesTheCashBoxIntoTheOwnersInventory(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void onlyAdminsSeeAndUseTheAdminTab(GameTestHelper helper) {
        ShopTests.onlyAdminsSeeAndUseTheAdminTab(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void infiniteNeedsAnEmptyStockAndCashBox(GameTestHelper helper) {
        ShopTests.infiniteNeedsAnEmptyStockAndCashBox(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCatalogPickerCyclesThroughEveryCatalog(GameTestHelper helper) {
        ShopTests.theCatalogPickerCyclesThroughEveryCatalog(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void catalogSelectionsAreReadOnly(GameTestHelper helper) {
        ShopTests.catalogSelectionsAreReadOnly(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void theCurrencySlotIsForAdmins(GameTestHelper helper) {
        ShopTests.theCurrencySlotIsForAdmins(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void sneakingWithEmptyHandsOpensSetup(GameTestHelper helper) {
        ShopTests.sneakingWithEmptyHandsOpensSetup(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void twoOpenScreensShareOneMachine(GameTestHelper helper) {
        ShopTests.twoOpenScreensShareOneMachine(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void breakingTheMachineClosesItsSetup(GameTestHelper helper) {
        ShopTests.breakingTheMachineClosesItsSetup(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void shiftClickOnlyStocksOnTheStockTab(GameTestHelper helper) {
        ShopTests.shiftClickOnlyStocksOnTheStockTab(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void sneakingWithAnItemShowsTheEmptyHandsHint(GameTestHelper helper) {
        ShopTests.sneakingWithAnItemShowsTheEmptyHandsHint(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void strangersSneakingWithBlocksPlaceThemAsUsual(GameTestHelper helper) {
        ShopTests.strangersSneakingWithBlocksPlaceThemAsUsual(helper);
    }
    *///?}
}
