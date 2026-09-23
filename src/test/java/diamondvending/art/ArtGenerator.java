package diamondvending.art;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates every derived file (textures, models, test structures) from the machine's layout.
 * Run with {@code ./gradlew :26.1-neoforge:generateArt}; {@link GeneratedFilesTest} fails if the committed files drift.
 */
public final class ArtGenerator {
    public enum Kind { PNG, JSON, NBT }

    /** One generated file: its path relative to the repository root, and its content. */
    public record Output(String path, Kind kind, byte[] bytes) {}

    private static final String ASSETS = "src/main/resources/assets/diamondvending/";

    private ArtGenerator() {}

    public static List<Output> generateAll() {
        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output("src/gametest/resources/data/diamondvending/structure/gametest_platform.nbt",
                Kind.NBT, NbtWriter.toGzip(Structures.gametestPlatform())));

        for (Dye dye : Dye.values()) {
            outputs.add(png(ASSETS + "textures/block/vending_machine_front_" + dye.id() + ".png", Textures.front(dye)));
            outputs.add(png(ASSETS + "textures/block/vending_machine_side_" + dye.id() + ".png", Textures.side(dye)));
            outputs.add(png(ASSETS + "textures/item/vending_machine_" + dye.id() + ".png", Textures.item(dye)));
            outputs.add(json(ASSETS + "models/item/vending_machine_" + dye.id() + ".json", Models.itemModel(dye)));
            for (Models.Part part : Models.Part.values()) {
                outputs.add(json(ASSETS + "models/" + Models.partModelName(dye, part) + ".json", Models.partModel(dye, part)));
            }
        }
        outputs.add(json(ASSETS + "blockstates/vending_machine.json", Models.blockstate()));
        outputs.add(json("src/main/resources-26.1/assets/diamondvending/items/vending_machine.json", Models.itemDefinition26()));
        outputs.add(json("src/main/resources-1.21.1/assets/diamondvending/models/item/vending_machine.json", Models.itemModel1211()));
        outputs.add(png(ASSETS + "icon.png", Textures.icon()));
        outputs.add(png("docs/images/machine-colors.png", Textures.preview()));
        return outputs;
    }

    private static Output json(String path, Object value) {
        return new Output(path, Kind.JSON, Json.write(value).getBytes(UTF_8));
    }

    private static Output png(String path, BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Output(path, Kind.PNG, bytes.toByteArray());
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : ".");
        List<Output> outputs = generateAll();
        for (Output output : outputs) {
            Path file = root.resolve(output.path());
            Files.createDirectories(file.getParent());
            Files.write(file, output.bytes());
        }
        System.out.println("Wrote " + outputs.size() + " generated files under " + root.toAbsolutePath());
    }
}
