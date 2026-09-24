package diamondvending.core;

import java.util.ArrayList;
import java.util.List;

/**
 * What the LED display and the warning lamp show (spec §3.5). Pure rules on translation keys and tick counts; the
 * client turns the keys into words and draws them.
 */
public final class Display {
    /** How long a flash (THANK YOU, NEED 3, …) stays up: about 2 seconds. */
    public static final int FLASH_TICKS = 40;
    /** Characters that fit on the display at once; longer text scrolls. */
    public static final int WINDOW = 7;
    /** Scroll speed: one character every this many ticks. */
    public static final int TICKS_PER_CHARACTER = 3;
    /** The warning lamp is on for this many ticks, then off for as many. */
    public static final int BLINK_TICKS = 10;
    /** Space between problems, and before scrolling text starts over. */
    public static final String GAP = "   ";

    private Display() {}

    /** One thing the display says: translation keys shown one after another, their number argument, and whether it's red. */
    public record Line(List<String> keys, int number, boolean alarm) {}

    /**
     * What the display says now (spec §3.5): a flash from the last {@link #FLASH_TICKS} wins; then every active
     * problem, in red; then the viewing player's credit; otherwise "SELECT ITEM".
     *
     * @param flash           the last flash, or null if there hasn't been one
     * @param ticksSinceFlash ticks since that flash
     * @param credit          the viewing player's credit
     */
    public static Line line(List<Problem> problems, Flash flash, int flashNumber, long ticksSinceFlash, int credit) {
        if (flashing(flash, ticksSinceFlash)) {
            return new Line(List.of(Texts.flash(flash)), flashNumber, flash.alarm());
        }
        if (!problems.isEmpty()) {
            List<String> keys = new ArrayList<>();
            for (Problem problem : problems) keys.add(Texts.problemDisplay(problem));
            return new Line(List.copyOf(keys), 0, true);
        }
        if (credit > 0) return new Line(List.of(Texts.CREDIT), credit, false);
        return new Line(List.of(Texts.SELECT_ITEM), 0, false);
    }

    /** Whether a flash from {@code ticksSinceFlash} ago is still up (null: there hasn't been one). */
    public static boolean flashing(Flash flash, long ticksSinceFlash) {
        return flash != null && ticksSinceFlash >= 0 && ticksSinceFlash < FLASH_TICKS;
    }

    /** The part of {@code text} on the display now: text that fits stays still; longer text scrolls left and loops, like an LED sign. */
    public static String window(String text, long ticks) {
        if (text.length() <= WINDOW) return text;
        String loop = text + GAP;
        int start = (int) Math.floorMod(ticks / TICKS_PER_CHARACTER, (long) loop.length());
        return (loop + loop).substring(start, start + WINDOW);
    }

    /** Whether the warning lamp is lit now: it blinks while any problem is active (spec §3.5 b). */
    public static boolean lampLit(boolean anyProblem, long ticks) {
        return anyProblem && Math.floorMod(ticks / BLINK_TICKS, 2L) == 0;
    }
}
