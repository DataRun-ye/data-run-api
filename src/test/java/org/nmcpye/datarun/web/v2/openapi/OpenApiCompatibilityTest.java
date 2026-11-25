package org.nmcpye.datarun.web.v2.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Each rule of the v2 evolution policy, on small documents: what is additive, what breaks a client. */
class OpenApiCompatibilityTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** One POST with a request body and a 200 answer whose schema is a component, as a v2 action would have. */
    private static final String BASE = """
        {"openapi":"3.0.1","security":[{"bearerAuth":[]}],
         "paths":{"/api/v2/things/{id}:touch":{"post":{
           "parameters":[{"in":"path","name":"id","required":true,"schema":{"$ref":"#/components/schemas/Id"}},
                         {"in":"query","name":"mode","required":false,"schema":{"type":"string","enum":["FAST","SLOW"]}}],
           "requestBody":{"required":true,"content":{"application/json":{"schema":{"$ref":"#/components/schemas/Touch"}}}},
           "responses":{"200":{"description":"ok","headers":{"ETag":{"schema":{"type":"string"}}},
                               "content":{"application/json":{"schema":{"$ref":"#/components/schemas/Thing"}}}},
                        "default":{"$ref":"#/components/responses/Problem"}}}}},
         "components":{
           "schemas":{
             "Id":{"type":"string","minLength":1,"maxLength":36},
             "Touch":{"type":"object","required":["reason"],"properties":{
               "reason":{"type":"string","maxLength":500},"dryRun":{"type":"boolean"},
               "kind":{"type":"string","enum":["SOFT","HARD"]}}},
             "Thing":{"type":"object","required":["id","version"],"properties":{
               "id":{"$ref":"#/components/schemas/Id"},"version":{"type":"integer","format":"int64"},
               "state":{"type":"string","enum":["OPEN","DONE"]},"parent":{"$ref":"#/components/schemas/Thing"}}},
             "Problem":{"type":"object","required":["code"],"properties":{"code":{"type":"string",
               "x-extensible-enum":["bad-request"]}}}},
           "responses":{"Problem":{"description":"problem","content":{"application/problem+json":{
             "schema":{"$ref":"#/components/schemas/Problem"}}}}},
           "securitySchemes":{"bearerAuth":{"type":"http","scheme":"bearer"}}}}
        """;

    private static JsonNode doc(String json) throws Exception {
        return OpenApiDocuments.read(json);
    }

    private static List<String> breaking(String changed) throws Exception {
        return OpenApiCompatibility.compare(doc(BASE), doc(changed)).stream()
            .filter(OpenApiCompatibility.Change::breaking).map(Object::toString).toList();
    }

    private static List<String> additive(String changed) throws Exception {
        return OpenApiCompatibility.compare(doc(BASE), doc(changed)).stream()
            .filter(change -> !change.breaking()).map(Object::toString).toList();
    }

    @Test
    void anUnchangedDocumentHasNoChanges() throws Exception {
        assertThat(OpenApiCompatibility.compare(doc(BASE), doc(BASE))).isEmpty();
    }

    @Test
    void growingIsAdditive() throws Exception {
        String grown = BASE
            .replace("\"dryRun\":{\"type\":\"boolean\"}", "\"dryRun\":{\"type\":\"boolean\"},\"note\":{\"type\":\"string\"}")
            .replace("\"enum\":[\"OPEN\",\"DONE\"]", "\"enum\":[\"OPEN\",\"DONE\",\"PAUSED\"]")
            .replace("\"enum\":[\"SOFT\",\"HARD\"]", "\"enum\":[\"SOFT\",\"HARD\",\"MEDIUM\"]")
            .replace("\"x-extensible-enum\":[\"bad-request\"]", "\"x-extensible-enum\":[\"bad-request\",\"not-found\"]")
            .replace("\"version\":{\"type\":\"integer\",\"format\":\"int64\"},",
                "\"version\":{\"type\":\"integer\",\"format\":\"int64\"},\"label\":{\"type\":\"string\"},")
            .replace("\"maxLength\":500", "\"maxLength\":2000")
            .replace("\"paths\":{", "\"paths\":{\"/api/v2/things\":{\"get\":{\"responses\":{\"default\":"
                + "{\"$ref\":\"#/components/responses/Problem\"}}}},");
        assertThat(breaking(grown)).isEmpty();
        assertThat(additive(grown)).hasSize(7);
    }

    @Test
    void removingOrRenamingWhatClientsUseBreaks() throws Exception {
        assertThat(breaking(BASE.replace("\"/api/v2/things/{id}:touch\"", "\"/api/v2/things/{id}:poke\"")))
            .contains("BREAKING POST /api/v2/things/{id}:touch: operation removed");
        assertThat(breaking(BASE.replace("\"state\":{\"type\":\"string\",\"enum\":[\"OPEN\",\"DONE\"]},", "")))
            .anyMatch(change -> change.contains(".state: member removed"));
        assertThat(breaking(BASE.replace("\"required\":[\"id\",\"version\"]", "\"required\":[\"id\"]")))
            .anyMatch(change -> change.contains(".version: member no longer always present"));
        assertThat(breaking(BASE.replace("\"version\":{\"type\":\"integer\",\"format\":\"int64\"}",
            "\"version\":{\"type\":\"string\"}"))).anyMatch(change -> change.contains("type [integer] to [string]"));
        assertThat(breaking(BASE.replace("\"headers\":{\"ETag\":{\"schema\":{\"type\":\"string\"}}},", "")))
            .anyMatch(change -> change.contains("header ETag: response header removed"));
    }

    @Test
    void askingMoreOfTheCallerBreaks() throws Exception {
        assertThat(breaking(BASE.replace("\"name\":\"mode\",\"required\":false", "\"name\":\"mode\",\"required\":true")))
            .anyMatch(change -> change.contains("parameter query mode: parameter became required"));
        assertThat(breaking(BASE.replace("\"required\":[\"reason\"]", "\"required\":[\"reason\",\"dryRun\"]")))
            .anyMatch(change -> change.contains(".dryRun: member became required"));
        assertThat(breaking(BASE.replace("\"enum\":[\"SOFT\",\"HARD\"]", "\"enum\":[\"SOFT\"]")))
            .anyMatch(change -> change.contains("enum values removed [HARD]"));
        assertThat(breaking(BASE.replace("\"maxLength\":500", "\"maxLength\":100")))
            .anyMatch(change -> change.contains("maxLength 500 to 100"));
        assertThat(breaking(BASE.replace("\"enum\":[\"FAST\",\"SLOW\"]", "\"enum\":[\"FAST\"]")))
            .anyMatch(change -> change.contains("enum values removed [SLOW]"));
        assertThat(breaking(BASE.replace("\"parameters\":[", "\"parameters\":[{\"in\":\"query\",\"name\":\"must\","
            + "\"required\":true,\"schema\":{\"type\":\"string\"}},")))
            .anyMatch(change -> change.contains("required parameter added"));
    }

    @Test
    void aResponsesListsAreOpenButItsOtherLimitsMayOnlyNarrow() throws Exception {
        assertThat(breaking(BASE.replace("\"enum\":[\"OPEN\",\"DONE\"]", "\"enum\":[\"OPEN\"]")))
            .as("a response value no longer sent").isEmpty();
        assertThat(breaking(BASE.replace("\"Id\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":36}",
            "\"Id\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":40}")))
            .as("a longer id breaks a client keeping 36 characters")
            .anyMatch(change -> change.contains("maxLength 36 to 40"));
        assertThat(breaking(BASE.replace("\"Id\":{\"type\":\"string\",\"minLength\":1,\"maxLength\":36}",
            "\"Id\":{\"type\":\"string\",\"format\":\"uuid\",\"minLength\":1,\"maxLength\":36}")))
            .as("a format on an id is narrowing a request").isNotEmpty();
    }

    @Test
    void newSuccessStatusesAndSecurityChangesBreakAndUnknownSchemaChangesAreFlagged() throws Exception {
        assertThat(breaking(BASE.replace("\"default\":{\"$ref\":\"#/components/responses/Problem\"}}}}}",
            "\"202\":{\"description\":\"later\"},\"default\":{\"$ref\":\"#/components/responses/Problem\"}}}}}")))
            .anyMatch(change -> change.contains("response 202: new success status"));
        assertThat(breaking(BASE.replace("\"security\":[{\"bearerAuth\":[]}]", "\"security\":[]")))
            .contains("BREAKING document: global security requirement changed");
        assertThat(breaking(BASE.replace("\"Thing\":{\"type\":\"object\",", "\"Thing\":{\"type\":\"object\","
            + "\"discriminator\":{\"propertyName\":\"state\"},")))
            .anyMatch(change -> change.contains("unclassified change of discriminator"));
    }

    /** OpenAPI 3.1 writes nullability into the type: a response that may now be null breaks a client. */
    @Test
    void typesAreSetsAsOpenApi31WritesThem() throws Exception {
        String nullableLabel = BASE.replace("\"version\":{\"type\":\"integer\",\"format\":\"int64\"},",
            "\"version\":{\"type\":[\"integer\",\"null\"],\"format\":\"int64\"},");
        assertThat(breaking(nullableLabel)).anyMatch(change -> change.contains(".version: type [integer] to [integer, null]"));
        String nullableReason = BASE.replace("\"reason\":{\"type\":\"string\",\"maxLength\":500}",
            "\"reason\":{\"type\":[\"string\",\"null\"],\"maxLength\":500}");
        assertThat(breaking(nullableReason)).as("a request accepting null as well").isEmpty();
        String numberVersion = BASE.replace("\"version\":{\"type\":\"integer\",\"format\":\"int64\"}",
            "\"version\":{\"type\":\"number\",\"format\":\"int64\"}");
        assertThat(breaking(numberVersion)).as("a response may not widen integer to number")
            .anyMatch(change -> change.contains("type [integer] to [number]"));
    }

    @Test
    void recursiveSchemasAreComparedOnce() throws Exception {
        assertThat(OpenApiCompatibility.compare(doc(BASE), doc(BASE))).isEmpty();
        assertThat(breaking(BASE.replace("\"parent\":{\"$ref\":\"#/components/schemas/Thing\"}",
            "\"parent\":{\"type\":\"string\"}"))).anyMatch(change -> change.contains(".parent"));
    }

    @Test
    void theNormalFormSortsMembersAndRequiredListsAndDropsServers() throws Exception {
        JsonNode one = doc("{\"b\":1,\"a\":{\"required\":[\"y\",\"x\"]},\"servers\":[{\"url\":\"http://x\"}]}");
        assertThat(JSON.writeValueAsString(one)).isEqualTo("{\"a\":{\"required\":[\"x\",\"y\"]},\"b\":1}");
    }
}
