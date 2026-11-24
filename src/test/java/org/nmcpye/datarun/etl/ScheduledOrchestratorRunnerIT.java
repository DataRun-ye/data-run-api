package org.nmcpye.datarun.etl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.admin.EtlRunLock;
import org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * When the scheduled runner starts a run, and the lock that keeps two runs from overlapping, against
 * PostgreSQL. The runner borrows its connections from a two-connection data source that hands them out in
 * turn, as a pool may.
 */
class ScheduledOrchestratorRunnerIT {

    private static final long LOCK_KEY = 123456789012345L;

    private static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.2");

    private static JdbcTemplate observer;

    private AlternatingDataSource pool;
    private final AtomicInteger runs = new AtomicInteger();

    @BeforeAll
    static void startDatabase() {
        PG.start();
        observer = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        observer.execute("""
            CREATE TABLE outbox (
              outbox_id bigserial PRIMARY KEY,
              submission_serial_number bigint NOT NULL,
              submission_id varchar(255) NOT NULL,
              submission_uid varchar(11) NOT NULL,
              topic varchar(255) NOT NULL,
              event_type varchar(128),
              payload jsonb,
              status varchar(255),
              attempt int,
              last_error text,
              next_attempt_at timestamp,
              ingest_id uuid,
              claimed_by varchar(255),
              claimed_at timestamp,
              created_at timestamp)""");
    }

    @AfterAll
    static void stopDatabase() {
        PG.stop();
    }

    @BeforeEach
    void openPool() throws Exception {
        observer.update("DELETE FROM outbox");
        pool = new AlternatingDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
    }

    @AfterEach
    void closePool() {
        pool.close();
    }

    private void outboxRow(String status, String nextAttemptAt) {
        observer.update("""
            INSERT INTO outbox (submission_serial_number, submission_id, submission_uid, topic, event_type, status,
                                attempt, next_attempt_at, created_at)
            VALUES (1, 'sub1', 'subUid00001', 'version0001', 'SAVE', ?, 0, CAST(? AS timestamp), now())""",
            status, nextAttemptAt);
    }

    private EtlOrchestrator countingOrchestrator() {
        return new EtlOrchestrator(null, null, null, null, null, new EtlProperties()) {
            @Override
            public void runOnce(String pipelineName, int batchSize) {
                runs.incrementAndGet();
            }
        };
    }

    private ScheduledOrchestratorRunner runner(EtlOrchestrator orchestrator) {
        return new ScheduledOrchestratorRunner(new NamedParameterJdbcTemplate(pool), orchestrator, new EtlRunLock(pool));
    }

    private ScheduledOrchestratorRunner runner() {
        return runner(countingOrchestrator());
    }

    private int heldLocks() {
        return observer.queryForObject("""
            SELECT count(*) FROM pg_locks
            WHERE locktype = 'advisory' AND granted AND ((classid::bigint << 32) | objid::bigint) = ?""",
            Integer.class, LOCK_KEY);
    }

    @Test
    void aFailedRowWhoseRetryTimeHasComeStartsARunWhenNothingIsPending() {
        outboxRow("failed", "2026-01-01 00:00:00");

        runner().scheduledRun();

        assertThat(runs).hasValue(1);
    }

    @Test
    void rowsThatAreLeasedWaitingToRetryDeadOrDoneStartNoRun() {
        outboxRow("pending", "2999-01-01 00:00:00");
        outboxRow("failed", "2999-01-01 00:00:00");
        outboxRow("dead", null);
        outboxRow("success", null);

        runner().scheduledRun();

        assertThat(runs).hasValue(0);
    }

    /**
     * The lock is taken and released on one connection kept for the run, whatever connection the pool hands out
     * next, so each run releases it (pinned before as released on another connection, held for good, and
     * every later run skipped).
     */
    @Test
    void eachRunReleasesTheLockWhateverConnectionComesNext() {
        outboxRow("pending", null);
        ScheduledOrchestratorRunner runner = runner(countingOrchestrator());

        for (int run = 1; run <= 3; run++) {
            runner.scheduledRun();
            assertThat(heldLocks()).isZero();
        }
        assertThat(runs).hasValue(3);
    }

    @Test
    void aRunIsSkippedWhileAnotherRunHoldsTheLock() throws Exception {
        outboxRow("pending", null);
        ScheduledOrchestratorRunner runner = runner(countingOrchestrator());

        try (Connection otherRun = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             PreparedStatement lock = otherRun.prepareStatement("SELECT pg_advisory_lock(?)")) {
            lock.setLong(1, LOCK_KEY);
            lock.execute();
            runner.scheduledRun();
            assertThat(runs).as("skipped while the other run holds the lock").hasValue(0);
        }

        runner.scheduledRun();
        assertThat(runs).as("runs once the other run has ended").hasValue(1);
    }

    @Test
    void aRunThatFailsStillReleasesTheLock() {
        outboxRow("pending", null);
        EtlOrchestrator failing = new EtlOrchestrator(null, null, null, null, null, new EtlProperties()) {
            @Override
            public void runOnce(String pipelineName, int batchSize) {
                throw new IllegalStateException("run failed");
            }
        };

        assertThatThrownBy(runner(failing)::scheduledRun).hasMessage("run failed");

        assertThat(heldLocks()).isZero();
        runner(countingOrchestrator()).scheduledRun();
        assertThat(runs).hasValue(1);
    }
}
