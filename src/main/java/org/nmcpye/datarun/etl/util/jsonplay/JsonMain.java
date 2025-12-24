package org.nmcpye.datarun.etl.util.jsonplay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nmcpye.datarun.etl.util.FlatJsonDescriptorBuilder;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class JsonMain {
    // ---------- Playground main demonstrating behaviors ----------
    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        String pointer = "/items/sku";
        String pointer2 = "/total";
        String pointer3 = "/items";
        JsonNode root = mapper.readTree(JsonSamples.SAMPLE_COMPLEX);


        var valueAt = root.at(pointer);
        var valueAt1 = FlatJsonDescriptorBuilder.build(root);
        var valueAt2 = root.at(pointer2);
        System.out.println("=== RAW TREE (pretty) ===\n" + JsonNodePlayground.JsonNodeUtils.pretty(root));

        // asText() on different node types
        System.out.println("asText on name: '" + root.get("name").asText() + "'");
        System.out.println("asText on age (number): '" + root.get("age").asText() + "'");
        System.out.println("asText on address (object): '" + root.get("address").asText() + "'  <-- empty string for container nodes");
        System.out.println("toString() on address: '" + root.get("address").toString() + "'  <-- JSON text of object\n");

        // get() vs path()
        System.out.println("root.get('missing') == null? " + (root.get("missing") == null));
        System.out.println("root.path('missing') isMissingNode? " + root.path("missing").isMissingNode());

        // safe chaining
        String city = root.path("address").path("city").asText("<no-city>");
        System.out.println("city via safe chain: " + city);

        // at() with JSON Pointer
        JsonNode phone0 = JsonNodePlayground.JsonNodeUtils.atPointer(root, "/phones/0");
        System.out.println("phones[0] via at(/phones/0): " + phone0.asText());
        JsonNode notFound = JsonNodePlayground.JsonNodeUtils.atPointer(root, "/phones/99");
        System.out.println("phones[99] isMissingNode: " + notFound.isMissingNode());

        // iteration over array
        JsonNode phones = root.get("phones");
        if (phones != null && phones.isArray()) {
            System.out.println("Iterate phones (for-each):");
            for (JsonNode p : phones) System.out.println("  - " + p.asText());
        }

        // iteration over object (values only) and fields()
        JsonNode cfg = root.get("config");
        System.out.println("config.isObject()? " + (cfg != null && cfg.isObject()));
        System.out.println("Iterate config values (for-each) - yields values only:");
        for (JsonNode v : cfg) System.out.println("  value: " + v.asText());
        System.out.println("Iterate config fields (key,value):");
        Iterator<Map.Entry<String, JsonNode>> fit = cfg.fields();
        while (fit.hasNext()) {
            Map.Entry<String, JsonNode> e = fit.next();
            System.out.println("  " + e.getKey() + " => " + e.getValue().asText());
        }

        // casting/mutating safely
        if (root.has("config") && root.get("config").isObject()) {
            ObjectNode configObj = (ObjectNode) root.get("config");
            configObj.put("mode", "slower");
            System.out.println("mutated config: " + configObj.toString());
        }

        // flattening example
        Map<String, String> flat = JsonNodePlayground.JsonNodeUtils.flatten(root);
        System.out.println("\nFlattened map:");
        flat.forEach((k, v) -> System.out.println("  " + k + " -> " + v));

        // demonstrate traversePath helper (path parts: "phones", "0")
        JsonNode viaParts = JsonNodePlayground.JsonNodeUtils.traversePath(root, "phones", "0");
        System.out.println("via traversePath(phones,0): " + viaParts.asText());

        // demonstrate wrong casts cause ClassCastException if you don't check
        try {
            System.out.println("Attempting bad cast: (ObjectNode) root.get('phones')");
            ObjectNode bad = (ObjectNode) root.get("phones");
            System.out.println(bad.toString());
        } catch (ClassCastException cce) {
            System.out.println("Caught ClassCastException when casting array to ObjectNode — check isObject() first");
        }

        // show MissingNode behaviour explicitly
        JsonNode miss = root.path("does_not_exist");
        System.out.println("MissingNode.getNodeType: " + miss.getNodeType());
        System.out.println("MissingNode.isMissingNode(): " + miss.isMissingNode());

        // final note: JSONPath is NOT provided by Jackson core; use a separate library if you need it
        System.out.println("\nNOTE: Jackson supports JSON Pointer (node.at('/a/0/b')) but does NOT implement JSONPath syntax. If you need JSONPath queries, use a dedicated library (e.g. jayway JsonPath).\n");

        System.out.println("=== END OF DEMO ===");
    }
}
