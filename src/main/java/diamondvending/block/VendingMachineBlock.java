package diamondvending.block;

import com.mojang.serialization.MapCodec;
import diamondvending.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
//? if >=26.1 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}

import java.util.UUID;

/**
 * The 2×2 vending machine (spec §2). Four block positions share this block; the lower-left part (as seen from the
 * front) is the master and owns the {@link VendingMachineBlockEntity}.
 */
public class VendingMachineBlock extends BaseEntityBlock {
    public static final MapCodec<VendingMachineBlock> CODEC = simpleCodec(VendingMachineBlock::new);
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<MachineSide> SIDE = EnumProperty.create("side", MachineSide.class);
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    /** Owners and admins mine it like an iron block. */
    private static final float OWNER_HARDNESS = 5.0F;
    /** Placing/recoloring parts must not trigger shape checks on half-built neighbours. */
    private static final int PLACE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;
    private static final int REMOVE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

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

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SIDE, COLOR);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return MachinePart.of(state) == MachinePart.LOWER_LEFT ? new VendingMachineBlockEntity(pos, state) : null;
    }

    /** The machine's owner, read from the master part; null if it has none. */
    public static UUID ownerOf(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(MachinePart.masterOf(pos, state)) instanceof VendingMachineBlockEntity machine ? machine.getOwner() : null;
    }

    // ---- placing -------------------------------------------------------------------------------------------------

    /** The clicked spot becomes the lower-left part; the other three spots must be free and inside the world. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos master = context.getClickedPos();
        for (MachinePart part : MachinePart.values()) {
            if (part == MachinePart.LOWER_LEFT) continue;
            BlockPos pos = part.posFrom(master, facing);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).canBeReplaced(context)) {
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
                level.setBlock(part.posFrom(pos, facing), part.applyTo(state), PLACE_FLAGS);
            }
        }
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof VendingMachineBlockEntity machine) {
            machine.setOwner(player.getUUID(), player.getName().getString());
        }
    }

    // ---- breaking ------------------------------------------------------------------------------------------------

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!MachineAccess.canManage(player, ownerOf(level, pos, state))) return 0.0F;
        int divisor = player.hasCorrectToolForDrops(state) ? 30 : 100;
        return player.getDestroySpeed(state) / OWNER_HARDNESS / divisor;
    }

    /** Tells a non-owner why nothing happens when they start mining. */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && !MachineAccess.canManage(player, ownerOf(level, pos, state))) {
            Messages.actionBar(player, Component.translatable(Messages.OWNER_ONLY));
        }
        super.attack(state, level, pos, player);
    }

    /** One player break takes the whole machine; survival breaks drop one machine item in the machine's color. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos master = MachinePart.masterOf(pos, state);
            if (!player.isCreative()) {
                popResource(level, master, MachineItems.forColor(state.getValue(COLOR)));
            }
            removeOtherParts(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void removeOtherParts(Level level, BlockPos pos, BlockState state) {
        MachinePart self = MachinePart.of(state);
        Direction facing = state.getValue(FACING);
        BlockPos master = self.masterFrom(pos, facing);
        for (MachinePart part : MachinePart.values()) {
            if (part == self) continue;
            BlockPos partPos = part.posFrom(master, facing);
            BlockState partState = level.getBlockState(partPos);
            if (partState.is(this)) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), REMOVE_FLAGS);
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, partPos, Block.getId(partState));
            }
        }
    }

    // ---- integrity -----------------------------------------------------------------------------------------------

    //? if >=26.1 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return keepIfWhole(state, pos, neighborPos, neighborState);
    }
    //?} else {
    /*@Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
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
