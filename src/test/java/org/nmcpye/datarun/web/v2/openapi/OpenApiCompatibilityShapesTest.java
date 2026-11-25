package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The shapes a v2 edit has: a list of polymorphic operations (oneOf of references with a discriminator), field
 * values as maps keyed by id (additionalProperties), and rows that may take several shapes (anyOf, inline and by
 * reference). Each kind of break inside them is caught, by direction.
 */
class OpenApiCompatibilityShapesTest {

    private static final String SHAPES = """
        {"openapi":"3.1.0","security":[{"bearerAuth":[]}],
         "paths":{"/api/v2/things/{id}:edit":{"post":{
           "requestBody":{"required":true,"content":{"application/json":{"schema":{"$ref":"#/components/schemas/Edit"}}}},
           "responses":{"200":{"description":"ok","content":{"application/json":{"schema":{"$ref":"#/components/schemas/State"}}}},
                        "default":{"$ref":"#/components/responses/Problem"}}}}},
         "components":{
           "schemas":{
             "Edit":{"type":"object","required":["operations"],"properties":{
               "labels":{"type":"object","additionalProperties":{"type":"string"}},
               "operations":{"type":"array","items":{
                 "oneOf":[{"$ref":"#/components/schemas/SetValue"},{"$ref":"#/components/schemas/RemoveRow"}],
                 "discriminator":{"propertyName":"op","mapping":{
                   "setValue":"#/components/schemas/SetValue","removeRow":"#/components/schemas/RemoveRow"}}}}}},
             "SetValue":{"type":"object","required":["op","field"],"properties":{
               "op":{"type":"string"},"field":{"type":"string"},"value":{"type":"string"}}},
             "RemoveRow":{"type":"object","required":["op","row"],"properties":{
               "op":{"type":"string"},"row":{"type":"string"}}},
             "State":{"type":"object","properties":{
               "values":{"type":"object","additionalProperties":{"type":"integer"}},
               "rows":{"type":"array","items":{"anyOf":[{"$ref":"#/components/schemas/Row"},
                 {"type":"object","title":"Gap","properties":{"at":{"type":"integer"}}}]}}}},
             "Row":{"type":"object","required":["id"],"properties":{
               "id":{"type":"string"},"values":{"type":"object","additionalProperties":{"type":"string"}},
               "children":{"type":"array","items":{"anyOf":[{"$ref":"#/components/schemas/Row"}]}}}},
             "Problem":{"type":"object","properties":{"code":{"type":"string"}}}},
           "responses":{"Problem":{"description":"problem","content":{"application/problem+json":{
             "schema":{"$ref":"#/components/schemas/Problem"}}}}},
           "securitySchemes":{"bearerAuth":{"type":"http","scheme":"bearer"}}}}
        """;

    private static List<OpenApiCompatibility.Change> compare(String changed) throws Exception {
        JsonNode before = OpenApiDocuments.read(SHAPES);
        return OpenApiCompatibility.compare(before, OpenApiDocuments.read(changed));
    }

    private static List<String> breaking(String changed) throws Exception {
        return compare(changed).stream().filter(OpenApiCompatibility.Change::breaking).map(Object::toString).toList();
    }

    @Test
    void anUnchangedDocumentAndARecursiveVariantCompareCleanly() throws Exception {
        assertThat(compare(SHAPES)).isEmpty();
    }

    @Test
    void aRequestVariantFollowedThroughItsReferenceMayNotAskMore() throws Exception {
        assertThat(breaking(SHAPES.replace("\"required\":[\"op\",\"field\"]", "\"required\":[\"op\",\"field\",\"value\"]")))
            .anyMatch(change -> change.contains("oneOf[#/components/schemas/SetValue].value: member became required"));
        assertThat(breaking(SHAPES.replace("\"value\":{\"type\":\"string\"}}}", "\"value\":{\"type\":\"integer\"}}}")))
            .anyMatch(change -> change.contains("oneOf[#/components/schemas/SetValue].value: type [string] to [integer]"));
    }

    @Test
    void aRequestVariantRemovedBreaksAndOneAddedDoesNot() throws Exception {
        String removed = SHAPES.replace(",{\"$ref\":\"#/components/schemas/RemoveRow\"}]", "]")
            .replace(",\"removeRow\":\"#/components/schemas/RemoveRow\"", "");
        assertThat(breaking(removed))
            .anyMatch(change -> change.contains("oneOf variant removed #/components/schemas/RemoveRow"))
            .anyMatch(change -> change.contains("discriminator value removed removeRow"));
        String added = SHAPES.replace(",{\"$ref\":\"#/components/schemas/RemoveRow\"}]",
                ",{\"$ref\":\"#/components/schemas/RemoveRow\"},{\"$ref\":\"#/components/schemas/RemoveRow2\"}]")
            .replace("\"removeRow\":\"#/components/schemas/RemoveRow\"}",
                "\"removeRow\":\"#/components/schemas/RemoveRow\",\"restoreRow\":\"#/components/schemas/RemoveRow2\"}")
            .replace("\"State\":{", "\"RemoveRow2\":{\"type\":\"object\",\"properties\":{\"op\":{\"type\":\"string\"}}},\"State\":{");
        assertThat(breaking(added)).isEmpty();
        assertThat(compare(added)).anyMatch(change -> change.toString().contains("oneOf variant added"));
    }

    @Test
    void aResponseVariantFollowedThroughItsReferenceOrInlineMayNotDropOrWiden() throws Exception {
        assertThat(breaking(SHAPES.replace("\"id\":{\"type\":\"string\"},\"values\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"string\"}},",
            "\"id\":{\"type\":\"string\"},")))
            .anyMatch(change -> change.contains("anyOf[#/components/schemas/Row].values: member removed"));
        assertThat(breaking(SHAPES.replace("\"at\":{\"type\":\"integer\"}", "\"at\":{\"type\":\"string\"}")))
            .anyMatch(change -> change.contains("anyOf[title Gap].at: type [integer] to [string]"));
        String dropped = SHAPES.replace(",\n                 {\"type\":\"object\",\"title\":\"Gap\",\"properties\":{\"at\":{\"type\":\"integer\"}}}]", "]");
        assertThat(breaking(dropped)).as("a response may stop sending a shape").isEmpty();
    }

    @Test
    void mapValuesAreComparedByDirection() throws Exception {
        assertThat(breaking(SHAPES.replace("\"values\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"integer\"}}",
            "\"values\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"number\"}}")))
            .as("a response's map values widened").anyMatch(change -> change.contains("values{}: type [integer] to [number]"));
        assertThat(breaking(SHAPES.replace("\"labels\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"string\"}}",
            "\"labels\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"string\",\"maxLength\":5}}")))
            .as("a request's map values narrowed").anyMatch(change -> change.contains("labels{}: maxLength null to 5"));
        assertThat(breaking(SHAPES.replace("\"values\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"integer\"}}",
            "\"values\":{\"type\":\"object\",\"additionalProperties\":false}")))
            .as("a response's map gone").anyMatch(change -> change.contains("values: additional members SCHEMA to NONE"));
        assertThat(breaking(SHAPES.replace("\"labels\":{\"type\":\"object\",\"additionalProperties\":{\"type\":\"string\"}}",
            "\"labels\":{\"type\":\"object\"}"))).as("a request's map accepting any value").isEmpty();
    }
}
