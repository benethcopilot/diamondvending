package diamondvending.client;

import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Problem;
import diamondvending.core.SetupButtons;
import diamondvending.core.SetupTab;
import diamondvending.core.Texts;
import diamondvending.menu.VendingSetupMenu;
import diamondvending.shop.Selection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The setup screen (spec §4): the panel, a red banner with the machine's problems, tab buttons (a red "!" on the tab
 * that fixes a problem) and each tab's controls around {@link VendingSetupMenu}'s slots. Every control only asks — it
 * sends a menu button number ({@link SetupButtons}) and the server decides.
 */
public final class VendingSetupScreen extends AbstractContainerScreen<VendingSetupMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int EDGE = 0xFF555555;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int TEXT = 0xFF404040;
    private static final int GOOD = 0xFF2A7A2A;
    private static final int BANNER = 0xFFAA2222;
    private static final int BANNER_TEXT = 0xFFFFFFFF;
    private static final int WARNING = 0xFFAA2222;
    private static final int EDITING = 0xFFFFD700;
    private static final int BANNER_Y = 23;
    private static final int BANNER_LINES = 3;
    private static final int EDITOR_X = 72;

    private final Map<SetupTab, Button> tabs = new EnumMap<>(SetupTab.class);
    private Button fewer;
    private Button more;
    private Button clear;
    private Button withdraw;
    private Button infinite;
    private Button previousCatalog;
    private Button nextCatalog;
    private EditBox price;
    private boolean settingPrice;
    // The price box shows button priceFor's price as read from the machine (priceRead). Typed digits (priceEdited) stay
    // in the box until Enter, another control, another button or closing the screen sends them: a half-typed price
    // (1, then 15, on the way to 150) must never go on sale.
    private int priceFor = -1;
    private int priceRead = -1;
    private boolean priceEdited;

    public VendingSetupScreen(VendingSetupMenu menu, Inventory inventory, Component title) {
        //? if >=26.1 {
        super(menu, inventory, title, VendingSetupMenu.WIDTH, VendingSetupMenu.HEIGHT);
        //?} else {
        /*super(menu, inventory, title);
        imageWidth = VendingSetupMenu.WIDTH;
        imageHeight = VendingSetupMenu.HEIGHT;
        *///?}
        inventoryLabelX = VendingSetupMenu.INVENTORY_X;
        inventoryLabelY = VendingSetupMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        for (SetupTab tab : SetupTab.values()) {
            tabs.put(tab, addRenderableWidget(Button.builder(Component.empty(), button -> showTab(tab))
                    .bounds(x + 4 + tab.ordinal() * 50, y + 4, 48, 16).build()));
        }
        fewer = addRenderableWidget(Button.builder(Component.literal("-"), button -> press(SetupButtons.fewer(menu.selected())))
                .bounds(x + EDITOR_X + 44, y + 70, 16, 14).build());
        more = addRenderableWidget(Button.builder(Component.literal("+"), button -> press(SetupButtons.more(menu.selected())))
                .bounds(x + EDITOR_X + 80, y + 70, 16, 14).build());
        price = addRenderableWidget(new DigitBox(font, x + EDITOR_X + 44, y + 89, 36, 14, Component.translatable(Texts.SETUP_PRICE)));
        price.setMaxLength(3);
        price.setResponder(this::priceTyped);
        clear = addRenderableWidget(Button.builder(Component.translatable(Texts.SETUP_CLEAR), button -> press(SetupButtons.clear(menu.selected())))
                .bounds(x + EDITOR_X, y + 107, 56, 14).build());
        withdraw = addRenderableWidget(Button.builder(Component.translatable(Texts.SETUP_WITHDRAW), button -> press(SetupButtons.withdraw()))
                .bounds(x + 120, y + 55, 80, 14).build());
        infinite = addRenderableWidget(Button.builder(Component.empty(), button -> press(SetupButtons.toggleInfinite()))
                .bounds(x + 8, y + 56, 96, 16).build());
        previousCatalog = addRenderableWidget(Button.builder(Component.literal("<"), button -> press(SetupButtons.cycleCatalog(-1)))
                .bounds(x + 112, y + 68, 14, 14).build());
        nextCatalog = addRenderableWidget(Button.builder(Component.literal(">"), button -> press(SetupButtons.cycleCatalog(1)))
                .bounds(x + 190, y + 68, 14, 14).build());
        priceFor = -1; // a fresh (e.g. resized) price box starts from the machine's price
        priceEdited = false;
        refresh();
    }

    /** Shows a tab, as its tab button does. */
    public void showTab(SetupTab tab) {
        press(SetupButtons.showTab(tab));
        refresh();
    }

    /** Sends a typed price first, then this control's press. */
    private void press(int id) {
        commitPrice();
        send(id);
    }

    /** Asks the menu, then the server (the client's menu only switches tabs itself). */
    private void send(int id) {
        if (menu.clickMenuButton(minecraft.player, id)) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void priceTyped(String text) {
        if (!settingPrice) priceEdited = true;
    }

    /** Sends the typed price for the button it was typed for. An empty box sends nothing and shows the price again. */
    private void commitPrice() {
        if (!priceEdited) return;
        priceEdited = false;
        String text = price.getValue();
        if (text.isEmpty() || priceFor < 0) {
            priceRead = -1;
            return;
        }
        send(SetupButtons.price(priceFor, Math.min(Integer.parseInt(text), SetupButtons.MAX_PRICE)));
    }

    /** Closing (Escape or the inventory key) keeps a typed price. */
    @Override
    public void onClose() {
        commitPrice();
        super.onClose();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refresh();
    }

    /** Shows, hides and labels the controls for the current tab and the machine as it is now. */
    private void refresh() {
        VendingMachineBlockEntity machine = menu.machine();
        if (!menu.tab().shownTo(menu.viewerIsAdmin(), machine.isInfinite())) press(SetupButtons.showTab(SetupTab.SELECTIONS));
        SetupTab current = menu.tab();
        Set<SetupTab> attention = SetupTab.needingAttention(machine.syncedProblems());
        for (SetupTab tab : SetupTab.values()) {
            Button button = tabs.get(tab);
            button.visible = tab.shownTo(menu.viewerIsAdmin(), machine.isInfinite());
            button.active = tab != current;
            button.setMessage(tabLabel(tab, attention.contains(tab)));
        }
        Selection selection = machine.getSelection(menu.selected());
        boolean editing = current == SetupTab.SELECTIONS && !machine.usesCatalog() && selection.isSetUp();
        fewer.visible = more.visible = price.visible = clear.visible = editing;
        fewer.active = selection.quantity() > 1;
        more.active = selection.quantity() < selection.template().getMaxStackSize();
        if (priceEdited && (!editing || !price.isFocused() || priceFor != menu.selected())) commitPrice();
        if (!editing) {
            price.setFocused(false);
        } else if (!priceEdited && (priceFor != menu.selected() || selection.price() != priceRead)) {
            settingPrice = true;
            price.setValue(Integer.toString(selection.price()));
            settingPrice = false;
            priceFor = menu.selected();
            priceRead = selection.price();
        }
        withdraw.visible = current == SetupTab.CASH_BOX;
        withdraw.active = !menu.cashBoxEmpty();
        infinite.visible = previousCatalog.visible = nextCatalog.visible = current == SetupTab.ADMIN;
        infinite.setMessage(Component.translatable(machine.isInfinite() ? Texts.SETUP_INFINITE_ON : Texts.SETUP_INFINITE_OFF));
        infinite.active = machine.isInfinite() || menu.stockAndCashEmpty();
    }

    private static Component tabLabel(SetupTab tab, boolean attention) {
        MutableComponent label = Component.translatable(switch (tab) {
            case SELECTIONS -> Texts.SETUP_TAB_SELECTIONS;
            case STOCK -> Texts.SETUP_TAB_STOCK;
            case CASH_BOX -> Texts.SETUP_TAB_CASH_BOX;
            case ADMIN -> Texts.SETUP_TAB_ADMIN;
        });
        return attention ? label.append(Component.literal(" !").withStyle(ChatFormatting.RED)) : label;
    }

    // ---- drawing (absolute coordinates in drawBackground, panel-relative in drawLabels) ---------------------------

    private void drawBackground(Gui gui) {
        int x = leftPos;
        int y = topPos;
        gui.fill(x, y, x + imageWidth, y + imageHeight, EDGE);
        gui.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        if (!menu.machine().syncedProblems().isEmpty()) {
            gui.fill(x + 4, y + BANNER_Y, x + imageWidth - 4, y + BANNER_Y + BANNER_LINES * 10 + 2, BANNER);
        }
        for (Slot slot : menu.slots) {
            if (slot.isActive()) drawSlot(gui, x + slot.x, y + slot.y);
        }
        if (menu.tab() == SetupTab.SELECTIONS) {
            Slot editing = menu.getSlot(VendingSetupMenu.FIRST_GHOST + menu.selected());
            outline(gui, x + editing.x - 2, y + editing.y - 2, 20, EDITING);
        }
    }

    private static void drawSlot(Gui gui, int x, int y) {
        gui.fill(x - 1, y - 1, x + 17, y + 17, SLOT_DARK);
        gui.fill(x, y, x + 17, y + 17, SLOT_LIGHT);
        gui.fill(x, y, x + 16, y + 16, SLOT_FILL);
    }

    private static void outline(Gui gui, int x, int y, int size, int color) {
        gui.fill(x, y, x + size, y + 1, color);
        gui.fill(x, y + size - 1, x + size, y + size, color);
        gui.fill(x, y, x + 1, y + size, color);
        gui.fill(x + size - 1, y, x + size, y + size, color);
    }

    private void drawLabels(Gui gui) {
        VendingMachineBlockEntity machine = menu.machine();
        List<Problem> problems = machine.syncedProblems();
        if (problems.isEmpty()) {
            gui.text(font, title, 8, BANNER_Y + 4, TEXT);
            gui.text(font, Component.translatable(Texts.SETUP_ALL_GOOD), 8, BANNER_Y + 16, GOOD);
        } else {
            // Spec §4: the red banner lists every problem, in the words the display scrolls (each names who fixes it).
            for (int i = 0; i < Math.min(BANNER_LINES, problems.size()); i++) {
                gui.text(font, Component.translatable(Texts.problemDisplay(problems.get(i))), 8, BANNER_Y + 3 + i * 10, BANNER_TEXT);
            }
        }
        gui.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT);
        switch (menu.tab()) {
            case SELECTIONS -> drawEditor(gui, machine);
            case STOCK, CASH_BOX -> gui.text(font, tabLabel(menu.tab(), false), VendingSetupMenu.GRID_X, VendingSetupMenu.GRID_Y - 12, TEXT);
            case ADMIN -> drawAdmin(gui, machine);
        }
    }

    private void drawEditor(Gui gui, VendingMachineBlockEntity machine) {
        int index = menu.selected();
        Selection selection = machine.getSelection(index);
        Component heading = selection.isSetUp()
                ? Component.translatable(Texts.SETUP_BUTTON_ITEM, index + 1, selection.template().getHoverName())
                : Component.translatable(Texts.SETUP_BUTTON, index + 1);
        gui.text(font, firstLine(heading, VendingSetupMenu.WIDTH - EDITOR_X - 4), EDITOR_X, VendingSetupMenu.GHOSTS_Y + 1, TEXT);
        if (machine.usesCatalog()) {
            wrap(gui, Component.translatable(Texts.SETUP_FROM_CATALOG, machine.catalogLabel()), EDITOR_X, VendingSetupMenu.GHOSTS_Y + 16, TEXT);
        } else if (!selection.isSetUp()) {
            wrap(gui, Component.translatable(Texts.SETUP_PICK_ITEM), EDITOR_X, VendingSetupMenu.GHOSTS_Y + 16, TEXT);
        } else {
            gui.text(font, Component.translatable(Texts.SETUP_QUANTITY), EDITOR_X, 73, TEXT);
            String count = Integer.toString(selection.quantity());
            gui.text(font, Component.literal(count), EDITOR_X + 70 - font.width(count) / 2, 73, TEXT);
            gui.text(font, Component.translatable(Texts.SETUP_PRICE), EDITOR_X, 92, TEXT);
            gui.item(new ItemStack(machine.currency().displayItem()), EDITOR_X + 84, 88);
        }
    }

    private void drawAdmin(Gui gui, VendingMachineBlockEntity machine) {
        boolean blocked = !machine.isInfinite() && !menu.stockAndCashEmpty();
        wrap(gui, Component.translatable(blocked ? Texts.SETUP_EMPTY_FIRST : Texts.SETUP_INFINITE_EXPLAINED), 8, 76, 96, blocked ? WARNING : TEXT);
        gui.text(font, Component.translatable(Texts.SETUP_CATALOG), 112, 57, TEXT);
        Component catalog = machine.usesCatalog() ? Component.literal(machine.catalogLabel()) : Component.translatable(Texts.SETUP_NO_CATALOG);
        FormattedCharSequence name = firstLine(catalog, 58);
        gui.text(font, name, 158 - font.width(name) / 2, 71, TEXT);
        gui.text(font, Component.translatable(Texts.SETUP_CURRENCY), 112, 90, TEXT);
        Component currency = machine.currencySlot() == null
                ? Component.translatable(Texts.SETUP_CURRENCY_DEFAULT, machine.currency().name())
                : new ItemStack(machine.currencySlot()).getHoverName();
        // Beside the currency slot: room for two lines, e.g. "Empty:" / "diamonds".
        List<FormattedCharSequence> lines = font.split(currency, VendingSetupMenu.WIDTH - 132 - 4);
        for (int i = 0; i < Math.min(2, lines.size()); i++) gui.text(font, lines.get(i), 132, 102 + i * 10, TEXT);
    }

    private FormattedCharSequence firstLine(Component text, int width) {
        List<FormattedCharSequence> lines = font.split(text, width);
        return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
    }

    private void wrap(Gui gui, Component text, int x, int y, int color) {
        wrap(gui, text, x, y, VendingSetupMenu.WIDTH - x - 4, color);
    }

    private void wrap(Gui gui, Component text, int x, int y, int width, int color) {
        for (FormattedCharSequence line : font.split(text, width)) {
            gui.text(font, line, x, y, color);
            y += 10;
        }
    }

    /** A text box that only takes digits, typed or pasted (26.1's EditBox has no filter any more). */
    private static final class DigitBox extends EditBox {
        DigitBox(Font font, int x, int y, int width, int height, Component label) {
            super(font, x, y, width, height, label);
        }

        @Override
        public void insertText(String text) {
            super.insertText(text.replaceAll("[^0-9]", ""));
        }
    }

    private static boolean isEnter(int key) {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    /** Enter in the price box: sends the price. */
    private boolean commitPriceKey() {
        commitPrice();
        return true;
    }

    // ---- the four methods that differ between versions -----------------------------------------------------------

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackground(new Gui(graphics));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabels(new Gui(graphics));
    }

    // While the price box is being typed in, letters and numbers are for it — not the inventory or hotbar keys.
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (price.isFocused() && !event.isEscape()) {
            if (isEnter(event.key())) return commitPriceKey();
            return price.keyPressed(event) || price.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawBackground(new Gui(graphics));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabels(new Gui(graphics));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    // While the price box is being typed in, letters and numbers are for it — not the inventory or hotbar keys.
    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (price.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            if (isEnter(key)) return commitPriceKey();
            return price.keyPressed(key, scanCode, modifiers) || price.canConsumeInput() || super.keyPressed(key, scanCode, modifiers);
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    *///?}
}
