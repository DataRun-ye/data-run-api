package org.nmcpye.datarun.etl.orchestrator;

import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.etl.dto.EtlRunCompletion;
import org.nmcpye.datarun.outbox.dto.OutboxDto;
import org.nmcpye.datarun.etl.entity.EtlRun;
import org.nmcpye.datarun.etl.metrics.EtlMetrics;
import org.nmcpye.datarun.etl.metrics.EtlMetrics.Outcome;
import org.nmcpye.datarun.etl.service.EtlRunService;
import org.nmcpye.datarun.etl.service.EventProcessorService;
import org.nmcpye.datarun.etl.service.SubmissionNotFoundException;
import org.nmcpye.datarun.etl.service.impl.TallAndEventsOutput;
import org.nmcpye.datarun.outbox.processing.ChangeOutput;
import org.nmcpye.datarun.outbox.processing.ChangeReference;
import org.nmcpye.datarun.outbox.service.OutboxClaimService;
import org.nmcpye.datarun.outbox.service.OutboxProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Orchestrator that runs one ETL cycle: it claims outbox rows and applies each change to its outputs through the seam
 * {@link ChangeOutput}: those the application registers, in the order they declare (the submission feed, while its
 * switch is on), then the legacy projection into tall and events, built for each row ({@link TallAndEventsOutput}).
 * Each output is handed a {@link ChangeReference} built from the row, never the row.
 * <p>
 * Each claimed row first has its attempt counted, in a transaction of its own, then is applied in another: every
 * output's {@link ChangeOutput#begin}, then each output in a savepoint of its own, then the row's outcome. An output's
 * failure rolls back its savepoint only, so the other outputs' work commits whatever it does. A failure the output keeps
 * leaves the row a success; one it does not keep is recorded in the same transaction: the row is marked failed and
 * tried again after a growing wait, or dead once its tries are used up, and every output applies it again then. When a
 * failure leaves the transaction unable to commit (the row's time ran out), the other outputs apply the change again in
 * a new transaction, with the failure's record. A failure outside every output is recorded after the transaction rolled
 * back, in a new one, as before.
 * <p>
 * A lost connection or an interrupt also stops the batch: the row that met it is recorded like any failure, with the
 * usual retry wait, so it can neither hold the head of the queue nor die before its normal schedule, and the rows not
 * started are released for the next run without losing an attempt.
 * <p>
 * An output's own time limit must stay below the row timeout: the orchestrator refuses to start otherwise. Each output
 * is given the time left as its deadline, short of a tenth of the row timeout kept for the row's outcome.
 */
@Component
public class EtlOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(EtlOrchestrator.class);

    private final EtlRunService etlRunService;
    private final OutboxClaimService outboxClaimService;
    private final OutboxProcessingService outboxProcessingService;
    private final List<ChangeOutput> outputs;
    private final EventProcessorService tallAndEvents;
    private final TransactionTemplate txTemplate;
    private final JdbcTemplate savepoints;
    private final Duration rowTimeout;
    private final EtlRetryPolicy retryPolicy;
    private final EtlMetrics metrics;

    public EtlOrchestrator(EtlRunService etlRunService,
                           OutboxClaimService outboxClaimService,
                           OutboxProcessingService outboxProcessingService,
                           List<ChangeOutput> outputs,
                           EventProcessorService tallAndEvents,
                           PlatformTransactionManager txManager,
                           DataSource dataSource,
                           EtlProperties properties,
                           EtlMetrics metrics) {
        this.etlRunService = etlRunService;
        this.outboxClaimService = outboxClaimService;
        this.outboxProcessingService = outboxProcessingService;
        this.outputs = List.copyOf(outputs);
        this.tallAndEvents = tallAndEvents;
        this.txTemplate = new TransactionTemplate(txManager);
        // Statements in a row's transaction are cancelled once it has run this long.
        this.rowTimeout = properties.getRowTimeout();
        this.txTemplate.setTimeout((int) Math.max(1, rowTimeout.toSeconds()));
        // savepoints of the row's transaction, on its connection
        this.savepoints = new JdbcTemplate();
        this.savepoints.setDataSource(dataSource);
        this.retryPolicy = new EtlRetryPolicy(properties);
        this.metrics = metrics;
        requireLimitsBelowTheRowTimeout(this.outputs, rowTimeout);
    }

    /** Every output that is on and keeps a time limit must stay below the row timeout; otherwise nothing starts. */
    static void requireLimitsBelowTheRowTimeout(List<ChangeOutput> outputs, Duration rowTimeout) {
        for (ChangeOutput output : outputs) {
            if (!output.isOn()) {
                continue;
            }
            output.timeLimit().ifPresent(limit -> {
                if (limit.isNegative() || limit.isZero() || limit.compareTo(rowTimeout) >= 0) {
                    throw new IllegalStateException("The change output " + output.name() + " may take " + limit
                        + " per change; that must be more than nothing and less than the ETL's row timeout, "
                        + "datarun.etl.row-timeout (" + rowTimeout + "). Lower the output's limit or raise the row "
                        + "timeout.");
                }
            });
        }
    }

    /**
     * Run a single processing cycle:
     * - start run -> claim batch -> for each outbox: per-outbox TX { every output, each in a savepoint; outcome }
     * - finish run and persist counters
     */
    public EtlRunResult runOnce(String pipelineName, int batchSize) {
        String createdBy = "etl-orchestrator";
        String codeVersion = System.getenv().getOrDefault("CODE_VERSION", "unknown");

        EtlRun run = etlRunService.startRun(pipelineName, createdBy, codeVersion);
        UUID runId = run.getEtlRunId();
        UUID ingestId = UUID.randomUUID();

        long submittedCount = 0;
        long successCount = 0;
        long failureCount = 0;
        long minSerial = -1L;
        long maxSerial = -1L;

        try {
            List<OutboxDto> claimed = outboxClaimService.claimBatchWithIngestId(ingestId, pipelineName, batchSize);
            minSerial = claimed.stream().mapToLong(OutboxDto::getSubmissionSerialNumber).min().orElse(-1L);
            maxSerial = claimed.stream().mapToLong(OutboxDto::getSubmissionSerialNumber).max().orElse(-1L);

            submittedCount = claimed.size();
            log.info("Run {} claimed {} rows (ingestId={})", runId, submittedCount, ingestId);

            int done = 0;
            boolean stopped = false;
            for (OutboxDto outbox : claimed) {
                RowResult result = Thread.currentThread().isInterrupted()
                    ? RowResult.STOP_BEFORE_START : process(runId, outbox, ingestId);
                if (result == RowResult.STOP_BEFORE_START) {
                    stopped = true;
                    break;
                }
                done++;
                if (result == RowResult.SUCCESS) {
                    successCount++;
                } else if (result != RowResult.SKIPPED) {
                    failureCount++;
                }
                if (result.stopsTheBatch) {
                    stopped = true;
                    break;
                }
            }
            if (stopped) {
                release(runId, ingestId, claimed.subList(done, claimed.size()));
            }

            // finalize run
            String status = stopped ? "stopped" : "finished";
            etlRunService.finishRun(EtlRunCompletion.builder().etlRunId(runId).status(status).submittedCount(submittedCount)
                .successCount(successCount).failureCount(failureCount).minSubmissionSerial(minSerial).maxSubmissionSerial(maxSerial)
                .build());
            log.info("Run {} {}: submitted={}, success={}, failure={}", runId, status, submittedCount, successCount, failureCount);
            return new EtlRunResult((int) submittedCount, (int) successCount, (int) failureCount, stopped);
        } catch (Exception topEx) {
            // best-effort mark run as failed
            try {
                etlRunService.finishRun(EtlRunCompletion.builder().etlRunId(runId).status("failed").submittedCount(submittedCount)
                    .successCount(successCount).failureCount(failureCount).minSubmissionSerial(minSerial).maxSubmissionSerial(maxSerial)
                    .build());
            } catch (Exception e) {
                log.error("Failed to update run status for run {}", runId, e);
            }
            log.error("Orchestration error for run {}: {}", runId, topEx.getMessage(), topEx);
            throw new RuntimeException(topEx);
        }
    }

    private enum RowResult {
        SUCCESS(Outcome.SUCCESS, false), FAILED(Outcome.FAILED, false), DEAD(Outcome.DEAD, false),
        /** A newer claim holds the row; that run owns it. */
        SKIPPED(null, false),
        /** The row could not be started (or the thread was interrupted first): the batch stops before it. */
        STOP_BEFORE_START(null, true),
        /** The row met a lost connection or an interrupt; it is recorded as failed and the batch stops after it. */
        FAILED_AND_STOP(Outcome.FAILED, true),
        DEAD_AND_STOP(Outcome.DEAD, true);

        private final Outcome outcome;
        private final boolean stopsTheBatch;

        RowResult(Outcome outcome, boolean stopsTheBatch) {
            this.outcome = outcome;
            this.stopsTheBatch = stopsTheBatch;
        }
    }

    private RowResult process(UUID runId, OutboxDto outbox, UUID ingestId) {
        long start = System.nanoTime();
        RowResult result = project(runId, outbox, ingestId);
        if (result.outcome != null) {
            metrics.rowProcessed(result.outcome, Duration.ofNanos(System.nanoTime() - start));
        }
        return result;
    }

    private RowResult project(UUID runId, OutboxDto outbox, UUID ingestId) {
        int attempt;
        try {
            OptionalInt started = outboxClaimService.startAttempt(outbox.getOutboxId(), ingestId);
            if (started.isEmpty()) {
                log.warn("Outbox row {} was claimed again by a newer run; leaving it to that run", outbox.getOutboxId());
                return RowResult.SKIPPED;
            }
            attempt = started.getAsInt();
        } catch (Throwable e) {
            // Not even the start could be written: nothing in this batch can be.
            log.warn("Could not start outbox row {}; the batch stops: {}", outbox.getOutboxId(), EtlFailures.describe(e));
            return RowResult.STOP_BEFORE_START;
        }
        outbox.setAttempt(attempt);
        if (retryPolicy.startedTooOften(attempt)) {
            recordDead(runId, outbox, "started " + attempt + " times without an outcome");
            return RowResult.DEAD;
        }
        List<ChangeOutput> on = new ArrayList<>(outputs.stream().filter(ChangeOutput::isOn).toList());
        on.add(new TallAndEventsOutput(tallAndEvents, outbox));
        OutputFailure[] left = new OutputFailure[1];
        Throwable failure;
        try {
            return txTemplate.execute(status -> applyAndRecord(runId, outbox, attempt, on, null, left));
        } catch (Throwable e) {
            failure = e;
        }
        if (left[0] != null && !EtlFailures.stopsTheBatch(failure)) {
            // An output's failure left the transaction unable to commit: the other outputs apply the change again,
            // in a new transaction, with that failure's outcome, so their work does not wait for the retry.
            OutputFailure first = left[0];
            List<ChangeOutput> others = on.stream().filter(output -> output != first.output()).toList();
            try {
                return txTemplate.execute(status ->
                    applyAndRecord(runId, outbox, attempt, others, first, new OutputFailure[1]));
            } catch (Throwable e) {
                failure = first.error();
            }
        }
        return recordAfterRollback(runId, outbox, attempt, failure);
    }

    /** An output's failure that it does not keep: the runtime records it on the outbox row. */
    private record OutputFailure(ChangeOutput output, RuntimeException error) {
    }

    /**
     * Applies the change to these outputs in the current transaction and records the row's outcome: success, or the
     * first failure an output did not keep ({@code carried}, when given, counts as that). Such a failure is also put in
     * {@code left}, for when this transaction cannot commit. Every output begins first (the feed takes its lock before
     * anything is written), then each applies the change, by a deadline set when it began; each step, and each output's
     * keeping of its failure, runs in a savepoint of its own, so no output's failure rolls back another's work.
     */
    private RowResult applyAndRecord(UUID runId, OutboxDto outbox, int attempt, List<ChangeOutput> outputs,
                                     OutputFailure carried, OutputFailure[] left) {
        long rowDeadline = System.nanoTime() + rowTimeout.toNanos() - rowTimeout.toNanos() / 10;
        ChangeReference change = new ChangeReference(outbox.getSubmissionId(), outbox.getSubmissionVersion());
        long[] deadlines = new long[outputs.size()];
        RuntimeException[] failures = new RuntimeException[outputs.size()];
        for (int i = 0; i < outputs.size(); i++) {
            ChangeOutput output = outputs.get(i);
            long deadline = deadline(output, rowDeadline);
            deadlines[i] = deadline;
            failures[i] = inSavepoint(() -> output.begin(deadline));
        }
        OutputFailure failure = carried;
        for (int i = 0; i < outputs.size(); i++) {
            ChangeOutput output = outputs.get(i);
            long deadline = deadlines[i];
            RuntimeException e = failures[i] != null ? failures[i] : inSavepoint(() -> output.apply(change, deadline));
            if (e != null && !keeps(output, change, e) && failure == null) {
                failure = new OutputFailure(output, e);
                left[0] = failure;
            }
        }
        if (failure == null) {
            outboxProcessingService.recordSuccess(runId, outbox);
            return RowResult.SUCCESS;
        }
        return recordInThisTransaction(runId, outbox, attempt, failure.error());
    }

    /**
     * Runs the step in a savepoint of its own: released when it succeeds, rolled back when it throws, so what it wrote
     * goes with it and nothing else. Its failure, or {@code null}.
     */
    private RuntimeException inSavepoint(Runnable step) {
        Savepoint savepoint = savepoints.execute((ConnectionCallback<Savepoint>) Connection::setSavepoint);
        try {
            step.run();
        } catch (RuntimeException e) {
            if (EtlFailures.stopsTheBatch(e)) {
                // the transaction is lost with its connection, or the thread is stopping
                throw e;
            }
            savepoints.execute((ConnectionCallback<Void>) connection -> {
                connection.rollback(savepoint);
                return null;
            });
            return e;
        }
        savepoints.execute((ConnectionCallback<Void>) connection -> {
            connection.releaseSavepoint(savepoint);
            return null;
        });
        return null;
    }

    /**
     * Whether the output keeps its failure, asked in a savepoint of its own: an output that throws while keeping it
     * does not keep it, so the row records the failure, and what the others applied stays.
     */
    private boolean keeps(ChangeOutput output, ChangeReference change, RuntimeException failure) {
        boolean[] kept = {false};
        RuntimeException e = inSavepoint(() -> kept[0] = output.keepsFailure(change, failure));
        if (e != null) {
            log.warn("The change output {} could not keep its failure ({}) on submission {}, so the outbox row records "
                + "it: {}", output.name(), EtlFailures.describe(failure), change.submissionId(), EtlFailures.describe(e));
            return false;
        }
        return kept[0];
    }

    /** The output's limit from now, or the row's deadline when that comes first or the output keeps no limit. */
    private static long deadline(ChangeOutput output, long rowDeadline) {
        return output.timeLimit()
            .map(limit -> {
                long own = System.nanoTime() + limit.toNanos();
                return own - rowDeadline < 0 ? own : rowDeadline;
            })
            .orElse(rowDeadline);
    }

    /** Records an output's failure on the row, in the row's transaction; a failure to record it is thrown. */
    private RowResult recordInThisTransaction(UUID runId, OutboxDto outbox, int attempt, RuntimeException failure) {
        if (failure instanceof SubmissionNotFoundException) {
            // No later try can find it: the submission was removed outside the application.
            log.error("Outbox row {} is dead after attempt {}: {}", outbox.getOutboxId(), attempt, failure.getMessage());
            outboxProcessingService.recordDead(runId, outbox, failure.getMessage());
            return RowResult.DEAD;
        }
        String error = EtlFailures.describe(failure);
        if (log.isDebugEnabled()) {
            log.debug("Outbox row {} failed on attempt {}", outbox.getOutboxId(), attempt,
                EtlFailures.withoutMessages(failure));
        }
        if (retryPolicy.isLastAttempt(attempt)) {
            log.error("Outbox row {} is dead after attempt {}: {}", outbox.getOutboxId(), attempt, error);
            outboxProcessingService.recordDead(runId, outbox, error);
            return RowResult.DEAD;
        }
        Duration retryAfter = retryPolicy.delayAfter(attempt);
        log.warn("Outbox row {} failed on attempt {}, tried again in {}: {}", outbox.getOutboxId(), attempt, retryAfter,
            error);
        outboxProcessingService.recordRetry(runId, outbox, error, retryAfter);
        return RowResult.FAILED;
    }

    /** Records a failure after the row's transaction rolled back, in a new transaction. */
    private RowResult recordAfterRollback(UUID runId, OutboxDto outbox, int attempt, Throwable e) {
        if (e instanceof SubmissionNotFoundException) {
            // No later try can find it: the submission was removed outside the application.
            recordDead(runId, outbox, e.getMessage());
            return RowResult.DEAD;
        }
        // The row's transaction has rolled back: nothing of it is left to wait on.
        String error = EtlFailures.describe(e);
        if (EtlFailures.stopsTheBatch(e)) {
            log.warn("Outbox row {} met a lost connection or an interrupt on attempt {}; the batch stops: {}",
                outbox.getOutboxId(), attempt, error);
            // Recorded like any failure, so the row waits its turn instead of heading the next claim.
            boolean[] dead = new boolean[1];
            withInterruptSetAside(() -> dead[0] = recordFailure(runId, outbox, attempt, error));
            return dead[0] ? RowResult.DEAD_AND_STOP : RowResult.FAILED_AND_STOP;
        }
        if (log.isDebugEnabled()) {
            log.debug("Outbox row {} failed on attempt {}", outbox.getOutboxId(), attempt, EtlFailures.withoutMessages(e));
        }
        return recordFailure(runId, outbox, attempt, error) ? RowResult.DEAD : RowResult.FAILED;
    }

    /** Records a failed row: dead on its last attempt, else failed with the usual retry wait. True when dead. */
    private boolean recordFailure(UUID runId, OutboxDto outbox, int attempt, String error) {
        if (retryPolicy.isLastAttempt(attempt)) {
            recordDead(runId, outbox, error);
            return true;
        }
        recordRetry(runId, outbox, error, retryPolicy.delayAfter(attempt));
        return false;
    }

    /**
     * Ends the lease of the rows a stopped batch did not start, so the next run takes them at once; they lose no
     * attempt. The row that stopped the batch was recorded as failed, with its retry wait.
     */
    private void release(UUID runId, UUID ingestId, List<OutboxDto> rows) {
        if (rows.isEmpty()) {
            return;
        }
        withInterruptSetAside(() -> {
            try {
                int released = outboxClaimService.release(ingestId, rows.stream().map(OutboxDto::getOutboxId).toList());
                log.warn("Run {} stopped; released {} of its claimed rows for the next run", runId, released);
            } catch (RuntimeException e) {
                log.warn("Run {} stopped and could not release its {} unstarted rows; they are claimed again when "
                    + "their lease ends: {}", runId, rows.size(), EtlFailures.describe(e));
            }
        });
    }

    /** Runs a short write with the thread's interrupt set aside, so the pool hands out a connection; restores it. */
    private static void withInterruptSetAside(Runnable write) {
        boolean interrupted = Thread.interrupted();
        try {
            write.run();
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void recordRetry(UUID runId, OutboxDto outbox, String error, Duration retryAfter) {
        log.warn("Outbox row {} failed on attempt {}, tried again in {}: {}",
            outbox.getOutboxId(), outbox.getAttempt(), retryAfter, error);
        try {
            outboxProcessingService.recordRetry(runId, outbox, error, retryAfter);
        } catch (RuntimeException e) {
            log.error("Could not record the failure of outbox row {}; it is claimed again when its lease ends",
                outbox.getOutboxId(), e);
        }
    }

    private void recordDead(UUID runId, OutboxDto outbox, String error) {
        log.error("Outbox row {} is dead after attempt {}: {}", outbox.getOutboxId(), outbox.getAttempt(), error);
        try {
            outboxProcessingService.recordDead(runId, outbox, error);
        } catch (RuntimeException e) {
            log.error("Could not record outbox row {} as dead; it is claimed again when its lease ends",
                outbox.getOutboxId(), e);
        }
    }
}
