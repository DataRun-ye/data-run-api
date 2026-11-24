package org.nmcpye.datarun.etl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.outbox.service.OutboxClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.nmcpye.datarun.etl.EtlFixture.TEMPLATE;

/**
 * The outbox consumer (claim, project, record) against PostgreSQL migrated by Liquibase.
 * <p>
 * The pool has four connections, as production's pool has more than one: with a single connection a second
 * transaction would wait for the pool instead of for PostgreSQL. A watchdog cancels an audit insert stuck for
 * 3 s, so no test can hang the build; each test says whether it expects that. Tests named {@code today_} pin
 * behaviour the ETL runtime fix changes.
 */
@IntegrationTest
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=4")
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
        assertThat((String) row.get("last_error")).contains("NotASemanticType");
        assertThat(fixture.auditRows(failing)).extracting(r -> r.get("status")).containsExactly("failure");
        assertThat(fixture.runs()).singleElement().satisfies(r -> assertThat(r).containsEntry("failure_count", 1L));
    }

    /** A row claimed more often than it may be tried, each run ending without an outcome, is dead unprojected. */
    @Test
    void aRowClaimedMoreOftenThanItMayBeTriedIsDeadWithoutBeingProjected() {
        fixture.submission("okSub000004", TEMPLATE, 1);
        long outboxId = fixture.outbox("okSub000004", "SAVE");
        fixture.setAttempt(outboxId, 10);

        orchestrator.runOnce("test", 10);

        Map<String, Object> row = fixture.outboxRow(outboxId);
        assertThat(row).containsEntry("status", "dead").containsEntry("attempt", 11)
            .containsEntry("last_error", "claimed 11 times without an outcome");
        assertThat(fixture.tallRows("okSub000004")).isZero();
    }

    /**
     * Today: a row whose submission is gone is counted as a success but stays pending; only the claim's lease
     * keeps it from being claimed again at once.
     */
    @Test
    void today_aRowWhoseSubmissionIsGoneCountsAsSuccessButStaysPending() {
        long gone = fixture.outboxWithoutSubmission("goneSub0001", 1);

        orchestrator.runOnce("test", 10);
        orchestrator.runOnce("test", 10);

        assertThat(fixture.outboxRow(gone)).containsEntry("status", "pending").containsEntry("attempt", 1);
        assertThat(fixture.outboxRow(gone).get("last_error")).isNull();
        assertThat(fixture.isLeased(gone)).isTrue();
        assertThat(fixture.runs()).extracting(r -> r.get("submitted_count"), r -> r.get("success_count"))
            .containsExactly(tuple(1L, 1L), tuple(0L, 0L));
        assertThat(fixture.auditRows(gone)).isEmpty();
        assertThat(watchdog.cancelled()).isEmpty();
    }

    /** A claim leases its rows: a second claim skips them until the lease ends, then takes them as a new attempt. */
    @Test
    void aClaimLeasesItsRowsUntilTheLeaseEnds() {
        fixture.submission("okSub000003", TEMPLATE, 1);
        long outboxId = fixture.outbox("okSub000003", "SAVE");

        List<OutboxDto> first = claimService.claimBatchWithIngestId(UUID.randomUUID(), "first", 10);
        List<OutboxDto> second = claimService.claimBatchWithIngestId(UUID.randomUUID(), "second", 10);

        assertThat(first).extracting(OutboxDto::getOutboxId).containsExactly(outboxId);
        assertThat(first).extracting(OutboxDto::getAttempt).containsExactly(1);
        assertThat(second).isEmpty();
        assertThat(fixture.outboxRow(outboxId)).containsEntry("status", "pending").containsEntry("claimed_by", "first");
        assertThat(fixture.leaseSecondsLeft(outboxId)).isBetween(14 * 60.0, 15 * 60.0);

        fixture.endLease(outboxId);
        List<OutboxDto> third = claimService.claimBatchWithIngestId(UUID.randomUUID(), "third", 10);
        assertThat(third).extracting(OutboxDto::getOutboxId, OutboxDto::getAttempt).containsExactly(tuple(outboxId, 2));
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
