package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.etl.config.EtlProperties;
import org.nmcpye.datarun.submission.feed.FeedActivation;
import org.nmcpye.datarun.submission.normalized.NormalizationFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Savepoint;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

/**
 * The submission feed's publisher, as the ETL reaches it: the only entry point into the feed's writes from outside this
 * package ({@code FeedWriteOwnershipTest}). Nothing is written while the switch {@code submission-feed} is off.
 *
 * <p><b>A live change</b> is published inside the ETL's per-row transaction (contract 8, item 4). The ETL takes the
 * feed's lock first ({@link #lockFeed}), writes tall and events as before, then calls {@link #publishChange}, which
 * runs the feed part in a savepoint of that transaction: it reads the submission's head and the version the feed holds,
 * normalizes, and writes the submission's record with its changed, new and deleted row records ({@link FeedWriter}).
 * A failure rolls back to the savepoint only, so tall and events still commit with the outbox row's success, and the
 * submission waits in the retry list as a pending change with its reason code ({@link PendingChanges}). The feed part
 * has a time limit below the row's ({@code datarun.feed.part-timeout}).</p>
 *
 * <p><b>After the live backlog</b> the ETL's poll calls {@link #publishAfterBacklog} for one batch at a time, the
 * live backlog first again after each: pending changes due for a retry, each failure counting an attempt until the
 * cap makes it dead; then republish marks, each written again at the same version with new positions, its mark kept
 * while it fails ({@link RepublishMarks}); then reconcile, which publishes every head the feed lacks or holds at another
 * version, at start (also switch-on) and nightly ({@link Reconcile}). Each batch is one transaction under the feed's lock, each submission in its own savepoint, and
 * stops starting submissions once it has run half the feed part's time limit, so it holds the lock briefly.</p>
 *
 * <p><b>The savepoint is a JDBC savepoint on the transaction's connection.</b> Spring's {@code Propagation.NESTED} is
 * not available here: the application's transaction manager is JPA's, whose Hibernate dialect offers no savepoints, so
 * a nested transaction would fail to start. The feed part writes through JDBC only, so a savepoint on the connection
 * rolls back exactly what it wrote; and since it never passes through a transactional proxy, a failure never marks the
 * row's transaction rollback-only.</p>
 */
@Component
public class SubmissionPublisher {

    private static final Logger log = LoggerFactory.getLogger(SubmissionPublisher.class);

    private final ActivationSwitches switches;
    private final DataSource dataSource;
    private final NamedParameterJdbcTemplate jdbc;
    private final FeedPublisherProperties properties;
    private final Duration partTimeout;
    private final FeedWriter writer;
    private final PendingChanges pending;
    private final RepublishMarks republishMarks;
    private final Reconcile reconcile = new Reconcile();
    private final Freshness freshness = new Freshness();
    private final TransactionTemplate afterBacklog;
    private final TransactionTemplate reading;
    private final Map<FeedWriter.Outcome, Counter> changes;
    private final Counter failures;
    private final Map<NormalizationFlag, Counter> flags;

    @Autowired
    public SubmissionPublisher(ActivationSwitches switches, DataSource dataSource,
                               PlatformTransactionManager transactionManager, ObjectMapper objectMapper,
                               FeedPublisherProperties properties, EtlProperties etlProperties,
                               ObjectProvider<SplitPartners> splitPartners, MeterRegistry registry) {
        this(switches, dataSource, transactionManager, properties, etlProperties, new FeedWriter(new FormDefinitions(),
            objectMapper, splitPartners.getIfAvailable(() -> SplitPartners.NONE)), registry);
    }

    SubmissionPublisher(ActivationSwitches switches, DataSource dataSource,
                        PlatformTransactionManager transactionManager, FeedPublisherProperties properties,
                        EtlProperties etlProperties, FeedWriter writer, MeterRegistry registry) {
        this.switches = switches;
        this.dataSource = dataSource;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.properties = properties;
        this.partTimeout = partTimeout(properties.getPartTimeout(), etlProperties.getRowTimeout());
        this.writer = writer;
        this.pending = new PendingChanges(properties);
        this.republishMarks = new RepublishMarks(properties);
        this.afterBacklog = new TransactionTemplate(transactionManager);
        // a backstop: each submission's own statements stop at the feed part's limit
        this.afterBacklog.setTimeout((int) Math.max(1, partTimeout.multipliedBy(2).toSeconds()));
        this.reading = new TransactionTemplate(transactionManager);
        this.reading.setReadOnly(true);
        Map<FeedWriter.Outcome, Counter> byOutcome = new EnumMap<>(FeedWriter.Outcome.class);
        for (FeedWriter.Outcome outcome : FeedWriter.Outcome.values()) {
            byOutcome.put(outcome, changeCounter(registry, outcome.name().toLowerCase()));
        }
        this.changes = byOutcome;
        this.failures = changeCounter(registry, "failed");
        Map<NormalizationFlag, Counter> byFlag = new EnumMap<>(NormalizationFlag.class);
        for (NormalizationFlag flag : NormalizationFlag.values()) {
            byFlag.put(flag, Counter.builder("datarun.feed.normalization.flags")
                .description("What the submission normalizer flagged in the records the feed published, by flag")
                .tag("flag", flag.name())
                .register(registry));
        }
        this.flags = byFlag;
    }

    private static Counter changeCounter(MeterRegistry registry, String outcome) {
        return Counter.builder("datarun.feed.changes")
            .description("Submissions the feed's publisher handled, by outcome")
            .tag("outcome", outcome)
            .register(registry);
    }

    /** The feed part's time limit: the setting, or half the row's timeout when the setting does not stay below it. */
    static Duration partTimeout(Duration configured, Duration rowTimeout) {
        if (configured.compareTo(rowTimeout) < 0 && !configured.isNegative() && !configured.isZero()) {
            return configured;
        }
        Duration half = rowTimeout.dividedBy(2);
        log.warn("datarun.feed.part-timeout ({}) does not stay below the ETL's row timeout ({}); the feed part may take "
            + "{}", configured, rowTimeout, half);
        return half;
    }

    /** Whether the feed is published: the switch {@code submission-feed}. */
    public boolean isOn() {
        return switches.isOn(FeedActivation.SUBMISSION_FEED);
    }

    /**
     * Takes the feed's lock in the caller's transaction ({@link FeedLock}): the ETL's first statement in a row's
     * transaction, before tall and events, while the feed is on.
     *
     * @throws IllegalStateException when no transaction is active
     */
    public void lockFeed() {
        requireTransaction();
        FeedLock.take(jdbc.getJdbcOperations());
    }

    /**
     * Publishes the submission's head in a savepoint of the caller's transaction, which took the feed's lock first.
     * A failure of the feed part rolls back to the savepoint and records a pending change; it is never thrown. Only a
     * failure that leaves the transaction unusable (a lost connection) is thrown, and the caller's transaction then
     * fails as a whole.
     *
     * @param submissionId the submission's id ({@code data_submission.id}), as the outbox row names it
     */
    public void publishChange(String submissionId) {
        requireTransaction();
        Attempt attempt = attempt(submissionId, false);
        if (attempt.failure() != null) {
            recordFailure(submissionId, attempt);
        }
    }

    /**
     * Publishes one batch of the work that waits behind the live backlog: pending changes due for a retry, else
     * republish marks, else reconcile's next submissions. Called by the ETL's poll once no outbox row is due, again
     * after each batch that did something.
     *
     * @return whether the batch did anything; false when nothing waits, or the feed is off
     */
    public boolean publishAfterBacklog() {
        if (!isOn()) {
            return false;
        }
        return retryPending() > 0 || republish() > 0 || reconcileBatch();
    }

    /** Requests a reconcile pass at start, which is also switch-on: the switch is read at start. */
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileAtStart() {
        if (isOn()) {
            reconcile.request();
        }
    }

    /** Requests a reconcile pass every night ({@code datarun.feed.reconcile-cron}, 02:00 by default). */
    @Scheduled(cron = "${datarun.feed.reconcile-cron:0 0 2 * * *}")
    public void reconcileNightly() {
        if (isOn()) {
            reconcile.request();
        }
    }

    /**
     * Publishes reconcile's next submissions, scanning further when none are queued, and records the pass in
     * freshness once it ends. Whether a pass ran.
     */
    private boolean reconcileBatch() {
        synchronized (reconcile) {
            if (!reconcile.begin()) {
                return false;
            }
            int batchSize = Math.max(1, properties.getBatchSize());
            int scanSize = Math.max(1, properties.getReconcileScanSize());
            List<String> batch = reading.execute(status -> reconcile.next(jdbc, scanSize, batchSize));
            if (batch != null && !batch.isEmpty()) {
                int[] failed = {0};
                int attempted = inFeedTransaction(() -> publishEach(batch, false, (submissionId, attempt) -> {
                    if (attempt.failure() != null) {
                        failed[0]++;
                        recordFailure(submissionId, attempt);
                    }
                }));
                reconcile.attempted(attempted, failed[0]);
            }
            Reconcile.Summary summary = reconcile.finishIfDone();
            if (summary != null) {
                inFeedTransaction(() -> {
                    freshness.reconciled(jdbc, summary);
                    return 0;
                });
                log.info("Reconcile compared {} submissions with their heads: {} were unpublished, {} published, {} "
                        + "failed, {} left to their retries", summary.checked(), summary.differing(), summary.published(),
                    summary.failed(), summary.waiting());
            }
            return true;
        }
    }

    /** Retries the pending changes that are due, in one transaction under the feed's lock. */
    private int retryPending() {
        return inFeedTransaction(() -> {
            List<String> due = pending.due(jdbc, Math.max(1, properties.getBatchSize()));
            return publishEach(due, false, (submissionId, attempt) -> {
                if (attempt.failure() != null) {
                    recordFailure(submissionId, attempt);
                }
            });
        });
    }

    /**
     * Writes the marked submissions again at their held version with new positions, in one transaction under the
     * feed's lock; a mark stays until its republication is written.
     */
    private int republish() {
        return inFeedTransaction(() -> {
            List<String> due = republishMarks.due(jdbc, Math.max(1, properties.getBatchSize()));
            return publishEach(due, true, (submissionId, attempt) -> {
                if (attempt.failure() == null) {
                    republishMarks.written(jdbc, submissionId);
                } else {
                    int attempts = republishMarks.failed(jdbc, submissionId, attempt.failure());
                    log.warn("The republication of submission {} failed ({}, {}); its mark stays and it is tried "
                        + "again, failure {}", submissionId, attempt.failure(), attempt.type(), attempts);
                }
            });
        });
    }

    /** Runs work in a transaction of its own that takes the feed's lock first; returns what the work returns. */
    private int inFeedTransaction(IntSupplier work) {
        Integer done = afterBacklog.execute(status -> {
            FeedLock.take(jdbc.getJdbcOperations());
            return work.getAsInt();
        });
        return done == null ? 0 : done;
    }

    /**
     * Publishes these submissions one by one, each in its own savepoint, handing each attempt to {@code outcome};
     * stops starting new ones once half the feed part's time limit has passed. Returns how many were attempted: the
     * rest wait for the next batch.
     */
    private int publishEach(List<String> submissionIds, boolean republish,
                            BiConsumer<String, Attempt> outcome) {
        long started = System.nanoTime();
        int attempted = 0;
        for (String submissionId : submissionIds) {
            if (attempted > 0 && System.nanoTime() - started > partTimeout.toNanos() / 2) {
                break;
            }
            attempted++;
            outcome.accept(submissionId, attempt(submissionId, republish));
        }
        return attempted;
    }

    /**
     * One submission's publication in a savepoint of the current transaction, within the feed part's time limit:
     * written, or rolled back to the savepoint with the failure's reason code.
     */
    private Attempt attempt(String submissionId, boolean republish) {
        FeedStatements statements = new FeedStatements(dataSource, System.nanoTime() + partTimeout.toNanos());
        Savepoint savepoint = jdbc.getJdbcOperations().execute((ConnectionCallback<Savepoint>) Connection::setSavepoint);
        FeedWriter.Result result;
        try {
            result = writer.publish(statements, submissionId, republish);
        } catch (RuntimeException failure) {
            jdbc.getJdbcOperations().execute((ConnectionCallback<Void>) connection -> {
                connection.rollback(savepoint);
                return null;
            });
            failures.increment();
            return new Attempt(null, FeedFailure.reasonOf(failure), FeedFailure.typeOf(failure));
        }
        jdbc.getJdbcOperations().execute((ConnectionCallback<Void>) connection -> {
            connection.releaseSavepoint(savepoint);
            return null;
        });
        changes.get(result.outcome()).increment();
        result.flags().forEach((flag, count) -> flags.get(flag).increment(count));
        return new Attempt(result, null, null);
    }

    /**
     * One publication's outcome.
     *
     * @param result  what was written, or {@code null} when it failed
     * @param failure the failure's reason code, or {@code null}
     * @param type    the failure's exception type, for the log
     */
    private record Attempt(FeedWriter.Result result, String failure, String type) {
    }

    /** Records a failed feed part as a pending change, in the current transaction, and logs it without values. */
    private void recordFailure(String submissionId, Attempt attempt) {
        PendingChanges.Failure recorded = pending.recordFailure(jdbc, submissionId, attempt.failure());
        if (recorded.newlyDead()) {
            log.error("The feed gave up on submission {} after {} failures ({}, {}); reconcile publishes it once the "
                + "cause is fixed", submissionId, recorded.attempts(), attempt.failure(), attempt.type());
        } else if (recorded.dead()) {
            log.warn("The feed part of dead submission {} failed again ({}, {}); reconcile tries it at each pass",
                submissionId, attempt.failure(), attempt.type());
        } else {
            log.warn("The feed part of submission {} failed ({}, {}); it waits for a retry after the live backlog, "
                    + "failure {} of {}", submissionId, attempt.failure(), attempt.type(), recorded.attempts(),
                properties.getMaxAttempts());
        }
    }

    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("the feed is written only inside a transaction that holds its lock");
        }
    }
}
