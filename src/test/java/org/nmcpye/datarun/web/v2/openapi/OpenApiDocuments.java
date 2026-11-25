package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** The checked-in v2 contract and the normal form both sides of a comparison are put in. */
final class OpenApiDocuments {

    /** The published v2 contract, relative to the project directory (Maven runs tests there). */
    static final Path SNAPSHOT = Path.of("docs", "api", "openapi-v2.json");

    /** Set to {@code true} to write the generated document over the snapshot (additive changes only). */
    static final String UPDATE = "openapi.v2.update";

    /** With {@link #UPDATE}, also accept breaking changes: a deliberate, reviewed decision. */
    static final String ACCEPT_BREAKING = "openapi.v2.acceptBreaking";

    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private OpenApiDocuments() {
    }

    static JsonNode read(String json) throws IOException {
        return normal(JSON.readTree(json));
    }

    static JsonNode readSnapshot() throws IOException {
        return Files.exists(SNAPSHOT) ? read(Files.readString(SNAPSHOT, StandardCharsets.UTF_8)) : null;
    }

    static void writeSnapshot(JsonNode document) throws IOException {
        Files.createDirectories(SNAPSHOT.getParent());
        Files.writeString(SNAPSHOT, JSON.writeValueAsString(normal(document)) + "\n", StandardCharsets.UTF_8);
    }

    /**
     * Object members sorted, {@code required} lists sorted, and {@code servers} left out: the server URL is the
     * deployment's, not the contract's.
     */
    static JsonNode normal(JsonNode node) {
        if (node.isObject()) {
            TreeMap<String, JsonNode> sorted = new TreeMap<>();
            node.fields().forEachRemaining(field -> sorted.put(field.getKey(), field.getValue()));
            sorted.remove("servers");
            ObjectNode out = JsonNodeFactory.instance.objectNode();
            sorted.forEach((name, value) -> out.set(name, name.equals("required") && value.isArray()
                ? sortedStrings(value) : normal(value)));
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = JsonNodeFactory.instance.arrayNode();
            node.forEach(element -> out.add(normal(element)));
            return out;
        }
        return node;
    }

    private static JsonNode sortedStrings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        values.sort(null);
        ArrayNode out = JsonNodeFactory.instance.arrayNode();
        values.forEach(out::add);
        return out;
    }
}
