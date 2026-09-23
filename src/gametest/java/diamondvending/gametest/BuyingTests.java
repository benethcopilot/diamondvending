package diamondvending.gametest;

import diamondvending.shop.Currency;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.function.Consumer;

/**
 * In-game tests for buying (Plan 3). Each test also needs a method in {@code fabric/FabricBuyingTests} and (for 1.21.1)
 * {@code neoforge/NeoForgeBuyingTests}; NeoForge 26.1 registers {@link AllTests#ALL}.
 *
 * <p>Machines here are placed by mock players facing south, so they face north: the front is the machine's north side
 * and its right-hand column is at x − 1 (see {@link MachineTests}).
 */
public final class BuyingTests {
    /** Every test by snake_case name. */
    public static final Map<String, Consumer<GameTestHelper>> ALL = Map.ofEntries(
            Map.entry("diamonds_are_the_default_currency", BuyingTests::diamondsAreTheDefaultCurrency),
            Map.entry("money_reads_naturally", BuyingTests::moneyReadsNaturally));

    private BuyingTests() {}

    // ---- helpers -------------------------------------------------------------------------------------------------

    /** The component's translation, after checking its key. */
    static TranslatableContents translation(GameTestHelper helper, Component component, String key) {
        helper.assertTrue(component.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected the text " + key + " but got " + component);
        return (TranslatableContents) component.getContents();
    }

    // ---- currency ------------------------------------------------------------------------------------------------

    public static void diamondsAreTheDefaultCurrency(GameTestHelper helper) {
        Currency currency = Currency.DEFAULT;
        ItemStack renamed = new ItemStack(Items.DIAMOND);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Lucky"));
        helper.assertTrue(currency.matches(new ItemStack(Items.DIAMOND, 5)), "diamonds should be money");
        helper.assertTrue(currency.matches(renamed), "a renamed diamond is still a diamond");
        helper.assertFalse(currency.matches(new ItemStack(Items.EMERALD)), "emeralds are not money unless a datapack says so");
        helper.assertFalse(currency.matches(ItemStack.EMPTY), "an empty hand is not money");
        helper.assertTrue(currency.displayItem() == Items.DIAMOND, "prices should show a diamond");
        helper.succeed();
    }

    public static void moneyReadsNaturally(GameTestHelper helper) {
        TranslatableContents one = translation(helper, Currency.DEFAULT.money(1), "currency.diamondvending.minecraft.diamond.one");
        TranslatableContents three = translation(helper, Currency.DEFAULT.money(3), "currency.diamondvending.minecraft.diamond.many");
        helper.assertTrue(one.getArgs()[0].equals(1) && three.getArgs()[0].equals(3), "the amount should be the first argument");
        helper.assertTrue("%s × %s".equals(three.getFallback()), "unnamed currencies should read like \"3 × Emerald\"");
        translation(helper, Currency.DEFAULT.name(), "currency.diamondvending.minecraft.diamond.name");
        helper.succeed();
    }
}
