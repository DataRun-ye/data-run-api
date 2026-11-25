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
     * change waits in the retry list. Kept below the ETL's row timeout ({@code datarun.etl.row-timeout}): a setting at
     * or above it is replaced by half the row timeout, so the feed part always fails before the row's transaction does.
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
     * The freshness target: the heartbeat is sent only while the oldest unpublished outbox change and the oldest
     * pending change are younger than this, and nothing is dead (contract 12). Integrity check 12 counts pending
     * changes older than the contract's two minutes.
     */
    private Duration freshnessTarget = Duration.ofMinutes(2);
}
