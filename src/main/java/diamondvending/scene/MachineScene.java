package diamondvending.scene;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Display;
import diamondvending.core.DropAnimation;
import diamondvending.core.MachineLayout;
import diamondvending.core.Problem;
import diamondvending.core.Rect;
import diamondvending.core.Texts;
import diamondvending.shop.Selection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Everything drawn on a machine's front (spec §2.3), as flat instructions in canvas pixels — u left→right, v top→bottom
 * on the 32 × 32 front (spec §2.2). Built from what a client knows about the machine, so every loader and version draws
 * the same thing and the server can test it; the renderer only turns it into draw calls.
 */
public record MachineScene(List<Item> items, List<Text> texts, List<Glow> glows) {
    public static final MachineScene EMPTY = new MachineScene(List.of(), List.of(), List.of());

    public static final int LED_OK = 0xFF3CFF6A;
    public static final int LED_ALARM = 0xFFFF3B3B;
    public static final int TAG_TEXT = 0xFF1E1E1E;
    public static final int TAG_ALARM = 0xFFC00000;
    public static final int LABEL = 0xFFF4F4F4;
    public static final int LAMP = 0xFFFF2A2A;

    public static final float SHELF_ITEM = 3.2F;
    public static final float TRAY_ITEM = 1.8F;
    public static final float ICON = 1.1F;
    public static final float TAG_TEXT_HEIGHT = 0.9F;
    public static final float LED_TEXT_HEIGHT = 0.85F;

    public enum Align { LEFT, CENTER, RIGHT }

    /** An item model centred at (u, v), {@code size} pixels across. */
    public record Item(ItemStack stack, float u, float v, float size) {}

    /** One line of text, {@code height} pixels tall, whose middle is at v; u is its left edge, centre or right edge. ARGB colour. */
    public record Text(String text, float u, float v, float height, int color, Align align) {}

    /** A flat glowing rectangle: price labels and the warning lamp. ARGB colour. */
    public record Glow(float u0, float v0, float u1, float v1, int color) {}

    /**
     * The scene for one viewer at one moment.
     *
     * @param machine what the client knows about the machine (its synced block entity)
     * @param viewer  whose credit the display shows (spec §3.5 a)
     * @param time    game time, with the fraction of the current tick
     * @param words   turns a translation into words: {@code Component::getString} on the client
     */
    public static MachineScene of(VendingMachineBlockEntity machine, UUID viewer, float time, Function<Component, String> words) {
        List<Item> items = new ArrayList<>();
        List<Text> texts = new ArrayList<>();
        List<Glow> glows = new ArrayList<>();
        long ticks = (long) Math.floor(time);
        ItemStack coin = new ItemStack(machine.currency().displayItem());

        // Shelves: each set-up button's item, with a white label underneath: "7 · 3" and the currency's icon.
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            Selection selection = machine.getSelection(i);
            if (!selection.isSetUp()) continue;
            Rect slot = MachineLayout.shelfSlot(i);
            float u = (float) slot.centerU();
            float tagV = (float) slot.v1() + 0.8F;
            items.add(new Item(selection.template(), u, (float) slot.centerV(), SHELF_ITEM));
            glows.add(new Glow(u - 3.3F, tagV - 0.55F, u + 2.8F, tagV + 0.55F, LABEL));
            boolean soldOut = !machine.isInfinite() && machine.syncedStockCount(i) < selection.quantity();
            if (soldOut) {
                texts.add(new Text(words.apply(Component.translatable(Texts.TAG_SOLD_OUT)), u - 0.25F, tagV, TAG_TEXT_HEIGHT, TAG_ALARM, Align.CENTER));
            } else if (selection.price() == 0) {
                texts.add(new Text((i + 1) + " · " + words.apply(Component.translatable(Texts.TAG_FREE)), u - 0.25F, tagV, TAG_TEXT_HEIGHT, TAG_TEXT, Align.CENTER));
            } else {
                texts.add(new Text((i + 1) + " · " + selection.price(), u + 1.4F, tagV, TAG_TEXT_HEIGHT, TAG_TEXT, Align.RIGHT));
                items.add(new Item(coin, u + 2.1F, tagV, ICON));
            }
        }

        // The tray: what's waiting, side by side.
        for (int k = 0; k < machine.tray().size(); k++) {
            ItemStack stack = machine.tray().get(k);
            if (!stack.isEmpty()) items.add(new Item(stack, (float) MachineLayout.TRAY.u0() + 1 + k * 2, (float) MachineLayout.TRAY.centerV(), TRAY_ITEM));
        }

        // The item that was just bought, dropping from its shelf into the tray.
        int vended = machine.lastVendSelection();
        if (vended >= 0 && machine.getSelection(vended).isSetUp()) {
            double[] at = DropAnimation.position(vended, time - machine.lastVendTime());
            if (at != null) items.add(new Item(machine.getSelection(vended).template(), (float) at[0], (float) at[1], SHELF_ITEM));
        }

        // The LED display and the warning lamp (spec §3.5).
        List<Problem> problems = machine.syncedProblems();
        Display.Line line = Display.line(problems, machine.lastFlash(), machine.lastFlashNumber(), ticks - machine.lastFlashTime(),
                machine.syncedCredit(viewer));
        String full = line.keys().stream()
                .map(key -> words.apply(Component.translatable(key, line.number())))
                .collect(Collectors.joining(Display.GAP));
        boolean scrolls = full.length() > Display.WINDOW;
        Rect display = MachineLayout.DISPLAY;
        texts.add(new Text(Display.window(full, ticks),
                scrolls ? (float) display.u0() + 0.2F : (float) display.centerU(), (float) display.centerV(),
                LED_TEXT_HEIGHT, line.alarm() ? LED_ALARM : LED_OK, scrolls ? Align.LEFT : Align.CENTER));
        if (Display.lampLit(!problems.isEmpty(), ticks)) {
            Rect lamp = MachineLayout.LAMP;
            glows.add(new Glow((float) lamp.u0(), (float) lamp.v0(), (float) lamp.u1(), (float) lamp.v1(), LAMP));
        }
        return new MachineScene(List.copyOf(items), List.copyOf(texts), List.copyOf(glows));
    }
}
