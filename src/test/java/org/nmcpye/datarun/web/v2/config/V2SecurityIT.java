package org.nmcpye.datarun.web.v2.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.web.filter.RequestIdFilter;
import org.nmcpye.datarun.web.v2.fixtures.V2TestUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The v2 chain with real tokens and accounts: every v2 route needs an authenticated caller, the bearer JWT is the
 * only credential, refusals are problems with the bearer challenge, and the main chain answers v1 as before.
 */
@IntegrationTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2SecurityIT {

    /** No such v2 route: a request that passes the chain reaches MVC and is answered 404. */
    private static final String V2_PATH = "/api/v2/no-such-route";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void accounts() {
        V2TestUsers.ensure(jdbc, transactions);
    }

    private MockHttpServletResponse perform(RequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse();
    }

    private JsonNode problem(MockHttpServletResponse response, int status, String code) throws Exception {
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        JsonNode body = json.readTree(response.getContentAsByteArray());
        assertThat(body.get("code").asText()).isEqualTo(code);
        assertThat(body.get("instance").asText())
            .isEqualTo("urn:uuid:" + response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
        return body;
    }

    @Test
    void withoutACredentialAV2RouteIsA401ProblemWithTheBearerChallenge() throws Exception {
        MockHttpServletResponse response = perform(get(V2_PATH));
        JsonNode body = problem(response, 401, "unauthenticated");
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(body.get("detail").asText()).contains("POST /api/v1/authenticate");
    }

    @Test
    void aRejectedTokenIsA401ProblemThatSaysSoWithoutRepeatingIt() throws Exception {
        MockHttpServletResponse response = perform(get(V2_PATH)
            .header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"));
        JsonNode body = problem(response, 401, "unauthenticated");
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Bearer error=\"invalid_token\"");
        assertThat(body.get("detail").asText()).isEqualTo(V2SecurityConfiguration.REJECTED_TOKEN_DETAIL);
        assertThat(response.getContentAsString()).doesNotContain("not.a.token");
    }

    @Test
    void aTokenWhoseAccountIsDeactivatedIsRefused() throws Exception {
        MockHttpServletResponse response = perform(get(V2_PATH)
            .header(HttpHeaders.AUTHORIZATION, V2TestUsers.bearer(jwtKey, V2TestUsers.INACTIVE)));
        problem(response, 401, "unauthenticated");
    }

    /** The main chain still takes HTTP Basic for operational compatibility; v2 does not. */
    @Test
    void httpBasicIsNotAV2Credential() throws Exception {
        String basic = "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        problem(perform(get(V2_PATH).header(HttpHeaders.AUTHORIZATION, basic)), 401, "unauthenticated");
        assertThat(perform(get("/api/v1/myDetails").header(HttpHeaders.AUTHORIZATION, basic)).getStatus())
            .as("the same credential on v1").isEqualTo(200);
    }

    @Test
    void aValidTokenPassesTheChainAndTheRequestReachesTheApplication() throws Exception {
        MockHttpServletResponse response = perform(get(V2_PATH)
            .header(HttpHeaders.AUTHORIZATION, V2TestUsers.bearer(jwtKey, V2TestUsers.CALLER_A)));
        problem(response, 404, "not-found");
    }

    /** v1 is answered by the main chain exactly as before: a 401 with the challenge and no body. */
    @Test
    void v1RefusalsAreUnchanged() throws Exception {
        MockHttpServletResponse response = perform(get("/api/v1/myDetails"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getContentAsByteArray()).isEmpty();
    }
}
