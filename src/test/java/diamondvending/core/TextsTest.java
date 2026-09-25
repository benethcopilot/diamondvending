package diamondvending.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;

/** Every key a player can be shown must have English text (spec §10: lang completeness). */
class TextsTest {
    private static final Path LANG = Path.of(System.getProperty("diamondvending.root", "../.."),
            "src/main/resources/assets/diamondvending/lang/en_us.json");
    private static final Pattern ENTRY = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    static Map<String, String> english() throws IOException {
        Map<String, String> entries = new HashMap<>();
        Matcher matcher = ENTRY.matcher(Files.readString(LANG));
        while (matcher.find()) entries.put(matcher.group(1), matcher.group(2));
        return entries;
    }

    @Test
    void everyKeyHasEnglishText() throws IOException {
        Map<String, String> english = english();
        for (String key : Texts.all()) {
            assertFalse(english.getOrDefault(key, "").isBlank(), "en_us.json has no text for " + key);
        }
    }
}
