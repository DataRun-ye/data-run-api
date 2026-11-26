package org.nmcpye.datarun.submission.feed.publisher;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings of the submission feed's publisher ({@link SubmissionPublisher}). They matter only while the switch
 * {@code submission-feed} is on.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "datarun.feed")
public class FeedPublisherProperties {

    /**
     * How long one submission's feed part may take: its statements are cancelled once it has run this long, and the
     * change waits in the retry list. It must stay below the row timeout of the runtime that applies outbox changes
     * ({@code datarun.etl.row-timeout}), which refuses to start otherwise, so the feed part always fails before the
     * row's transaction does.
     */
    private Duration partTimeout = Duration.ofSeconds(20);

    /** Failures of a pending change's feed part before it is dead (the attempt cap). */
    private int maxAttempts = 10;

    /** Wait before the first retry of a pending change, or of a failed republication; it doubles with each failure. */
    private Duration retryDelay = Duration.ofMinutes(1);

    /** The longest wait between two retries. */
    private Duration maxRetryDelay = Duration.ofHours(1);

    /** Submissions published in one transaction by the retries, the republication and reconcile. */
    private int batchSize = 200;

    /** Submissions reconcile compares with their head in one read. */
    private int reconcileScanSize = 2000;

    /**
     * Republish marks an admin's republication writes in one transaction: each such transaction holds the feed's lock
     * only briefly, so live changes go on between them.
     */
    private int markBatchSize = 5000;

    /**
     * How long one reconcile batch may go on reading while it finds nothing to publish: it reads at least one page of
     * {@code reconcile-scan-size}, then stops once this time is spent, and the next batch goes on from there. The
     * runtime that runs the batches refuses to start unless it is shorter than its poll
     * ({@code datarun.etl.max-poll-duration}), so one batch never takes a whole poll.
     */
    private Duration reconcileScanBudget = Duration.ofSeconds(1);

    /**
     * The freshness target: the heartbeat is sent only while the oldest unpublished outbox change and the oldest
     * pending change are younger than this, and nothing is dead (contract 12). Integrity check 12 counts pending
     * changes older than the contract's two minutes.
     */
    private Duration freshnessTarget = Duration.ofMinutes(2);
}
