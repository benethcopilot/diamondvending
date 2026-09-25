package diamondvending.menu;

import diamondvending.block.MachineAccess;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.catalog.Catalogs;
import diamondvending.core.MachineLayout;
import diamondvending.core.SetupButtons;
import diamondvending.core.SetupTab;
import diamondvending.registry.ModContent;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.PlayerItems;
import diamondvending.shop.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if >=26.1 {
import net.minecraft.world.inventory.ContainerInput;
//?} else {
/*import net.minecraft.world.inventory.ClickType;
*///?}

import java.util.function.UnaryOperator;

/**
 * The setup screen's menu (spec §4): tabs, 12 ghost slots for what the buttons sell, the Stock, the Cash Box and the
 * admin's currency slot, with the player's inventory below. The client only asks: ghost-slot clicks arrive as vanilla
 * container clicks and everything else as menu buttons ({@link SetupButtons}), and every one is re-checked here.
 */
public class VendingSetupMenu extends AbstractContainerMenu {
    // The screen's size and where the slots sit on it (the client screen draws around them).
    public static final int WIDTH = 208;
    public static final int HEIGHT = 224;
    public static final int GHOSTS_X = 12;
    public static final int GHOSTS_Y = 56;
    public static final int GRID_X = 24;
    public static final int GRID_Y = 72;
    public static final int CURRENCY_X = 112;
    public static final int CURRENCY_Y = 102;
    public static final int INVENTORY_X = 24;
    public static final int INVENTORY_Y = 142;
    public static final int HOTBAR_Y = 200;

    // Slot indexes.
    public static final int FIRST_GHOST = 0;
    public static final int FIRST_STOCK = FIRST_GHOST + MachineLayout.SELECTIONS;
    public static final int FIRST_CASH = FIRST_STOCK + VendingMachineBlockEntity.STOCK_SLOTS;
    public static final int CURRENCY = FIRST_CASH + VendingMachineBlockEntity.CASH_BOX_SLOTS;
    public static final int FIRST_PLAYER = CURRENCY + 1;
    private static final int END = FIRST_PLAYER + 36;

    private final VendingMachineBlockEntity machine;
    private final DataSlot admin = DataSlot.standalone();
    private SetupTab tab = SetupTab.SELECTIONS;
    private int selected;

    public VendingSetupMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModContent.SETUP_MENU.type(), id);
        Player player = inventory.player;
        Level level = player.level();
        // A machine that's already gone (the client can be a moment behind) gets a stand-in, and the screen closes.
        machine = level.getBlockEntity(pos) instanceof VendingMachineBlockEntity found ? found
                : new VendingMachineBlockEntity(pos, ModContent.VENDING_MACHINE.get().defaultBlockState());
        boolean server = !level.isClientSide();
        Container stock = server ? new MachineSlots(machine.stock(), machine::changed) : new SimpleContainer(VendingMachineBlockEntity.STOCK_SLOTS);
        Container cashBox = server ? new MachineSlots(machine.cashBox(), machine::changed) : new SimpleContainer(VendingMachineBlockEntity.CASH_BOX_SLOTS);
        for (int i = 0; i < MachineLayout.SELECTIONS; i++) {
            addSlot(new ShelfSlot(i, GHOSTS_X + i % 3 * 18, GHOSTS_Y + i / 3 * 18));
        }
        for (int i = 0; i < VendingMachineBlockEntity.STOCK_SLOTS; i++) addSlot(new TabSlot(stock, i, SetupTab.STOCK, true));
        for (int i = 0; i < VendingMachineBlockEntity.CASH_BOX_SLOTS; i++) addSlot(new TabSlot(cashBox, i, SetupTab.CASH_BOX, false));
        addSlot(new CurrencySlot());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        if (server) admin.set(MachineAccess.isAdmin(player) ? 1 : 0);
        addDataSlot(admin);
    }

    public VendingMachineBlockEntity machine() {
        return machine;
    }

    public SetupTab tab() {
        return tab;
    }

    /** The button (0–11) whose amount and price the Items tab is editing. */
    public int selected() {
        return selected;
    }

    /** Whether the player looking at this menu is an admin (decided by the server, synced to the client). */
    public boolean viewerIsAdmin() {
        return admin.get() == 1;
    }

    public boolean stockAndCashEmpty() {
        return rangeEmpty(FIRST_STOCK, CURRENCY);
    }

    public boolean cashBoxEmpty() {
        return rangeEmpty(FIRST_CASH, CURRENCY);
    }

    private boolean rangeEmpty(int from, int to) {
        for (int i = from; i < to; i++) {
            if (slots.get(i).hasItem()) return false;
        }
        return true;
    }

    // ---- clicks --------------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (!handledHere(slotIndex, button, input == ContainerInput.PICKUP, player)) super.clicked(slotIndex, button, input, player);
    }
    //?} else {
    /*@Override
    public void clicked(int slotIndex, int button, ClickType type, Player player) {
        if (!handledHere(slotIndex, button, type == ClickType.PICKUP, player)) super.clicked(slotIndex, button, type, player);
    }
    *///?}

    /** Ghost slots and slots on hidden tabs: true when the click is dealt with and vanilla must not move anything. */
    private boolean handledHere(int slotIndex, int button, boolean pickup, Player player) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;
        Slot slot = slots.get(slotIndex);
        if (!slot.isActive()) return true;
        if (slot instanceof ShelfSlot shelf) {
            if (pickup) copyToButton(shelf.index, button, player);
            return true;
        }
        if (slot instanceof CurrencySlot) {
            if (pickup) setCurrency(player);
            return true;
        }
        return false;
    }

    /** Ghost-slot click: the cursor's item (all of it, or one with a right-click) becomes what the button sells; nothing is used up. */
    private void copyToButton(int index, int button, Player player) {
        selected = index;
        if (player.level().isClientSide() || !MachineAccess.canManage(player, machine) || machine.usesCatalog()) return;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;
        ItemStack template = button == 1 ? carried.copyWithCount(1) : carried.copy();
        machine.setSelection(index, Selection.of(template, machine.getSelection(index).price()));
    }

    /** Currency ghost-slot click (admins): the cursor's item becomes the currency; an empty cursor clears it. */
    private void setCurrency(Player player) {
        if (player.level().isClientSide() || !MachineAccess.isAdmin(player)) return;
        ItemStack carried = getCarried();
        machine.setCurrencySlot(carried.isEmpty() ? null : carried.getItem());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        SetupButtons.Press press = SetupButtons.decode(id);
        if (press == null) return false;
        if (press instanceof SetupButtons.ShowTab show) return showTab(show.tab());
        // Everything else is the server's to decide; the client only asks (spec §4).
        if (player.level().isClientSide()) return true;
        if (!MachineAccess.canManage(player, machine)) return false;
        return switch (press) {
            case SetupButtons.ShowTab show -> showTab(show.tab());
            case SetupButtons.Withdraw ignored -> withdraw(player);
            case SetupButtons.ToggleInfinite ignored -> toggleInfinite(player);
            case SetupButtons.CycleCatalog cycle -> cycleCatalog(player, cycle.step());
            case SetupButtons.Clear clear -> edit(clear.index(), selection -> Selection.EMPTY);
            case SetupButtons.ChangeQuantity change -> edit(change.index(), selection -> withQuantity(selection, change.by()));
            case SetupButtons.SetPrice price -> edit(price.index(), selection -> withPrice(selection, price.price()));
        };
    }

    private boolean showTab(SetupTab wanted) {
        if (!wanted.shownTo(viewerIsAdmin(), machine.isInfinite())) return false;
        tab = wanted;
        return true;
    }

    private boolean withdraw(Player player) {
        if (machine.isInfinite()) return false;
        ItemSlots.takeAll(machine.cashBox()).forEach(stack -> PlayerItems.give(player, stack));
        machine.changed();
        return true;
    }

    private boolean toggleInfinite(Player player) {
        if (!MachineAccess.isAdmin(player)) return false;
        // Spec §4: going infinite needs an empty Stock and Cash Box, so nobody's items silently vanish.
        if (!machine.isInfinite() && !stockAndCashEmpty()) return false;
        machine.setInfinite(!machine.isInfinite());
        if (machine.isInfinite() && (tab == SetupTab.STOCK || tab == SetupTab.CASH_BOX)) tab = SetupTab.SELECTIONS;
        return true;
    }

    private boolean cycleCatalog(Player player, int step) {
        if (!MachineAccess.isAdmin(player)) return false;
        machine.setCatalog(Catalogs.cycle(machine.catalogId(), step));
        return true;
    }

    /** Changes a button's selection; refused while a catalog decides what's sold (spec §4). */
    private boolean edit(int index, UnaryOperator<Selection> change) {
        if (machine.usesCatalog()) return false;
        Selection before = machine.getSelection(index);
        Selection after = change.apply(before);
        if (after != before) machine.setSelection(index, after);
        return true;
    }

    private static Selection withQuantity(Selection selection, int by) {
        if (!selection.isSetUp()) return selection;
        int quantity = Math.max(1, Math.min(selection.quantity() + by, selection.template().getMaxStackSize()));
        return Selection.of(selection.template().copyWithCount(quantity), selection.price());
    }

    private static Selection withPrice(Selection selection, int price) {
        return selection.isSetUp() ? Selection.of(selection.template(), price) : selection;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot instanceof ShowSlot || !slot.isActive() || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = index >= FIRST_PLAYER
                // From the player: only into the Stock, and only while the Stock tab is showing.
                ? tab == SetupTab.STOCK && !machine.isInfinite() && moveItemStackTo(stack, FIRST_STOCK, FIRST_CASH, false)
                : moveItemStackTo(stack, FIRST_PLAYER, END, true);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return before;
    }

    /** Double-click gathering takes from shown slots only — never from a hidden tab's Stock or Cash Box (spec §4). */
    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(carried, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(machine, player) && MachineAccess.canManage(player, machine);
    }

    // ---- slots ---------------------------------------------------------------------------------------------------

    /** A slot that only shows an item; the menu handles its clicks, and it never takes or gives anything itself. */
    private abstract static class ShowSlot extends Slot {
        ShowSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public abstract ItemStack getItem();

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** Ghost slot: what button {@code index} sells, shown with the amount per purchase. */
    private final class ShelfSlot extends ShowSlot {
        final int index;

        ShelfSlot(int index, int x, int y) {
            super(x, y);
            this.index = index;
        }

        @Override
        public ItemStack getItem() {
            return machine.getSelection(index).template();
        }

        @Override
        public boolean isActive() {
            return tab == SetupTab.SELECTIONS;
        }
    }

    /** The admin's currency ghost slot: the currency item, or empty for the default currency. */
    private final class CurrencySlot extends ShowSlot {
        CurrencySlot() {
            super(CURRENCY_X, CURRENCY_Y);
        }

        @Override
        public ItemStack getItem() {
            Item item = machine.currencySlot();
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        @Override
        public boolean isActive() {
            return tab == SetupTab.ADMIN;
        }
    }

    /** A Stock or Cash Box slot: real items, there only while its tab shows on an owned machine. The Cash Box is take-only. */
    private final class TabSlot extends Slot {
        private final SetupTab home;
        private final boolean placeable;

        TabSlot(Container container, int index, SetupTab home, boolean placeable) {
            super(container, index, GRID_X + index % 9 * 18, GRID_Y + index / 9 * 18);
            this.home = home;
            this.placeable = placeable;
        }

        @Override
        public boolean isActive() {
            return tab == home && !machine.isInfinite();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return placeable && super.mayPlace(stack);
        }
    }
}
