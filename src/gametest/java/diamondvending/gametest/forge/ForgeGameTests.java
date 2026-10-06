package diamondvending.gametest.forge;

import diamondvending.DiamondVending;
import diamondvending.gametest.AllTests;
import diamondvending.gametest.MachineTests;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.List;
import java.util.Map;

/**
 * Every game test on Forge 1.20.1: one generator turns each {@link AllTests#ALL} entry into a test, so there are no
 * adapter methods to keep in step. Forge runs the tests whose structure is in an enabled namespace.
 */
@GameTestHolder(DiamondVending.MOD_ID)
public final class ForgeGameTests {
    private ForgeGameTests() {}

    @GameTestGenerator
    public static List<TestFunction> all() {
        return AllTests.ALL.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(test -> new TestFunction("defaultBatch", test.getKey(), MachineTests.STRUCTURE, MachineTests.MAX_TICKS, 0, true, test.getValue()))
                .toList();
    }
}
