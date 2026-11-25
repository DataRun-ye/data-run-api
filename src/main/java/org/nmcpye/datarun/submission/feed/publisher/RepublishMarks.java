package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * The republish marks ({@code feed_ops.republish}): submissions whose current records are to be written again, at the
 * same version with new positions (contract 11), one row each. Written in the caller's transaction, which holds the
 * feed's lock.
 *
 * <p>A mark is removed only once its republication is written. A failed one keeps its mark, with the attempt counted,
 * the reason code and a wait that doubles from {@code datarun.feed.retry-delay} up to
 * {@code datarun.feed.max-retry-delay}: reconcile compares versions only, so it would never find a republication that
 * was owed. Marks are set by the admin's republication (feed build step F4).</p>
 */
final class RepublishMarks {

    /** The marks due, in submission order; a failed one once its wait is over. */
    private static final String DUE = """
        SELECT submission_id FROM feed_ops.republish
        WHERE next_attempt_at IS NULL OR next_attempt_at <= now()
        ORDER BY submission_id
        LIMIT :limit""";

    private static final String WRITTEN = "DELETE FROM feed_ops.republish WHERE submission_id = :submissionId";

    private static final String FAILED = """
        UPDATE feed_ops.republish
        SET attempts = attempts + 1, reason = :reason, last_failed_at = now(),
            next_attempt_at = now() + make_interval(secs => least(:maxDelaySeconds,
                                                                  :delaySeconds * power(2, least(attempts, 40))))
        WHERE submission_id = :submissionId
        RETURNING attempts""";

    private final FeedPublisherProperties properties;

    RepublishMarks(FeedPublisherProperties properties) {
        this.properties = properties;
    }

    /** The submissions whose mark is due, at most {@code limit}. */
    List<String> due(NamedParameterJdbcTemplate jdbc, int limit) {
        return jdbc.queryForList(DUE, Map.of("limit", limit), String.class);
    }

    /** Removes the mark of a republication that was written. */
    void written(NamedParameterJdbcTemplate jdbc, String submissionId) {
        jdbc.update(WRITTEN, Map.of("submissionId", submissionId));
    }

    /** Keeps the mark of a republication that failed, counting the attempt; returns the attempts so far. */
    int failed(NamedParameterJdbcTemplate jdbc, String submissionId, String reason) {
        Integer attempts = jdbc.queryForObject(FAILED, new MapSqlParameterSource()
            .addValue("submissionId", submissionId)
            .addValue("reason", reason)
            .addValue("delaySeconds", (double) properties.getRetryDelay().toMillis() / 1000)
            .addValue("maxDelaySeconds", (double) properties.getMaxRetryDelay().toMillis() / 1000), Integer.class);
        return attempts == null ? 0 : attempts;
    }
}
