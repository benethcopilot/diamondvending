package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if <26.1 {
/*import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * NeoForge 1.21.1 finds these through {@code @GameTestHolder}; templates resolve to {@code diamondvending:<name>}.
 * On 26.1 this class is empty and {@link NeoForgeGameTestMod} registers the tests instead.
 */
//? if <26.1 {
/*@GameTestHolder(DiamondVending.MOD_ID)
@PrefixGameTestTemplate(false)
*///?}
public final class NeoForgeGameTests {
    //? if <26.1 {
    /*@GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void platformIsReady(GameTestHelper helper) {
        MachineTests.platformIsReady(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placesAllFourParts(GameTestHelper helper) {
        MachineTests.placesAllFourParts(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placementNeedsRoom(GameTestHelper helper) {
        MachineTests.placementNeedsRoom(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void placesInTheItemsColor(GameTestHelper helper) {
        MachineTests.placesInTheItemsColor(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void worksInEveryFacing(GameTestHelper helper) {
        MachineTests.worksInEveryFacing(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void breakingAnyPartRemovesTheMachineAndDropsIt(GameTestHelper helper) {
        MachineTests.breakingAnyPartRemovesTheMachineAndDropsIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creativeBreakingDropsNothing(GameTestHelper helper) {
        MachineTests.creativeBreakingDropsNothing(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void onlyOwnersAndAdminsCanMineIt(GameTestHelper helper) {
        MachineTests.onlyOwnersAndAdminsCanMineIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ownerlessMachinesAreAdminOnly(GameTestHelper helper) {
        MachineTests.ownerlessMachinesAreAdminOnly(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void aBrokenMachineCleansItselfUp(GameTestHelper helper) {
        MachineTests.aBrokenMachineCleansItselfUp(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void neighbouringMachinesStaySeparate(GameTestHelper helper) {
        MachineTests.neighbouringMachinesStaySeparate(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void ownersCanDyeTheWholeMachine(GameTestHelper helper) {
        MachineTests.ownersCanDyeTheWholeMachine(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void strangersCannotDyeIt(GameTestHelper helper) {
        MachineTests.strangersCannotDyeIt(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void dyeingTheSameColorUsesNoDye(GameTestHelper helper) {
        MachineTests.dyeingTheSameColorUsesNoDye(helper);
    }

    @GameTest(template = MachineTests.STRUCTURE_NAME, timeoutTicks = MachineTests.MAX_TICKS)
    public static void creativeDyeingKeepsTheDye(GameTestHelper helper) {
        MachineTests.creativeDyeingKeepsTheDye(helper);
    }
    *///?}
}
