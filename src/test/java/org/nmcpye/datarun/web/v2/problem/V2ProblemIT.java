package org.nmcpye.datarun.web.v2.problem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.nmcpye.datarun.web.v2.fixtures.V2ProblemFixtureController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * v2 errors through the running application: every failure of a v2 request is an RFC 9457 problem whose
 * {@code instance} names the request id, the answers never repeat what the client sent, and a v1 request's error
 * keeps v1's shape.
 */
@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.USER)
class V2ProblemIT {

    private static final String BASE = "/api/v2/fixtures/problems";

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    private MockHttpServletResponse perform(RequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse();
    }

    /** The problem body, after checking the media type and that instance names the response's request id. */
    private JsonNode problem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        JsonNode body = json.readTree(response.getContentAsByteArray());
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.get("status").asInt()).isEqualTo(status);
        assertThat(body.get("type").asText()).isEqualTo(ProblemCode.TYPE_PREFIX + code);
        assertThat(body.get("title").asText()).isEqualTo(ProblemCode.byCode(code).title());
        assertThat(body.get("instance").asText())
            .isEqualTo("urn:uuid:" + response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        assertThat(body.has("message")).as("no v1 member").isFalse();
        assertThat(body.has("path")).as("no v1 member").isFalse();
        return body;
    }

    @Test
    void aHandlersRefusalIsItsProblem() throws Exception {
        JsonNode body = problem(perform(get(BASE + "/refusal")), 422, "validation-failed");
        assertThat(body.get("errors").get(0).get("parameter").asText()).isEqualTo("count");
        assertThat(body.get("errors").get(0).get("code").asText()).isEqualTo("too-small");
    }

    @Test
    void aBodyThatIsNotJsonIsMalformedAndNotRepeated() throws Exception {
        MockHttpServletResponse response = perform(post(BASE + "/body").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": " + V2ProblemFixtureController.SECRET));
        JsonNode body = problem(response, 400, "bad-request");
        assertThat(body.get("detail").asText()).isEqualTo("The request body is not valid JSON.");
        assertThat(response.getContentAsString()).doesNotContain(V2ProblemFixtureController.SECRET);
    }

    @Test
    void aValueOfTheWrongTypeIsLocatedAndNotRepeated() throws Exception {
        MockHttpServletResponse response = perform(post(BASE + "/body").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"n\", \"count\": \"" + V2ProblemFixtureController.SECRET + "\"}"));
        JsonNode body = problem(response, 400, "bad-request");
        assertThat(body.get("errors").get(0).get("pointer").asText()).isEqualTo("/count");
        assertThat(body.get("errors").get(0).get("code").asText()).isEqualTo("invalid-type");
        assertThat(response.getContentAsString()).doesNotContain(V2ProblemFixtureController.SECRET);
    }

    @Test
    void aBodyFailingValidationIs422WithAFindingPerValue() throws Exception {
        MockHttpServletResponse response = perform(post(BASE + "/body").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \" \", \"count\": 0}"));
        JsonNode body = problem(response, 422, "validation-failed");
        assertThat(body.get("errors").findValuesAsText("pointer")).containsExactlyInAnyOrder("/name", "/count");
        assertThat(body.get("errors").findValuesAsText("code")).containsExactlyInAnyOrder("not-blank", "min");
    }

    @Test
    void aParameterOfTheWrongTypeIsMalformed() throws Exception {
        JsonNode body = problem(perform(get(BASE + "/typed").param("count", "abc")), 400, "bad-request");
        assertThat(body.get("errors").get(0).get("parameter").asText()).isEqualTo("count");
        problem(perform(get(BASE + "/typed")), 400, "bad-request");
    }

    @Test
    void aServerFaultSaysNothingOfItsCause() throws Exception {
        MockHttpServletResponse response = perform(get(BASE + "/fault"));
        problem(response, 500, "internal-error");
        assertThat(response.getContentAsString()).doesNotContain(V2ProblemFixtureController.SECRET)
            .doesNotContain("IllegalStateException");
    }

    @Test
    void routingFailuresOnV2PathsAreProblemsToo() throws Exception {
        problem(perform(get("/api/v2/no-such-route")), 404, "not-found");

        MockHttpServletResponse notAllowed = perform(delete(BASE + "/refusal"));
        problem(notAllowed, 405, "method-not-allowed");
        assertThat(notAllowed.getHeader(HttpHeaders.ALLOW)).contains("GET");

        problem(perform(post(BASE + "/body").contentType(MediaType.TEXT_PLAIN).content("x")), 415,
            "unsupported-media-type");
    }

    /** The same kind of failure on a v1 path keeps the v1 shape, so no v1 client sees a change. */
    @Test
    void v1ErrorsKeepTheirShape() throws Exception {
        MockHttpServletResponse response = perform(get("/api/v1/no-such-route"));
        assertThat(response.getStatus()).isEqualTo(404);
        JsonNode body = json.readTree(response.getContentAsByteArray());
        assertThat(body.get("message").asText()).isEqualTo("error.http.404");
        assertThat(body.get("path").asText()).isEqualTo("/api/v1/no-such-route");
        assertThat(body.has("code")).isFalse();
    }
}
