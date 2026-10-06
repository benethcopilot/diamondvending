package diamondvending.block;

import diamondvending.Messages;
import diamondvending.core.Hit;
import diamondvending.core.Region;
import diamondvending.core.Texts;
import diamondvending.registry.ModContent;
import diamondvending.shop.CoinSlot;
import diamondvending.shop.PickupTray;
import diamondvending.shop.Purchase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
//? if >=26.1 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}
//? if >=1.20.5 {
import com.mojang.serialization.MapCodec;
//?}
//? if <26.1 && >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}

/**
 * The 2×2 vending machine (spec §2). Four block positions share this block; the lower-left part (as seen from the
 * front) is the master and owns the {@link VendingMachineBlockEntity}.
 */
public class VendingMachineBlock extends BaseEntityBlock {
    //? if >=1.20.5 {
    public static final MapCodec<VendingMachineBlock> CODEC = simpleCodec(VendingMachineBlock::new);
    //?}
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<MachineSide> SIDE = EnumProperty.create("side", MachineSide.class);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    /** Owners and admins mine it like an iron block. */
    private static final float OWNER_HARDNESS = 5.0F;

    public VendingMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(SIDE, MachineSide.LEFT)
                .setValue(COLOR, DyeColor.RED));
    }

    /**
     * Spec §2.1: lit, immovable, blast-proof. Hardness −1 keeps drills, quarries and other automation out; owners
     * and admins get their own mining speed from {@link #getDestroyProgress}.
     */
    public static BlockBehaviour.Properties defaultProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(COLOR).getMapColor())
                .strength(-1.0F, 1200.0F)
                .sound(SoundType.METAL)
                .lightLevel(state -> 6)
                .pushReaction(PushReaction.BLOCK)
                .noLootTable();
    }

    //? if >=1.20.5 {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SIDE, COLOR);
    }

    // The overrides below are public: 1.20.1 declares these methods public, newer versions protected (widening is allowed).
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return MachinePart.of(state) == MachinePart.LOWER_LEFT ? new VendingMachineBlockEntity(pos, state) : null;
    }

    /** Server only: machines using a catalog re-sync after /reload ({@link VendingMachineBlockEntity#serverTick}). */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModContent.VENDING_MACHINE_BLOCK_ENTITY.get(), VendingMachineBlockEntity::serverTick);
    }

    /** The machine a part belongs to (its master part's block entity); null if that's missing. */
    public static VendingMachineBlockEntity machineOf(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine ? machine : null;
    }

    // ---- placing -------------------------------------------------------------------------------------------------

    /**
     * The clicked spot becomes the lower-left part; the other three spots must be free, inside the world and clear of
     * creatures (the game checks the clicked spot itself).
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos master = context.getClickedPos();
        for (MachinePart part : MachinePart.values()) {
            if (part == MachinePart.LOWER_LEFT) continue;
            BlockPos pos = part.posFrom(master, facing);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced(context)
                    || !level.isUnobstructed(defaultBlockState(), pos, CollisionContext.empty())) {
                return null;
            }
        }
        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(COLOR, MachineItems.colorOf(context.getItemInHand()));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        for (MachinePart part : MachinePart.values()) {
            if (part != MachinePart.LOWER_LEFT) {
                level.setBlock(part.posFrom(pos, facing), part.applyTo(state), Block.UPDATE_ALL);
            }
        }
        if (!(level.getBlockEntity(pos) instanceof VendingMachineBlockEntity machine)) return;
        Player player = placer instanceof Player p ? p : null;
        if (player != null) machine.setOwner(player.getUUID(), player.getName().getString());
        // Spec §5.4: an item that kept its setup puts it back; only an admin gets an infinite machine back.
        MachineSetup setup = stack.get(ModContent.MACHINE_SETUP.get());
        if (setup != null) setup.applyTo(machine, player != null && MachineAccess.isAdmin(player));
    }

    // ---- breaking ------------------------------------------------------------------------------------------------

    /**
     * NeoForge 26.1 deprecates the position-less {@code getDestroySpeed}/{@code hasCorrectToolForDrops} in favour of its
     * own overloads, which vanilla (and so Fabric) lacks. The vanilla calls work on every loader, so we keep them.
     */
    @Override
    @SuppressWarnings("deprecation")
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!MachineAccess.canManage(player, machineOf(level, pos, state))) return 0.0F;
        int divisor = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / OWNER_HARDNESS / divisor;
    }

    /** Tells someone who may not break it why nothing happens when they start mining. */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        VendingMachineBlockEntity machine = machineOf(level, pos, state);
        if (!level.isClientSide() && !MachineAccess.canManage(player, machine)) {
            Messages.actionBar(player, Component.translatable(MachineAccess.refusal(player, machine)));
        }
        super.attack(state, level, pos, player);
    }

    //? if >=1.20.5 {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropForPlayer(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropForPlayer(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    /**
     * Survival breaks drop one machine item in the machine's color, carrying its setup. The other parts then remove
     * themselves through {@link #keepIfWhole}, like a door's other half, so blocks hanging on them (torches, signs…) get
     * their updates too.
     */
    private static void dropForPlayer(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            BlockPos master = MachinePart.masterOf(pos, state);
            VendingMachineBlockEntity machine = level.getBlockEntity(master) instanceof VendingMachineBlockEntity found ? found : null;
            popResource(level, master, MachineItems.forMachine(state.getValue(COLOR), machine));
        }
    }

    //? if <26.1 {
    /*// 1.20.1 and 1.21.1: spill when the master is really removed — a repaint keeps the same block, so it must not spill.
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VendingMachineBlockEntity machine) {
            machine.spillContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}

    // ---- using ---------------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) handleClick(stack, state, level, pos, player, hit);
        return InteractionResult.SUCCESS;
    }
    //?} else if >=1.20.5 {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) handleClick(stack, state, level, pos, player, hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) handleClick(player.getItemInHand(hand), state, level, pos, player, hit);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    /**
     * Spec §3.2, on the server. Owners and admins holding a dye repaint the machine; otherwise the spot clicked on the
     * front decides. Every click is used up (see {@link #useItemOn}), so blocks in hand are never placed against it.
     */
    private void handleClick(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine)) return;
        machine.refreshOwnerName(player);
        if (player.isSecondaryUseActive()) {
            // Spec §3.2 rule 1: the game only lets a sneak-click reach a block when both hands are empty.
            openSetup(machine, player);
            return;
        }
        DyeColor dye = MachineItems.dyeColorOf(stack);
        if (dye != null && MachineAccess.canManage(player, machine)) {
            dye(stack, state, level, pos, player, dye);
            return;
        }
        Hit target = FrontFace.hit(state, pos, hit);
        // Holding right-click repeats the click every 4 ticks: one press buys (or inserts) once.
        boolean pressable = target.region() == Region.BUTTON || target.region() == Region.COIN_SLOT;
        if (pressable && machine.isRepeatPress(player.getUUID(), target, level.getGameTime())) return;
        switch (target.region()) {
            case BUTTON -> Purchase.pressButton(machine, player, target.button());
            case COIN_SLOT -> CoinSlot.insert(machine, player);
            case COIN_RETURN -> CoinSlot.giveBack(machine, player);
            case TRAY -> PickupTray.collect(machine, player);
            default -> {
                // Spec §3.5 c: someone who may not change it tried to dye it.
                if (dye != null) Messages.actionBar(player, Component.translatable(MachineAccess.refusal(player, machine)));
            }
        }
    }

    /** Opens the setup screen for the owner or an admin (spec §4); anyone else is told why not. */
    private static void openSetup(VendingMachineBlockEntity machine, Player player) {
        if (!MachineAccess.canManage(player, machine)) {
            Messages.actionBar(player, Component.translatable(MachineAccess.refusal(player, machine)));
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ModContent.SETUP_MENU.open(serverPlayer, Component.translatable(Texts.SETUP_TITLE), machine.getBlockPos());
        }
    }

    /** Spec §3.2 rule 2: repaints the whole machine, using one dye unless in creative. The same color changes nothing. */
    private void dye(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, DyeColor color) {
        if (state.getValue(COLOR) == color) return;
        Direction facing = state.getValue(FACING);
        BlockPos master = MachinePart.masterOf(pos, state);
        for (MachinePart part : MachinePart.values()) {
            BlockPos partPos = part.posFrom(master, facing);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlock(partPos, partState.setValue(COLOR, color), Block.UPDATE_ALL);
            }
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ---- integrity -----------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                  BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return keepIfWhole(state, pos, neighborPos, neighborState);
    }
    //?} else {
    /*@Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return keepIfWhole(state, pos, neighborPos, neighborState);
    }
    *///?}

    /** A part whose neighbouring part is gone (or wrong) removes itself, so broken machines never linger (spec §9). */
    private BlockState keepIfWhole(BlockState state, BlockPos pos, BlockPos neighborPos, BlockState neighborState) {
        MachinePart self = MachinePart.of(state);
        Direction facing = state.getValue(FACING);
        BlockPos master = self.masterFrom(pos, facing);
        for (MachinePart part : MachinePart.values()) {
            if (part == self || !part.posFrom(master, facing).equals(neighborPos)) continue;
            boolean intact = neighborState.is(this)
                    && neighborState.getValue(FACING) == facing
                    && MachinePart.of(neighborState) == part;
            return intact ? state : Blocks.AIR.defaultBlockState();
        }
        return state;
    }
}
