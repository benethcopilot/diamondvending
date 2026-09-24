package diamondvending.shop;

import diamondvending.Messages;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.DenyReason;
import diamondvending.core.Flash;
import diamondvending.core.Problem;
import diamondvending.core.PurchaseDecision;
import diamondvending.core.PurchaseInput;
import diamondvending.core.PurchaseRules;
import diamondvending.core.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Pressing a button: the all-or-nothing purchase (spec §3.3). */
public final class Purchase {
    private Purchase() {}

    /** Buys button {@code index} (0–11) for the player, or tells them exactly why not. */
    public static void pressButton(VendingMachineBlockEntity machine, Player player, int index) {
        MachineSounds.button(machine.getLevel(), machine.getBlockPos());
        Selection selection = machine.getSelection(index);
        Currency currency = machine.currency();
        List<ItemStack> credit = machine.credit(player.getUUID());
        List<ItemStack> wallet = PlayerItems.paySlots(player);
        int inCredit = ItemSlots.count(credit, currency::matches);
        int inWallet = ItemSlots.count(wallet, currency::matches);
        int price = selection.price();

        // Work out the exact stacks that would be paid, on copies: the cash box must have room for those.
        int fromCredit = Math.min(inCredit, price);
        int fromWallet = Math.min(inWallet, price - fromCredit);
        List<ItemStack> payment = new ArrayList<>(ItemSlots.take(ItemSlots.copyOf(credit), currency::matches, fromCredit));
        payment.addAll(ItemSlots.take(ItemSlots.copyOf(wallet), currency::matches, fromWallet));

        PurchaseInput input = new PurchaseInput(
                machine.catalogMissing(),
                selection.isSetUp(),
                machine.isInfinite(),
                selection.quantity(),
                machine.stockCountFor(index),
                ItemSlots.fitsAll(machine.tray(), List.of(selection.template())),
                ItemSlots.fitsAll(machine.cashBox(), payment),
                price,
                inCredit,
                inWallet);
        switch (PurchaseRules.decide(input)) {
            case PurchaseDecision.Approved approved -> complete(machine, player, index, selection, approved);
            case PurchaseDecision.Denied denied -> refuse(machine, player, index, denied.reason(), inCredit + inWallet);
        }
    }

    private static void complete(VendingMachineBlockEntity machine, Player player, int index, Selection selection, PurchaseDecision.Approved approved) {
        Currency currency = machine.currency();
        List<ItemStack> paid = new ArrayList<>(ItemSlots.take(machine.credit(player.getUUID()), currency::matches, approved.fromCredit()));
        paid.addAll(ItemSlots.take(PlayerItems.paySlots(player), currency::matches, approved.fromInventory()));
        player.getInventory().setChanged();
        if (!machine.isInfinite()) {
            // Owned: the money goes in the cash box and the goods come out of stock. Infinite: the money is destroyed.
            paid.forEach(stack -> ItemSlots.insert(machine.cashBox(), stack));
            ItemSlots.take(machine.stock(), selection::sells, selection.quantity());
        }
        ItemSlots.insert(machine.tray(), selection.template());
        MachineSounds.vend(machine.getLevel(), machine.getBlockPos());
        MachineSounds.thankYou(machine.getLevel(), machine.getBlockPos());
        machine.changed();
        machine.sendVend(index);
    }

    /** Spec §3.5 c: tells the buyer what's wrong and who can fix it, with an error buzz. */
    private static void refuse(VendingMachineBlockEntity machine, Player player, int index, DenyReason reason, int funds) {
        Currency currency = machine.currency();
        int button = index + 1;
        Component message = switch (reason) {
            case CATALOG_MISSING -> Messages.explanation(Problem.CATALOG_MISSING, currency);
            case EMPTY -> Component.translatable(Texts.BUTTON_EMPTY, button);
            case SOLD_OUT -> Component.translatable(Texts.SOLD_OUT, button);
            case TRAY_FULL -> Messages.explanation(Problem.TRAY_FULL, currency);
            case CASH_BOX_FULL -> Messages.explanation(Problem.CASH_BOX_FULL, currency);
            case NOT_ENOUGH_MONEY -> Component.translatable(Texts.NEED_MONEY, button,
                    currency.money(machine.getSelection(index).price()), funds);
        };
        MachineSounds.error(machine.getLevel(), machine.getBlockPos());
        Messages.actionBar(player, message);
        machine.sendFlash(Flash.of(reason), index);
    }
}
