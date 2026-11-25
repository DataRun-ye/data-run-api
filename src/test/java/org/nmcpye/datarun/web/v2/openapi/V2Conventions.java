package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The v2 conventions that show in the published document, checked so every later module keeps them: paths,
 * names and spellings (vocabulary study rules R1, R2, R3, R9, R10), opaque ids and never a uid, a problem answer
 * on every operation, and the bearer token everywhere.
 */
final class V2Conventions {

    private static final Pattern LITERAL = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final Pattern ACTION = Pattern.compile("(\\{[a-z][a-zA-Z0-9]*}|[a-z0-9]+(-[a-z0-9]+)*):[a-z][a-zA-Z0-9]*");
    private static final Pattern VARIABLE = Pattern.compile("\\{[a-z][a-zA-Z0-9]*}");
    private static final Pattern CAMEL = Pattern.compile("[a-z][a-zA-Z0-9]*");
    private static final Pattern UPPER_SNAKE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");
    private static final Pattern UID = Pattern.compile("uids?|.*(Uid|UID)s?");
    private static final Pattern ID = Pattern.compile("id|.*Id");
    private static final Pattern IDS = Pattern.compile("ids|.*Ids");
    private static final String ID_REF = "#/components/schemas/Id";
    private static final String PROBLEM_REF = "#/components/responses/Problem";

    private V2Conventions() {
    }

    static List<String> violations(JsonNode document) {
        List<String> found = new ArrayList<>();
        JsonNode security = document.path("security");
        if (!security.isArray() || security.isEmpty() || !security.get(0).has("bearerAuth")) {
            found.add("document: the bearer token is not required globally");
        }
        document.path("paths").fields().forEachRemaining(path -> {
            String route = path.getKey();
            if (!route.startsWith("/api/v2/")) {
                found.add(route + ": not under /api/v2/");
            }
            for (String segment : route.substring(1).split("/")) {
                if (!LITERAL.matcher(segment).matches() && !ACTION.matcher(segment).matches()
                    && !VARIABLE.matcher(segment).matches()) {
                    found.add(route + ": segment " + segment + " is not kebab-case, {camelCase} or an action :verb");
                }
            }
            path.getValue().fields().forEachRemaining(method -> operation(route, method, found));
        });
        document.path("components").path("schemas").fields()
            .forEachRemaining(schema -> schema(schema.getKey(), schema.getValue(), "Problem".equals(schema.getKey()), found));
        uuidFormats(document, "document", found);
        return found;
    }

    private static void operation(String route, Map.Entry<String, JsonNode> method, List<String> found) {
        String at = method.getKey().toUpperCase() + " " + route;
        if (!List.of("get", "post", "delete").contains(method.getKey())) {
            found.add(at + ": v2 reads with GET and changes by named actions (POST), never " + method.getKey().toUpperCase());
        }
        JsonNode operation = method.getValue();
        if (!PROBLEM_REF.equals(operation.path("responses").path("default").path("$ref").asText())) {
            found.add(at + ": no default problem response");
        }
        if (operation.has("security")) {
            found.add(at + ": overrides the global security requirement");
        }
        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asText();
            String in = parameter.path("in").asText();
            if ("header".equals(in)) {
                continue;
            }
            if (!CAMEL.matcher(name).matches()) {
                found.add(at + ": parameter " + name + " is not camelCase");
            }
            reference(at + " parameter", name, parameter.path("schema"), found);
        }
    }

    private static void schema(String at, JsonNode schema, boolean problem, List<String> found) {
        schema.path("properties").fields().forEachRemaining(property -> {
            String name = property.getKey();
            if (!CAMEL.matcher(name).matches()) {
                found.add(at + "." + name + ": member is not camelCase");
            }
            if ("status".equals(name) && !problem) {
                found.add(at + ".status: a bare status; name the state it is");
            }
            reference(at, name, property.getValue(), found);
            schema(at + "." + name, property.getValue(), false, found);
        });
        if (schema.has("items")) {
            schema(at + "[]", schema.get("items"), false, found);
        }
        for (JsonNode value : schema.path("enum")) {
            if (value.isTextual() && !UPPER_SNAKE.matcher(value.asText()).matches()) {
                found.add(at + ": enum value " + value.asText() + " is not UPPER_SNAKE");
            }
        }
        if (!at.equals("Problem.code")) {
            for (JsonNode value : schema.path("x-extensible-enum")) {
                if (value.isTextual() && !UPPER_SNAKE.matcher(value.asText()).matches()) {
                    found.add(at + ": enum value " + value.asText() + " is not UPPER_SNAKE");
                }
            }
        }
    }

    /** Ids are the opaque Id schema, never a uid. */
    private static void reference(String at, String name, JsonNode schema, List<String> found) {
        if (UID.matcher(name).matches()) {
            found.add(at + "." + name + ": v2 never carries a uid");
        } else if (IDS.matcher(name).matches()) {
            if (!ID_REF.equals(schema.path("items").path("$ref").asText())) {
                found.add(at + "." + name + ": ids are an array of " + ID_REF);
            }
        } else if (ID.matcher(name).matches() && !ID_REF.equals(schema.path("$ref").asText())) {
            found.add(at + "." + name + ": an id is " + ID_REF);
        }
    }

    /** No uuid format anywhere but the Idempotency-Key header: ids are opaque. */
    private static void uuidFormats(JsonNode node, String at, List<String> found) {
        if (node.isObject()) {
            if ("header".equals(node.path("in").asText()) && "Idempotency-Key".equals(node.path("name").asText())) {
                return;
            }
            if ("uuid".equals(node.path("format").asText())) {
                found.add(at + ": format uuid; v2 ids are opaque strings");
            }
            node.fields().forEachRemaining(field -> uuidFormats(field.getValue(), at + "/" + field.getKey(), found));
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                uuidFormats(node.get(i), at + "/" + i, found);
            }
        }
    }
}
