package diamondvending.gametest;

import diamondvending.DiamondVending;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.function.Consumer;

/**
 * The in-game tests, shared by every loader and version. Each test also needs a method in
 * {@code fabric/FabricGameTests} and (for 1.21.1) {@code neoforge/NeoForgeGameTests}; NeoForge 26.1 registers {@link #ALL}.
 */
public final class MachineTests {
    public static final String STRUCTURE_NAME = "gametest_platform";
    public static final String STRUCTURE = DiamondVending.MOD_ID + ":" + STRUCTURE_NAME;
    public static final int MAX_TICKS = 100;

    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("platform_is_ready", MachineTests::platformIsReady));

    private MachineTests() {}

    /**
     * Converts platform coordinates (floor at y = 0) to test-relative ones. 1.21.1 measures from the test's
     * structure block, which sits one block below the structure; 26.1 measures from the structure itself.
     */
    static BlockPos platform(int x, int y, int z) {
        //? if >=26.1 {
        return new BlockPos(x, y, z);
        //?} else {
        /*return new BlockPos(x, y + 1, z);
        *///?}
    }

    /** Smoke test: the generated platform loaded — floor at y = 0, air above. */
    public static void platformIsReady(GameTestHelper helper) {
        helper.assertBlockPresent(Blocks.POLISHED_ANDESITE, platform(0, 0, 0));
        helper.assertBlockPresent(Blocks.AIR, platform(3, 1, 3));
        helper.succeed();
    }
}
