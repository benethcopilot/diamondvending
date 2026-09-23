package diamondvending.gametest.fabric;

import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestHelper;
//? if >=26.1 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.minecraft.gametest.framework.GameTest;
*///?}

/**
 * Fabric's game-test entrypoint (declared in the test mod's fabric.mod.json). One public, non-static method per
 * test in {@link MachineTests}. 26.1 uses Fabric's {@code @GameTest(structure)}; 1.21.1 uses vanilla's {@code @GameTest(template)}.
 */
public final class FabricGameTests {
    //? if >=26.1 {
    @GameTest(structure = MachineTests.STRUCTURE, maxTicks = MachineTests.MAX_TICKS)
    //?} else {
    /*@GameTest(template = MachineTests.STRUCTURE, timeoutTicks = MachineTests.MAX_TICKS)
    *///?}
    public void platformIsReady(GameTestHelper helper) {
        MachineTests.platformIsReady(helper);
    }
}
