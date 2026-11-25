package org.nmcpye.datarun.web.v2.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.web.v2.fixtures.V2CommandFixtureController;
import org.nmcpye.datarun.web.v2.fixtures.V2TestUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * v2 commands end to end, on a test-only versioned counter, with real tokens: the key is claimed before the row
 * lock and the If-Match check, a retry is answered from the first outcome (even once its If-Match is stale) and
 * never applied twice, refusals leave nothing behind, keys are per caller and expire, and the record holds ids, a
 * hash and a version only.
 */
@IntegrationTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class V2CommandsIT {

    private static final String COUNTER = "c1";
    private static final String INCREMENT = "/api/v2/fixtures/counters/" + COUNTER + ":increment";

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
    void fixtures() {
        V2TestUsers.ensure(jdbc, transactions);
        write(() -> {
            jdbc.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS v2_fixture_counter (
                    id varchar(36) PRIMARY KEY, version bigint NOT NULL, value bigint NOT NULL)""");
            jdbc.getJdbcTemplate().execute("""
                CREATE TABLE IF NOT EXISTS v2_fixture_counter_version (
                    id varchar(36) NOT NULL, version bigint NOT NULL, value bigint NOT NULL,
                    PRIMARY KEY (id, version))""");
        });
    }

    @BeforeEach
    void freshCounter() {
        write(() -> {
            jdbc.update("DELETE FROM v2_fixture_counter_version", Map.of());
            jdbc.update("DELETE FROM v2_fixture_counter", Map.of());
            jdbc.update("INSERT INTO v2_fixture_counter (id, version, value) VALUES (:id, 0, 0)", Map.of("id", COUNTER));
            jdbc.update("INSERT INTO v2_fixture_counter_version (id, version, value) VALUES (:id, 0, 0)",
                Map.of("id", COUNTER));
            jdbc.update("DELETE FROM api.idempotency_key WHERE actor_user_id IN (:callers)", Map.of("callers",
                java.util.List.of(V2TestUsers.CALLER_A_ID, V2TestUsers.CALLER_B_ID)));
        });
        V2CommandFixtureController.RUNS.set(0);
    }

    private void write(Runnable work) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> work.run());
    }

    private MockHttpServletResponse increment(String login, String key, String ifMatch, String body) throws Exception {
        return send(post(INCREMENT), login, key, ifMatch, body);
    }

    private MockHttpServletResponse send(MockHttpServletRequestBuilder request, String login, String key,
                                         String ifMatch, String body) throws Exception {
        request
            .header(HttpHeaders.AUTHORIZATION, V2TestUsers.bearer(jwtKey, login))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
        if (key != null) {
            request.header(IdempotencyKey.HEADER, key);
        }
        if (ifMatch != null) {
            request.header(HttpHeaders.IF_MATCH, ifMatch);
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    private MockHttpServletResponse increment(String key, String ifMatch, String body) throws Exception {
        return increment(V2TestUsers.CALLER_A, key, ifMatch, body);
    }

    private JsonNode body(MockHttpServletResponse response) throws Exception {
        return json.readTree(response.getContentAsByteArray());
    }

    private String problemCode(MockHttpServletResponse response) throws Exception {
        assertThat(response.getContentType()).startsWith("application/problem+json");
        return body(response).get("code").asText();
    }

    private long storedVersion() {
        return jdbc.queryForObject("SELECT version FROM v2_fixture_counter WHERE id = :id", Map.of("id", COUNTER),
            Long.class);
    }

    private int records() {
        return jdbc.queryForObject("SELECT count(*) FROM api.idempotency_key WHERE actor_user_id IN (:callers)",
            Map.of("callers", java.util.List.of(V2TestUsers.CALLER_A_ID, V2TestUsers.CALLER_B_ID)), Integer.class);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    @Test
    void aRetryIsAnsweredFromTheFirstOutcomeEvenWhenItsIfMatchIsStaleByNow() throws Exception {
        String key = key();
        MockHttpServletResponse first = increment(key, "\"0\"", "{\"by\": 2}");
        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(first.getHeader(HttpHeaders.ETAG)).isEqualTo("\"1\"");
        assertThat(first.getHeader(IdempotentCommands.REPLAYED_HEADER)).isNull();
        assertThat(body(first).get("version").asLong()).isEqualTo(1);
        assertThat(body(first).get("value").asLong()).isEqualTo(2);

        MockHttpServletResponse retry = increment(key, "\"0\"", "{ \"by\" : 2.0 }");

        assertThat(retry.getStatus()).isEqualTo(200);
        assertThat(retry.getHeader(IdempotentCommands.REPLAYED_HEADER)).isEqualTo("true");
        assertThat(retry.getHeader(HttpHeaders.ETAG)).isEqualTo("\"1\"");
        assertThat(retry.getContentAsString()).isEqualTo(first.getContentAsString());
        assertThat(V2CommandFixtureController.RUNS.get()).as("the command ran once").isEqualTo(1);
        assertThat(storedVersion()).isEqualTo(1);
    }

    /** The path is compared decoded: a colon sent as {@code %3A} by one client and as is by another is one request. */
    @Test
    void theSameRequestWithItsColonEncodedIsTheSameRequest() throws Exception {
        String key = key();
        increment(key, "\"0\"", "{\"by\": 2}");

        MockHttpServletResponse encoded = send(post(java.net.URI.create("/api/v2/fixtures/counters/c1%3Aincrement")),
            V2TestUsers.CALLER_A, key, "\"0\"", "{\"by\": 2}");

        assertThat(encoded.getStatus()).isEqualTo(200);
        assertThat(encoded.getHeader(IdempotentCommands.REPLAYED_HEADER)).isEqualTo("true");
        assertThat(V2CommandFixtureController.RUNS.get()).isEqualTo(1);
    }

    @Test
    void aReplayRendersTheVersionItProducedNotTheCurrentOne() throws Exception {
        String key = key();
        increment(key, "\"0\"", "{\"by\": 2}");
        assertThat(increment(key(), "\"1\"", "{\"by\": 3}").getStatus()).isEqualTo(200);

        MockHttpServletResponse replay = increment(key, "\"0\"", "{\"by\": 2}");

        assertThat(replay.getHeader(HttpHeaders.ETAG)).isEqualTo("\"1\"");
        assertThat(body(replay).get("version").asLong()).isEqualTo(1);
        assertThat(body(replay).get("value").asLong()).isEqualTo(2);
        assertThat(storedVersion()).isEqualTo(2);
    }

    @Test
    void theSameKeyForADifferentRequestIs422AndRunsNothing() throws Exception {
        String key = key();
        increment(key, "\"0\"", "{\"by\": 2}");

        MockHttpServletResponse otherBody = increment(key, "\"0\"", "{\"by\": 3}");
        assertThat(otherBody.getStatus()).isEqualTo(422);
        assertThat(problemCode(otherBody)).isEqualTo("idempotency-key-reused");

        MockHttpServletResponse otherPrecondition = increment(key, "\"1\"", "{\"by\": 2}");
        assertThat(problemCode(otherPrecondition)).as("If-Match is part of the request").isEqualTo("idempotency-key-reused");
        assertThat(V2CommandFixtureController.RUNS.get()).isEqualTo(1);
        assertThat(storedVersion()).isEqualTo(1);
    }

    @Test
    void aStaleIfMatchIs412AndLeavesNoRecordSoTheKeyRunsAgain() throws Exception {
        increment(key(), "\"0\"", "{\"by\": 2}");
        String key = key();

        MockHttpServletResponse stale = increment(key, "\"0\"", "{\"by\": 5}");

        assertThat(stale.getStatus()).isEqualTo(412);
        assertThat(problemCode(stale)).isEqualTo("precondition-failed");
        assertThat(body(stale).get("currentVersion").asLong()).isEqualTo(1);
        assertThat(stale.getHeader(HttpHeaders.ETAG)).isEqualTo("\"1\"");
        assertThat(stale.getHeader(HttpHeaders.LINK))
            .isEqualTo("</api/v2/fixtures/counters/c1/versions>; rel=\"version-history\"");
        assertThat(records()).as("only the first command's record").isEqualTo(1);

        MockHttpServletResponse decided = increment(key, "\"1\"", "{\"by\": 5}");
        assertThat(decided.getStatus()).isEqualTo(200);
        assertThat(body(decided).get("value").asLong()).isEqualTo(7);
    }

    @Test
    void missingOrMalformedHeadersAreRefusedBeforeAnythingRuns() throws Exception {
        assertThat(problemCode(increment(null, "\"0\"", "{\"by\": 1}"))).isEqualTo("idempotency-key-missing");
        assertThat(problemCode(increment("line-7", "\"0\"", "{\"by\": 1}"))).isEqualTo("invalid-idempotency-key");
        MockHttpServletResponse noPrecondition = increment(key(), null, "{\"by\": 1}");
        assertThat(noPrecondition.getStatus()).isEqualTo(428);
        assertThat(problemCode(noPrecondition)).isEqualTo("precondition-required");
        assertThat(problemCode(increment(key(), "*", "{\"by\": 1}"))).isEqualTo("precondition-required");
        assertThat(problemCode(increment(key(), "0", "{\"by\": 1}"))).isEqualTo("invalid-if-match");
        assertThat(problemCode(increment(key(), "\"0\"", "{}"))).as("an invalid body").isEqualTo("validation-failed");
        assertThat(V2CommandFixtureController.RUNS.get()).isZero();
        assertThat(records()).isZero();

        assertThat(increment("\"" + key() + "\"", "\"0\"", "{\"by\": 1}").getStatus())
            .as("the draft's quoted form").isEqualTo(200);
    }

    /** A body the command does not read is not hashed as empty: the request fails and nothing is claimed. */
    @Test
    void aBodyTheCommandDoesNotReadFailsInsteadOfBeingIgnored() throws Exception {
        MockHttpServletResponse withBody = send(post("/api/v2/fixtures/counters/c1:bump"), V2TestUsers.CALLER_A,
            key(), "\"0\"", "{\"by\": 5}");
        assertThat(withBody.getStatus()).isEqualTo(500);
        assertThat(problemCode(withBody)).isEqualTo("internal-error");
        assertThat(records()).isZero();
        assertThat(storedVersion()).isZero();

        MockHttpServletResponse without = mvc.perform(post("/api/v2/fixtures/counters/c1:bump")
            .header(HttpHeaders.AUTHORIZATION, V2TestUsers.bearer(jwtKey, V2TestUsers.CALLER_A))
            .header(IdempotencyKey.HEADER, key()).header(HttpHeaders.IF_MATCH, "\"0\"")).andReturn().getResponse();
        assertThat(without.getStatus()).isEqualTo(200);
        assertThat(storedVersion()).isEqualTo(1);
    }

    @Test
    void aBodyOverTheLimitIs413() throws Exception {
        String big = "{\"by\": 1, \"padding\": \"" + "x".repeat(1024 * 1024) + "\"}";
        MockHttpServletResponse response = increment(key(), "\"0\"", big);
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(problemCode(response)).isEqualTo("payload-too-large");
        assertThat(V2CommandFixtureController.RUNS.get()).isZero();
    }

    @Test
    void keysBelongToTheirCaller() throws Exception {
        String key = key();
        assertThat(increment(V2TestUsers.CALLER_A, key, "\"0\"", "{\"by\": 1}").getStatus()).isEqualTo(200);

        MockHttpServletResponse other = increment(V2TestUsers.CALLER_B, key, "\"1\"", "{\"by\": 1}");
        assertThat(other.getStatus()).isEqualTo(200);
        assertThat(other.getHeader(IdempotentCommands.REPLAYED_HEADER)).isNull();
        assertThat(body(other).get("version").asLong()).isEqualTo(2);

        MockHttpServletResponse replay = increment(V2TestUsers.CALLER_A, key, "\"0\"", "{\"by\": 1}");
        assertThat(replay.getHeader(IdempotentCommands.REPLAYED_HEADER)).isEqualTo("true");
        assertThat(body(replay).get("version").asLong()).isEqualTo(1);
    }

    @Test
    void theRecordHoldsTheCallersIdAHashAndTheOutcomeOnly() throws Exception {
        String key = key();
        increment(key, "\"0\"", "{\"by\": 2}");

        Map<String, Object> record = jdbc.queryForMap("""
            SELECT actor_user_id, octet_length(request_hash) AS hash_bytes, resource_type, resource_id,
                   resource_version, response_status,
                   expires_at - created_at AS kept
            FROM api.idempotency_key WHERE idempotency_key = :key""",
            new MapSqlParameterSource("key", UUID.fromString(key)));
        assertThat(record.get("actor_user_id")).isEqualTo(V2TestUsers.CALLER_A_ID);
        assertThat(record.get("hash_bytes")).isEqualTo(32);
        assertThat(record.get("resource_type")).isEqualTo("fixture-counter");
        assertThat(record.get("resource_id")).isEqualTo(COUNTER);
        assertThat(record.get("resource_version")).isEqualTo(1L);
        assertThat(((Number) record.get("response_status")).intValue()).isEqualTo(200);
        assertThat(record.get("kept").toString()).isEqualTo("0 years 0 mons 3 days 0 hours 0 mins 0.0 secs");
    }

    @Test
    void anExpiredKeyRunsAsANewRequest() throws Exception {
        String key = key();
        increment(key, "\"0\"", "{\"by\": 2}");
        // The pool does not auto-commit: a write outside a transaction would be lost.
        write(() -> jdbc.update("""
            UPDATE api.idempotency_key SET created_at = now() - interval '80 hours', expires_at = now() - interval '8 hours'
            WHERE idempotency_key = :key""", new MapSqlParameterSource("key", UUID.fromString(key))));

        MockHttpServletResponse again = increment(key, "\"0\"", "{\"by\": 2}");

        assertThat(again.getStatus()).as("forgotten, so it runs, and its If-Match is stale").isEqualTo(412);
        assertThat(V2CommandFixtureController.RUNS.get()).isEqualTo(2);
        MockHttpServletResponse redecided = increment(key, "\"1\"", "{\"by\": 2}");
        assertThat(redecided.getStatus()).isEqualTo(200);
        assertThat(redecided.getHeader(IdempotentCommands.REPLAYED_HEADER)).isNull();
        assertThat(increment(key, "\"1\"", "{\"by\": 2}").getHeader(IdempotentCommands.REPLAYED_HEADER))
            .isEqualTo("true");
    }
}
