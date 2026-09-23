package diamondvending.art;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelsTest {
    /** Model references: blockstate variants, item definitions and overrides. */
    private static final Pattern MODEL_REF = Pattern.compile("\"model\": \"diamondvending:([a-z_/]+)\"");
    /** Texture references: the texture variables used by our models. */
    private static final Pattern TEXTURE_REF = Pattern.compile("\"(?:particle|front|side|layer0)\": \"diamondvending:([a-z_/]+)\"");

    private static Set<String> generatedPaths() {
        Set<String> paths = new HashSet<>();
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) paths.add(output.path());
        return paths;
    }

    private static String textOf(String path) {
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) {
            if (output.path().equals(path)) return new String(output.bytes(), StandardCharsets.UTF_8);
        }
        throw new AssertionError("not generated: " + path);
    }

    @Test
    void blockstateHasAVariantForEveryStateCombination() {
        String blockstate = textOf("src/main/resources/assets/diamondvending/blockstates/vending_machine.json");
        int variants = blockstate.split("\"color=").length - 1;
        assertEquals(16 * 4 * 2 * 2, variants);
    }

    @Test
    void everyReferencedModelAndTextureIsGenerated() {
        Set<String> paths = generatedPaths();
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) {
            if (output.kind() != ArtGenerator.Kind.JSON) continue;
            String json = new String(output.bytes(), StandardCharsets.UTF_8);
            Matcher models = MODEL_REF.matcher(json);
            while (models.find()) {
                String file = "src/main/resources/assets/diamondvending/models/" + models.group(1) + ".json";
                assertTrue(paths.contains(file), output.path() + " references missing model " + models.group(1));
            }
            Matcher textures = TEXTURE_REF.matcher(json);
            while (textures.find()) {
                String file = "src/main/resources/assets/diamondvending/textures/" + textures.group(1) + ".png";
                assertTrue(paths.contains(file), output.path() + " references missing texture " + textures.group(1));
            }
        }
    }

    @Test
    void versionSpecificItemModelsCoverEveryColor() {
        String modern = textOf("src/main/resources-26.1/assets/diamondvending/items/vending_machine.json");
        String legacy = textOf("src/main/resources-1.21.1/assets/diamondvending/models/item/vending_machine.json");
        for (Dye dye : Dye.values()) {
            assertTrue(modern.contains("\"when\": \"" + dye.id() + "\""), "26.1 item definition misses " + dye.id());
            assertTrue(legacy.contains("\"custom_model_data\": " + (dye.ordinal() + 1)), "1.21.1 item model misses " + dye.id());
        }
    }
}
