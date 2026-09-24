package diamondvending.scene;

import diamondvending.Messages;
import diamondvending.block.VendingMachineBlockEntity;
import diamondvending.core.Hit;
import diamondvending.core.Problem;
import diamondvending.core.Texts;
import diamondvending.shop.Currency;
import diamondvending.shop.ItemSlots;
import diamondvending.shop.Selection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The tooltip next to the crosshair when looking at a machine's front (spec §2.3), built from what the client knows. */
public final class HoverText {
    private HoverText() {}

    /** One tooltip line: its words, an optional icon drawn before them, and whether it's red. */
    public record Line(Component text, ItemStack icon, boolean alarm) {
        static Line of(Component text) {
            return new Line(text, ItemStack.EMPTY, false);
        }
    }

    /** Every problem's explanation first (spec §3.5 b), then what the crosshair is on, then who owns the machine. */
    public static List<Line> lines(VendingMachineBlockEntity machine, Hit hit, UUID viewer) {
        List<Line> lines = new ArrayList<>();
        Currency currency = machine.currency();
        ItemStack coin = new ItemStack(currency.displayItem());
        for (Problem problem : machine.syncedProblems()) {
            lines.add(new Line(Messages.explanation(problem, currency), ItemStack.EMPTY, true));
        }
        switch (hit.region()) {
            case BUTTON -> {
                Selection selection = machine.getSelection(hit.button());
                if (!selection.isSetUp()) {
                    lines.add(Line.of(Component.translatable(Texts.HUD_NOTHING)));
                } else {
                    lines.add(new Line(Component.translatable(Texts.HUD_ITEM, selection.template().getHoverName(), selection.quantity()),
                            selection.template(), false));
                    lines.add(selection.price() == 0
                            ? Line.of(Component.translatable(Texts.HUD_FREE))
                            : new Line(currency.money(selection.price()), coin, false));
                    if (!machine.isInfinite() && machine.syncedStockCount(hit.button()) < selection.quantity()) {
                        lines.add(new Line(Component.translatable(Texts.HUD_SOLD_OUT), ItemStack.EMPTY, true));
                    }
                }
            }
            case COIN_SLOT -> {
                lines.add(new Line(Component.translatable(Texts.HUD_INSERT, currency.name()), coin, false));
                lines.add(Line.of(Component.translatable(Texts.HUD_YOUR_CREDIT, currency.money(machine.syncedCredit(viewer)))));
            }
            case COIN_RETURN -> lines.add(Line.of(Component.translatable(Texts.HUD_RETURN_CREDIT, machine.syncedCredit(viewer))));
            case TRAY -> lines.add(Line.of(Component.translatable(Texts.HUD_TAKE_ITEMS, ItemSlots.count(machine.tray(), stack -> true))));
            default -> {
            }
        }
        boolean shopMachine = machine.isInfinite() || machine.getOwner() == null;
        lines.add(Line.of(shopMachine
                ? Component.translatable(Texts.HUD_SHOP_MACHINE)
                : Component.translatable(Texts.HUD_OWNED_BY, machine.getOwnerName())));
        return lines;
    }
}
