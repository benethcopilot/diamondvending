package diamondvending.shop;

import diamondvending.Messages;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Flash;
import diamondvending.core.Texts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** The coin slot and the coin-return button (spec §3.4). Credit belongs to one player and is kept as the real items. */
public final class CoinSlot {
    private CoinSlot() {}

    /** Loads the whole held stack of money as this player's credit, up to 9 stacks; the rest stays in hand. */
    public static void insert(VendingMachineBlockEntity machine, Player player) {
        Level level = machine.getLevel();
        BlockPos pos = machine.getBlockPos();
        Currency currency = machine.currency();
        InteractionHand hand = handHoldingMoney(player, currency);
        if (hand == null) {
            MachineSounds.error(level, pos);
            Messages.actionBar(player, Component.translatable(Texts.WRONG_CURRENCY, currency.name()));
            machine.sendFlash(Flash.WRONG_COIN, 0);
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        ItemStack left = ItemSlots.insert(machine.creditOf(player.getUUID()), held);
        int accepted = held.getCount() - left.getCount();
        if (accepted > 0) {
            held.shrink(accepted);
            MachineSounds.credit(level, pos);
            machine.changed();
        }
        if (!left.isEmpty()) {
            if (accepted == 0) MachineSounds.error(level, pos);
            Messages.actionBar(player, Component.translatable(Texts.CREDIT_FULL));
            machine.sendFlash(Flash.CREDIT_FULL, 0);
        }
    }

    /** Gives this player back all of their credit, exactly as inserted; what doesn't fit drops at their feet. */
    public static void giveBack(VendingMachineBlockEntity machine, Player player) {
        List<ItemStack> credit = machine.takeCredit(player.getUUID());
        if (credit.isEmpty()) return;
        credit.forEach(stack -> PlayerItems.give(player, stack));
        MachineSounds.pickup(machine.getLevel(), machine.getBlockPos());
        machine.changed();
    }

    /** The main hand if it holds money, otherwise the offhand if that does; null if neither. */
    private static InteractionHand handHoldingMoney(Player player, Currency currency) {
        if (currency.matches(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (currency.matches(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }
}
