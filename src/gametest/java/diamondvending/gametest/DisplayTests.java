package diamondvending.gametest;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Display;
import diamondvending.core.DropAnimation;
import diamondvending.core.Flash;
import diamondvending.core.MachineLayout;
import diamondvending.core.Rect;
import diamondvending.core.Texts;
import diamondvending.scene.MachineScene;
import diamondvending.shop.Selection;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * In-game tests for what machines show (Plan 4). Each test also needs a method in {@code fabric/FabricDisplayTests}
 * and (for 1.21.1) {@code neoforge/NeoForgeDisplayTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 */
public final class DisplayTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("a_purchase_says_thank_you_and_drops_the_item", DisplayTests::aPurchaseSaysThankYouAndDropsTheItem),
            Map.entry("a_refused_purchase_flashes_the_reason", DisplayTests::aRefusedPurchaseFlashesTheReason),
            Map.entry("the_coin_slot_flashes_wrong_coin", DisplayTests::theCoinSlotFlashesWrongCoin),
            Map.entry("shelves_show_what_each_button_sells", DisplayTests::shelvesShowWhatEachButtonSells),
            Map.entry("the_tray_shows_whats_waiting", DisplayTests::theTrayShowsWhatsWaiting),
            Map.entry("the_display_says_select_item_then_your_credit", DisplayTests::theDisplaySaysSelectItemThenYourCredit),
            Map.entry("problems_turn_the_display_red_and_light_the_lamp", DisplayTests::problemsTurnTheDisplayRedAndLightTheLamp),
            Map.entry("flashes_reach_clients_whole", DisplayTests::flashesReachClientsWhole),
            Map.entry("a_flash_shows_for_two_seconds", DisplayTests::aFlashShowsForTwoSeconds),
            Map.entry("a_bought_item_falls_into_the_tray", DisplayTests::aBoughtItemFallsIntoTheTray));

    private DisplayTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** What a client knows about the machine: its update tag, loaded into a fresh block entity (which has no level). */
    static VendingMachineBlockEntity clientView(GameTestHelper helper, VendingMachineBlockEntity machine) {
        CompoundTag update = machine.getUpdateTag(BuyingTests.registries(helper));
        update.putString("id", "diamondvending:vending_machine");
        return BuyingTests.reload(helper, machine, update);
    }

    /** Words for scenes in tests: a translation's key and arguments, so tests don't depend on a language being loaded. */
    static final Function<Component, String> KEYS = component -> component.getContents() instanceof TranslatableContents t
            ? t.getKey() + Arrays.toString(t.getArgs()) : component.getString();

    /**
     * Words for the display in tests: the mod's own English, read from its en_us.json. The display shows only 7
     * characters at a time, so display checks need the real, short words — keys would all start "display".
     */
    static final Function<Component, String> ENGLISH = englishWords();

    private static Function<Component, String> englishWords() {
        Map<String, String> english = new HashMap<>();
        try (InputStream in = VendingMachineBlockEntity.class.getResourceAsStream("/assets/diamondvending/lang/en_us.json")) {
            JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                    .entrySet().forEach(entry -> english.put(entry.getKey(), entry.getValue().getAsString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return component -> component.getContents() instanceof TranslatableContents t
                ? String.format(english.getOrDefault(t.getKey(), t.getKey()), t.getArgs()) : component.getString();
    }

    static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.01;
    }

    static void assertItem(GameTestHelper helper, MachineScene scene, Item item, double u, double v) {
        helper.assertTrue(scene.items().stream().anyMatch(i -> i.stack().is(item) && near(i.u(), u) && near(i.v(), v)),
                "expected " + item + " at (" + u + ", " + v + ") in " + scene.items());
    }

    static int count(MachineScene scene, Item item) {
        return (int) scene.items().stream().filter(i -> i.stack().is(item)).count();
    }

    /** The LED display's text. */
    static MachineScene.Text led(GameTestHelper helper, MachineScene scene) {
        return scene.texts().stream().filter(t -> near(t.v(), MachineLayout.DISPLAY.centerV())).findFirst()
                .orElseThrow(() -> new AssertionError("the scene has no display text: " + scene.texts()));
    }

    /** Checks the display of a scene built with {@link #ENGLISH} at {@code ticks}. */
    static void assertLed(GameTestHelper helper, MachineScene scene, String says, long ticks, int color) {
        MachineScene.Text led = led(helper, scene);
        String expected = Display.window(says, ticks);
        helper.assertTrue(led.text().equals(expected) && led.color() == color,
                "the display should show \"" + expected + "\" in " + Integer.toHexString(color) + ", but shows \"" + led.text()
                        + "\" in " + Integer.toHexString(led.color()));
    }

    static boolean lampLit(MachineScene scene) {
        return scene.glows().stream().anyMatch(g -> g.color() == MachineScene.LAMP);
    }

    // ---- block events --------------------------------------------------------------------------------------------

    public static void aPurchaseSaysThankYouAndDropsTheItem(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.pressButton(helper, BuyingTests.buyerWith(helper, 5), 0);
        helper.succeedWhen(() -> {
            helper.assertTrue(machine.lastFlash() == Flash.THANK_YOU, "the display should say thank you, not " + machine.lastFlash());
            helper.assertTrue(machine.lastVendSelection() == 0, "button 1's item should drop, not " + machine.lastVendSelection());
        });
    }

    public static void aRefusedPurchaseFlashesTheReason(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.pressButton(helper, BuyingTests.buyerWith(helper, 1), 0);
        helper.succeedWhen(() -> helper.assertTrue(machine.lastFlash() == Flash.NEED_MONEY && machine.lastFlashNumber() == 3,
                "the display should flash NEED 3, not " + machine.lastFlash() + " " + machine.lastFlashNumber()));
    }

    public static void theCoinSlotFlashesWrongCoin(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.placeMachine(helper, new RecordingPlayer(helper, GameType.SURVIVAL));
        RecordingPlayer buyer = new RecordingPlayer(helper, GameType.SURVIVAL);
        buyer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 3));
        BuyingTests.click(helper, buyer, MachineLayout.COIN_SLOT);
        helper.succeedWhen(() -> helper.assertTrue(machine.lastFlash() == Flash.WRONG_COIN,
                "the display should flash WRONG COIN, not " + machine.lastFlash()));
    }

    // ---- scene ---------------------------------------------------------------------------------------------------

    public static void shelvesShowWhatEachButtonSells(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // button 1: 2 apples for 3, 10 in stock
        machine.setSelection(4, Selection.of(new ItemStack(Items.BREAD), 0));
        machine.stock().set(1, new ItemStack(Items.BREAD, 5));
        machine.setSelection(11, Selection.of(new ItemStack(Items.CAKE), 5)); // none in stock
        MachineScene scene = MachineScene.of(clientView(helper, machine), UUID.randomUUID(), 0, KEYS);
        for (int button : new int[] {0, 4, 11}) {
            Rect slot = MachineLayout.shelfSlot(button);
            Item item = button == 0 ? Items.APPLE : button == 4 ? Items.BREAD : Items.CAKE;
            assertItem(helper, scene, item, slot.centerU(), slot.centerV());
        }
        helper.assertTrue(scene.texts().stream().anyMatch(t -> t.text().equals("1 · 3")), "button 1's tag should read 1 · 3");
        helper.assertTrue(count(scene, Items.DIAMOND) == 1, "the priced tag shows the currency's icon");
        String free = "5 · " + KEYS.apply(Component.translatable(Texts.TAG_FREE));
        helper.assertTrue(scene.texts().stream().anyMatch(t -> t.text().equals(free)), "a price of 0 reads FREE");
        String soldOut = KEYS.apply(Component.translatable(Texts.TAG_SOLD_OUT));
        helper.assertTrue(scene.texts().stream().anyMatch(t -> t.text().equals(soldOut) && t.color() == MachineScene.TAG_ALARM),
                "button 12, with nothing in stock, is marked SOLD OUT in red");
        helper.assertTrue(scene.glows().stream().filter(g -> g.color() == MachineScene.LABEL).count() == 3, "each set-up button has a white label");
        helper.succeed();
    }

    public static void theTrayShowsWhatsWaiting(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        machine.tray().set(0, new ItemStack(Items.BREAD, 3));
        machine.tray().set(4, new ItemStack(Items.COOKIE, 1));
        MachineScene scene = MachineScene.of(clientView(helper, machine), UUID.randomUUID(), 0, KEYS);
        Rect tray = MachineLayout.TRAY;
        assertItem(helper, scene, Items.BREAD, tray.u0() + 1, tray.centerV());
        assertItem(helper, scene, Items.COOKIE, tray.u0() + 1 + 4 * 2, tray.centerV());
        helper.succeed();
    }

    /** Spec §3.5 a: each player's display shows their own credit. */
    public static void theDisplaySaysSelectItemThenYourCredit(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        UUID buyer = UUID.randomUUID();
        machine.creditOf(buyer).set(0, new ItemStack(Items.DIAMOND, 4));
        VendingMachineBlockEntity client = clientView(helper, machine);
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), 0, ENGLISH), "SELECT ITEM", 0, MachineScene.LED_OK);
        assertLed(helper, MachineScene.of(client, buyer, 0, ENGLISH), "CREDIT 4", 0, MachineScene.LED_OK);
        helper.succeed();
    }

    public static void problemsTurnTheDisplayRedAndLightTheLamp(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        BuyingTests.fill(machine.tray(), Items.COBBLESTONE);
        VendingMachineBlockEntity client = clientView(helper, machine);
        MachineScene lit = MachineScene.of(client, UUID.randomUUID(), 0, ENGLISH);
        assertLed(helper, lit, "TRAY FULL - TAKE YOUR ITEMS", 0, MachineScene.LED_ALARM);
        helper.assertTrue(lampLit(lit), "the lamp should be lit at the start of a blink");
        helper.assertFalse(lampLit(MachineScene.of(client, UUID.randomUUID(), Display.BLINK_TICKS, KEYS)), "and dark half a second later");
        helper.succeed();
    }

    /** Block events cross the network as single bytes; the client test once showed NEED 0 for a 3-diamond button. */
    public static void flashesReachClientsWhole(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper); // button 1 costs 3
        VendingMachineBlockEntity client = clientView(helper, machine);
        for (Flash flash : Flash.values()) {
            overTheNetwork(helper, client, machine.flashEvent(flash, 0));
            helper.assertTrue(client.lastFlash() == flash, "a client should get " + flash + ", not " + client.lastFlash());
        }
        overTheNetwork(helper, client, machine.flashEvent(Flash.NEED_MONEY, 0)); // at game time 0
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), 0, ENGLISH), "NEED 3", 0, MachineScene.LED_ALARM);
        overTheNetwork(helper, client, machine.vendEvent(MachineLayout.SELECTIONS - 1));
        helper.assertTrue(client.lastVendSelection() == MachineLayout.SELECTIONS - 1, "a client should see button 12's item drop");
        helper.succeed();
    }

    /** Delivers a block event to {@code client} the way the server does: in a packet, as bytes. */
    static void overTheNetwork(GameTestHelper helper, VendingMachineBlockEntity client, BlockEventData event) {
        RegistryFriendlyByteBuf bytes = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ClientboundBlockEventPacket.STREAM_CODEC.encode(bytes, new ClientboundBlockEventPacket(event.pos(), event.block(), event.paramA(), event.paramB()));
        ClientboundBlockEventPacket received = ClientboundBlockEventPacket.STREAM_CODEC.decode(bytes);
        client.triggerEvent(received.getB0(), received.getB1());
    }

    public static void aFlashShowsForTwoSeconds(GameTestHelper helper) {
        VendingMachineBlockEntity machine = BuyingTests.appleMachine(helper);
        VendingMachineBlockEntity client = clientView(helper, machine);
        overTheNetwork(helper, client, machine.flashEvent(Flash.NEED_MONEY, 0)); // at game time 0
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), 5, ENGLISH), "NEED 3", 5, MachineScene.LED_ALARM);
        long later = Display.FLASH_TICKS + 5;
        assertLed(helper, MachineScene.of(client, UUID.randomUUID(), later, ENGLISH), "SELECT ITEM", later, MachineScene.LED_OK);
        helper.succeed();
    }

    public static void aBoughtItemFallsIntoTheTray(GameTestHelper helper) {
        VendingMachineBlockEntity client = clientView(helper, BuyingTests.appleMachine(helper));
        client.triggerEvent(VendingMachineBlockEntity.EVENT_VEND, 0); // button 1, at game time 0
        MachineScene falling = MachineScene.of(client, UUID.randomUUID(), 5, KEYS);
        double[] at = DropAnimation.position(0, 5);
        helper.assertTrue(count(falling, Items.APPLE) == 2, "the shelf apple plus one falling");
        assertItem(helper, falling, Items.APPLE, at[0], at[1]);
        helper.assertTrue(count(MachineScene.of(client, UUID.randomUUID(), DropAnimation.TICKS, KEYS), Items.APPLE) == 1,
                "once it lands only the shelf apple is left");
        helper.succeed();
    }
}
