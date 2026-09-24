package diamondvending.gametest.fabric;

//? if >=26.1 {
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.client.VendingSetupScreen;
import diamondvending.core.SetupTab;
import diamondvending.shop.Selection;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
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
            context.takeScreenshot("hover_button_1");
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

            // Crosshair on the tray (canvas 12, 28.25 → x 0.75, y -59.77 on the z = 1 face; eyes at -58.38, 2.5 away).
            server.runCommand("tp @a 0.75 -60 3.5 180 29.0");
            context.waitTicks(5);
            context.takeScreenshot("hover_tray");

            // The setup screen: an admin (creative; the test machine has no owner) sneak-right-clicks with empty hands.
            server.runCommand("clear @a");
            server.runCommand("gamemode creative @a");
            server.runCommand("tp @a 1 -60 3.5 180 10");
            context.waitTicks(5);
            context.getInput().holdKey(options -> options.keyShift);
            context.waitTicks(2); // the server learns the player is sneaking on the next tick
            context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            context.waitForScreen(VendingSetupScreen.class);
            context.getInput().releaseKey(options -> options.keyShift);
            context.runOnClient(client -> client.getToastManager().clear()); // the first diamonds' advancement toasts
            context.waitTicks(5);
            context.takeScreenshot("setup_items");
            // A real button press, through the network: button 1 sold 2 apples, "+" makes it 3.
            context.clickScreenButton("+");
            context.waitTicks(5);
            int apples = server.computeOnServer(s -> buttonOne(s).quantity());
            if (apples != 3) throw new AssertionError("\"+\" should make button 1 sell 3 apples, it sells " + apples);
            // Typing into the price box changes nothing until Enter: a half-typed price must never be on sale.
            context.runOnClient(client -> client.screen.setFocused(
                    client.screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow()));
            context.getInput().typeChars("45");
            context.waitTicks(5);
            int typing = server.computeOnServer(s -> buttonOne(s).price());
            if (typing != 3) throw new AssertionError("a half-typed price went on sale: button 1 costs " + typing);
            context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
            context.waitTicks(5);
            int entered = server.computeOnServer(s -> buttonOne(s).price());
            if (entered != 345) throw new AssertionError("Enter should make button 1 cost 345, it costs " + entered);
            for (SetupTab tab : new SetupTab[] {SetupTab.STOCK, SetupTab.CASH_BOX, SetupTab.ADMIN}) {
                context.runOnClient(client -> ((VendingSetupScreen) client.screen).showTab(tab));
                context.waitTicks(3);
                context.takeScreenshot("setup_" + tab.name().toLowerCase(Locale.ROOT));
            }
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            context.waitTicks(3);
            server.runCommand("gamemode survival @a");

            // Behind the machine: nothing may float in the air or show through.
            server.runCommand("tp @a 1 -60 -3 0 10");
            context.waitTicks(10);
            context.takeScreenshot("machine_back");

            // Up against the front, looking up at the display: the lower-left block is out of view, so a renderer culled
            // by that block's box alone (NeoForge's default) would hide the whole front. The world is left saved here.
            server.runCommand("tp @a 1.6 -60 1.3 180 -21");
            context.waitTicks(10);
            context.takeScreenshot("close_up");
        }
    }

    /** What button 1 of the test machine sells, on the server. */
    private static Selection buttonOne(MinecraftServer server) {
        return ((VendingMachineBlockEntity) server.overworld().getBlockEntity(new BlockPos(0, -60, 0))).getSelection(0);
    }
}
//?} else {
/*public final class FabricClientTests {
}
*///?}
