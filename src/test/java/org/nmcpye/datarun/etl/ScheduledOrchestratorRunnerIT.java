package org.nmcpye.datarun.etl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.admin.EtlRunLock;
import org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.etl.orchestrator.EtlRunResult;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

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
    private SimpleMeterRegistry registry;
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
        registry = new SimpleMeterRegistry();
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

    /** An orchestrator that claims the given batch sizes in turn, then nothing. */
    private EtlOrchestrator orchestratorClaiming(int... batches) {
        return new EtlOrchestrator(null, null, null, null, null, new EtlProperties(), null) {
            @Override
            public EtlRunResult runOnce(String pipelineName, int batchSize) {
                int run = runs.getAndIncrement();
                int claimed = run < batches.length ? batches[run] : 0;
                return new EtlRunResult(claimed, claimed, 0, false);
            }
        };
    }

    private EtlOrchestrator countingOrchestrator() {
        return orchestratorClaiming(1);
    }

    private ScheduledOrchestratorRunner runner(EtlOrchestrator orchestrator, EtlProperties properties) {
        return new ScheduledOrchestratorRunner(new NamedParameterJdbcTemplate(pool), orchestrator, new EtlRunLock(pool),
            new EtlMetrics(registry, pool, properties), properties);
    }

    private ScheduledOrchestratorRunner runner(EtlOrchestrator orchestrator) {
        return runner(orchestrator, new EtlProperties());
    }

    private ScheduledOrchestratorRunner runner() {
        return runner(countingOrchestrator());
    }

    private double lastSuccess() {
        return registry.get("datarun.etl.last.success").gauge().value();
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
        assertThat(lastSuccess()).as("a poll with nothing due still succeeds")
            .isCloseTo(System.currentTimeMillis() / 1000.0, within(5.0));
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
            assertThat(lastSuccess()).as("a skipped poll is not a success").isNaN();
        }

        runner.scheduledRun();
        assertThat(runs).as("runs once the other run has ended").hasValue(1);
        assertThat(lastSuccess()).isCloseTo(System.currentTimeMillis() / 1000.0, within(5.0));
    }

    @Test
    void aRunThatFailsStillReleasesTheLock() {
        outboxRow("pending", null);
        EtlOrchestrator failing = new EtlOrchestrator(null, null, null, null, null, new EtlProperties(), null) {
            @Override
            public EtlRunResult runOnce(String pipelineName, int batchSize) {
                throw new IllegalStateException("run failed");
            }
        };

        assertThatThrownBy(runner(failing)::scheduledRun).hasMessage("run failed");

        assertThat(lastSuccess()).as("a failed poll is not a success").isNaN();
        assertThat(heldLocks()).isZero();
        runner(countingOrchestrator()).scheduledRun();
        assertThat(runs).hasValue(1);
    }

    @Test
    void aPollClaimsBatchesUntilOneComesBackShort() {
        outboxRow("pending", null);
        EtlProperties properties = new EtlProperties();
        properties.setBatchSize(100);

        runner(orchestratorClaiming(100, 100, 100, 40), properties).scheduledRun();

        assertThat(runs).hasValue(4);
    }

    @Test
    void aPollStopsClaimingOnceItHasRunForItsMaximumDuration() {
        outboxRow("pending", null);
        EtlProperties properties = new EtlProperties();
        properties.setBatchSize(100);
        properties.setMaxPollDuration(Duration.ZERO);

        runner(orchestratorClaiming(100, 100, 100), properties).scheduledRun();

        assertThat(runs).as("one batch, then the next poll").hasValue(1);
    }

    @Test
    void thePollIsScheduledWithTheConfiguredDelayUnlessTheEtlIsDisabled() {
        EtlProperties properties = new EtlProperties();
        properties.setPollDelay(Duration.ofSeconds(45));
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        List<String> enabledLog = runnerLogOf(() -> runner(countingOrchestrator(), properties).configureTasks(registrar));

        assertThat(registrar.getTriggerTaskList()).singleElement().satisfies(task -> {
            assertThat(nextPollAfter(task.getTrigger(), null)).isEqualTo(NOW.plusSeconds(45));
            assertThat(nextPollAfter(task.getTrigger(), NOW.plusSeconds(100))).isEqualTo(NOW.plusSeconds(145));
        });

        properties.setEnabled(false);
        ScheduledTaskRegistrar disabled = new ScheduledTaskRegistrar();
        List<String> disabledLog = runnerLogOf(() -> runner(countingOrchestrator(), properties).configureTasks(disabled));
        assertThat(disabled.getTriggerTaskList()).isEmpty();

        // scripts/staging/prepare-candidate.sh fails preparation on the "polls" line: keep them in step.
        assertThat(enabledLog).anySatisfy(line -> assertThat(line).startsWith("The ETL polls the outbox"));
        assertThat(disabledLog).anySatisfy(line -> assertThat(line).startsWith("The ETL does not poll the outbox on this instance"));
        assertThat(disabledLog).noneSatisfy(line -> assertThat(line).startsWith("The ETL polls the outbox"));
    }

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    /** When the trigger starts the next poll, the previous one having ended at {@code lastCompletion}. */
    private static Instant nextPollAfter(Trigger trigger, Instant lastCompletion) {
        SimpleTriggerContext context = new SimpleTriggerContext(Clock.fixed(NOW, ZoneOffset.UTC));
        if (lastCompletion != null) {
            context.update(lastCompletion, lastCompletion, lastCompletion);
        }
        return trigger.nextExecution(context);
    }

    /**
     * A poll that stops is not a success: it is counted as stopped, and the pause before the next poll doubles up
     * to the cap. A poll that succeeds counts, and brings the pause back.
     */
    @Test
    void aStoppedPollIsNotASuccessAndLengthensThePauseUntilAPollSucceeds() {
        outboxRow("pending", null);
        EtlProperties properties = new EtlProperties();
        properties.setPollDelay(Duration.ofSeconds(30));
        properties.setMaxPollDelay(Duration.ofMinutes(5));
        boolean[] stopping = {true};
        EtlOrchestrator orchestrator = new EtlOrchestrator(null, null, null, null, null, new EtlProperties(), null) {
            @Override
            public EtlRunResult runOnce(String pipelineName, int batchSize) {
                return new EtlRunResult(1, 0, stopping[0] ? 1 : 0, stopping[0]);
            }
        };
        ScheduledOrchestratorRunner runner = runner(orchestrator, properties);
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        runner.configureTasks(registrar);
        Trigger trigger = registrar.getTriggerTaskList().get(0).getTrigger();

        List<Long> pauses = new ArrayList<>();
        for (int poll = 1; poll <= 5; poll++) {
            runner.scheduledRun();
            pauses.add(Duration.between(NOW, nextPollAfter(trigger, NOW)).toSeconds());
        }

        assertThat(pauses).containsExactly(60L, 120L, 240L, 300L, 300L);
        assertThat(lastSuccess()).as("a stopped poll is not a success").isNaN();
        assertThat(registry.get("datarun.etl.polls").tag("outcome", "stopped").counter().count()).isEqualTo(5);

        stopping[0] = false;
        runner.scheduledRun();
        assertThat(Duration.between(NOW, nextPollAfter(trigger, NOW)).toSeconds()).isEqualTo(30);
        assertThat(lastSuccess()).isCloseTo(System.currentTimeMillis() / 1000.0, within(5.0));
        assertThat(registry.get("datarun.etl.polls").tag("outcome", "succeeded").counter().count()).isEqualTo(1);
    }

    private static List<String> runnerLogOf(Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(ScheduledOrchestratorRunner.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void aPollStopsClaimingAfterABatchThatStopped() {
        outboxRow("pending", null);
        EtlProperties properties = new EtlProperties();
        properties.setBatchSize(100);
        EtlOrchestrator stopping = new EtlOrchestrator(null, null, null, null, null, new EtlProperties(), null) {
            @Override
            public EtlRunResult runOnce(String pipelineName, int batchSize) {
                runs.incrementAndGet();
                return new EtlRunResult(100, 40, 0, true);
            }
        };

        runner(stopping, properties).scheduledRun();

        assertThat(runs).as("a full batch that stopped ends the poll").hasValue(1);
    }
}
