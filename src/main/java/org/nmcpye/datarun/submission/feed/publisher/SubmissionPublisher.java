package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.nmcpye.datarun.config.activation.ActivationSwitches;
import org.nmcpye.datarun.outbox.processing.ChangeBacklog;
import org.nmcpye.datarun.outbox.processing.ChangeOutput;
import org.nmcpye.datarun.outbox.processing.ChangeReference;
import org.nmcpye.datarun.outbox.processing.DeferredWork;
import org.nmcpye.datarun.submission.feed.FeedActivation;
import org.nmcpye.datarun.submission.normalized.NormalizationFlag;
import org.nmcpye.datarun.submission.normalized.jdbc.StoredSubmissionJdbcReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

/**
 * The submission feed's publisher: an output of the outbox's changes ({@link ChangeOutput}) and the work it keeps for
 * when no change is due ({@link DeferredWork}). The runtime that claims outbox rows reaches it only through those two
 * interfaces, and it calls nothing of the runtime's or of the other outputs': it reads the journal and writes the feed.
 * Nothing is written while the switch {@code submission-feed} is off.
 *
 * <p><b>A live change</b> is published in the runtime's per-row transaction (contract 8, item 4): {@link #begin} takes
 * the feed's lock, outside every savepoint, before any output applies the change; {@link #apply} then reads the
 * submission's head and the version the feed holds, normalizes, and writes the submission's record with its changed,
 * new and deleted row records ({@link FeedWriter}), in the savepoint the runtime gives it, by its own time limit
 * ({@code datarun.feed.part-timeout}), which the runtime keeps below its row timeout. Its failure rolls back that
 * savepoint only, and the submission waits as a pending change with its reason code ({@link #keepsFailure},
 * {@link PendingChanges}); the change is done for the runtime. Another output's failure never rolls back the feed's
 * work: the outbox row's retry then finds the head held and writes nothing. It is applied first.</p>
 *
 * <p><b>Its deferred work</b> runs one batch at a time once no outbox change is due, the live changes first again after
 * each ({@link #runBatch}): pending changes due for a retry, each failure counting an attempt until the cap makes it
 * dead; then republish marks, each written again at the same version with new positions, its mark kept while it fails
 * ({@link RepublishMarks}); then the first publication, which writes every submission's head in id order from its mark
 * ({@link FirstPublication}); then reconcile, which publishes every head the feed lacks or holds at another version, at
 * start (also switch-on) and nightly ({@link Reconcile}). An admin starts the first publication and marks a scope for
 * republication ({@link FeedPublicationEndpoint}); neither writes an outbox row. Each batch is one transaction of its
 * own under the feed's
 * lock, each submission in its own savepoint, and stops starting submissions once it has run half the feed part's
 * limit, so it holds the lock briefly.</p>
 *
 * <p><b>Freshness:</b> at each poll the runtime asks {@link #withinFreshnessTarget}, which writes what waits into
 * {@code feed.freshness} (backlog, pending and dead changes, the oldest of each) and says whether the feed is within
 * {@code datarun.feed.freshness-target}; the runtime's heartbeat stops while it is not (contract 12). Freshness counts
 * only the feed's own state: the backlog is the change queue's open changes ({@link ChangeBacklog}) whose version the
 * feed lacks, and its dead changes are its own.</p>
 *
 * <p>It reads stored submissions only through their read seam ({@link StoredSubmissionJdbcReader}) and the change
 * queue only through {@link ChangeBacklog}; the SQL here and in this package names the feed's own tables only.</p>
 *
 * <p><b>Savepoints are JDBC savepoints on the transaction's connection.</b> Spring's {@code Propagation.NESTED} is not
 * available: the application's transaction manager is JPA's, whose Hibernate dialect offers no savepoints, so a nested
 * transaction would fail to start. The feed writes through JDBC only, so a savepoint on the connection rolls back
 * exactly what it wrote.</p>
 */
@Component
@Order(1)
public class SubmissionPublisher implements ChangeOutput, DeferredWork {

    private static final Logger log = LoggerFactory.getLogger(SubmissionPublisher.class);

    private final ActivationSwitches switches;
    private final DataSource dataSource;
    private final NamedParameterJdbcTemplate jdbc;
    private final FeedPublisherProperties properties;
    private final Duration partTimeout;
    private final FeedWriter writer;
    private final PendingChanges pending;
    private final RepublishMarks republishMarks;
    private final FirstPublication firstPublication = new FirstPublication();
    private final StoredSubmissionJdbcReader submissions;
    private final Reconcile reconcile;
    private final ChangeBacklog backlog;
    private final Freshness freshness = new Freshness();
    private final TransactionTemplate afterBacklog;
    private final TransactionTemplate reading;
    private final Map<FeedWriter.Outcome, Counter> changes;
    private final Counter failures;
    private final Map<NormalizationFlag, Counter> flags;
    /** Whether the last freshness reading was within the target; {@code null} before the first. */
    private volatile Boolean withinTarget;

    @Autowired
    public SubmissionPublisher(ActivationSwitches switches, DataSource dataSource,
                               PlatformTransactionManager transactionManager, ObjectMapper objectMapper,
                               FeedPublisherProperties properties, ObjectProvider<SplitPartners> splitPartners,
                               StoredSubmissionJdbcReader submissions, ChangeBacklog backlog, MeterRegistry registry) {
        this(switches, dataSource, transactionManager, properties, new FeedWriter(new FormDefinitions(), objectMapper,
            splitPartners.getIfAvailable(() -> SplitPartners.NONE)), submissions, backlog, registry);
    }

    SubmissionPublisher(ActivationSwitches switches, DataSource dataSource,
                        PlatformTransactionManager transactionManager, FeedPublisherProperties properties,
                        FeedWriter writer, StoredSubmissionJdbcReader submissions, ChangeBacklog backlog,
                        MeterRegistry registry) {
        this.switches = switches;
        this.dataSource = dataSource;
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.properties = properties;
        this.partTimeout = properties.getPartTimeout();
        this.writer = writer;
        this.pending = new PendingChanges(properties);
        this.republishMarks = new RepublishMarks(properties);
        this.submissions = submissions;
        this.reconcile = new Reconcile(submissions, properties.getReconcileScanBudget());
        this.backlog = backlog;
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

    @Override
    public String name() {
        return "submission-feed";
    }

    /** Whether the feed is published: the switch {@code submission-feed}. */
    @Override
    public boolean isOn() {
        return switches.isOn(FeedActivation.SUBMISSION_FEED);
    }

    /** {@code datarun.feed.part-timeout}. */
    @Override
    public Optional<Duration> timeLimit() {
        return Optional.of(partTimeout);
    }

    /**
     * Takes the feed's lock in the runtime's transaction ({@link FeedLock}), before any output applies the change,
     * waiting for it until the deadline (the feed part's limit). A lock another writer holds past that fails the feed
     * part with a timeout, which the feed keeps as a pending change while the other outputs apply the change.
     *
     * @throws IllegalStateException when no transaction is active
     */
    @Override
    public void begin(long deadlineNanos) {
        requireTransaction();
        FeedLock.take(new FeedStatements(dataSource, deadlineNanos).named().getJdbcOperations());
    }

    /**
     * Publishes the head of the change's submission, in the runtime's transaction and savepoint, by the deadline.
     * Throws when the feed part fails; the runtime then rolls back the savepoint and hands the failure to
     * {@link #keepsFailure}.
     */
    @Override
    public void apply(ChangeReference change, long deadlineNanos) {
        requireTransaction();
        FeedWriter.Result result = writer.publish(new FeedStatements(dataSource, deadlineNanos), change.submissionId(),
            false);
        counted(result);
    }

    /**
     * Records the failed feed part as a pending change, in the runtime's transaction: the feed keeps it. When the part
     * failed because the lock was not free in time, the change is recorded without the lock ({@link PendingChanges}).
     */
    @Override
    public boolean keepsFailure(ChangeReference change, RuntimeException failure) {
        failures.increment();
        recordFailure(change.submissionId(),
            new Attempt(null, FeedFailure.reasonOf(failure), FeedFailure.typeOf(failure)));
        return true;
    }

    /**
     * Publishes one batch of the work that waits for when no outbox change is due: pending changes due for a retry,
     * else republish marks, else the first publication's next heads, else reconcile's next submissions.
     *
     * @return whether the batch did anything; false when nothing waits, or the feed is off
     */
    @Override
    public boolean runBatch() {
        if (!isOn()) {
            return false;
        }
        return retryPending() > 0 || republish() > 0 || firstPublicationBatch() || reconcileBatch();
    }

    /**
     * Writes what waits into {@code feed.freshness}, under the feed's lock, and says whether the feed is within its
     * freshness target. Logs once when it falls behind, and once when it is back.
     */
    @Override
    public boolean withinFreshnessTarget() {
        if (!isOn()) {
            return true;
        }
        Freshness.Reading reading = afterBacklog.execute(status -> {
            FeedLock.take(jdbc.getJdbcOperations());
            return freshness.refresh(jdbc, backlog.openChanges());
        });
        boolean within = reading != null && reading.withinTarget(properties.getFreshnessTarget());
        Boolean before = withinTarget;
        withinTarget = within;
        if (!within && !Boolean.FALSE.equals(before) && reading != null) {
            log.warn("The feed is behind its freshness target ({}): {} changes not published yet, the oldest {} s; {} "
                    + "pending changes, the oldest {} s; {} dead. The heartbeat stops until it is within it again",
                properties.getFreshnessTarget(), reading.backlog(), seconds(reading.oldestBacklogAge()),
                reading.pending(), seconds(reading.oldestPendingAge()), reading.dead());
        } else if (within && Boolean.FALSE.equals(before)) {
            log.info("The feed is within its freshness target again; the heartbeat resumes");
        }
        return within;
    }

    private static Object seconds(Double age) {
        return age == null ? "none" : Math.round(age);
    }

    /** Reconcile's scan budget: one batch reads pages for at most about this long while it finds nothing to publish. */
    @Override
    public Optional<Duration> scanBudget() {
        return Optional.of(properties.getReconcileScanBudget());
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
     * Publishes reconcile's next submissions, scanning further when none are queued for at most the scan budget, and
     * records the pass in freshness once it ends. Whether a pass runs: true also when this batch only read pages, so
     * the runtime calls again, live changes first, and the pass goes on from where it stopped.
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

    /**
     * Starts the first publication (an admin's, feed build step F4): every submission's head, in id order, in batches
     * behind the live backlog, from the beginning. Where it stands; empty when one is under way already, which goes
     * on.
     *
     * @throws IllegalStateException when the feed is off
     */
    Optional<FirstPublication.State> startFirstPublication() {
        requireOn();
        Optional<FirstPublication.State> started = Optional.ofNullable(afterBacklog.execute(status -> {
            FeedLock.take(jdbc.getJdbcOperations());
            return firstPublication.start(jdbc).orElse(null);
        }));
        if (started.isPresent()) {
            log.info("The first publication starts: every submission's head, in batches of {}, behind the live backlog",
                Math.max(1, properties.getBatchSize()));
        }
        return started;
    }

    /** Where the first publication stands; empty when none was ever started. */
    Optional<FirstPublication.State> firstPublicationState() {
        return firstPublication.read(jdbc);
    }

    /**
     * Marks a scope's records for republication (an admin's, feed build step F4): the listed submissions when some
     * are listed, else one form's, else every record the feed holds. The marks are written in batches behind the live
     * backlog, each at the version the feed holds with new positions; a newer held version is skipped. How many marks
     * it added.
     *
     * @throws IllegalStateException when the feed is off
     */
    int markForRepublication(String formId, List<String> submissionIds) {
        requireOn();
        int marked = inFeedTransaction(() -> republishMarks.mark(jdbc, formId, submissionIds));
        log.info("Republication marked {} records ({})", marked, submissionIds != null && !submissionIds.isEmpty()
            ? submissionIds.size() + " submissions listed" : formId != null ? "form " + formId : "every form");
        return marked;
    }

    /** How many republish marks wait. */
    long republishMarksWaiting() {
        return republishMarks.waiting(jdbc);
    }

    private void requireOn() {
        if (!isOn()) {
            throw new IllegalStateException("The submission feed is off (activation switch submission-feed)");
        }
    }

    /**
     * Writes the first publication's next batch of heads, in id order from its mark, and moves the mark past them, in
     * one transaction under the feed's lock: a restart repeats at most the batch it lost, and finds those heads held.
     * A head that cannot be written is a pending change, retried as usual. Whether a first publication was under way.
     */
    private boolean firstPublicationBatch() {
        if (firstPublication.read(jdbc).filter(FirstPublication.State::running).isEmpty()) {
            return false;
        }
        int batchSize = Math.max(1, properties.getBatchSize());
        FirstPublication.State[] after = new FirstPublication.State[1];
        inFeedTransaction(() -> {
            Optional<FirstPublication.State> open = firstPublication.read(jdbc).filter(FirstPublication.State::running);
            if (open.isEmpty()) {
                return 0;
            }
            List<StoredSubmissionJdbcReader.HeadVersion> page =
                submissions.headVersionsAfter(open.get().mark(), batchSize);
            List<String> heads = page.stream().filter(head -> head.version() != null)
                .map(StoredSubmissionJdbcReader.HeadVersion::submissionId).toList();
            long[] counts = new long[3];
            int attempted = publishEach(heads, false, (submissionId, attempt) -> {
                if (attempt.failure() != null) {
                    counts[2]++;
                    recordFailure(submissionId, attempt);
                } else if (attempt.result().outcome() == FeedWriter.Outcome.PUBLISHED) {
                    counts[0]++;
                } else {
                    counts[1]++;
                }
            });
            boolean whole = attempted == heads.size();
            String mark = !whole ? heads.get(attempted - 1)
                : page.isEmpty() ? open.get().mark() : page.get(page.size() - 1).submissionId();
            boolean finished = whole && page.size() < batchSize;
            after[0] = firstPublication.advance(jdbc, mark, counts[0], counts[1], counts[2], finished);
            if (finished && after[0].failed() == 0) {
                freshness.firstPublicationCompleted(jdbc);
            }
            return 1;
        });
        if (after[0] != null && !after[0].running()) {
            log.info("The first publication finished: {} heads written, {} held already, {} failed and waiting as "
                + "pending changes", after[0].written(), after[0].skipped(), after[0].failed());
        }
        return true;
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
     * One submission's publication in a savepoint of one of the feed's own transactions, within the feed part's time
     * limit: written, or rolled back to the savepoint with the failure's reason code.
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
        counted(result);
        return new Attempt(result, null, null);
    }

    private void counted(FeedWriter.Result result) {
        changes.get(result.outcome()).increment();
        result.flags().forEach((flag, count) -> flags.get(flag).increment(count));
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
