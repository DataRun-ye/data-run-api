package org.nmcpye.datarun.web.v2.idempotency;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.v2.problem.ApiProblemException;
import org.nmcpye.datarun.web.v2.problem.ProblemCode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Keys are UUIDs, quoted or bare; equal requests hash equal (key order, whitespace, number spelling) and any
 * difference in method, path, query, If-Match or body hashes differently.
 */
class IdempotencyFingerprintTest {

    private static final String KEY = "8e03978e-40d5-43e8-bc93-6894a57f9324";

    @Test
    void aKeyIsOneUuidQuotedAsTheDraftWantsOrBare() {
        UUID expected = UUID.fromString(KEY);
        assertThat(IdempotencyKey.parse(List.of(KEY))).isEqualTo(expected);
        assertThat(IdempotencyKey.parse(List.of("\"" + KEY + "\""))).isEqualTo(expected);
        assertThat(IdempotencyKey.parse(List.of("  " + KEY.toUpperCase() + " "))).isEqualTo(expected);
    }

    @Test
    void aMissingKeyAndAnythingButOneUuidAre400() {
        assertThat(problem(null)).isEqualTo(ProblemCode.IDEMPOTENCY_KEY_MISSING);
        assertThat(problem(List.of())).isEqualTo(ProblemCode.IDEMPOTENCY_KEY_MISSING);
        assertThat(problem(List.of(" "))).isEqualTo(ProblemCode.IDEMPOTENCY_KEY_MISSING);
        for (String bad : List.of("line-7", "8e03978e40d543e8bc936894a57f9324", "{" + KEY + "}", "\"" + KEY,
            KEY + "0", "00000000-0000-0000-0000-000000000000", "ffffffff-ffff-ffff-ffff-ffffffffffff", "\"\"")) {
            assertThat(problem(List.of(bad))).as(bad).isEqualTo(ProblemCode.INVALID_IDEMPOTENCY_KEY);
        }
        assertThat(problem(List.of(KEY, KEY))).as("two field lines").isEqualTo(ProblemCode.INVALID_IDEMPOTENCY_KEY);
    }

    @Test
    void equalDocumentsHaveOneCanonicalForm() throws Exception {
        assertThat(canonical("{ \"b\" : [1, 2.0, {\"y\": true, \"x\": null}], \"a\": \"té\\n\" }"))
            .isEqualTo("{\"a\":\"té\\n\",\"b\":[1,2,{\"x\":null,\"y\":true}]}");
        for (String two : List.of("2", "2.0", "2.000", "2e0", "20e-1", "0.2E1")) {
            assertThat(canonical("{\"v\":" + two + "}")).as(two).isEqualTo("{\"v\":2}");
        }
        assertThat(canonical("{\"v\":-0.0}")).isEqualTo("{\"v\":0}");
        assertThat(canonical("{\"v\":1000}")).isEqualTo("{\"v\":1E+3}");
        assertThat(canonical("{\"v\":0.1}")).isNotEqualTo(canonical("{\"v\":0.10000000000000001}"));
        assertThat(canonical("{\"v\":12345678901234567890123}")).as("never through a double")
            .isEqualTo("{\"v\":12345678901234567890123}");
        assertThat(canonical("{\"a\":1,\"a\":2}")).as("the last of a repeated member").isEqualTo("{\"a\":2}");
        assertThat(canonical("[3,1,2]")).as("array order is kept").isEqualTo("[3,1,2]");
        assertThat(canonical("{\"م\":\"شبكة\"}"))
            .as("Arabic text is kept as sent").isEqualTo("{\"م\":\"شبكة\"}");
        assertThatThrownBy(() -> CanonicalJson.canonicalize("{\"a\":".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(java.io.IOException.class);
    }

    @Test
    void theFingerprintIsEqualForEqualRequestsOnly() {
        byte[] base = fingerprint("POST", "/api/v2/things/7/changes", "dryRun=false&mode=a", "\"3\"",
            "{\"reason\":\"r\",\"value\":2}");
        assertThat(base).hasSize(32);
        assertThat(fingerprint("POST", "/api/v2/things/7/changes", "mode=a&dryRun=false", "\"3\"",
            "{ \"value\": 2.0, \"reason\": \"r\" }")).as("reordered, respelled").isEqualTo(base);

        assertThat(fingerprint("PUT", "/api/v2/things/7/changes", "dryRun=false&mode=a", "\"3\"",
            "{\"reason\":\"r\",\"value\":2}")).isNotEqualTo(base);
        assertThat(fingerprint("POST", "/api/v2/things/8/changes", "dryRun=false&mode=a", "\"3\"",
            "{\"reason\":\"r\",\"value\":2}")).isNotEqualTo(base);
        assertThat(fingerprint("POST", "/api/v2/things/7/changes", "dryRun=true&mode=a", "\"3\"",
            "{\"reason\":\"r\",\"value\":2}")).isNotEqualTo(base);
        assertThat(fingerprint("POST", "/api/v2/things/7/changes", "dryRun=false&mode=a", "\"4\"",
            "{\"reason\":\"r\",\"value\":2}")).isNotEqualTo(base);
        assertThat(fingerprint("POST", "/api/v2/things/7/changes", "dryRun=false&mode=a", "\"3\"",
            "{\"reason\":\"r\",\"value\":3}")).isNotEqualTo(base);
        assertThat(RequestFingerprint.of("POST", "/p", "a=bc", "", null))
            .as("parts never run together").isNotEqualTo(RequestFingerprint.of("POST", "/p", "ab=c", "", null));
        assertThat(RequestFingerprint.of("POST", "/p", null, "", "not json".getBytes(StandardCharsets.UTF_8)))
            .as("a body that is not JSON is hashed as sent")
            .isNotEqualTo(RequestFingerprint.of("POST", "/p", null, "", "not  json".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void queriesAreOrderedByNameKeepingEachNamesValueOrder() {
        assertThat(RequestFingerprint.canonicalQuery("b=2&a=1&a=0"))
            .isEqualTo(RequestFingerprint.canonicalQuery("a=1&b=2&a=0"));
        assertThat(RequestFingerprint.canonicalQuery("a=1&a=0"))
            .isNotEqualTo(RequestFingerprint.canonicalQuery("a=0&a=1"));
        assertThat(RequestFingerprint.canonicalQuery("q=a%20b")).isEqualTo(RequestFingerprint.canonicalQuery("q=a+b"));
        assertThat(RequestFingerprint.canonicalQuery(null)).isEmpty();
    }

    private static ProblemCode problem(List<String> lines) {
        ApiProblemException problem = catchThrowableOfType(ApiProblemException.class,
            () -> IdempotencyKey.parse(lines));
        assertThat(problem).isNotNull();
        return problem.code();
    }

    private static String canonical(String json) throws Exception {
        return new String(CanonicalJson.canonicalize(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    private static byte[] fingerprint(String method, String path, String query, String ifMatch, String body) {
        return RequestFingerprint.of(method, path, query, ifMatch, body.getBytes(StandardCharsets.UTF_8));
    }
}
