package diamondvending.art;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A tiny deterministic JSON writer (2-space indent, keys in insertion order, short scalar lists inline). */
final class Json {
    private Json() {}

    /** An insertion-ordered object from alternating keys and values. */
    static Map<String, Object> obj(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value, 0);
        return out.append('\n').toString();
    }

    private static void write(StringBuilder out, Object value, int depth) {
        if (value instanceof String s) out.append('"').append(s.replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        else if (value instanceof Number || value instanceof Boolean) out.append(value);
        else if (value instanceof List<?> list) writeList(out, list, depth);
        else if (value instanceof Map<?, ?> map) writeMap(out, map, depth);
        else throw new IllegalArgumentException("unsupported JSON value: " + value);
    }

    private static void writeList(StringBuilder out, List<?> list, int depth) {
        boolean inline = list.stream().allMatch(e -> e instanceof Number || e instanceof String);
        if (list.isEmpty() || inline) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(", ");
                write(out, list.get(i), depth);
            }
            out.append(']');
            return;
        }
        out.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            indent(out, depth + 1);
            write(out, list.get(i), depth + 1);
            out.append(i < list.size() - 1 ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append(']');
    }

    private static void writeMap(StringBuilder out, Map<?, ?> map, int depth) {
        out.append("{\n");
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            indent(out, depth + 1);
            out.append('"').append(entry.getKey()).append("\": ");
            write(out, entry.getValue(), depth + 1);
            out.append(++i < map.size() ? ",\n" : "\n");
        }
        indent(out, depth);
        out.append('}');
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(depth));
    }
}
