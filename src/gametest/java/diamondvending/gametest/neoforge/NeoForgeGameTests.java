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
    *///?}
}
