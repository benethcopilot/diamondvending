package diamondvending.block;

import diamondvending.core.Facing;
import diamondvending.core.Hit;
import diamondvending.core.MachineLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Works out what on the front a click hit (spec §2.2), from the exact click spot — like vanilla's chiseled bookshelf. */
public final class FrontFace {
    private FrontFace() {}

    /** What the click hit, or {@link Hit#NONE} if it wasn't on the machine's front face. */
    public static Hit hit(BlockState state, BlockPos pos, BlockHitResult click) {
        Direction facing = state.getValue(VendingMachineBlock.FACING);
        if (click.getDirection() != facing) return Hit.NONE;
        MachinePart part = MachinePart.of(state);
        Vec3 local = click.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        return MachineLayout.hitOnFront(Facing.valueOf(facing.name()), part.right(), part.upper(), local.x, local.y, local.z);
    }
}
