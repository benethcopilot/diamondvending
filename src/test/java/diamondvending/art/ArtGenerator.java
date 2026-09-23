package diamondvending.art;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates every derived file (textures, models, test structures) from the machine's layout.
 * Run with {@code ./gradlew :26.1-neoforge:generateArt}; {@link GeneratedFilesTest} fails if the committed files drift.
 */
public final class ArtGenerator {
    public enum Kind { PNG, JSON, NBT }

    /** One generated file: its path relative to the repository root, and its content. */
    public record Output(String path, Kind kind, byte[] bytes) {}

    private ArtGenerator() {}

    public static List<Output> generateAll() {
        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output("src/gametest/resources/data/diamondvending/structure/gametest_platform.nbt",
                Kind.NBT, NbtWriter.toGzip(Structures.gametestPlatform())));
        return outputs;
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
