package org.nmcpye.datarun.web.v2.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.web.v2.fixtures.V2TestUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whether Spring MVC routes the naming rules' action form, {@code POST /api/v2/<resources>/{id}:<verb>}
 * (AIP-136), cleanly. Real HTTP requests to the running server (Undertow, the security firewall, the v2 chain and
 * the dispatcher), so the path is parsed as a request from the proxy would be: the colon sent as is, as nginx
 * passes it, and percent-encoded, as some clients send it.
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2ActionRoutingIT {

    private static final String THINGS = "/api/v2/fixtures/things";

    @LocalServerPort
    private int port;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper json = new ObjectMapper();
    private String bearer;

    @BeforeAll
    void caller() {
        V2TestUsers.ensure(jdbc, transactions);
        bearer = V2TestUsers.bearer(jwtKey, V2TestUsers.CALLER_A);
    }

    private HttpResponse<String> send(String method, String rawPath) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + rawPath))
            .header("Authorization", bearer)
            .method(method, HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(20))
            .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode routed(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).as("%s %s", response.request().method(), response.uri()).isEqualTo(200);
        return json.readTree(response.body());
    }

    /** A ULID, a 24-hex legacy id and UUIDv7 text: every id shape v2 serves. */
    @ParameterizedTest
    @ValueSource(strings = {"01JDQ3ZK5V8Y7M2N4P6R8T0W2X", "5f2b7c9d1e3a4b6c8d0e2f4a",
        "0193a7c2-5d4e-7f81-9a2b-3c4d5e6f7a8b"})
    void anActionOnAnIdRoutesToItsVerbWithTheIdIntact(String id) throws Exception {
        for (String colon : new String[] {":", "%3A", "%3a"}) {
            JsonNode touch = routed(send("POST", THINGS + "/" + id + colon + "touch"));
            assertThat(touch.get("route").asText()).as("colon sent as %s", colon).isEqualTo("touch");
            assertThat(touch.get("id").asText()).as("the id does not swallow the verb").isEqualTo(id);

            JsonNode tidyUp = routed(send("POST", THINGS + "/" + id + colon + "tidyUp"));
            assertThat(tidyUp.get("route").asText()).isEqualTo("tidyUp");
            assertThat(tidyUp.get("id").asText()).isEqualTo(id);
        }
        JsonNode read = routed(send("GET", THINGS + "/" + id));
        assertThat(read.get("route").asText()).isEqualTo("read");
        assertThat(read.get("id").asText()).isEqualTo(id);
        JsonNode notes = routed(send("GET", THINGS + "/" + id + "/notes"));
        assertThat(notes.get("route").asText()).as("a sub-collection beside the actions").isEqualTo("notes");
        assertThat(notes.get("id").asText()).isEqualTo(id);
    }

    @Test
    void anActionOnTheCollectionRoutes() throws Exception {
        assertThat(routed(send("POST", THINGS + ":search")).get("route").asText()).isEqualTo("search");
        assertThat(routed(send("POST", THINGS + "%3Asearch")).get("route").asText()).isEqualTo("search");
    }

    /**
     * A verb no route declares reaches no action. The single-resource read still matches the path, with the verb
     * as part of its id, so a POST is 405 (Allow: GET) and a GET hands the read an id no stored id can be (it holds
     * a colon), which the id guard answers as unknown.
     */
    @Test
    void anUnknownVerbReachesNoAction() throws Exception {
        String id = "01JDQ3ZK5V8Y7M2N4P6R8T0W2X";
        HttpResponse<String> post = send("POST", THINGS + "/" + id + ":explode");
        assertThat(post.statusCode()).isEqualTo(405);
        assertThat(post.headers().firstValue("Allow").orElse("")).contains("GET");
        assertThat(json.readTree(post.body()).get("code").asText()).isEqualTo("method-not-allowed");

        JsonNode read = routed(send("GET", THINGS + "/" + id + ":touch"));
        assertThat(read.get("route").asText()).isEqualTo("read");
        assertThat(read.get("id").asText()).isEqualTo(id + ":touch");
        assertThat(org.nmcpye.datarun.web.v2.ids.ApiIds.isWellFormed(read.get("id").asText())).isFalse();
    }

    /** Why a collection action needs the collection in the method's own pattern. */
    @Test
    void aCollectionActionUnderAClassMappedAtTheCollectionLandsOnAPathSegment() throws Exception {
        assertThat(send("POST", "/api/v2/fixtures/combined:probe").statusCode()).isEqualTo(404);
        assertThat(routed(send("POST", "/api/v2/fixtures/combined/:probe")).get("route").asText()).isEqualTo("probe");
    }
}
