package org.nmcpye.datarun.etl.util.jsonplay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.util.*;
import java.util.stream.Collectors;

public class DescriptorAccess {

    /**
     * Resolves a JSON Pointer with wildcards (*) to a list of JsonNodes.
     * Supports objects, arrays, primitives. Returns empty list if nothing matches.
     */
    public static List<JsonNode> resolve(JsonNode root, String pointer) {
        if (root == null) root = MissingNode.getInstance();
        List<String> tokens = tokensFromPointer(pointer);
        List<JsonNode> out = new ArrayList<>();
        resolveRecursive(root, tokens, 0, out);
        return out;
    }

    // Split pointer into tokens: "/items/*/sku" -> ["items","*","sku"]
    private static List<String> tokensFromPointer(String pointer) {
        if (pointer == null || pointer.isEmpty() || pointer.equals("/")) return Collections.emptyList();
        String p = pointer.startsWith("/") ? pointer.substring(1) : pointer;
        if (p.isEmpty()) return Collections.emptyList();
        return Arrays.asList(p.split("/"));
    }

    // recursively resolve wildcard pointer to all matching JsonNodes
    public static List<JsonNode> resolveAll(JsonNode root, String descriptorPointer) {
        if (root == null) root = MissingNode.getInstance();
        List<String> tokens = tokensFromPointer(descriptorPointer);
        List<JsonNode> results = new ArrayList<>();
        resolveRecursive(root, tokens, 0, results);
        JsonNode node = results.get(0);
        return results;
    }

    private static void resolveRecursive(JsonNode node, List<String> tokens, int idx, List<JsonNode> out) {
        if (node == null || node.isMissingNode()) return;
        if (idx >= tokens.size()) {
            out.add(node);
            return;
        }
        String tok = tokens.get(idx);
        if ("*".equals(tok)) {
            // wildcard: if node is array, iterate elements; if object, iterate values; otherwise no match
            if (node.isArray()) {
                for (JsonNode el : node) resolveRecursive(el, tokens, idx + 1, out);
            } else if (node.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                    JsonNode child = it.next().getValue();
                    resolveRecursive(child, tokens, idx + 1, out);
                }
            } else {
                // wildcard on primitive -> no match
            }
        } else {
            // normal token: try array index if token is numeric, else field access
            if (node.isArray()) {
                // token might be an index e.g. "0"
                try {
                    int i = Integer.parseInt(tok);
                    JsonNode child = node.path(i); // safe (MissingNode) if out-of-range
                    if (!child.isMissingNode()) resolveRecursive(child, tokens, idx + 1, out);
                } catch (NumberFormatException ex) {
                    // can't index array by non-number token -> no match
                }
            } else if (node.isObject()) {
                JsonNode child = node.path(tok); // safe
                if (!child.isMissingNode()) resolveRecursive(child, tokens, idx + 1, out);
            } else {
                // primitive: can't go deeper
            }
        }
    }

    // Exists: whether any node matches the descriptor pointer
    public static boolean exists(JsonNode root, String descriptorPointer) {
        List<JsonNode> l = resolveAll(root, descriptorPointer);
        return !l.isEmpty();
    }

    // Count: if pointer points to an array node (like "/items") returns array.size()
    // If pointer contains wildcard ("/items/*/sku") returns number of matched nodes (elements matching the whole pointer)
    // Returns -1 when pointer resolves to nothing
    public static int count(JsonNode root, String descriptorPointer) {
        List<JsonNode> nodes = resolveAll(root, descriptorPointer);
        if (nodes.isEmpty()) return -1;
        // If the descriptorPointer resolves to one array node, return its size
        if (nodes.size() == 1 && nodes.get(0).isArray()) {
            return nodes.get(0).size();
        }
        // Otherwise return number of matched nodes
        return nodes.size();
    }

    // First: get first matching node (or null)
    public static JsonNode first(JsonNode root, String descriptorPointer) {
        List<JsonNode> l = resolveAll(root, descriptorPointer);
        return l.isEmpty() ? null : l.get(0);
    }

    // Samples: get up to n sample nodes (useful for sampling elements of an array)
    public static List<JsonNode> samples(JsonNode root, String descriptorPointer, int n) {
        List<JsonNode> all = resolveAll(root, descriptorPointer);
        if (all.isEmpty()) return Collections.emptyList();
        if (n >= all.size()) return all;
        return all.subList(0, n);
    }

    private static String nodeToCompactString(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return "null";
        if (n.isValueNode()) return n.asText();
        // container: return compact JSON string
        return n.toString();
    }

    // ----- flattenWithDescriptor -----
    // For each descriptor element:
    //   EXTRACT  -> extract first matching value (primitive) as text (or compact JSON for containers)
    //   KEEP_JSON -> keep container as compact JSON (if array-of-primitives, keep array as JSON)
    //   EXPLODE  -> do not explode into the flat map; instead provide count and samples for the array pointer.
    // The result maps pointer -> value (or pointer@count / pointer@sample for arrays).
    public static Map<String, String> flattenWithDescriptor2(JsonNode root,
                                                            List<FlatDescriptor.Element> elements,
                                                            int samplePerWildcard) {
        Map<String, String> out = new LinkedHashMap<>();
        for (FlatDescriptor.Element e : elements) {
            String ptr = e.pointer;
            List<JsonNode> matches = resolveAll(root, ptr);
            if (matches.isEmpty()) {
                out.put(ptr, null);
                continue;
            }

            // If descriptor says EXPLODE and pointer is the array base (no wildcard),
            // provide count and samples; exploding to rows is done by explodeToRows().
            if (e.access == FlatDescriptor.Access.EXPLODE && !ptr.contains("*")) {
                JsonNode arr = matches.get(0);
                if (arr.isArray()) {
                    out.put(ptr + "@count", String.valueOf(arr.size()));
                    // sample first element(s) as compact JSON
                    List<JsonNode> s = new ArrayList<>();
                    int limit = Math.min(samplePerWildcard, arr.size());
                    for (int i = 0; i < limit; i++) s.add(arr.get(i));
                    String joined = s.stream().map(DescriptorAccess::nodeToCompactString).collect(Collectors.joining("|"));
                    out.put(ptr + "@sample", joined);
                    continue;
                } else {
                    // pointer resolved but isn't array at runtime; fall back to number of matches
                    out.put(ptr + "@count", String.valueOf(matches.size()));
                    out.put(ptr + "@sample", matches.stream().map(DescriptorAccess::nodeToCompactString).collect(Collectors.joining("|")));
                    continue;
                }
            }

            // KEEP_JSON: keep the compact JSON of the first match (if array-of-primitives, keeps the array JSON)
            if (e.access == FlatDescriptor.Access.KEEP_JSON) {
                JsonNode val = matches.get(0);
                out.put(ptr, nodeToCompactString(val));
                continue;
            }

            // EXTRACT: pick first match; for value nodes, return asText(); for containers return compact JSON
            if (e.access == FlatDescriptor.Access.EXTRACT) {
                JsonNode val = matches.get(0);
                if (val.isValueNode()) out.put(ptr, val.asText());
                else out.put(ptr, nodeToCompactString(val));
                continue;
            }

            // default fallback: join matches
            out.put(ptr, matches.stream().map(DescriptorAccess::nodeToCompactString).collect(Collectors.joining("|")));
        }
        return out;
    }

    // ----- explodeToRows -----
    // Given an array pointer (e.g. "/items") and a set of child pointers that point into elements
    // (e.g. "/items/*/sku", "/items/*/qty"), produce a list of maps (one map per element)
    // where keys are the child pointers (you can choose to store relative names instead).
    //
    // Child pointers MUST include the wildcard '*' at the same array level ("/items/*/...").
    public static List<Map<String, String>> explodeToRows(JsonNode root,
                                                          String arrayPointer,
                                                          List<String> childPointers,
                                                          int sampleLimitPerElement) {
        // find the array node at arrayPointer
        List<JsonNode> arrMatches = resolveAll(root, arrayPointer);
        if (arrMatches.isEmpty()) return Collections.emptyList();
        JsonNode arr = arrMatches.get(0);
        if (!arr.isArray()) return Collections.emptyList();

        ArrayNode arrayNode = (ArrayNode) arr;
        int n = arrayNode.size();
        List<Map<String, String>> rows = new ArrayList<>(n);

        // precompute tokens after wildcard for each child pointer
        Map<String, List<String>> childTailTokens = new LinkedHashMap<>();
        for (String childPtr : childPointers) {
            List<String> tokens = tokensFromPointer(childPtr);
            int starIndex = tokens.indexOf("*");
            if (starIndex < 0) {
                // child pointer didn't contain wildcard — try to resolve relative by trimming arrayPointer tokens
                // fallback: compute tokens after arrayPointer tokens
                List<String> base = tokensFromPointer(arrayPointer);
                if (tokens.size() >= base.size()) {
                    List<String> tail = tokens.subList(base.size(), tokens.size());
                    childTailTokens.put(childPtr, new ArrayList<>(tail));
                } else {
                    childTailTokens.put(childPtr, Collections.emptyList());
                }
            } else {
                // tail tokens after the '*' (these are applied to each element node)
                childTailTokens.put(childPtr, tokens.subList(starIndex + 1, tokens.size()));
            }
        }

        // for each element, build a row map
        for (int i = 0; i < n; i++) {
            JsonNode element = arrayNode.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : childTailTokens.entrySet()) {
                String childPtr = entry.getKey();
                List<String> tail = entry.getValue();
                JsonNode value = resolveRelative(element, tail);
                String outVal = (value == null || value.isMissingNode()) ? null : nodeToCompactString(value);
                row.put(childPtr, outVal);
            }
            rows.add(row);
            // optional sampling guard
            if (rows.size() >= sampleLimitPerElement && sampleLimitPerElement > 0) break;
        }
        return rows;
    }

    // resolve a sequence of tokens relative to a node (no wildcards expected here)
    private static JsonNode resolveRelative(JsonNode base, List<String> tokens) {
        JsonNode cur = base;
        for (String t : tokens) {
            if (cur == null || cur.isMissingNode()) return MissingNode.getInstance();
            if (cur.isArray()) {
                try {
                    int idx = Integer.parseInt(t);
                    cur = cur.path(idx);
                } catch (NumberFormatException ex) {
                    // can't index array with non-int -> missing
                    return MissingNode.getInstance();
                }
            } else if (cur.isObject()) {
                cur = cur.path(t);
            } else {
                return MissingNode.getInstance();
            }
        }
        return cur == null ? MissingNode.getInstance() : cur;
    }
}
