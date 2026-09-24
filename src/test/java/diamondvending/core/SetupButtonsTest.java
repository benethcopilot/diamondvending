package diamondvending.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The setup screen's button numbers: every button decodes to what it asks for, and nothing else decodes at all. */
class SetupButtonsTest {
    @Test
    void everyButtonDecodesToWhatItAsksFor() {
        for (SetupTab tab : SetupTab.values()) assertEquals(new SetupButtons.ShowTab(tab), SetupButtons.decode(SetupButtons.showTab(tab)));
        assertEquals(new SetupButtons.Withdraw(), SetupButtons.decode(SetupButtons.withdraw()));
        assertEquals(new SetupButtons.ToggleInfinite(), SetupButtons.decode(SetupButtons.toggleInfinite()));
        assertEquals(new SetupButtons.CycleCatalog(-1), SetupButtons.decode(SetupButtons.cycleCatalog(-1)));
        assertEquals(new SetupButtons.CycleCatalog(1), SetupButtons.decode(SetupButtons.cycleCatalog(1)));
        for (int index = 0; index < MachineLayout.SELECTIONS; index++) {
            assertEquals(new SetupButtons.Clear(index), SetupButtons.decode(SetupButtons.clear(index)));
            assertEquals(new SetupButtons.ChangeQuantity(index, -1), SetupButtons.decode(SetupButtons.fewer(index)));
            assertEquals(new SetupButtons.ChangeQuantity(index, 1), SetupButtons.decode(SetupButtons.more(index)));
            for (int price : new int[] {0, 1, 3, SetupButtons.MAX_PRICE}) {
                assertEquals(new SetupButtons.SetPrice(index, price), SetupButtons.decode(SetupButtons.price(index, price)));
            }
        }
    }

    @Test
    void numbersNoButtonSendsMeanNothing() {
        for (int id : new int[] {-1, 4, 9, 14, 99, 112, 212, 312, 9_999, 22_000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            assertNull(SetupButtons.decode(id), "button number " + id);
        }
    }

    @Test
    void askingForSomethingOutOfRangeIsABug() {
        assertThrows(IllegalArgumentException.class, () -> SetupButtons.price(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> SetupButtons.price(12, 5));
        assertThrows(IllegalArgumentException.class, () -> SetupButtons.clear(-1));
    }
}
