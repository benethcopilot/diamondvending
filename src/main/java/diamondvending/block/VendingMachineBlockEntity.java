package diamondvending.block;

import diamondvending.DiamondVending;
import diamondvending.core.Flash;
import diamondvending.core.Hit;
import diamondvending.core.MachineFacts;
import diamondvending.core.MachineLayout;
import diamondvending.core.MachineProblems;
import diamondvending.core.Problem;
import diamondvending.registry.ModContent;
import diamondvending.shop.Currency;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.Optional;
//?} else {
/*import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
*///?}

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Everything a machine holds, kept on the master part (spec §8.3): owner, 12 selections, stock, cash box, tray,
 * each player's credit and the infinite flag. Saved in a format map makers can write with {@code /data}; clients get
 * a trimmed copy (see {@link #getUpdateTag}).
 */
public class VendingMachineBlockEntity extends BlockEntity {
    public static final int STOCK_SLOTS = 27;
    public static final int CASH_BOX_SLOTS = 27;
    public static final int TRAY_SLOTS = 9;
    /** Credit is capped at 9 stacks per player per machine (spec §3.4). */
    public static final int CREDIT_SLOTS = 9;
    /** How long a button has to be let go before pressing it again counts as a new press (0.5 s). */
    public static final int REPEAT_TICKS = 10;
    /**
     * Block event: button {@code param}'s item was sold — it drops on screen and the display says THANK YOU. Block
     * event ids and params reach clients as single bytes (0–255), so they carry small numbers only.
     */
    public static final int EVENT_VEND = 1;
    /** Block events {@code EVENT_FLASH + n}: the display flashes {@code Flash.values()[n]}; {@code param} is the button pressed. */
    public static final int EVENT_FLASH = 16;

    // Saved keys — also what map makers write with /data, so keep them stable.
    private static final String OWNER = "owner";
    private static final String OWNER_NAME = "owner_name";
    private static final String INFINITE = "infinite";
    private static final String SELECTIONS = "selections";
    private static final String SLOT = "slot";
    private static final String ITEM = "item";
    private static final String PRICE = "price";
    private static final String STOCK = "stock";
    private static final String CASH_BOX = "cash_box";
    private static final String TRAY = "tray";
    private static final String CREDITS = "credits";
    private static final String PLAYER = "player";
    // Sent to clients in the update tag only, never saved.
    private static final String SYNC_STOCK = "sync_stock";
    private static final String SYNC_CREDITS = "sync_credits";
    private static final String SYNC_PROBLEMS = "sync_problems";

    private UUID owner;
    private String ownerName = "";
    private boolean infinite;
    private final Selection[] selections = new Selection[MachineLayout.SELECTIONS];
    private final NonNullList<ItemStack> stock = NonNullList.withSize(STOCK_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> cashBox = NonNullList.withSize(CASH_BOX_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> tray = NonNullList.withSize(TRAY_SLOTS, ItemStack.EMPTY);
    private final Map<UUID, NonNullList<ItemStack>> credits = new HashMap<>();
    /** Each player's latest button or coin-slot press, to spot held right-clicks. Not saved. */
    private final Map<UUID, Press> lastPresses = new HashMap<>();
    // What the display last showed, from block events. Kept on both sides, never saved.
    private Flash lastFlash;
    private int lastFlashNumber;
    private long lastFlashTime;
    private int lastVendSelection = -1;
    private long lastVendTime;
    private int[] syncedStock = new int[0];
    private int[] syncedCredits = new int[0];
    private int[] syncedProblems = new int[0];

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), pos, state);
        Arrays.fill(selections, Selection.EMPTY);
    }

    // ---- owner ---------------------------------------------------------------------------------------------------

    /** The owner's UUID, or null for machines placed without a player. */
    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(UUID owner, String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
        changed();
    }

    /** Keeps the owner's last-known name current (spec §5.3) — players can change their names. */
    public void refreshOwnerName(Player player) {
        String name = player.getName().getString();
        if (player.getUUID().equals(owner) && !name.equals(ownerName)) setOwner(owner, name);
    }

    // ---- contents ------------------------------------------------------------------------------------------------

    public Selection getSelection(int index) {
        return selections[index];
    }

    public void setSelection(int index, Selection selection) {
        selections[index] = selection;
        changed();
    }

    public boolean isInfinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
        changed();
    }

    /** The 27 stock slots, live. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> stock() {
        return stock;
    }

    /** The 27 cash box slots, live. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> cashBox() {
        return cashBox;
    }

    /** The 9 tray slots, live. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> tray() {
        return tray;
    }

    /** A player's credit, live — or an empty list if they have none. Call {@link #changed()} after editing. */
    public List<ItemStack> credit(UUID player) {
        NonNullList<ItemStack> items = credits.get(player);
        return items != null ? items : List.of();
    }

    /** A player's credit slots, live, created empty if they have none yet. Call {@link #changed()} after editing. */
    public NonNullList<ItemStack> creditOf(UUID player) {
        return credits.computeIfAbsent(player, id -> NonNullList.withSize(CREDIT_SLOTS, ItemStack.EMPTY));
    }

    /** Removes a player's credit and returns it, exactly the items they put in. */
    public List<ItemStack> takeCredit(UUID player) {
        NonNullList<ItemStack> items = credits.remove(player);
        return items == null ? List.of() : ItemSlots.takeAll(items);
    }

    /** What this machine takes as money (spec §5.5). Plan 5 adds the admin currency slot and catalog currency. */
    public Currency currency() {
        return Currency.DEFAULT;
    }

    /** Items in stock that button {@code index} sells; 0 for an empty button. */
    public int stockCountFor(int index) {
        Selection selection = selections[index];
        return selection.isSetUp() ? ItemSlots.count(stock, selection::sells) : 0;
    }

    /** Active problems, in display order (spec §3.5 b). */
    public List<Problem> problems() {
        int setUp = 0;
        int inStock = 0;
        for (int i = 0; i < selections.length; i++) {
            if (!selections[i].isSetUp()) continue;
            setUp++;
            if (stockCountFor(i) >= selections[i].quantity()) inStock++;
        }
        return MachineProblems.of(new MachineFacts(infinite, false, setUp, inStock,
                ItemSlots.hasEmptySlot(cashBox), ItemSlots.hasEmptySlot(tray)));
    }

    /** Saves and re-syncs the machine. Call after any change to its contents. */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private record Press(Hit hit, long time) {}

    /**
     * Whether this press is a held right-click repeating (the client resends it every 4 ticks) rather than a new
     * press: the same player pressing the same thing within {@link #REPEAT_TICKS} of their last try. Every try is
     * remembered, so holding a button down buys once, and letting go for half a second makes the next press count.
     */
    public boolean isRepeatPress(UUID player, Hit hit, long gameTime) {
        Press last = lastPresses.put(player, new Press(hit, gameTime));
        return last != null && last.hit().equals(hit) && gameTime - last.time() < REPEAT_TICKS;
    }

    /**
     * Server side: makes every nearby display flash (spec §3.5 c). {@code button} is the button that was pressed, or 0
     * when there wasn't one: NEED shows its price.
     */
    public void sendFlash(Flash flash, int button) {
        send(flashEvent(flash, button));
    }

    /** Server side: button {@code selection}'s item drops into the tray on every nearby screen (spec §2.3). */
    public void sendVend(int selection) {
        send(vendEvent(selection));
    }

    /** The block event {@link #sendFlash} sends. */
    public BlockEventData flashEvent(Flash flash, int button) {
        return new BlockEventData(worldPosition, getBlockState().getBlock(), EVENT_FLASH + flash.ordinal(), button);
    }

    /** The block event {@link #sendVend} sends. */
    public BlockEventData vendEvent(int selection) {
        return new BlockEventData(worldPosition, getBlockState().getBlock(), EVENT_VEND, selection);
    }

    private void send(BlockEventData event) {
        if (level != null) level.blockEvent(event.pos(), event.block(), event.paramA(), event.paramB());
    }

    /** Remembers the machine's block events, on the server and on each client (the client draws from them). */
    @Override
    public boolean triggerEvent(int id, int param) {
        long now = level != null ? level.getGameTime() : 0;
        if (id == EVENT_VEND) {
            lastVendSelection = param >= 0 && param < selections.length ? param : -1;
            lastVendTime = now;
            lastFlash = Flash.THANK_YOU;
            lastFlashNumber = 0;
            lastFlashTime = now;
            return true;
        }
        if (id >= EVENT_FLASH && id < EVENT_FLASH + Flash.values().length) {
            lastFlash = Flash.values()[id - EVENT_FLASH];
            // NEED shows the price of the button that was pressed — a price can be bigger than a block event can carry.
            boolean button = param >= 0 && param < selections.length;
            lastFlashNumber = lastFlash == Flash.NEED_MONEY && button ? selections[param].price() : 0;
            lastFlashTime = now;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** The last flash, or null if there hasn't been one. */
    public Flash lastFlash() {
        return lastFlash;
    }

    public int lastFlashNumber() {
        return lastFlashNumber;
    }

    /** Game time of the last flash. */
    public long lastFlashTime() {
        return lastFlashTime;
    }

    /** The button whose item last dropped, or −1. */
    public int lastVendSelection() {
        return lastVendSelection;
    }

    /** Game time of the last sale. */
    public long lastVendTime() {
        return lastVendTime;
    }

    /** Drops everything the machine holds (spec §5.3): the tray, every player's credit, the stock and the cash box. */
    public void spillContents() {
        if (level == null || level.isClientSide()) return;
        Containers.dropContents(level, worldPosition, tray);
        credits.values().forEach(items -> Containers.dropContents(level, worldPosition, items));
        Containers.dropContents(level, worldPosition, stock);
        Containers.dropContents(level, worldPosition, cashBox);
        ItemSlots.clear(tray);
        ItemSlots.clear(stock);
        ItemSlots.clear(cashBox);
        credits.clear();
    }

    //? if >=26.1 {
    /** 26.1 calls this when the block is really removed — not when it's only repainted. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        spillContents();
    }
    //?}

    // ---- client view ---------------------------------------------------------------------------------------------

    /** Client side: items in stock for button {@code index}, as last synced. */
    public int syncedStockCount(int index) {
        return index < syncedStock.length ? syncedStock[index] : 0;
    }

    /** Client side: how much spendable credit this player has, as last synced. */
    public int syncedCredit(UUID player) {
        for (int i = 0; i + 4 < syncedCredits.length; i += 5) {
            if (UUIDUtil.uuidFromIntArray(Arrays.copyOfRange(syncedCredits, i, i + 4)).equals(player)) return syncedCredits[i + 4];
        }
        return 0;
    }

    /** Client side: the machine's problems, as last synced. */
    public List<Problem> syncedProblems() {
        List<Problem> problems = new ArrayList<>();
        for (int ordinal : syncedProblems) {
            if (ordinal >= 0 && ordinal < Problem.values().length) problems.add(Problem.values()[ordinal]);
        }
        return problems;
    }

    // ---- saving --------------------------------------------------------------------------------------------------

    private void clearContents() {
        Arrays.fill(selections, Selection.EMPTY);
        ItemSlots.clear(stock);
        ItemSlots.clear(cashBox);
        ItemSlots.clear(tray);
        credits.clear();
    }

    private void warnLostSelection(int slot) {
        DiamondVending.LOGGER.warn("Vending machine at {}: the item for button {} no longer exists, so that button is now empty",
                worldPosition, slot + 1);
    }

    //? if >=26.1 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store(OWNER, UUIDUtil.CODEC, owner);
        output.putString(OWNER_NAME, ownerName);
        output.putBoolean(INFINITE, infinite);
        ValueOutput.ValueOutputList selectionList = output.childrenList(SELECTIONS);
        for (int i = 0; i < selections.length; i++) {
            if (!selections[i].isSetUp()) continue;
            ValueOutput entry = selectionList.addChild();
            entry.putInt(SLOT, i);
            entry.store(ITEM, ItemStack.CODEC, selections[i].template());
            entry.putInt(PRICE, selections[i].price());
        }
        ContainerHelper.saveAllItems(output.child(STOCK), stock);
        ContainerHelper.saveAllItems(output.child(CASH_BOX), cashBox);
        ContainerHelper.saveAllItems(output.child(TRAY), tray);
        ValueOutput.ValueOutputList creditList = output.childrenList(CREDITS);
        credits.forEach((player, items) -> {
            if (ItemSlots.isEmpty(items)) return;
            ValueOutput entry = creditList.addChild();
            entry.store(PLAYER, UUIDUtil.CODEC, player);
            ContainerHelper.saveAllItems(entry, items);
        });
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        clearContents();
        owner = input.read(OWNER, UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr(OWNER_NAME, "");
        infinite = input.getBooleanOr(INFINITE, false);
        for (ValueInput entry : input.childrenListOrEmpty(SELECTIONS)) {
            int slot = entry.getIntOr(SLOT, -1);
            if (slot < 0 || slot >= selections.length) continue;
            Optional<ItemStack> item = entry.read(ITEM, ItemStack.CODEC);
            if (item.isEmpty()) {
                warnLostSelection(slot);
                continue;
            }
            selections[slot] = Selection.of(item.get(), entry.getIntOr(PRICE, 0));
        }
        ContainerHelper.loadAllItems(input.childOrEmpty(STOCK), stock);
        ContainerHelper.loadAllItems(input.childOrEmpty(CASH_BOX), cashBox);
        ContainerHelper.loadAllItems(input.childOrEmpty(TRAY), tray);
        for (ValueInput entry : input.childrenListOrEmpty(CREDITS)) {
            Optional<UUID> player = entry.read(PLAYER, UUIDUtil.CODEC);
            if (player.isEmpty()) continue;
            NonNullList<ItemStack> items = NonNullList.withSize(CREDIT_SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(entry, items);
            if (!ItemSlots.isEmpty(items)) credits.put(player.get(), items);
        }
        syncedStock = input.getIntArray(SYNC_STOCK).orElse(new int[0]);
        syncedCredits = input.getIntArray(SYNC_CREDITS).orElse(new int[0]);
        syncedProblems = input.getIntArray(SYNC_PROBLEMS).orElse(new int[0]);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putUUID(OWNER, owner);
        tag.putString(OWNER_NAME, ownerName);
        tag.putBoolean(INFINITE, infinite);
        ListTag selectionList = new ListTag();
        for (int i = 0; i < selections.length; i++) {
            if (!selections[i].isSetUp()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT, i);
            entry.put(ITEM, selections[i].template().save(registries));
            entry.putInt(PRICE, selections[i].price());
            selectionList.add(entry);
        }
        tag.put(SELECTIONS, selectionList);
        tag.put(STOCK, ContainerHelper.saveAllItems(new CompoundTag(), stock, registries));
        tag.put(CASH_BOX, ContainerHelper.saveAllItems(new CompoundTag(), cashBox, registries));
        tag.put(TRAY, ContainerHelper.saveAllItems(new CompoundTag(), tray, registries));
        ListTag creditList = new ListTag();
        credits.forEach((player, items) -> {
            if (ItemSlots.isEmpty(items)) return;
            CompoundTag entry = ContainerHelper.saveAllItems(new CompoundTag(), items, registries);
            entry.putUUID(PLAYER, player);
            creditList.add(entry);
        });
        tag.put(CREDITS, creditList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        clearContents();
        owner = tag.hasUUID(OWNER) ? tag.getUUID(OWNER) : null;
        ownerName = tag.getString(OWNER_NAME);
        infinite = tag.getBoolean(INFINITE);
        ListTag selectionList = tag.getList(SELECTIONS, Tag.TAG_COMPOUND);
        for (int i = 0; i < selectionList.size(); i++) {
            CompoundTag entry = selectionList.getCompound(i);
            int slot = entry.contains(SLOT) ? entry.getInt(SLOT) : -1;
            if (slot < 0 || slot >= selections.length) continue;
            ItemStack item = entry.contains(ITEM) ? ItemStack.parse(registries, entry.get(ITEM)).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
            if (item.isEmpty()) {
                warnLostSelection(slot);
                continue;
            }
            selections[slot] = Selection.of(item, entry.getInt(PRICE));
        }
        ContainerHelper.loadAllItems(tag.getCompound(STOCK), stock, registries);
        ContainerHelper.loadAllItems(tag.getCompound(CASH_BOX), cashBox, registries);
        ContainerHelper.loadAllItems(tag.getCompound(TRAY), tray, registries);
        ListTag creditList = tag.getList(CREDITS, Tag.TAG_COMPOUND);
        for (int i = 0; i < creditList.size(); i++) {
            CompoundTag entry = creditList.getCompound(i);
            if (!entry.hasUUID(PLAYER)) continue;
            NonNullList<ItemStack> items = NonNullList.withSize(CREDIT_SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(entry, items, registries);
            if (!ItemSlots.isEmpty(items)) credits.put(entry.getUUID(PLAYER), items);
        }
        syncedStock = tag.getIntArray(SYNC_STOCK);
        syncedCredits = tag.getIntArray(SYNC_CREDITS);
        syncedProblems = tag.getIntArray(SYNC_PROBLEMS);
    }
    *///?}

    // ---- syncing -------------------------------------------------------------------------------------------------

    /**
     * What clients get (spec §8.3): everything they draw — selections, tray, owner name, infinite — plus stock counts,
     * each player's credit total and the problems. Never the stock, cash box or credit items themselves.
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove(STOCK);
        tag.remove(CASH_BOX);
        tag.remove(CREDITS);
        int[] stockCounts = new int[selections.length];
        for (int i = 0; i < selections.length; i++) stockCounts[i] = stockCountFor(i);
        tag.putIntArray(SYNC_STOCK, stockCounts);
        tag.putIntArray(SYNC_CREDITS, creditTotals());
        tag.putIntArray(SYNC_PROBLEMS, problems().stream().mapToInt(Enum::ordinal).toArray());
        return tag;
    }

    /** For each player with spendable credit: their UUID as 4 ints, then the total. */
    private int[] creditTotals() {
        List<Integer> totals = new ArrayList<>();
        credits.forEach((player, items) -> {
            int total = ItemSlots.count(items, currency()::matches);
            if (total == 0) return;
            for (int part : UUIDUtil.uuidToIntArray(player)) totals.add(part);
            totals.add(total);
        });
        return totals.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
