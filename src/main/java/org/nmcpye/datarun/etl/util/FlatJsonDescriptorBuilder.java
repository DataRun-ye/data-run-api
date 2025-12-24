package org.nmcpye.datarun.etl.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.util.*;

public class FlatJsonDescriptorBuilder {

    private static final int MAX_FIELD_SAMPLE = 5;   // sample up to N items of any array
    private static final int MAX_VISIT = 5000;       // safety

    public static FlatJsonDescriptor build(JsonNode root) {
        FlatJsonDescriptor out = new FlatJsonDescriptor();
        Deque<String> path = new ArrayDeque<>(); // store tokens (not pointer string)
        int[] counter = new int[]{0};

        walk(root, path, out, counter);

        return out;
    }

    private static void walk(JsonNode node, Deque<String> path, FlatJsonDescriptor out, int[] counter) {
        if (node == null) node = MissingNode.getInstance();
        if (counter[0]++ > MAX_VISIT) return;

        String pointer = toPointer(path);
        FlatJsonDescriptor.ElementDescriptor ed = new FlatJsonDescriptor.ElementDescriptor();
        ed.pointer = pointer;
        ed.parentPointer = parentPointer(path);
        ed.sampleTypes = detectTypes(node);
        ed.optional = false; // this entry exists because we're visiting it
        ed.nullable = node.isNull();
        ed.sampleValue = node.isValueNode() ? trim(node.asText(null)) : null;

        if (node.isArray()) {
            ed.kind = FlatJsonDescriptor.ElementDescriptor.NodeKind.ARRAY;
            ed.arraySampleSize = node.size();
            ed.elementKind = detectArrayElementKind((ArrayNode) node);
            ed.access = (ed.elementKind == FlatJsonDescriptor.ElementDescriptor.ElementKind.OBJECT)
                ? FlatJsonDescriptor.ElementDescriptor.Access.EXPLODE
                : FlatJsonDescriptor.ElementDescriptor.Access.KEEP_JSON;
            out.elements.add(ed);

            // sample elements (first N) and emit descriptors for them as separate entries
            int i = 0;
            for (JsonNode el : node) {
                if (i >= MAX_FIELD_SAMPLE) break;
                path.addLast(String.valueOf(i));
                walk(el, path, out, counter);
                path.removeLast();
                i++;
            }
            return;
        }

        if (node.isObject()) {
            ed.kind = FlatJsonDescriptor.ElementDescriptor.NodeKind.OBJECT;
            ed.elementKind = FlatJsonDescriptor.ElementDescriptor.ElementKind.UNKNOWN;
            ed.access = FlatJsonDescriptor.ElementDescriptor.Access.KEEP_JSON;
            out.elements.add(ed);

            // visit fields (no nested descriptors; each field becomes its own flat entry)
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                path.addLast(e.getKey());
                walk(e.getValue(), path, out, counter);
                path.removeLast();
            }
            return;
        }

        if (node.isValueNode()) {
            ed.kind = FlatJsonDescriptor.ElementDescriptor.NodeKind.PRIMITIVE;
            ed.arraySampleSize = node.size();
            ed.elementKind = FlatJsonDescriptor.ElementDescriptor.ElementKind.PRIMITIVE;
            ed.access = FlatJsonDescriptor.ElementDescriptor.Access.EXTRACT_VALUE;
            out.elements.add(ed);

            // sample elements (first N) and emit descriptors for them as separate entries
            int i = 0;
            for (JsonNode el : node) {
                if (i >= MAX_FIELD_SAMPLE) break;
                path.addLast(String.valueOf(i));
                walk(el, path, out, counter);
                path.removeLast();
                i++;
            }
            return;
        }
        // value node (string/number/boolean/null)
        ed.kind = FlatJsonDescriptor.ElementDescriptor.NodeKind.PRIMITIVE;
        ed.elementKind = FlatJsonDescriptor.ElementDescriptor.ElementKind.UNKNOWN;
        ed.access = FlatJsonDescriptor.ElementDescriptor.Access.EXTRACT_VALUE;
        out.elements.add(ed);
    }

    private static String parentPointer(Deque<String> path) {
        if (path.isEmpty()) return "";
        // parent pointer = pointer without last token
        Iterator<String> it = path.iterator();
        List<String> toks = new ArrayList<>();
        while (it.hasNext()) toks.add(it.next());
        if (toks.size() <= 1) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < toks.size() - 1; i++) sb.append("/").append(escape(toks.get(i)));
        return sb.toString();
    }

    private static String toPointer(Deque<String> path) {
        if (path.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String t : path) sb.append("/").append(escape(t));
        return sb.toString();
    }

    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    private static Set<FlatJsonDescriptor.ElementDescriptor.FieldType> detectTypes(JsonNode n) {
        Set<FlatJsonDescriptor.ElementDescriptor.FieldType> s = new LinkedHashSet<>();
        if (n.isObject()) s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.OBJECT);
        else if (n.isArray()) s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.ARRAY);
        else if (n.isTextual()) s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.STRING);
        else if (n.isNumber()) s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.NUMBER);
        else if (n.isBoolean()) s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.BOOLEAN);
        else if (n.isNull()) s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.NULL);
        else s.add(FlatJsonDescriptor.ElementDescriptor.FieldType.MIXED);
        return s;
    }

    private static FlatJsonDescriptor.ElementDescriptor.ElementKind detectArrayElementKind(ArrayNode arr) {
        boolean sawObj = false, sawPrim = false;
        for (JsonNode el : arr) {
            if (el.isObject()) sawObj = true;
            else sawPrim = true;
            if (sawObj && sawPrim) return FlatJsonDescriptor.ElementDescriptor.ElementKind.MIXED;
        }
        return sawObj ? FlatJsonDescriptor.ElementDescriptor.ElementKind.OBJECT : FlatJsonDescriptor.ElementDescriptor.ElementKind.PRIMITIVE;
    }

    private static String trim(String s) {
        if (s == null) return null;
        if (s.length() > 200) return s.substring(0, 200);
        return s;
    }
}
