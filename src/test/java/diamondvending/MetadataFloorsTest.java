package diamondvending;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader versions players need are chosen on purpose, not copied from the versions we build against —
 * otherwise every dependency bump would lock out modpacks pinned to slightly older loaders.
 */
class MetadataFloorsTest {
    private static final Path ROOT = Path.of(System.getProperty("diamondvending.root", "../.."));
    private static final String MOD_ID = "diamondvending";
    private static final String GLOBAL = "";
    private static final Pattern SECTION = Pattern.compile("^\\[(?:(fabric|neoforge|forge)\\.)?\"([^\"]+)\"]$");
    private static final Pattern ENTRY = Pattern.compile("^([a-z_.]+)\\s*=\\s*\"([^\"]*)\"");

    /** Section name ("fabric.1.21.1", or "" for the top) → key → value, from stonecutter.properties.toml. */
    private static Map<String, Map<String, String>> sections() throws IOException {
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        Map<String, String> current = new LinkedHashMap<>();
        sections.put(GLOBAL, current);
        for (String line : Files.readAllLines(ROOT.resolve("stonecutter.properties.toml"))) {
            String trimmed = line.trim();
            Matcher section = SECTION.matcher(trimmed);
            if (section.matches()) {
                current = new LinkedHashMap<>();
                String name = section.group(1) == null ? section.group(2) : section.group(1) + "." + section.group(2);
                sections.put(name, current);
            } else {
                Matcher entry = ENTRY.matcher(trimmed);
                if (entry.find()) current.put(entry.group(1), entry.group(2));
            }
        }
        return sections;
    }

    /** What a loader section sees: its own values over the top-level ones. */
    private static Map<String, String> resolved(Map<String, Map<String, String>> sections, String name) {
        Map<String, String> values = new HashMap<>(sections.get(GLOBAL));
        values.putAll(sections.get(name));
        return values;
    }

    /** Compares dotted numeric versions, ignoring any "+build" suffix. */
    private static int compare(String a, String b) {
        String[] x = a.split("\\+")[0].split("\\.");
        String[] y = b.split("\\+")[0].split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int xi = i < x.length ? Integer.parseInt(x[i]) : 0;
            int yi = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (xi != yi) return Integer.compare(xi, yi);
        }
        return 0;
    }

    private static void assertFloor(String section, Map<String, String> values, String floorKey, String buildKey) {
        String floor = values.get(floorKey);
        String build = values.get(buildKey);
        assertNotNull(floor, section + " needs a hand-chosen " + floorKey);
        assertNotNull(build, section + " is missing " + buildKey);
        assertTrue(compare(floor, build) <= 0, section + ": " + floorKey + " " + floor + " is above the build version " + build);
    }

    /** Our own copy of a metadata file on the test classpath (loader jars ship files with the same name). */
    private static String ourMetadata(String path, String marker) throws IOException {
        Enumeration<URL> urls = MetadataFloorsTest.class.getClassLoader().getResources(path);
        while (urls.hasMoreElements()) {
            try (InputStream in = urls.nextElement().openStream()) {
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (text.contains(marker)) return text;
            }
        }
        throw new AssertionError("no " + path + " for " + MOD_ID + " on the test classpath");
    }

    @Test
    void everyLoaderSectionDeclaresFloorsNoHigherThanItsBuildVersions() throws IOException {
        Map<String, Map<String, String>> sections = sections();
        int checked = 0;
        for (String name : sections.keySet()) {
            Map<String, String> values = resolved(sections, name);
            if (name.startsWith("fabric.")) {
                assertFloor(name, values, "deps.fabric_api_min", "deps.fabric_api");
                assertFloor(name, values, "deps.fabric_loader_min", "deps.fabric_loader");
                checked++;
            } else if (name.startsWith("neoforge.")) {
                assertFloor(name, values, "deps.neo_loader_min", "deps.neo_loader");
                checked++;
            } else if (name.startsWith("forge.")) {
                assertFloor(name, values, "deps.forge_loader_min", "deps.forge_loader");
                checked++;
            }
        }
        assertTrue(checked >= 5, "expected a section for every node, found " + sections.keySet());
    }

    @Test
    void theBuiltModAsksForTheFloors() throws IOException {
        String node = System.getProperty("diamondvending.node");
        assertNotNull(node, "run through Gradle, which sets diamondvending.node (e.g. 26.1-neoforge)");
        int dash = node.lastIndexOf('-');
        String loader = node.substring(dash + 1);
        Map<String, String> values = resolved(sections(), loader + "." + node.substring(0, dash));
        if (loader.equals("fabric")) {
            String json = ourMetadata("fabric.mod.json", "\"id\": \"" + MOD_ID + "\"");
            assertTrue(json.contains("\"fabricloader\": \">=" + values.get("deps.fabric_loader_min") + "\""), json);
            assertTrue(json.contains("\"fabric-api\": \">=" + values.get("deps.fabric_api_min") + "\""), json);
        } else if (loader.equals("forge")) {
            String toml = ourMetadata("META-INF/mods.toml", "modId = \"" + MOD_ID + "\"");
            assertTrue(toml.contains("versionRange = \"[" + values.get("deps.forge_loader_min") + ",)\""), toml);
        } else {
            String toml = ourMetadata("META-INF/neoforge.mods.toml", "modId = \"" + MOD_ID + "\"");
            assertTrue(toml.contains("versionRange = \"[" + values.get("deps.neo_loader_min") + ",)\""), toml);
        }
    }
}
