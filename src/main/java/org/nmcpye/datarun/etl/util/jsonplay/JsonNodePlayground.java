package org.nmcpye.datarun.etl.util.jsonplay;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Single-file playground + utility helpers for exploring JsonNode (Jackson).
 * <p>
 * Save as: JsonNodePlayground.java
 * Compile (requires jackson-databind on classpath):
 * javac -cp jackson-databind-2.15.2.jar JsonNodePlayground.java
 * Run:
 * java -cp .:jackson-databind-2.15.2.jar:jackson-core-2.15.2.jar:jackson-annotations-2.15.2.jar JsonNodePlayground
 * <p>
 * (Adjust versions/classpath for your environment.)
 */
public class JsonNodePlayground {

    // ---------- Utility helpers --------------
    public static class JsonNodeUtils {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        // Safe text getter: uses path() internally so it never NPEs.
        public static String getText(JsonNode root, String... pathParts) {
            JsonNode n = traversePath(root, pathParts);
            // if container (object/array) asText() returns "" — use toString() if you want JSON
            if (n == null || n.isMissingNode() || n.isNull()) return "";
            if (n.isContainerNode()) return n.toString(); // explicit: return JSON for object/array
            return n.asText();
        }

        // Safe int getter with default
        public static int getInt(JsonNode root, int defaultValue, String... pathParts) {
            JsonNode n = traversePath(root, pathParts);
            if (n == null || n.isMissingNode() || n.isNull()) return defaultValue;
            return n.asInt(defaultValue); // asInt won't throw
        }

        // Safe boolean getter
        public static boolean getBoolean(JsonNode root, boolean defaultValue, String... pathParts) {
            JsonNode n = traversePath(root, pathParts);
            if (n == null || n.isMissingNode() || n.isNull()) return defaultValue;
            return n.asBoolean(defaultValue);
        }

        // Returns the node located by JSON Pointer (e.g. "/address/city/0").
        // Returns MissingNode.instance() if not found — never null.
        public static JsonNode atPointer(JsonNode root, String jsonPointer) {
            if (root == null) return MissingNode.getInstance();
            try {
                JsonPointer p = JsonPointer.compile(jsonPointer);
                return root.at(p);
            } catch (IllegalArgumentException e) {
                // invalid pointer
                return MissingNode.getInstance();
            }
        }

        // traverse using simple pathParts (each element is a single field name OR an array index in string form)
        // This implements the "root.path("a").path("b")" behavior programmatically.
        public static JsonNode traversePath(JsonNode root, String... parts) {
            if (root == null) return MissingNode.getInstance();
            JsonNode cur = root;
            for (String p : parts) {
                if (cur == null) return MissingNode.getInstance();
                if (cur.isArray()) {
                    // try to parse index
                    try {
                        int idx = Integer.parseInt(p);
                        cur = cur.path(idx); // path returns MissingNode for out-of-range
                    } catch (NumberFormatException ex) {
                        // asked for a field name on an array -> Missing
                        return MissingNode.getInstance();
                    }
                } else {
                    cur = cur.path(p); // safe
                }
            }
            return cur == null ? MissingNode.getInstance() : cur;
        }

        // Checkers and safe casts
        public static boolean isObject(JsonNode n) {
            return n != null && n.isObject();
        }

        public static boolean isArray(JsonNode n) {
            return n != null && n.isArray();
        }

        public static Optional<ObjectNode> asObject(JsonNode n) {
            if (isObject(n)) return Optional.of((ObjectNode) n);
            return Optional.empty();
        }

        public static Optional<ArrayNode> asArray(JsonNode n) {
            if (isArray(n)) return Optional.of((ArrayNode) n);
            return Optional.empty();
        }

        // Flatten node to map path -> stringValue. Example keys: address.city, phones[0].number
        public static Map<String, String> flatten(JsonNode node) {
            Map<String, String> out = new LinkedHashMap<>();
            flattenRecursive(node, "", out);
            return out;
        }

        private static void flattenRecursive(JsonNode node, String prefix, Map<String, String> out) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                out.put(prefix, null);
                return;
            }
            if (node.isValueNode()) {
                out.put(prefix, node.asText());
                return;
            }
            if (node.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = node.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    String key = e.getKey();
                    String nextPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                    flattenRecursive(e.getValue(), nextPrefix, out);
                }
                return;
            }
            if (node.isArray()) {
                int idx = 0;
                for (JsonNode el : node) {
                    String nextPrefix = prefix + "[" + idx + "]";
                    flattenRecursive(el, nextPrefix, out);
                    idx++;
                }
                return;
            }
            // fallback
            out.put(prefix, node.toString());
        }

        // Pretty dump helper
        public static String pretty(JsonNode n) {
            try {
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(n);
            } catch (Exception e) {
                return n.toString();
            }
        }
    }
}
