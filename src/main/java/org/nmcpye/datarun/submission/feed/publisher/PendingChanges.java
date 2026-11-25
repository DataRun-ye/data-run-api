package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

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
     * @param attempts  the change's failures so far
     * @param dead      whether it is dead
     * @param newlyDead whether this failure made it dead
     */
    record Failure(int attempts, boolean dead, boolean newlyDead) {
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
        RETURNING attempts, dead_at IS NOT NULL AS dead, dead_at IS NOT DISTINCT FROM now() AS newly_dead""";

    /** The pending changes due for a retry, longest waiting first; dead ones never. */
    private static final String DUE = """
        SELECT submission_id FROM feed_ops.pending
        WHERE dead_at IS NULL AND next_attempt_at <= now()
        ORDER BY next_attempt_at, submission_id
        LIMIT :limit""";

    private final FeedPublisherProperties properties;

    PendingChanges(FeedPublisherProperties properties) {
        this.properties = properties;
    }

    /** The submissions whose pending change is due for a retry, at most {@code limit}. */
    List<String> due(NamedParameterJdbcTemplate jdbc, int limit) {
        return jdbc.queryForList(DUE, Map.of("limit", limit), String.class);
    }

    /** Records one failure of the submission's feed part, with its reason code. */
    Failure recordFailure(NamedParameterJdbcTemplate jdbc, String submissionId, String reason) {
        return jdbc.queryForObject(RECORD_FAILURE, new MapSqlParameterSource()
                .addValue("submissionId", submissionId)
                .addValue("reason", reason)
                .addValue("delaySeconds", (double) properties.getRetryDelay().toMillis() / 1000)
                .addValue("maxDelaySeconds", (double) properties.getMaxRetryDelay().toMillis() / 1000)
                .addValue("maxAttempts", properties.getMaxAttempts()),
            (rs, n) -> new Failure(rs.getInt("attempts"), rs.getBoolean("dead"), rs.getBoolean("newly_dead")));
    }
}
