package diamondvending.block;

import diamondvending.core.Facing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/** The four block positions of a machine. {@link #LOWER_LEFT} is the master. */
public enum MachinePart {
    LOWER_LEFT(false, false), LOWER_RIGHT(true, false), UPPER_LEFT(false, true), UPPER_RIGHT(true, true);

    private final boolean right;
    private final boolean upper;

    MachinePart(boolean right, boolean upper) {
        this.right = right;
        this.upper = upper;
    }

    public boolean right() {
        return right;
    }

    public boolean upper() {
        return upper;
    }

    public static MachinePart of(BlockState state) {
        boolean right = state.getValue(VendingMachineBlock.SIDE) == MachineSide.RIGHT;
        boolean upper = state.getValue(VendingMachineBlock.HALF) == DoubleBlockHalf.UPPER;
        if (upper) return right ? UPPER_RIGHT : UPPER_LEFT;
        return right ? LOWER_RIGHT : LOWER_LEFT;
    }

    /** This part's state, taking facing and color from any part's state. */
    public BlockState applyTo(BlockState anyPart) {
        return anyPart
                .setValue(VendingMachineBlock.SIDE, right ? MachineSide.RIGHT : MachineSide.LEFT)
                .setValue(VendingMachineBlock.HALF, upper ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
    }

    /** Where this part sits, given the master's position and the machine's facing. */
    public BlockPos posFrom(BlockPos master, Direction facing) {
        Facing f = Facing.valueOf(facing.name());
        return master.offset(right ? f.rightDx() : 0, upper ? 1 : 0, right ? f.rightDz() : 0);
    }

    /** The master's position, given this part's position and the machine's facing. */
    public BlockPos masterFrom(BlockPos pos, Direction facing) {
        Facing f = Facing.valueOf(facing.name());
        return pos.offset(right ? -f.rightDx() : 0, upper ? -1 : 0, right ? -f.rightDz() : 0);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState state) {
        return of(state).masterFrom(pos, state.getValue(VendingMachineBlock.FACING));
    }

    /** A box around the whole machine and the items drawn just in front of its glass, for deciding whether it's in view. */
    public static AABB bounds(BlockPos master, Direction facing) {
        return new AABB(master).minmax(new AABB(UPPER_RIGHT.posFrom(master, facing))).inflate(0.1);
    }
}
