package org.nmcpye.datarun.etl;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

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

    @Test
    void theMetersAreServedUnauthenticatedAtTheManagementPrometheusEndpoint() {
        String scrape = http.getForObject("/management/prometheus", String.class);

        assertThat(scrape).contains(
            "datarun_etl_outbox_rows{status=\"pending\",}",
            "datarun_etl_outbox_rows{status=\"failed\",}",
            "datarun_etl_outbox_rows{status=\"dead\",}",
            "datarun_etl_outbox_oldest_age_seconds ",
            "datarun_etl_last_success_seconds ",
            "datarun_etl_outbox_processed_total{outcome=\"success\",}",
            "datarun_etl_outbox_processing_seconds_count ");
    }
}
