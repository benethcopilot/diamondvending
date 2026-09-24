package diamondvending.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** A registered menu type for a block, and how to open it (each loader sends the block's position its own way). */
public interface MenuHandle<M extends AbstractContainerMenu> {
    MenuType<M> type();

    /** Opens the menu for {@code player}; the client's menu gets {@code pos} to find the block. */
    void open(ServerPlayer player, Component title, BlockPos pos);
}
