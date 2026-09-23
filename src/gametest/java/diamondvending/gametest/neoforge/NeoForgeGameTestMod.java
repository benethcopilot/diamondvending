package diamondvending.gametest.neoforge;

import diamondvending.DiamondVending;
import diamondvending.gametest.MachineTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
//? if >=26.1 {
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

/** The test-only mod. On 26.1 it registers each {@link MachineTests#ALL} entry as a test function and a test instance. */
@Mod("diamondvending_gametest")
public final class NeoForgeGameTestMod {
    public NeoForgeGameTestMod(IEventBus modBus) {
        //? if >=26.1 {
        DeferredRegister<Consumer<GameTestHelper>> functions = DeferredRegister.create(Registries.TEST_FUNCTION, DiamondVending.MOD_ID);
        MachineTests.ALL.forEach((name, test) -> functions.register(name, () -> test));
        functions.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, event -> {
            Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(DiamondVending.id("default"));
            MachineTests.ALL.keySet().forEach(name -> event.registerTest(DiamondVending.id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, DiamondVending.id(name)),
                    new TestData<>(environment, DiamondVending.id(MachineTests.STRUCTURE_NAME), MachineTests.MAX_TICKS, 0, true))));
        });
        //?}
    }
}
