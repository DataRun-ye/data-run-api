package org.nmcpye.datarun.web.v2.openapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Each published convention, violated once, is reported. */
class V2ConventionsTest {

    private static final String GOOD = """
        {"security":[{"bearerAuth":[]}],
         "paths":{"/api/v2/things/{thingId}:touch":{"post":{
             "parameters":[{"in":"path","name":"thingId","schema":{"$ref":"#/components/schemas/Id"}},
                           {"in":"header","name":"Idempotency-Key","schema":{"type":"string","format":"uuid"}}],
             "responses":{"default":{"$ref":"#/components/responses/Problem"}}}},
                  "/api/v2/things:search":{"post":{"responses":{"default":{"$ref":"#/components/responses/Problem"}}}},
                  "/api/v2/access-grants/{id}/notes":{"get":{
             "parameters":[{"in":"path","name":"id","schema":{"$ref":"#/components/schemas/Id"}}],
             "responses":{"default":{"$ref":"#/components/responses/Problem"}}}}},
         "components":{"schemas":{
           "Id":{"type":"string","maxLength":36},
           "Problem":{"properties":{"status":{"type":"integer"},"code":{"type":"string"}}},
           "Thing":{"properties":{"id":{"$ref":"#/components/schemas/Id"},
             "formId":{"$ref":"#/components/schemas/Id"},
             "teamIds":{"type":"array","items":{"$ref":"#/components/schemas/Id"}},
             "reviewStatus":{"type":"string","enum":["HAS_ISSUES","OPEN"]}}}}}}
        """;

    private static List<String> violations(String json) throws Exception {
        return V2Conventions.violations(OpenApiDocuments.read(json));
    }

    @Test
    void aDocumentKeepingTheConventionsHasNoViolation() throws Exception {
        assertThat(violations(GOOD)).isEmpty();
    }

    @Test
    void eachBrokenConventionIsReported() throws Exception {
        assertThat(violations(GOOD.replace("/api/v2/things:search", "/api/v1/things"))).anyMatch(v -> v.contains("not under /api/v2/"));
        assertThat(violations(GOOD.replace("/api/v2/access-grants", "/api/v2/accessGrants"))).anyMatch(v -> v.contains("segment accessGrants"));
        assertThat(violations(GOOD.replace("\"/api/v2/things:search\":{\"post\"", "\"/api/v2/things:search\":{\"patch\"")))
            .anyMatch(v -> v.contains("never PATCH"));
        assertThat(violations(GOOD.replace("\"/api/v2/things:search\":{\"post\":{\"responses\":{\"default\":{\"$ref\":\"#/components/responses/Problem\"}}}}",
            "\"/api/v2/things:search\":{\"post\":{\"responses\":{}}}"))).anyMatch(v -> v.contains("no default problem response"));
        assertThat(violations(GOOD.replace("\"name\":\"thingId\"", "\"name\":\"thing_id\""))).anyMatch(v -> v.contains("not camelCase"));
        assertThat(violations(GOOD.replace("\"formId\":{\"$ref\":\"#/components/schemas/Id\"}", "\"formId\":{\"type\":\"string\",\"format\":\"uuid\"}")))
            .anyMatch(v -> v.contains("formId: an id is"))
            .anyMatch(v -> v.contains("format uuid"));
        assertThat(violations(GOOD.replace("\"formId\"", "\"formUid\""))).anyMatch(v -> v.contains("never carries a uid"));
        assertThat(violations(GOOD.replace("\"teamIds\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Id\"}}",
            "\"teamIds\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}"))).anyMatch(v -> v.contains("teamIds"));
        assertThat(violations(GOOD.replace("\"reviewStatus\"", "\"status\""))).anyMatch(v -> v.contains("a bare status"));
        assertThat(violations(GOOD.replace("\"HAS_ISSUES\"", "\"hasIssues\""))).anyMatch(v -> v.contains("not UPPER_SNAKE"));
        assertThat(violations(GOOD.replace("\"security\":[{\"bearerAuth\":[]}],", ""))).anyMatch(v -> v.contains("bearer token"));
    }
}
