package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.fail;

/**
 * Approval-style golden files for A2. A scenario document (requests, responses, database snapshots) is normalized,
 * then compared with {@code src/test/resources/golden/a2/<scenario>.json}. With {@code GOLDEN_RECORD=true} in the
 * environment (or {@code -Dgolden.record=true}) the file is written instead. On a mismatch the received document is
 * written to {@code target/golden-received/a2/} for review.
 * <p>
 * Normalization: values random per database are replaced by named placeholders; ULIDs the server made (not present
 * in any request) become {@code <ulid:n>} in order of first appearance, so equal ids stay equal; object keys are
 * sorted. Server clock times and serial numbers are masked when the snapshot is taken.
 */
final class A2Golden {

    private static final Path GOLDEN = Paths.get("src", "test", "resources", "golden", "a2");
    private static final Path RECEIVED = Paths.get("target", "golden-received", "a2");
    private static final Pattern ULID = Pattern.compile("[0-9A-HJKMNP-TV-Z]{26}");

    private static final ObjectMapper JSON = new ObjectMapper();

    private A2Golden() {
    }

    static boolean recording() {
        return "true".equalsIgnoreCase(System.getenv("GOLDEN_RECORD")) || Boolean.getBoolean("golden.record");
    }

    static void verify(String scenario, ObjectNode document, Set<String> requestValues, Map<String, String> placeholders) {
        JsonNode normalized = normalize(document, requestValues, placeholders);
        String text = write(normalized);
        Path golden = GOLDEN.resolve(scenario + ".json");
        try {
            if (recording()) {
                Files.createDirectories(golden.getParent());
                Files.writeString(golden, text, StandardCharsets.UTF_8);
                return;
            }
            if (!Files.exists(golden)) {
                writeReceived(scenario, text);
                fail("No golden file " + golden + "; run with GOLDEN_RECORD=true and review it");
            }
            JsonNode expected = JSON.readTree(Files.readString(golden, StandardCharsets.UTF_8));
            JsonNode actual = JSON.readTree(text);
            if (!expected.equals(actual)) {
                Path received = writeReceived(scenario, text);
                fail("Golden mismatch for " + scenario + " at " + firstDifference("$", expected, actual)
                    + "; received document: " + received);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Path writeReceived(String scenario, String text) throws IOException {
        Path received = RECEIVED.resolve(scenario + ".json");
        Files.createDirectories(received.getParent());
        Files.writeString(received, text, StandardCharsets.UTF_8);
        return received;
    }

    static JsonNode normalize(JsonNode document, Set<String> requestValues, Map<String, String> placeholders) {
        Map<String, String> ulids = new LinkedHashMap<>();
        return sortAndMask(document, requestValues, placeholders, ulids);
    }

    private static JsonNode sortAndMask(JsonNode node, Set<String> requestValues, Map<String, String> placeholders,
                                        Map<String, String> ulids) {
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            for (String name : names) {
                sorted.set(mask(name, requestValues, placeholders, ulids),
                    sortAndMask(node.get(name), requestValues, placeholders, ulids));
            }
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            node.forEach(element -> array.add(sortAndMask(element, requestValues, placeholders, ulids)));
            return array;
        }
        if (node.isTextual()) {
            return TextNode.valueOf(mask(node.asText(), requestValues, placeholders, ulids));
        }
        return node;
    }

    private static String mask(String value, Set<String> requestValues, Map<String, String> placeholders,
                               Map<String, String> ulids) {
        String masked = value;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            if (masked.contains(placeholder.getKey())) {
                masked = masked.replace(placeholder.getKey(), placeholder.getValue());
            }
        }
        if (ULID.matcher(masked).matches() && !requestValues.contains(masked)) {
            masked = ulids.computeIfAbsent(masked, ulid -> "<ulid:" + (ulids.size() + 1) + ">");
        }
        return masked;
    }

    private static String write(JsonNode node) {
        try {
            DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"))
                .withArrayIndenter(new DefaultIndenter("  ", "\n"));
            return JSON.writer(printer).writeValueAsString(node) + "\n";
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String firstDifference(String path, JsonNode expected, JsonNode actual) {
        if (expected.getNodeType() != actual.getNodeType()) {
            return path + " (expected " + expected + ", got " + actual + ")";
        }
        if (expected.isObject()) {
            Iterator<String> names = expected.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!actual.has(name)) {
                    return path + "." + name + " (missing)";
                }
                if (!expected.get(name).equals(actual.get(name))) {
                    return firstDifference(path + "." + name, expected.get(name), actual.get(name));
                }
            }
            Iterator<String> actualNames = actual.fieldNames();
            while (actualNames.hasNext()) {
                String name = actualNames.next();
                if (!expected.has(name)) {
                    return path + "." + name + " (unexpected)";
                }
            }
        }
        if (expected.isArray()) {
            if (expected.size() != actual.size()) {
                return path + " (expected " + expected.size() + " elements, got " + actual.size() + ")";
            }
            for (int i = 0; i < expected.size(); i++) {
                if (!expected.get(i).equals(actual.get(i))) {
                    return firstDifference(path + "[" + i + "]", expected.get(i), actual.get(i));
                }
            }
        }
        return path + " (expected " + expected + ", got " + actual + ")";
    }
}
