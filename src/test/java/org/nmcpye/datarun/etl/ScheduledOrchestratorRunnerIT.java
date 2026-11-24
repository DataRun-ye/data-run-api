package org.nmcpye.datarun.etl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

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

    private ScheduledOrchestratorRunner runner() {
        EtlOrchestrator countingOrchestrator = new EtlOrchestrator(null, null, null, null, null, new EtlProperties()) {
            @Override
            public void runOnce(String pipelineName, int batchSize) {
                runs.incrementAndGet();
            }
        };
        return new ScheduledOrchestratorRunner(new NamedParameterJdbcTemplate(pool), countingOrchestrator);
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
     * Today: the session-level lock is taken on one borrowed connection and released on whichever connection
     * is borrowed next. Released on the other one, it stays held, and later runs are skipped.
     */
    @Test
    void today_theLockReleasedOnAnotherConnectionStaysHeldAndLaterRunsAreSkipped() {
        outboxRow("pending", null);
        ScheduledOrchestratorRunner runner = runner();

        runner.scheduledRun();
        assertThat(runs).hasValue(1);
        assertThat(heldLocks()).as("the lock outlives the run").isEqualTo(1);

        runner.scheduledRun();
        runner.scheduledRun();
        assertThat(runs).as("each later run finds the lock taken and skips").hasValue(1);
    }
}
