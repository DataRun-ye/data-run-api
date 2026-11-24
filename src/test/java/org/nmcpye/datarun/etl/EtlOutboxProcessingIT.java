package org.nmcpye.datarun.etl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.admin.EtlRunLock;
import org.nmcpye.datarun.etl.admin.ScheduledOrchestratorRunner;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.metrics.EtlHeartbeat;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.etl.orchestrator.EtlRunResult;
import org.nmcpye.datarun.etl.service.EtlRunService;
import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.service.OutboxClaimService;
import org.nmcpye.datarun.outbox.service.OutboxProcessingService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.nmcpye.datarun.etl.EtlFixture.TEMPLATE;

/**
 * The outbox consumer (claim, project, record) against PostgreSQL migrated by Liquibase.
 * <p>
 * A watchdog cancels an audit insert stuck for 3 s, so no test can hang the build; each test checks that none
 * was needed.
 */
@EtlIntegrationTest
class EtlOutboxProcessingIT {

    private static final double STUCK_AFTER_SECONDS = 3.0;

    @Autowired
    private EtlOrchestrator orchestrator;

    @Autowired
    private OutboxClaimService claimService;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EventProcessorService eventProcessor;

    @Autowired
    private EtlRunService etlRunService;

    @Autowired
    private OutboxProcessingService processingService;

    @Autowired
    private EtlProperties etlProperties;

    @Autowired
    private EtlMetrics metrics;

    private EtlFixture fixture;
    private AuditInsertWatchdog watchdog;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new EtlFixture(jdbc, transactionManager);
        fixture.clean();
        fixture.defineTemplate(TEMPLATE);
        watchdog = new AuditInsertWatchdog(dataSource, STUCK_AFTER_SECONDS);
    }

    @AfterEach
    void tearDown() throws Exception {
        watchdog.close();
        fixture.clean();
    }

    @Test
    void theMigratedSchemaHasAValidIndexOfUnfinishedOutboxRows() {
        assertThat(jdbc.queryForObject("""
            SELECT i.indisvalid FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid
            WHERE c.relname = 'idx_outbox_unfinished'""", Map.of(), Boolean.class)).isTrue();
    }

    @Test
    void aSavedSubmissionIsProjectedAndItsRowMarkedSuccess() {
        fixture.submission("okSub000001", TEMPLATE, 2);
        long outboxId = fixture.outbox("okSub000001", "SAVE");

        orchestrator.runOnce("test", 10);

        assertThat(fixture.outboxRow(outboxId)).containsEntry("status", "success");
        // 4 root values, and per member row the row itself plus its 3 values.
        assertThat(fixture.tallRows("okSub000001")).isEqualTo(4 + 2 * 4);
        assertThat(fixture.events("okSub000001")).extracting(e -> e.get("event_type"))
            .containsExactlyInAnyOrder("root", "repeat", "repeat");
        assertThat(fixture.auditRows(outboxId)).extracting(r -> r.get("status")).containsExactly("success");
        assertThat(fixture.runs()).singleElement().satisfies(run -> {
            assertThat(run).containsEntry("status", "finished");
            assertThat(run).containsEntry("submitted_count", 1L);
            assertThat(run).containsEntry("success_count", 1L);
            assertThat(run).containsEntry("failure_count", 0L);
        });
        assertThat(watchdog.cancelled()).isEmpty();
    }

    @Test
    void aDeleteMarksTheSubmissionEventsDeletedButLeavesItsTallRows() {
        fixture.submission("delSub00001", TEMPLATE, 1);
        fixture.outbox("delSub00001", "SAVE");
        orchestrator.runOnce("test", 10);
        long delete = fixture.outbox("delSub00001", "DELETE");

        orchestrator.runOnce("test", 10);

        assertThat(fixture.outboxRow(delete)).containsEntry("status", "success");
        assertThat(fixture.events("delSub00001")).allSatisfy(e -> assertThat(e.get("deleted_at")).isNotNull());
        assertThat(fixture.tallRows("delSub00001")).isEqualTo(4 + 4);
        assertThat(watchdog.cancelled()).isEmpty();
    }

    /**
     * A database error is recorded on the outbox row: failed, with its attempt, the database's own error line
     * (not the statement and its values) and a retry time one retry delay away.
     */
    @Test
    void aDatabaseFailureIsRecordedOnTheRowWithItsErrorAndARetryTime() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 1);
        long failing = fixture.outbox("failSub0001", "SAVE");

        orchestrator.runOnce("test", 10);

        assertThat(watchdog.cancelled()).isEmpty();
        Map<String, Object> row = fixture.outboxRow(failing);
        assertThat(row).containsEntry("status", "failed").containsEntry("attempt", 1);
        assertThat((String) row.get("last_error")).isEqualTo("ERROR: injected projection failure [SQLState P0001]");
        assertThat(fixture.leaseSecondsLeft(failing)).isBetween(50.0, 60.0);
        assertThat(fixture.auditRows(failing)).singleElement().satisfies(audit -> {
            assertThat(audit).containsEntry("status", "failure").containsEntry("attempt", 1);
            assertThat(audit).containsEntry("error", row.get("last_error"));
        });
        assertThat(fixture.tallRows("failSub0001")).isZero();
        assertThat(fixture.runs()).singleElement().satisfies(r -> assertThat(r).containsEntry("failure_count", 1L));
    }

    /** A failed row waits for its retry time, so the next run moves on to newer rows instead of stalling. */
    @Test
    void aFailedRowDoesNotKeepNewerRowsWaiting() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 1);
        fixture.submission("okSub000002", TEMPLATE, 1);
        long failing = fixture.outbox("failSub0001", "SAVE");
        long newer = fixture.outbox("okSub000002", "SAVE");

        orchestrator.runOnce("test", 1);
        orchestrator.runOnce("test", 1);
        orchestrator.runOnce("test", 1);

        assertThat(fixture.outboxRow(failing)).containsEntry("status", "failed").containsEntry("attempt", 1);
        assertThat(fixture.isLeased(failing)).isTrue();
        assertThat(fixture.auditRows(failing)).hasSize(1);
        assertThat(fixture.outboxRow(newer)).containsEntry("status", "success").containsEntry("attempt", 1);
        assertThat(fixture.runs()).extracting(r -> r.get("submitted_count")).containsExactly(1L, 1L, 0L);
        assertThat(watchdog.cancelled()).isEmpty();
    }

    /** Each further failure doubles the wait, up to an hour; the tenth failure makes the row dead. */
    @Test
    void aRowThatKeepsFailingWaitsLongerEachTimeAndIsDeadAfterItsLastTry() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 1);
        long failing = fixture.outbox("failSub0001", "SAVE");

        List<Long> waits = new ArrayList<>();
        for (int attempt = 1; attempt <= 9; attempt++) {
            orchestrator.runOnce("test", 10);
            assertThat(fixture.outboxRow(failing)).containsEntry("status", "failed").containsEntry("attempt", attempt);
            waits.add(Math.round(fixture.leaseSecondsLeft(failing) / 60));
            fixture.endLease(failing);
        }
        orchestrator.runOnce("test", 10);

        assertThat(waits).as("minutes to the next try").containsExactly(1L, 2L, 4L, 8L, 16L, 32L, 60L, 60L, 60L);
        Map<String, Object> row = fixture.outboxRow(failing);
        assertThat(row).containsEntry("status", "dead").containsEntry("attempt", 10);
        assertThat(row.get("next_attempt_at")).isNull();
        assertThat((String) row.get("last_error")).contains("injected projection failure");
        assertThat(fixture.auditRows(failing)).extracting(r -> r.get("status")).hasSize(10).endsWith("dead");

        orchestrator.runOnce("test", 10);
        assertThat(fixture.outboxRow(failing)).as("a dead row is not claimed again").containsEntry("attempt", 10);
    }

    /**
     * A failure raised in Java while the transaction is still open is recorded after that transaction rolled
     * back, so the run no longer waits on its own uncommitted audit row (pinned before as a hang).
     */
    @Test
    void aFailureRaisedOutsideTheDatabaseIsRecordedWithoutTheRunWaitingOnItself() throws Exception {
        fixture.defineTemplateTheApplicationCannotRead("etlTmpl0002");
        fixture.submission("javaSub0001", "etlTmpl0002", 0);
        long failing = fixture.outbox("javaSub0001", "SAVE");

        CompletableFuture.runAsync(() -> orchestrator.runOnce("test", 10)).get(5, TimeUnit.SECONDS);

        assertThat(watchdog.cancelled()).isEmpty();
        Map<String, Object> row = fixture.outboxRow(failing);
        assertThat(row).containsEntry("status", "failed").containsEntry("attempt", 1);
        assertThat((String) row.get("last_error"))
            .startsWith("IllegalArgumentException at ")
            .contains(", from org.nmcpye.datarun.")
            .doesNotContain("NotASemanticType");
        assertThat(fixture.auditRows(failing)).extracting(r -> r.get("status")).containsExactly("failure");
        assertThat(fixture.runs()).singleElement().satisfies(r -> assertThat(r).containsEntry("failure_count", 1L));
    }

    /**
     * A projection waiting on another session's lock gives up when the row timeout (3 s here) runs out, and is
     * tried again later, instead of holding the ETL for as long as that session stays open.
     */
    @Test
    void aRowWaitingOnAnotherSessionsLockFailsWhenTheRowTimeoutRunsOut() throws Exception {
        fixture.submission("lockSub0001", TEMPLATE, 1);
        fixture.outbox("lockSub0001", "SAVE");
        orchestrator.runOnce("test", 10);
        long update = fixture.outbox("lockSub0001", "UPDATE");

        double seconds;
        try (Connection otherSession = dataSource.getConnection()) {
            otherSession.setAutoCommit(false);
            try (PreparedStatement hold = otherSession.prepareStatement(
                "UPDATE analytics.tall_canonical SET updated_at = now() WHERE submission_uid = 'lockSub0001'")) {
                hold.executeUpdate();
            }
            long start = System.nanoTime();
            orchestrator.runOnce("test", 10);
            seconds = (System.nanoTime() - start) / 1e9;
            otherSession.rollback();
        }

        assertThat(seconds).isBetween(2.5, 8.0);
        Map<String, Object> row = fixture.outboxRow(update);
        assertThat(row).containsEntry("status", "failed").containsEntry("attempt", 1);
        assertThat((String) row.get("last_error")).endsWith("[SQLState 57014]");
        assertThat(watchdog.cancelled()).isEmpty();

        fixture.endLease(update);
        orchestrator.runOnce("test", 10);
        assertThat(fixture.outboxRow(update)).as("tried again once the lock is gone").containsEntry("status", "success");
    }

    /**
     * A failing projection logs identifiers and shapes, never what was submitted: the driver's batch error repeats
     * the statement with its values, and those logs end up in the server's container logs. The ETL logs at DEBUG
     * here, so the debug line is checked too.
     */
    @Test
    void aFailingProjectionLogsIdentifiersAndShapesButNoSubmittedValue() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 2);
        long failing = fixture.outbox("failSub0001", "SAVE");

        List<String> logged = logsOf(() -> orchestrator.runOnce("test", 10), "org.nmcpye.datarun.etl");

        // 4 root values and, per member row, the row and its 3 values; 3 instances; 8 field definitions.
        assertThat(logged).anySatisfy(line -> assertThat(line).startsWith(
            "Tall upsert failed for submission failSub0001 (outbox " + failing + "): 12 rows, 3 instances, 8 field definitions"));
        assertThat(logged).anySatisfy(line -> assertThat(line).contains("(message left out)"));
        assertThat(String.join("\n", logged)).doesNotContain("household failSub0001", "first visit", "member 1", "member 2");
    }

    /**
     * Whatever is logged anywhere while the projection fails, Spring's transaction and JDBC code at DEBUG
     * included: when a rollback itself fails, Spring logs the projection's exception at ERROR, and its SQL error
     * translation logs the driver's message at DEBUG. With logServerErrorDetail=false the driver puts neither
     * the batch's bound values nor the server's Detail into its messages.
     */
    @Test
    void aFailingProjectionLeavesNoSubmittedValueInAnyLogWithSpringAtDebug() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 2);
        fixture.outbox("failSub0001", "SAVE");

        List<String> logged = logsOf(() -> orchestrator.runOnce("test", 10),
            "org.springframework.transaction", "org.springframework.jdbc", "org.springframework.orm", "org.nmcpye.datarun");

        assertThat(logged).as("Spring logged the projection's exception itself").anySatisfy(line -> assertThat(line)
            .contains("Initiating transaction rollback on application exception")
            .contains("Batch entry 0 <unknown> was aborted"));
        assertThat(String.join("\n", logged))
            .doesNotContain("household failSub0001", "first visit", "member 1", "member 2", "Failing row contains");
    }

    /**
     * What was logged anywhere (root logger) while {@code action} ran, messages and exception messages, with the
     * named loggers at DEBUG.
     */
    private static List<String> logsOf(Runnable action, String... atDebug) {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        List<Logger> raised = new ArrayList<>();
        List<Level> levels = new ArrayList<>();
        for (String name : atDebug) {
            Logger logger = (Logger) LoggerFactory.getLogger(name);
            raised.add(logger);
            levels.add(logger.getLevel());
            logger.setLevel(Level.DEBUG);
        }
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            action.run();
        } finally {
            root.detachAppender(appender);
            for (int i = 0; i < raised.size(); i++) {
                raised.get(i).setLevel(levels.get(i));
            }
        }
        List<String> lines = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            StringBuilder line = new StringBuilder(event.getFormattedMessage());
            for (IThrowableProxy t = event.getThrowableProxy(); t != null; t = t.getCause()) {
                line.append('\n').append(t.getClassName()).append(": ").append(t.getMessage());
            }
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Known residual, left to the projector step: an array or object value longer than 10,000 characters is cut
     * into invalid JSON (TransformServiceRobust, TallCanonicalJdbcRepository), so the row fails on every attempt
     * and ends dead after about four hours of retries. Here a repeat row whose JSON is that long.
     */
    @Test
    void today_aValueLongerThan10000CharactersIsCutIntoInvalidJsonAndTheRowFails() {
        fixture.submissionWithFormData("longSub0001", TEMPLATE, """
            {"main":{"hh_name":"x"},"members":[{"_id":"longSub001m0","_index":1,"member_name":"%s"}]}"""
            .formatted("y".repeat(10_100)));
        long outboxId = fixture.outbox("longSub0001", "SAVE");

        orchestrator.runOnce("test", 10);

        Map<String, Object> row = fixture.outboxRow(outboxId);
        assertThat(row).containsEntry("status", "failed");
        assertThat((String) row.get("last_error")).startsWith("ERROR: invalid input syntax for type json");
        assertThat(fixture.tallRows("longSub0001")).isZero();
    }

    /** A row started more often than it may be tried, each run ending without an outcome, is dead unprojected. */
    @Test
    void aRowStartedMoreOftenThanItMayBeTriedIsDeadWithoutBeingProjected() {
        fixture.submission("okSub000004", TEMPLATE, 1);
        long outboxId = fixture.outbox("okSub000004", "SAVE");
        fixture.setAttempt(outboxId, 10);

        orchestrator.runOnce("test", 10);

        Map<String, Object> row = fixture.outboxRow(outboxId);
        assertThat(row).containsEntry("status", "dead").containsEntry("attempt", 11)
            .containsEntry("last_error", "started 11 times without an outcome");
        assertThat(fixture.tallRows("okSub000004")).isZero();
    }

    /**
     * An error (not an exception) on one row fails only that row: the rows behind it are projected in the same
     * run, and only the failing row ends dead after its tries (pinned before: the error escaped the batch, and
     * the rows behind it lost an attempt on every claim until they were dead without ever being tried).
     */
    @Test
    void anErrorOnOneRowFailsOnlyThatRowAndTheRowsBehindItAreProjected() {
        List<Long> rows = fiveRows("errSub0000");
        EtlOrchestrator throwingOnRow2 = orchestratorWhere("errSub00002", () -> {
            throw new StackOverflowError();
        });

        for (int run = 1; run <= 11; run++) {
            throwingOnRow2.runOnce("test", 10);
            fixture.endLease(rows.get(1));
        }

        assertThat(fixture.outboxRow(rows.get(1))).containsEntry("status", "dead").containsEntry("attempt", 10);
        assertThat((String) fixture.outboxRow(rows.get(1)).get("last_error")).startsWith("StackOverflowError at ");
        for (int i : new int[]{0, 2, 3, 4}) {
            assertThat(fixture.outboxRow(rows.get(i))).containsEntry("status", "success").containsEntry("attempt", 1);
        }
    }

    /**
     * A lost connection stops the batch at once, instead of waiting for the pool row after row. The row that met
     * it is recorded as failed with the usual retry wait; the rows not started are released with no attempt lost.
     */
    @Test
    void aBatchThatLosesItsConnectionStopsAndReleasesTheRowsItDidNotFinish() {
        List<Long> rows = fiveRows("lostSub000");
        EtlOrchestrator losingItsConnection = orchestratorWhere("lostSub0002", () -> {
            throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction",
                new SQLTransientConnectionException("Connection is not available, request timed out", "08003"));
        });

        EtlRunResult result = losingItsConnection.runOnce("test", 10);

        assertThat(result.stopped()).isTrue();
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(fixture.outboxRow(rows.get(0))).containsEntry("status", "success");
        assertThat(fixture.outboxRow(rows.get(1))).as("the row that met it is failed with its retry wait")
            .containsEntry("status", "failed").containsEntry("attempt", 1);
        assertThat(fixture.leaseSecondsLeft(rows.get(1))).isBetween(50.0, 60.0);
        assertThat((String) fixture.outboxRow(rows.get(1)).get("last_error"))
            .isEqualTo("Connection is not available, request timed out [SQLState 08003]");
        for (int i : new int[]{2, 3, 4}) {
            assertThat(fixture.outboxRow(rows.get(i))).containsEntry("status", "pending").containsEntry("attempt", 0)
                .containsEntry("next_attempt_at", null);
        }
        assertThat(fixture.runs()).singleElement().satisfies(r -> assertThat(r).containsEntry("status", "stopped"));

        orchestrator.runOnce("test", 10);
        assertThat(fixture.outboxRow(rows.get(1))).as("still waiting its retry").containsEntry("status", "failed");
        fixture.endLease(rows.get(1));
        orchestrator.runOnce("test", 10);
        assertThat(rows).allSatisfy(id -> assertThat(fixture.outboxRow(id)).containsEntry("status", "success"));
    }

    /**
     * A pool that keeps timing out, poll after poll: each poll charges only the row at the head one attempt and
     * gives it the usual retry wait, so no row can hold the head of the queue or die before its normal schedule
     * (pinned before: the head row was released with no wait, charged on every poll, and dead on the 11th).
     * No poll counts as a success.
     */
    @Test
    void aPoolThatKeepsTimingOutKillsNoRowBeforeItsSchedule() {
        List<Long> rows = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String uid = "poolSub" + String.format("%04d", i);
            fixture.submission(uid, TEMPLATE, 1);
            rows.add(fixture.outbox(uid, "SAVE"));
        }
        EtlOrchestrator poolTimingOut = orchestratorFailing(uid -> true, () -> {
            throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction",
                new SQLTransientConnectionException("Hikari - Connection is not available, request timed out after 30000ms."));
        });
        SimpleMeterRegistry polled = new SimpleMeterRegistry();
        ScheduledOrchestratorRunner runner = new ScheduledOrchestratorRunner(jdbc, poolTimingOut, new EtlRunLock(dataSource),
            new EtlMetrics(polled, dataSource, etlProperties), etlProperties, new EtlHeartbeat(etlProperties));

        for (int poll = 1; poll <= 12; poll++) {
            runner.scheduledRun();
        }

        assertThat(rows).noneSatisfy(id -> assertThat(fixture.outboxRow(id)).containsEntry("status", "dead"));
        for (int i = 0; i < 12; i++) {
            assertThat(fixture.outboxRow(rows.get(i))).containsEntry("status", "failed").containsEntry("attempt", 1);
            assertThat(fixture.leaseSecondsLeft(rows.get(i))).as("waits its retry").isGreaterThan(50.0);
        }
        for (int i = 12; i < 20; i++) {
            assertThat(fixture.outboxRow(rows.get(i))).containsEntry("status", "pending").containsEntry("attempt", 0);
        }
        assertThat(polled.get("datarun.etl.last.success").gauge().value()).as("no poll succeeded").isNaN();
        assertThat(polled.get("datarun.etl.polls").tag("outcome", "stopped").counter().count()).isEqualTo(12);
        assertThat(polled.get("datarun.etl.polls").tag("outcome", "succeeded").counter().count()).isZero();
    }

    /** An interrupt, as at shutdown, stops the batch the same way and is still set afterwards. */
    @Test
    void anInterruptStopsTheBatchAndReleasesTheRowsItDidNotFinish() {
        List<Long> rows = fiveRows("intSub0000");
        EtlOrchestrator interrupted = orchestratorWhere("intSub00002", () -> {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted mid-row");
        });

        EtlRunResult result = interrupted.runOnce("test", 10);
        boolean stillInterrupted = Thread.interrupted();

        assertThat(stillInterrupted).isTrue();
        assertThat(result.stopped()).isTrue();
        assertThat(fixture.outboxRow(rows.get(1))).containsEntry("status", "failed").containsEntry("attempt", 1);
        assertThat(fixture.isLeased(rows.get(1))).isTrue();
        for (int i : new int[]{2, 3, 4}) {
            assertThat(fixture.outboxRow(rows.get(i))).containsEntry("status", "pending").containsEntry("next_attempt_at", null);
        }
    }

    /** A claim is not an attempt: rows claimed by a run that died before reaching them have lost nothing. */
    @Test
    void rowsClaimedByARunThatDiedBeforeReachingThemHaveLostNoAttempt() {
        List<Long> rows = fiveRows("deadRun000");
        claimService.claimBatchWithIngestId(UUID.randomUUID(), "died", 10);
        rows.forEach(fixture::endLease);

        orchestrator.runOnce("test", 10);

        assertThat(rows).allSatisfy(id ->
            assertThat(fixture.outboxRow(id)).containsEntry("status", "success").containsEntry("attempt", 1));
    }

    /** Five submissions and their SAVE rows, uids {@code prefix + 1..5}; their outbox ids in claim order. */
    private List<Long> fiveRows(String prefix) {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fixture.submission(prefix + i, TEMPLATE, 1);
            ids.add(fixture.outbox(prefix + i, "SAVE"));
        }
        return ids;
    }

    /** The application's orchestrator, except that projecting {@code submissionUid} runs {@code failure}. */
    private EtlOrchestrator orchestratorWhere(String submissionUid, Runnable failure) {
        return orchestratorFailing(submissionUid::equals, failure);
    }

    /** The application's orchestrator, except that projecting a submission the predicate picks runs {@code failure}. */
    private EtlOrchestrator orchestratorFailing(Predicate<String> submissionUids, Runnable failure) {
        EventProcessorService failingOnSomeRows = (runId, outbox, ingestId) -> {
            if (submissionUids.test(outbox.getSubmissionUid())) {
                failure.run();
            }
            eventProcessor.processEvent(runId, outbox, ingestId);
        };
        return new EtlOrchestrator(etlRunService, claimService, processingService, failingOnSomeRows, transactionManager,
            etlProperties, metrics);
    }

    /**
     * A row whose submission is gone is dead at once, saying so: no later try can find it (pinned before as
     * counted a success and left pending, to be claimed forever).
     */
    @Test
    void aRowWhoseSubmissionIsGoneIsDeadAtOnce() {
        long gone = fixture.outboxWithoutSubmission("goneSub0001", 1);

        orchestrator.runOnce("test", 10);
        orchestrator.runOnce("test", 10);

        Map<String, Object> row = fixture.outboxRow(gone);
        assertThat(row).containsEntry("status", "dead").containsEntry("attempt", 1)
            .containsEntry("last_error", "submission not found");
        assertThat(row.get("next_attempt_at")).isNull();
        assertThat(fixture.runs())
            .extracting(r -> r.get("submitted_count"), r -> r.get("success_count"), r -> r.get("failure_count"))
            .containsExactly(tuple(1L, 0L, 1L), tuple(0L, 0L, 0L));
        assertThat(fixture.auditRows(gone)).extracting(r -> r.get("status")).containsExactly("dead");
        assertThat(watchdog.cancelled()).isEmpty();
    }

    /** A claim leases its rows without counting an attempt: a second claim skips them until the lease ends. */
    @Test
    void aClaimLeasesItsRowsUntilTheLeaseEnds() {
        fixture.submission("okSub000003", TEMPLATE, 1);
        long outboxId = fixture.outbox("okSub000003", "SAVE");

        List<OutboxDto> first = claimService.claimBatchWithIngestId(UUID.randomUUID(), "first", 10);
        List<OutboxDto> second = claimService.claimBatchWithIngestId(UUID.randomUUID(), "second", 10);

        assertThat(first).extracting(OutboxDto::getOutboxId).containsExactly(outboxId);
        assertThat(first).extracting(OutboxDto::getAttempt).containsExactly(0);
        assertThat(second).isEmpty();
        assertThat(fixture.outboxRow(outboxId)).containsEntry("status", "pending").containsEntry("claimed_by", "first");
        assertThat(fixture.leaseSecondsLeft(outboxId)).isBetween(14 * 60.0, 15 * 60.0);

        fixture.endLease(outboxId);
        List<OutboxDto> third = claimService.claimBatchWithIngestId(UUID.randomUUID(), "third", 10);
        assertThat(third).extracting(OutboxDto::getOutboxId, OutboxDto::getAttempt).containsExactly(tuple(outboxId, 0));
    }

    @Test
    void claimsTakeTheOldestSubmissionFirstAndTheOldestRowOnATie() {
        fixture.submission("olderSub001", TEMPLATE, 1);
        fixture.submission("newerSub001", TEMPLATE, 1);
        long newerSave = fixture.outbox("newerSub001", "SAVE");
        long olderSave = fixture.outbox("olderSub001", "SAVE");
        long olderUpdate = fixture.outbox("olderSub001", "UPDATE");

        List<OutboxDto> claimed = claimService.claimBatchWithIngestId(UUID.randomUUID(), "test", 10);

        assertThat(claimed).extracting(OutboxDto::getOutboxId).containsExactly(olderSave, olderUpdate, newerSave);
        assertThat(claimService.claimBatchWithIngestId(UUID.randomUUID(), "test", 10)).isEmpty();
    }
}
