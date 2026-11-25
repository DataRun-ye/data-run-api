package org.nmcpye.datarun.etl;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.security.jwt.JwtAuthenticationTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.nmcpye.datarun.etl.EtlFixture.TEMPLATE;

/** What the ETL tells Micrometer, read from the application's own meter registry. */
@EtlIntegrationTest
class EtlMetricsIT {

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private DataSource dataSource;

    @Value("${jhipster.security.authentication.jwt.base64-secret}")
    private String jwtKey;

    private EtlFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new EtlFixture(jdbc, transactionManager);
        fixture.clean();
        fixture.defineTemplate(TEMPLATE);
    }

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    private double processed(String outcome) {
        return registry.get("datarun.etl.outbox.processed").tag("outcome", outcome).counter().count();
    }

    private double rows(String status) {
        return registry.get("datarun.etl.outbox.rows").tag("status", status).gauge().value();
    }

    private double oldestAge() {
        return registry.get("datarun.etl.outbox.oldest.age").gauge().value();
    }

    @Test
    void eachRowsOutcomeIsCountedAndItsTimeRecorded() {
        double success = processed("success");
        double failed = processed("failed");
        double dead = processed("dead");
        long timed = registry.get("datarun.etl.outbox.processing").timer().count();
        fixture.failProjectionsOfFailUids();
        fixture.submission("okSub000001", TEMPLATE, 1);
        fixture.submission("failSub0001", TEMPLATE, 1);
        fixture.outbox("okSub000001", "SAVE");
        fixture.outbox("failSub0001", "SAVE");
        fixture.outboxWithoutSubmission("goneSub0001", 1);

        orchestrator.runOnce("test", 10);

        assertThat(processed("success") - success).isEqualTo(1);
        assertThat(processed("failed") - failed).isEqualTo(1);
        assertThat(processed("dead") - dead).isEqualTo(1);
        assertThat(registry.get("datarun.etl.outbox.processing").timer().count() - timed).isEqualTo(3);
    }

    @Test
    void theBacklogGaugesCountUnfinishedRowsByStatusAndAgeTheOldestChangeNotYetProjected() {
        fixture.outboxRow("pending", 10, null);
        fixture.outboxRow("pending", 1, 15);
        fixture.outboxRow("failed", 5, 3);
        fixture.outboxRow("dead", 120, null);
        fixture.outboxRow("success", 180, null);

        assertThat(rows("pending")).as("leased rows are still pending").isEqualTo(2);
        assertThat(rows("failed")).isEqualTo(1);
        assertThat(rows("dead")).isEqualTo(1);
        assertThat(oldestAge()).as("dead and done rows do not count").isCloseTo(600, within(5.0));
    }

    @Test
    void withNothingWaitingTheBacklogIsEmptyAndTheOldestAgeIsZero() {
        fixture.outboxRow("success", 30, null);

        assertThat(rows("pending")).isZero();
        assertThat(rows("failed")).isZero();
        assertThat(rows("dead")).isZero();
        assertThat(oldestAge()).isZero();
    }

    /** To an admin: the endpoint is not public since 6.5.5 (ManagementRouteSecurityIT). */
    @Test
    void theMetersAreServedToAnAdminAtTheManagementPrometheusEndpoint() {
        HttpHeaders admin = new HttpHeaders();
        admin.setBearerAuth(JwtAuthenticationTestUtils.createValidTokenForUser(jwtKey, "admin"));
        String scrape = http.exchange("/management/prometheus", HttpMethod.GET, new HttpEntity<>(admin), String.class)
            .getBody();

        assertThat(scrape).contains(
            "datarun_etl_outbox_rows{status=\"pending\",}",
            "datarun_etl_outbox_rows{status=\"failed\",}",
            "datarun_etl_outbox_rows{status=\"dead\",}",
            "datarun_etl_outbox_oldest_age_seconds ",
            "datarun_etl_last_success_seconds ",
            "datarun_etl_outbox_processed_total{outcome=\"success\",}",
            "datarun_etl_outbox_processing_seconds_count ");
    }

    /**
     * Scheduled, the backlog gauges show the last background reading: a scrape never waits on the database, so it
     * still answers when the pool is exhausted. With the ETL switched off nothing is scheduled.
     */
    @Test
    void whenScheduledAScrapeShowsTheLastBackgroundReadingAndNeverReadsTheOutbox() {
        EtlProperties properties = new EtlProperties();
        SimpleMeterRegistry scraped = new SimpleMeterRegistry();
        EtlMetrics background = new EtlMetrics(scraped, dataSource, properties);
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        background.configureTasks(registrar);

        assertThat(registrar.getFixedDelayTaskList()).singleElement().satisfies(task -> {
            assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofSeconds(15));
            assertThat(task.getInitialDelayDuration()).isZero();
        });
        fixture.outboxRow("pending", 10, null);
        assertThat(pendingIn(scraped)).as("no reading taken yet").isNaN();

        background.refreshBacklog();
        assertThat(pendingIn(scraped)).isEqualTo(1);

        fixture.outboxRow("pending", 5, null);
        assertThat(pendingIn(scraped)).as("a scrape shows the last reading").isEqualTo(1);

        properties.setEnabled(false);
        ScheduledTaskRegistrar disabled = new ScheduledTaskRegistrar();
        new EtlMetrics(new SimpleMeterRegistry(), dataSource, properties).configureTasks(disabled);
        assertThat(disabled.getFixedDelayTaskList()).isEmpty();
    }

    private static double pendingIn(SimpleMeterRegistry scraped) {
        return scraped.get("datarun.etl.outbox.rows").tag("status", "pending").gauge().value();
    }
}
