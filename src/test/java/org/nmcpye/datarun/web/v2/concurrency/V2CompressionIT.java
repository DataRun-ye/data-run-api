package org.nmcpye.datarun.web.v2.concurrency;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.web.v2.fixtures.V2TestUsers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The application's own response compression, as the production profile sets it (application-prod.yml:
 * application/json among the types, from 1 KB), keeps a v2 entity tag strong, so If-Match still matches. nginx's
 * gzip module would weaken it; the proxy must not compress v2 JSON (release-boundary.md).
 */
@IntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
    "server.compression.enabled=true",
    "server.compression.mime-types=text/html,text/xml,text/plain,text/css,application/javascript,application/json,"
        + "image/svg+xml",
    "server.compression.min-response-size=1024"})
class V2CompressionIT {

    @LocalServerPort
    private int port;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    @BeforeAll
    void caller() {
        V2TestUsers.ensure(jdbc, transactions);
    }

    @Test
    void theApplicationsCompressionKeepsTheEntityTagStrong() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v2/fixtures/large"))
            .header("Authorization", V2TestUsers.bearer(jwtKey, V2TestUsers.CALLER_A))
            .header("Accept-Encoding", "gzip")
            .timeout(Duration.ofSeconds(20))
            .GET().build();
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Encoding")).as("compressed").hasValue("gzip");
        assertThat(response.headers().firstValue("ETag")).as("still strong").hasValue("\"5\"");
        try (GZIPInputStream body = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            assertThat(new String(body.readAllBytes(), StandardCharsets.UTF_8)).contains("\"version\":5");
        }
    }
}
