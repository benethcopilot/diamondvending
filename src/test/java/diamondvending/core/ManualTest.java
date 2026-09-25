package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spec §6.2's coverage rule: every rule a player could trip on is explained in the manual. Each rule in the spec's list
 * is pinned to the words that explain it; if a page stops saying them, or a new rule joins the list without a line in
 * the book, this fails.
 */
class ManualTest {
    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("buttons are numbered 1-12 and the price tag's number is the button", List.of("1 to 12", "price tag"));
        RULES.put("payment uses credit first, then the inventory, not shulker boxes or bundles",
                List.of("credit first", "shulker boxes or bundles"));
        RULES.put("credit is only yours, coin return gives it back, and there is a limit",
                List.of("Only you can spend it", "Coin return gives it back", "9 stacks"));
        RULES.put("anyone can take from the tray", List.of("Anyone can take it"));
        RULES.put("what each warning means and who fixes it", List.of("NOT SET UP YET: the owner", "SOLD OUT: the owner",
                "TRAY FULL: anyone", "CASH BOX FULL: the owner", "CATALOG MISSING: ask an admin"));
        RULES.put("to open setup, empty both hands, then sneak + right-click", List.of("Empty BOTH hands", "sneak and right-click"));
        RULES.put("only the owner or an admin can set up, dye or break the machine",
                List.of("Only you or an admin can set up, dye or break"));
        RULES.put("stock sells out; a full cash box stops sales until emptied", List.of("sold out", "stops selling", "Withdraw all"));
        RULES.put("stock counts only the exact item the button sells (a worn or enchanted copy is different)",
                List.of("exact same item"));
        RULES.put("breaking keeps the setup on the item, but contents pop out",
                List.of("keeps its items and prices", "Stock, Cash Box, tray and credit all pop out"));
        RULES.put("dye the machine by right-clicking it with a dye", List.of("Right-click it with any dye"));
        RULES.put("infinite machines never run out and the money vanishes", List.of("never run out", "money disappears"));
        RULES.put("only admins can change or break an infinite machine, even its owner can't",
                List.of("Only admins can change or break them", "Not even the owner"));
        RULES.put("switching to infinite needs empty Stock and Cash Box", List.of("empty Stock and Cash Box first"));
        RULES.put("an infinite machine stays infinite only when an admin places it", List.of("stays infinite only if an admin places it"));
        RULES.put("catalogs and the currency slot", List.of("pick a catalog", "Currency slot"));
    }

    @Test
    void everyRuleIsInTheManual() throws IOException {
        String manual = manual();
        RULES.forEach((rule, words) -> {
            for (String phrase : words) {
                assertTrue(manual.contains(phrase), "spec §6.2: the manual must explain that " + rule
                        + ", but no page says \"" + phrase + "\"");
            }
        });
    }

    /** Every page's English title and text, in page order. */
    private static String manual() throws IOException {
        Map<String, String> english = TextsTest.english();
        StringBuilder text = new StringBuilder();
        for (String page : Texts.MANUAL_PAGES) {
            text.append(unescape(english.getOrDefault(Texts.manualTitle(page), ""))).append('\n');
            text.append(unescape(english.getOrDefault(Texts.manualText(page), ""))).append('\n');
        }
        return text.toString();
    }

    /** en_us.json values as TextsTest reads them still hold JSON escapes such as \n; the manual's words don't. */
    private static String unescape(String json) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                out.append(next == 'n' ? '\n' : next);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
