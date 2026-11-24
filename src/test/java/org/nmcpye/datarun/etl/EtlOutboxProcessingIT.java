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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
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
     * Today: a database error is audited, but the outbox row keeps no attempt, error or retry time, and the
     * audited error is the start of the failed statement, cut before the cause.
     */
    @Test
    void today_aDatabaseFailureIsAuditedButTheRowStaysPendingWithNoAttemptOrError() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 1);
        long failing = fixture.outbox("failSub0001", "SAVE");

        orchestrator.runOnce("test", 10);

        assertThat(watchdog.cancelled()).as("a database error ends the transaction at once: no wait").isEmpty();
        Map<String, Object> row = fixture.outboxRow(failing);
        assertThat(row).containsEntry("status", "pending").containsEntry("attempt", 0);
        assertThat(row.get("last_error")).isNull();
        assertThat(row.get("next_attempt_at")).isNull();
        assertThat(fixture.auditRows(failing)).singleElement().satisfies(audit -> {
            assertThat(audit).containsEntry("status", "failure");
            assertThat((String) audit.get("error"))
                .startsWith("PreparedStatementCallback")
                .endsWith("...(truncated)")
                .doesNotContain("injected projection failure");
        });
        assertThat(fixture.tallRows("failSub0001")).isZero();
        assertThat(fixture.runs()).singleElement().satisfies(r -> assertThat(r).containsEntry("failure_count", 1L));
    }

    /** Today: a failed row stays at the head of the queue, so rows that keep failing can stall it. */
    @Test
    void today_aFailedRowIsClaimedAgainAheadOfNewerRowsOnEveryRun() {
        fixture.failProjectionsOfFailUids();
        fixture.submission("failSub0001", TEMPLATE, 1);
        fixture.submission("okSub000002", TEMPLATE, 1);
        long failing = fixture.outbox("failSub0001", "SAVE");
        long newer = fixture.outbox("okSub000002", "SAVE");

        orchestrator.runOnce("test", 1);
        orchestrator.runOnce("test", 1);
        orchestrator.runOnce("test", 1);

        assertThat(fixture.outboxRow(failing)).containsEntry("status", "pending").containsEntry("attempt", 0);
        assertThat(fixture.auditRows(failing)).hasSize(3);
        assertThat(fixture.outboxRow(newer)).as("a batch of one never reaches the newer row")
            .containsEntry("status", "pending")
            .containsEntry("claimed_by", null);
        assertThat(watchdog.cancelled()).isEmpty();
    }

    /**
     * Today: when the failure is raised in Java while the transaction is still open, the failure audit is
     * written in a second transaction with the same (run, outbox) key as the first transaction's uncommitted
     * "processing" row. PostgreSQL makes the second insert wait for the first transaction, which waits for the
     * same thread: the run never ends on its own.
     */
    @Test
    void today_aFailureRaisedOutsideTheDatabaseLeavesTheRunWaitingOnItsOwnAuditRow() throws Exception {
        fixture.defineTemplateTheApplicationCannotRead("etlTmpl0002");
        fixture.submission("javaSub0001", "etlTmpl0002", 0);
        long failing = fixture.outbox("javaSub0001", "SAVE");

        CompletableFuture<Void> run = CompletableFuture.runAsync(() -> orchestrator.runOnce("test", 10));
        Thread.sleep(2500);
        assertThat(run).as("the run is still waiting after 2.5 s").isNotDone();

        run.get(10, TimeUnit.SECONDS);
        assertThat(watchdog.cancelled()).as("the audit insert had to be cancelled to end the run")
            .singleElement()
            .satisfies(wait -> {
                assertThat(wait.seconds()).isGreaterThanOrEqualTo(STUCK_AFTER_SECONDS);
                assertThat(wait.blockerState()).as("it waited for a transaction nobody would finish")
                    .isEqualTo("idle in transaction");
            });

        // Once unblocked, nothing about the failure is kept.
        Map<String, Object> row = fixture.outboxRow(failing);
        assertThat(row).containsEntry("status", "pending").containsEntry("attempt", 0);
        assertThat(row.get("last_error")).isNull();
        assertThat(fixture.auditRows(failing)).isEmpty();
        assertThat(fixture.runs()).singleElement().satisfies(r -> assertThat(r).containsEntry("failure_count", 1L));
    }

    /** Today: a row whose submission is gone is counted as a success but stays pending, so it is claimed forever. */
    @Test
    void today_aRowWhoseSubmissionIsGoneCountsAsSuccessButStaysPending() {
        long gone = fixture.outboxWithoutSubmission("goneSub0001", 1);

        orchestrator.runOnce("test", 10);
        orchestrator.runOnce("test", 10);

        assertThat(fixture.outboxRow(gone)).containsEntry("status", "pending").containsEntry("attempt", 0);
        assertThat(fixture.outboxRow(gone).get("last_error")).isNull();
        assertThat(fixture.runs()).hasSize(2)
            .allSatisfy(r -> assertThat(r).containsEntry("submitted_count", 1L).containsEntry("success_count", 1L));
        // Each run commits its "processing" audit row and nothing more.
        assertThat(fixture.auditRows(gone)).extracting(r -> r.get("status")).containsExactly("processing", "processing");
        assertThat(watchdog.cancelled()).isEmpty();
    }

    /** Today: a claim does not change a row's status, so a second claimer takes the same rows. */
    @Test
    void today_aClaimLeavesRowsPendingSoASecondClaimTakesTheSameRows() {
        fixture.submission("okSub000003", TEMPLATE, 1);
        long outboxId = fixture.outbox("okSub000003", "SAVE");

        List<OutboxDto> first = claimService.claimBatchWithIngestId(UUID.randomUUID(), "first", 10);
        List<OutboxDto> second = claimService.claimBatchWithIngestId(UUID.randomUUID(), "second", 10);

        assertThat(first).extracting(OutboxDto::getOutboxId).containsExactly(outboxId);
        assertThat(second).extracting(OutboxDto::getOutboxId).containsExactly(outboxId);
        assertThat(fixture.outboxRow(outboxId)).containsEntry("status", "pending").containsEntry("claimed_by", "second");
    }
}
