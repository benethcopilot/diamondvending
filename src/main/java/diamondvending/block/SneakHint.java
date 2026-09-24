package diamondvending.block;

import diamondvending.Messages;
import diamondvending.core.Texts;
import diamondvending.registry.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Spec §3.2 rule 1: the game skips the block when a player sneak-right-clicks it holding something, and places or uses
 * the item instead. From the machine's owner or an admin that's surely a try at opening setup, so the click is cancelled
 * and they're told how. Everyone else gets vanilla's behaviour. Called from each loader's "right-click a block" event,
 * on both sides (so the client doesn't place the block for a moment either).
 */
public final class SneakHint {
    private SneakHint() {}

    /** Whether to cancel this click; on the server, the player is told why. */
    public static boolean cancels(Player player, Level level, BlockHitResult hit) {
        if (!player.isSecondaryUseActive()) return false;
        if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) return false;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.is(ModContent.VENDING_MACHINE.get())) return false;
        if (!MachineAccess.canManage(player, VendingMachineBlock.ownerOf(level, hit.getBlockPos(), state))) return false;
        if (!level.isClientSide()) Messages.actionBar(player, Component.translatable(Texts.EMPTY_HANDS));
        return true;
    }
}
