package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compares two OpenAPI 3.0 documents and classifies every difference a client could notice as breaking or
 * additive, by v2's evolution rule: inside v2 only additive change is allowed (new operations, optional parameters
 * and members, new response members, new enumeration values, which clients ignore), so anything else is breaking.
 * <p>
 * Schemas are compared by direction. What a client sends (parameters, request bodies) may only be accepted more
 * widely; what it receives (responses) may only grow. References are followed, cycles are cut, and anything this
 * comparison does not model in a schema is breaking until a person looks: a false alarm costs a snapshot update, a
 * missed break costs a client.
 */
final class OpenApiCompatibility {

    record Change(String location, String what, boolean breaking) {
        @Override
        public String toString() {
            return (breaking ? "BREAKING " : "additive ") + location + ": " + what;
        }
    }

    private enum Direction { REQUEST, RESPONSE }

    private static final List<String> METHODS = List.of("get", "put", "post", "delete", "options", "head", "patch",
        "trace");

    /** Schema keys this comparison reads, or which change nothing on the wire. */
    private static final Set<String> MODELED = Set.of("$ref", "type", "format", "enum", "x-extensible-enum",
        "properties", "required", "items", "allOf", "oneOf", "anyOf", "nullable", "additionalProperties",
        "maxLength", "minLength", "maximum", "minimum", "maxItems", "minItems", "pattern", "default",
        "discriminator", "description", "title", "example", "examples", "deprecated", "externalDocs");

    private final JsonNode baseline;
    private final JsonNode current;
    private final List<Change> changes = new ArrayList<>();
    private final Set<String> compared = new HashSet<>();

    private OpenApiCompatibility(JsonNode baseline, JsonNode current) {
        this.baseline = baseline;
        this.current = current;
    }

    static List<Change> compare(JsonNode baseline, JsonNode current) {
        OpenApiCompatibility comparison = new OpenApiCompatibility(baseline, current);
        comparison.paths();
        comparison.security();
        return comparison.changes;
    }

    private void breaking(String location, String what) {
        changes.add(new Change(location, what, true));
    }

    private void additive(String location, String what) {
        changes.add(new Change(location, what, false));
    }

    private void paths() {
        JsonNode before = baseline.path("paths");
        JsonNode after = current.path("paths");
        for (String path : names(before)) {
            for (String method : METHODS) {
                JsonNode old = before.path(path).get(method);
                if (old == null) {
                    continue;
                }
                JsonNode now = after.path(path).get(method);
                String location = method.toUpperCase() + " " + path;
                if (now == null) {
                    breaking(location, "operation removed");
                } else {
                    operation(location, old, now);
                }
            }
        }
        for (String path : names(after)) {
            for (String method : METHODS) {
                if (after.path(path).has(method) && !before.path(path).has(method)) {
                    additive(method.toUpperCase() + " " + path, "operation added");
                }
            }
        }
    }

    private void operation(String at, JsonNode old, JsonNode now) {
        if (!Objects.equals(old.path("operationId").asText(null), now.path("operationId").asText(null))) {
            additive(at, "operationId changed (generated clients rename the call)");
        }
        parameters(at, old.path("parameters"), now.path("parameters"));
        requestBody(at, old.get("requestBody"), now.get("requestBody"));
        responses(at, old.path("responses"), now.path("responses"));
        if (!old.has("security") && now.has("security") || old.has("security") && now.has("security")
            && !old.get("security").equals(now.get("security"))) {
            breaking(at, "security requirement changed");
        }
        if (!old.path("deprecated").asBoolean(false) && now.path("deprecated").asBoolean(false)) {
            additive(at, "deprecated");
        }
    }

    private void parameters(String at, JsonNode old, JsonNode now) {
        Map<String, JsonNode> before = new java.util.LinkedHashMap<>();
        Map<String, JsonNode> after = new java.util.LinkedHashMap<>();
        for (JsonNode parameter : old) {
            JsonNode resolved = resolve(baseline, parameter);
            before.put(resolved.path("in").asText() + " " + resolved.path("name").asText(), resolved);
        }
        for (JsonNode parameter : now) {
            JsonNode resolved = resolve(current, parameter);
            after.put(resolved.path("in").asText() + " " + resolved.path("name").asText(), resolved);
        }
        before.forEach((key, parameter) -> {
            String location = at + " parameter " + key;
            JsonNode next = after.get(key);
            if (next == null) {
                breaking(location, "parameter removed");
                return;
            }
            if (!parameter.path("required").asBoolean(false) && next.path("required").asBoolean(false)) {
                breaking(location, "parameter became required");
            }
            for (String style : List.of("style", "explode", "allowEmptyValue")) {
                if (!Objects.equals(parameter.get(style), next.get(style))) {
                    breaking(location, style + " changed");
                }
            }
            schema(location, parameter.get("schema"), next.get("schema"), Direction.REQUEST);
        });
        after.forEach((key, parameter) -> {
            if (!before.containsKey(key)) {
                if (parameter.path("required").asBoolean(false)) {
                    breaking(at + " parameter " + key, "required parameter added");
                } else {
                    additive(at + " parameter " + key, "optional parameter added");
                }
            }
        });
    }

    private void requestBody(String at, JsonNode oldBody, JsonNode newBody) {
        String location = at + " request body";
        if (oldBody == null && newBody == null) {
            return;
        }
        JsonNode old = oldBody == null ? null : resolve(baseline, oldBody);
        JsonNode now = newBody == null ? null : resolve(current, newBody);
        if (now == null) {
            breaking(location, "request body removed");
            return;
        }
        if (old == null) {
            if (now.path("required").asBoolean(false)) {
                breaking(location, "required request body added");
            } else {
                additive(location, "optional request body added");
            }
            return;
        }
        if (!old.path("required").asBoolean(false) && now.path("required").asBoolean(false)) {
            breaking(location, "request body became required");
        }
        content(location, old.path("content"), now.path("content"), Direction.REQUEST);
    }

    private void responses(String at, JsonNode old, JsonNode now) {
        for (String status : names(old)) {
            String location = at + " response " + status;
            JsonNode before = resolve(baseline, old.get(status));
            JsonNode after = now.get(status) == null ? null : resolve(current, now.get(status));
            boolean success = status.startsWith("2");
            if (after == null) {
                if (success) {
                    breaking(location, "success response removed");
                } else {
                    additive(location, "error response no longer documented");
                }
                continue;
            }
            for (String header : names(before.path("headers"))) {
                if (!after.path("headers").has(header)) {
                    breaking(location + " header " + header, "response header removed");
                }
            }
            content(location, before.path("content"), after.path("content"), Direction.RESPONSE);
        }
        for (String status : names(now)) {
            if (!old.has(status)) {
                if (status.startsWith("2")) {
                    breaking(at + " response " + status, "new success status");
                } else {
                    additive(at + " response " + status, "response documented");
                }
            }
        }
    }

    private void content(String at, JsonNode old, JsonNode now, Direction direction) {
        for (String type : names(old)) {
            if (!now.has(type)) {
                breaking(at + " " + type, "media type removed");
                continue;
            }
            schema(at + " " + type, old.get(type).get("schema"), now.get(type).get("schema"), direction);
        }
        for (String type : names(now)) {
            if (!old.has(type)) {
                additive(at + " " + type, "media type added");
            }
        }
    }

    private void schema(String at, JsonNode oldSchema, JsonNode newSchema, Direction direction) {
        if (oldSchema == null && newSchema == null) {
            return;
        }
        if (oldSchema == null || newSchema == null) {
            breaking(at, "schema " + (oldSchema == null ? "added" : "removed"));
            return;
        }
        String key = direction + "|" + oldSchema.path("$ref").asText(at) + "|" + newSchema.path("$ref").asText(at);
        if ((oldSchema.has("$ref") || newSchema.has("$ref")) && !compared.add(key)) {
            return;
        }
        JsonNode old = flatten(baseline, resolve(baseline, oldSchema));
        JsonNode now = flatten(current, resolve(current, newSchema));
        boolean request = direction == Direction.REQUEST;

        types(at, old, now, request);
        String oldFormat = old.path("format").asText(null);
        String newFormat = now.path("format").asText(null);
        if (!Objects.equals(oldFormat, newFormat)) {
            boolean safe = request ? newFormat == null : oldFormat == null;
            if (safe) {
                additive(at, "format " + oldFormat + " to " + newFormat);
            } else {
                breaking(at, "format " + oldFormat + " to " + newFormat);
            }
        }
        values(at, "enum", old, now, request);
        values(at, "x-extensible-enum", old, now, request);
        upperBound(at, "maxLength", old, now, request);
        upperBound(at, "maxItems", old, now, request);
        upperBound(at, "maximum", old, now, request);
        lowerBound(at, "minLength", old, now, request);
        lowerBound(at, "minItems", old, now, request);
        lowerBound(at, "minimum", old, now, request);
        if (!Objects.equals(old.get("pattern"), now.get("pattern"))) {
            boolean safe = request ? !now.has("pattern") : !old.has("pattern");
            record(at, "pattern changed", !safe);
        }
        boolean oldNullable = old.path("nullable").asBoolean(false);
        boolean newNullable = now.path("nullable").asBoolean(false);
        if (oldNullable != newNullable) {
            record(at, "nullable " + oldNullable + " to " + newNullable, request ? oldNullable : newNullable);
        }
        if (request && !Objects.equals(old.get("default"), now.get("default"))) {
            breaking(at, "default changed");
        }
        mapValues(at, old.get("additionalProperties"), now.get("additionalProperties"), direction);
        properties(at, old, now, direction);
        if (old.has("items") || now.has("items")) {
            schema(at + "[]", old.get("items"), now.get("items"), direction);
        }
        variants(at, "oneOf", old, now, direction);
        variants(at, "anyOf", old, now, direction);
        discriminator(at, old.get("discriminator"), now.get("discriminator"), request);
        residual(at, old, now);
    }

    private enum Extra { ANY, NONE, SCHEMA }

    private static Extra extra(JsonNode additional) {
        if (additional == null || additional.isBoolean() && additional.booleanValue()) {
            return Extra.ANY;
        }
        return additional.isBoolean() ? Extra.NONE : Extra.SCHEMA;
    }

    /**
     * {@code additionalProperties}: none, any, or a schema for every other member, which is how a map keyed by id
     * (field values by field id) is written. Two schemas are compared like any other, by direction. Otherwise a
     * request may accept more (none to a schema to any), and a response may only constrain what it sends: a map
     * that disappears, or whose value schema is dropped, breaks a client reading it.
     */
    private void mapValues(String at, JsonNode oldExtra, JsonNode newExtra, Direction direction) {
        Extra before = extra(oldExtra);
        Extra after = extra(newExtra);
        if (before == Extra.SCHEMA && after == Extra.SCHEMA) {
            schema(at + "{}", oldExtra, newExtra, direction);
            return;
        }
        if (before == after) {
            return;
        }
        String what = "additional members " + before + " to " + after;
        boolean breaks = direction == Direction.REQUEST
            ? after == Extra.NONE || before == Extra.ANY && after == Extra.SCHEMA
            : before == Extra.SCHEMA;
        record(at, what, breaks);
    }

    /**
     * oneOf and anyOf variants, matched by the {@code $ref} they name (springdoc writes polymorphic types as
     * references), else by title, else by position, and compared by direction: a variant both sides keep is
     * followed into its schema. A request may be accepted in more shapes, never fewer; a response may take new
     * shapes (an open list) and drop old ones. A list appearing or disappearing entirely is breaking.
     */
    private void variants(String at, String key, JsonNode old, JsonNode now, Direction direction) {
        if (!old.has(key) && !now.has(key)) {
            return;
        }
        if (!old.has(key) || !now.has(key)) {
            breaking(at, key + (old.has(key) ? " removed" : " added"));
            return;
        }
        Map<String, JsonNode> before = variantsByKey(old.get(key));
        Map<String, JsonNode> after = variantsByKey(now.get(key));
        before.forEach((name, variant) -> {
            JsonNode kept = after.get(name);
            if (kept == null) {
                record(at, key + " variant removed " + name, direction == Direction.REQUEST);
            } else {
                schema(at + " " + key + "[" + name + "]", variant, kept, direction);
            }
        });
        after.keySet().stream().filter(name -> !before.containsKey(name))
            .forEach(name -> additive(at, key + " variant added " + name));
    }

    private static Map<String, JsonNode> variantsByKey(JsonNode variants) {
        Map<String, JsonNode> byKey = new java.util.LinkedHashMap<>();
        int index = 0;
        for (JsonNode variant : variants) {
            String name = variant.has("$ref") ? variant.get("$ref").asText()
                : variant.has("title") ? "title " + variant.get("title").asText() : "#" + index;
            byKey.put(name, variant);
            index++;
        }
        return byKey;
    }

    /** A discriminator's property must stay; a request may map more values, a response may map more or fewer. */
    private void discriminator(String at, JsonNode old, JsonNode now, boolean request) {
        if (old == null && now == null) {
            return;
        }
        if (old == null || now == null
            || !Objects.equals(old.path("propertyName").asText(null), now.path("propertyName").asText(null))) {
            breaking(at, "discriminator changed");
            return;
        }
        for (String value : names(old.path("mapping"))) {
            if (!now.path("mapping").has(value)) {
                record(at, "discriminator value removed " + value, request);
            } else if (!old.path("mapping").get(value).equals(now.path("mapping").get(value))) {
                breaking(at, "discriminator value " + value + " maps elsewhere");
            }
        }
        for (String value : names(now.path("mapping"))) {
            if (!old.path("mapping").has(value)) {
                additive(at, "discriminator value added " + value);
            }
        }
    }

    /**
     * Types as sets, since OpenAPI 3.1 writes a nullable member as {@code ["string", "null"]}; no type means any.
     * A request may accept more types (integer may widen to number), a response may send fewer (number may narrow
     * to integer); anything else is breaking.
     */
    private void types(String at, JsonNode old, JsonNode now, boolean request) {
        Set<String> before = typeSet(old.get("type"));
        Set<String> after = typeSet(now.get("type"));
        if (before.equals(after)) {
            return;
        }
        String what = "type " + (before.isEmpty() ? "any" : before) + " to " + (after.isEmpty() ? "any" : after);
        Set<String> widenedBefore = new TreeSet<>(before);
        Set<String> widenedAfter = new TreeSet<>(after);
        if (widenedBefore.remove("integer")) {
            widenedBefore.add("number");
        }
        if (widenedAfter.remove("integer")) {
            widenedAfter.add("number");
        }
        boolean safe;
        if (request) {
            // Accepted more widely: no type now, or every old type still accepted (integer within number).
            safe = after.isEmpty() || (!before.isEmpty() && (after.containsAll(before) || after.containsAll(widenedBefore)
                && !widenedBefore.equals(before)));
        } else {
            // Sent more narrowly: a type now where there was none, or only old types (integer within number).
            safe = !after.isEmpty() && (before.isEmpty() || before.containsAll(after) || widenedBefore.containsAll(widenedAfter)
                && after.contains("integer") && before.contains("number"));
        }
        record(at, what, !safe);
    }

    private static Set<String> typeSet(JsonNode type) {
        Set<String> types = new TreeSet<>();
        if (type == null) {
            return types;
        }
        if (type.isArray()) {
            type.forEach(t -> types.add(t.asText()));
        } else {
            types.add(type.asText());
        }
        return types;
    }

    private void properties(String at, JsonNode old, JsonNode now, Direction direction) {
        boolean request = direction == Direction.REQUEST;
        Set<String> oldRequired = strings(old.path("required"));
        Set<String> newRequired = strings(now.path("required"));
        JsonNode before = old.path("properties");
        JsonNode after = now.path("properties");
        for (String name : names(before)) {
            String location = at + "." + name;
            if (!after.has(name)) {
                breaking(location, "member removed");
                continue;
            }
            if (request && !oldRequired.contains(name) && newRequired.contains(name)) {
                breaking(location, "member became required");
            }
            if (!request && oldRequired.contains(name) && !newRequired.contains(name)) {
                breaking(location, "member no longer always present");
            }
            schema(location, before.get(name), after.get(name), direction);
        }
        for (String name : names(after)) {
            if (!before.has(name)) {
                if (request && newRequired.contains(name)) {
                    breaking(at + "." + name, "required member added");
                } else {
                    additive(at + "." + name, "member added");
                }
            }
        }
    }

    private void values(String at, String key, JsonNode old, JsonNode now, boolean request) {
        if (!old.has(key) && !now.has(key)) {
            return;
        }
        Set<String> before = strings(old.path(key));
        Set<String> after = strings(now.path(key));
        if (!old.has(key) || !now.has(key)) {
            // A list appearing narrows what a request may send. A response's values are an open list either way:
            // clients ignore values they do not know.
            record(at, key + (old.has(key) ? " removed" : " added"), request && now.has(key));
            return;
        }
        Set<String> removed = new TreeSet<>(before);
        removed.removeAll(after);
        Set<String> added = new TreeSet<>(after);
        added.removeAll(before);
        if (!removed.isEmpty()) {
            // Clients ignore values they do not know, so a response's list may change freely (lists are open).
            record(at, key + " values removed " + removed, request);
        }
        if (!added.isEmpty()) {
            additive(at, key + " values added " + added);
        }
    }

    private void upperBound(String at, String key, JsonNode old, JsonNode now, boolean request) {
        if (Objects.equals(old.get(key), now.get(key))) {
            return;
        }
        boolean tighter = now.has(key) && (!old.has(key) || now.get(key).decimalValue().compareTo(old.get(key).decimalValue()) < 0);
        record(at, key + " " + old.get(key) + " to " + now.get(key), request == tighter);
    }

    private void lowerBound(String at, String key, JsonNode old, JsonNode now, boolean request) {
        if (Objects.equals(old.get(key), now.get(key))) {
            return;
        }
        boolean tighter = now.has(key) && (!old.has(key) || now.get(key).decimalValue().compareTo(old.get(key).decimalValue()) > 0);
        record(at, key + " " + old.get(key) + " to " + now.get(key), request == tighter);
    }

    /** Whatever else differs in a schema, unread here, is breaking until a person decides otherwise. */
    private void residual(String at, JsonNode old, JsonNode now) {
        Set<String> keys = new TreeSet<>(names(old));
        keys.addAll(names(now));
        for (String key : keys) {
            if (MODELED.contains(key) || key.startsWith("x-")) {
                continue;
            }
            if (!Objects.equals(old.get(key), now.get(key))) {
                breaking(at, "unclassified change of " + key);
            }
        }
    }

    private void record(String at, String what, boolean breaking) {
        changes.add(new Change(at, what, breaking));
    }

    private void security() {
        if (!Objects.equals(baseline.get("security"), current.get("security"))) {
            breaking("document", "global security requirement changed");
        }
        JsonNode before = baseline.path("components").path("securitySchemes");
        JsonNode after = current.path("components").path("securitySchemes");
        for (String name : names(before)) {
            if (!before.get(name).equals(after.get(name))) {
                breaking("security scheme " + name, "changed or removed");
            }
        }
    }

    /** The node a {@code $ref} names within the same document, followed until none is left. */
    private static JsonNode resolve(JsonNode document, JsonNode node) {
        JsonNode resolved = node;
        for (int hops = 0; resolved != null && resolved.has("$ref") && hops < 32; hops++) {
            String ref = resolved.get("$ref").asText();
            if (!ref.startsWith("#/")) {
                throw new IllegalArgumentException("Only local references are compared: " + ref);
            }
            JsonNode target = document;
            for (String part : ref.substring(2).split("/")) {
                target = target.path(part.replace("~1", "/").replace("~0", "~"));
            }
            if (target.isMissingNode()) {
                throw new IllegalArgumentException("A reference names nothing: " + ref);
            }
            resolved = target;
        }
        return resolved;
    }

    /** allOf merged into one object: its members, its required list, and the other keys of the first part. */
    private static JsonNode flatten(JsonNode document, JsonNode schema) {
        if (!schema.has("allOf")) {
            return schema;
        }
        ObjectNode merged = JsonNodeFactory.instance.objectNode();
        ObjectNode properties = merged.putObject("properties");
        Set<String> required = new TreeSet<>();
        Iterator<Map.Entry<String, JsonNode>> fields = schema.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!field.getKey().equals("allOf")) {
                merged.set(field.getKey(), field.getValue());
            }
        }
        for (JsonNode part : schema.get("allOf")) {
            JsonNode resolved = flatten(document, resolve(document, part));
            resolved.path("properties").fields().forEachRemaining(member -> properties.set(member.getKey(), member.getValue()));
            required.addAll(strings(resolved.path("required")));
            if (!merged.has("type") && resolved.has("type")) {
                merged.set("type", resolved.get("type"));
            }
        }
        required.addAll(strings(schema.path("required")));
        if (!required.isEmpty()) {
            merged.set("required", JsonNodeFactory.instance.arrayNode().addAll(required.stream()
                .map(JsonNodeFactory.instance::textNode).toList()));
        }
        return merged;
    }

    private static Set<String> names(JsonNode node) {
        Set<String> names = new TreeSet<>();
        if (node != null && node.isObject()) {
            node.fieldNames().forEachRemaining(names::add);
        }
        return names;
    }

    private static Set<String> strings(JsonNode node) {
        Set<String> values = new TreeSet<>();
        if (node != null && node.isArray()) {
            node.forEach(value -> values.add(value.asText()));
        }
        return values;
    }
}
