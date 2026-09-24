package diamondvending.gametest.fabric;

//? if >=26.1 {
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.lwjgl.glfw.GLFW;
//?}

/**
 * Client game test, Fabric 26.1 only (older Fabric API has no client tests). Builds a stocked machine in a real game
 * window and saves screenshots for a person to check: {@code ./gradlew :26.1-fabric:runClientGametest}, then look in
 * versions/26.1-fabric/build/clientgametest/screenshots/.
 */
//? if >=26.1 {
public final class FabricClientTests implements FabricClientGameTest {
    // A south-facing machine: master (lower-left) at 0 -60 0, right column at x = 1, front face at z = 1.
    private static final String MACHINE = "diamondvending:vending_machine[facing=south,";
    private static final String CONTENTS = "{owner_name:\"Tester\","
            + "selections:[{slot:0,item:{id:\"minecraft:apple\",count:2},price:3},"
            + "{slot:1,item:{id:\"minecraft:oak_log\",count:4},price:1},"
            + "{slot:2,item:{id:\"minecraft:diamond_sword\",count:1},price:12},"
            + "{slot:4,item:{id:\"minecraft:bread\",count:1},price:0},"
            + "{slot:7,item:{id:\"minecraft:enchanted_golden_apple\",count:1},price:64},"
            + "{slot:11,item:{id:\"minecraft:cake\",count:1},price:5}],"
            + "stock:{Items:[{Slot:0b,id:\"minecraft:apple\",count:64},{Slot:1b,id:\"minecraft:oak_log\",count:64},"
            + "{Slot:2b,id:\"minecraft:diamond_sword\",count:1},{Slot:3b,id:\"minecraft:bread\",count:64},"
            + "{Slot:4b,id:\"minecraft:enchanted_golden_apple\",count:3}]},"
            + "tray:{Items:[{Slot:0b,id:\"minecraft:cookie\",count:3}]}}";
    private static final String FULL_TRAY = "{tray:{Items:["
            + "{Slot:0b,id:\"minecraft:cobblestone\",count:64},{Slot:1b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:2b,id:\"minecraft:cobblestone\",count:64},{Slot:3b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:4b,id:\"minecraft:cobblestone\",count:64},{Slot:5b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:6b,id:\"minecraft:cobblestone\",count:64},{Slot:7b,id:\"minecraft:cobblestone\",count:64},"
            + "{Slot:8b,id:\"minecraft:cobblestone\",count:64}]}}";

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            TestServerContext server = world.getServer();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("tp @a 1 -60 3.5 180 10"); // first, so the chunks around 0 0 are loaded
            context.waitTicks(20);
            server.runCommand("fill -4 -61 -4 5 -61 6 minecraft:smooth_stone");
            server.runCommand("fill -4 -60 -4 5 -54 6 minecraft:air");
            server.runCommand("setblock 0 -60 0 " + MACHINE + "half=lower,side=left]" + CONTENTS);
            server.runCommand("setblock 1 -60 0 " + MACHINE + "half=lower,side=right]");
            server.runCommand("setblock 0 -59 0 " + MACHINE + "half=upper,side=left]");
            server.runCommand("setblock 1 -59 0 " + MACHINE + "half=upper,side=right]");

            // In front, looking at the whole machine.
            world.getClientLevel().waitForChunksRender();
            context.waitTicks(20);
            context.takeScreenshot("machine_front");

            // Crosshair on button 1 (canvas 26, 8 → x 1.625, y -58.5 on the z = 1 face); eyes are 1.62 above the feet.
            server.runCommand("tp @a 1.625 -60 3.5 180 2.75");
            context.waitTicks(5);
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT); // no diamonds yet
            context.waitTicks(5);
            context.takeScreenshot("need_money");

            server.runCommand("give @a minecraft:diamond 10");
            context.waitTicks(20); // longer than the held-click window
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitTicks(4);
            context.takeScreenshot("item_falling");
            context.waitTicks(10);
            context.takeScreenshot("thank_you");

            server.runCommand("data merge block 0 -60 0 " + FULL_TRAY);
            server.runCommand("tp @a 1 -60 3.5 180 10");
            context.waitTicks(45); // past the THANK YOU flash
            context.takeScreenshot("problem_1");
            context.waitTicks(10);
            context.takeScreenshot("problem_2");

            // Behind the machine: nothing may float in the air or show through.
            server.runCommand("tp @a 1 -60 -3 0 10");
            context.waitTicks(10);
            context.takeScreenshot("machine_back");
        }
    }
}
//?} else {
/*public final class FabricClientTests {
}
*///?}
