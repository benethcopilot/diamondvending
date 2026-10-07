package diamondvending.art;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/** Just enough of Minecraft's NBT format to write structure files: compounds, lists, ints and strings. */
final class NbtWriter {
    private static final byte TAG_END = 0;
    private static final byte TAG_INT = 3;
    private static final byte TAG_STRING = 8;
    private static final byte TAG_LIST = 9;
    private static final byte TAG_COMPOUND = 10;

    private NbtWriter() {}

    /** An insertion-ordered compound, so output bytes are deterministic. */
    static Map<String, Object> compound() {
        return new LinkedHashMap<>();
    }

    /** Uncompressed NBT for a root compound with an empty name. */
    static byte[] toBytes(Map<String, Object> root) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");
            writeCompoundBody(out, root);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /** Gzip-compressed NBT — the format of {@code .nbt} structure files. */
    static byte[] toGzip(Map<String, Object> root) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(toBytes(root));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private static void writeCompoundBody(DataOutputStream out, Map<String, Object> compound) throws IOException {
        for (Map.Entry<String, Object> entry : compound.entrySet()) {
            out.writeByte(tagOf(entry.getValue()));
            out.writeUTF(entry.getKey());
            writePayload(out, entry.getValue());
        }
        out.writeByte(TAG_END);
    }

    @SuppressWarnings("unchecked")
    private static void writePayload(DataOutputStream out, Object value) throws IOException {
        if (value instanceof Integer i) {
            out.writeInt(i);
        } else if (value instanceof String s) {
            out.writeUTF(s);
        } else if (value instanceof List<?> list) {
            out.writeByte(list.isEmpty() ? TAG_END : tagOf(list.get(0)));
            out.writeInt(list.size());
            for (Object element : list) writePayload(out, element);
        } else if (value instanceof Map<?, ?> map) {
            writeCompoundBody(out, (Map<String, Object>) map);
        } else {
            throw new IllegalArgumentException("unsupported NBT value: " + value);
        }
    }

    private static byte tagOf(Object value) {
        if (value instanceof Integer) return TAG_INT;
        if (value instanceof String) return TAG_STRING;
        if (value instanceof List<?>) return TAG_LIST;
        if (value instanceof Map<?, ?>) return TAG_COMPOUND;
        throw new IllegalArgumentException("unsupported NBT value: " + value);
    }
}
