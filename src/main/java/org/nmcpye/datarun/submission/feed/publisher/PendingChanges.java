package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The pending changes ({@code feed_ops.pending}): submissions whose feed part failed, one row each, whatever wrote the
 * failed change. Written in the caller's transaction, which holds the feed's lock.
 *
 * <p>A failure counts one attempt and keeps the reason code. The change is retried after a wait that doubles with each
 * failure, from {@code datarun.feed.retry-delay} up to {@code datarun.feed.max-retry-delay}; at
 * {@code datarun.feed.max-attempts} failures it is dead. A dead change is retried no more by the retries, only by
 * reconcile, and a failure there keeps it dead. Any publication of the submission's head removes the row
 * ({@link FeedWriter}).</p>
 */
final class PendingChanges {

    /**
     * What a failure left.
     *
     * @param attempts the change's failures so far
     * @param dead     whether it is dead
     */
    record Failure(int attempts, boolean dead) {
    }

    private static final String RECORD_FAILURE = """
        INSERT INTO feed_ops.pending AS p (submission_id, attempts, reason, first_failed_at, last_failed_at,
                                           next_attempt_at, dead_at)
        VALUES (:submissionId, 1, :reason, now(), now(), now() + make_interval(secs => :delaySeconds),
                CASE WHEN :maxAttempts <= 1 THEN now() END)
        ON CONFLICT (submission_id) DO UPDATE SET
            attempts = p.attempts + 1,
            reason = excluded.reason,
            last_failed_at = excluded.last_failed_at,
            next_attempt_at = now() + make_interval(secs => least(:maxDelaySeconds,
                                                                  :delaySeconds * power(2, least(p.attempts, 40)))),
            dead_at = coalesce(p.dead_at, CASE WHEN p.attempts + 1 >= :maxAttempts THEN now() END)
        RETURNING attempts, dead_at IS NOT NULL AS dead""";

    private final FeedPublisherProperties properties;

    PendingChanges(FeedPublisherProperties properties) {
        this.properties = properties;
    }

    /** Records one failure of the submission's feed part, with its reason code. */
    Failure recordFailure(NamedParameterJdbcTemplate jdbc, String submissionId, String reason) {
        return jdbc.queryForObject(RECORD_FAILURE, new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("reason", reason)
                .addValue("delaySeconds", (double) properties.getRetryDelay().toMillis() / 1000)
                .addValue("maxDelaySeconds", (double) properties.getMaxRetryDelay().toMillis() / 1000)
                .addValue("maxAttempts", properties.getMaxAttempts()),
            (rs, n) -> new Failure(rs.getInt("attempts"), rs.getBoolean("dead")));
    }
}
