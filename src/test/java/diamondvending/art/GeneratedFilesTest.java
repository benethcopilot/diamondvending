package diamondvending.art;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fails when a generated file is missing or no longer matches the generator (i.e. someone edited it by hand or changed MachineLayout). */
class GeneratedFilesTest {
    private static final Path ROOT = Path.of(System.getProperty("diamondvending.root", "../.."));

    @Test
    void committedFilesMatchTheGenerator() throws IOException {
        List<String> stale = new ArrayList<>();
        for (ArtGenerator.Output output : ArtGenerator.generateAll()) {
            Path file = ROOT.resolve(output.path());
            if (!Files.exists(file) || !sameContent(output, Files.readAllBytes(file))) {
                stale.add(output.path());
            }
        }
        assertTrue(stale.isEmpty(), "Generated files are missing or stale. Run `./gradlew :26.1-neoforge:generateArt` and commit:\n"
                + String.join("\n", stale));
    }

    private static boolean sameContent(ArtGenerator.Output output, byte[] actual) throws IOException {
        return switch (output.kind()) {
            case JSON -> new String(output.bytes(), UTF_8).equals(new String(actual, UTF_8).replace("\r\n", "\n"));
            case NBT -> Arrays.equals(gunzip(output.bytes()), gunzip(actual));
            case PNG -> samePixels(output.bytes(), actual);
        };
    }

    private static byte[] gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return in.readAllBytes();
        }
    }

    private static boolean samePixels(byte[] expected, byte[] actual) throws IOException {
        BufferedImage a = ImageIO.read(new ByteArrayInputStream(expected));
        BufferedImage b = ImageIO.read(new ByteArrayInputStream(actual));
        if (b == null || a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) return false;
            }
        }
        return true;
    }
}
