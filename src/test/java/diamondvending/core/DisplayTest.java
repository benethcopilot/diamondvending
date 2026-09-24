package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTest {
    @Test
    void idleSaysSelectItem() {
        assertEquals(new Display.Line(List.of(Texts.SELECT_ITEM), 0, false), Display.line(List.of(), null, 0, 0, 0));
    }

    @Test
    void aViewerWithCreditSeesTheirCredit() {
        assertEquals(new Display.Line(List.of(Texts.CREDIT), 4, false), Display.line(List.of(), null, 0, 0, 4));
    }

    @Test
    void everyProblemShowsInRed() {
        Display.Line line = Display.line(List.of(Problem.CASH_BOX_FULL, Problem.TRAY_FULL), null, 0, 0, 4);
        assertEquals(List.of(Texts.problemDisplay(Problem.CASH_BOX_FULL), Texts.problemDisplay(Problem.TRAY_FULL)), line.keys());
        assertTrue(line.alarm());
    }

    @Test
    void aFlashWinsForTwoSeconds() {
        List<Problem> problems = List.of(Problem.TRAY_FULL);
        assertEquals(new Display.Line(List.of(Texts.flash(Flash.NEED_MONEY)), 3, true),
                Display.line(problems, Flash.NEED_MONEY, 3, Display.FLASH_TICKS - 1, 0));
        assertEquals(List.of(Texts.problemDisplay(Problem.TRAY_FULL)),
                Display.line(problems, Flash.NEED_MONEY, 3, Display.FLASH_TICKS, 0).keys());
    }

    @Test
    void thankYouIsGreen() {
        assertFalse(Display.line(List.of(), Flash.THANK_YOU, 0, 0, 0).alarm());
    }

    @Test
    void everyRefusalHasAFlash() {
        for (DenyReason reason : DenyReason.values()) assertNotNull(Flash.of(reason));
        assertEquals(Flash.NEED_MONEY, Flash.of(DenyReason.NOT_ENOUGH_MONEY));
    }

    @Test
    void shortTextStaysStill() {
        assertEquals("NEED 3", Display.window("NEED 3", 0));
        assertEquals("NEED 3", Display.window("NEED 3", 999));
    }

    @Test
    void longTextScrollsAndLoops() {
        String text = "SELECT ITEM"; // 11 characters + a 3-space gap = 14 per loop
        assertEquals("SELECT ", Display.window(text, 0));
        assertEquals("ELECT I", Display.window(text, Display.TICKS_PER_CHARACTER));
        assertEquals("M   SEL", Display.window(text, 10L * Display.TICKS_PER_CHARACTER));
        assertEquals("SELECT ", Display.window(text, 14L * Display.TICKS_PER_CHARACTER));
    }

    @Test
    void theLampBlinksOnlyWhileSomethingIsWrong() {
        assertFalse(Display.lampLit(false, 0));
        assertTrue(Display.lampLit(true, 0));
        assertFalse(Display.lampLit(true, Display.BLINK_TICKS));
        assertTrue(Display.lampLit(true, 2L * Display.BLINK_TICKS));
    }
}
