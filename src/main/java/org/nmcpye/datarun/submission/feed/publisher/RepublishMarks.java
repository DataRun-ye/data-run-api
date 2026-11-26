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
 * was owed. Marks are set by an admin's republication of a scope (feed build step F4, {@link #mark}): the records the
 * feed holds, of every form, of one form, or of listed submissions.</p>
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

    /** A mark for each record the feed holds; a submission marked already keeps its mark. */
    private static final String MARK_ALL = """
        INSERT INTO feed_ops.republish (submission_id, marked_at)
        SELECT f.id, now() FROM feed.submission f
        ON CONFLICT (submission_id) DO NOTHING""";

    private static final String MARK_FORM = """
        INSERT INTO feed_ops.republish (submission_id, marked_at)
        SELECT f.id, now() FROM feed.submission f WHERE f.form_id = :formId
        ON CONFLICT (submission_id) DO NOTHING""";

    private static final String MARK_LISTED = """
        INSERT INTO feed_ops.republish (submission_id, marked_at)
        SELECT f.id, now() FROM feed.submission f WHERE f.id IN (:submissionIds)
        ON CONFLICT (submission_id) DO NOTHING""";

    private static final String WAITING = "SELECT count(*) FROM feed_ops.republish";

    private final FeedPublisherProperties properties;

    RepublishMarks(FeedPublisherProperties properties) {
        this.properties = properties;
    }

    /**
     * Marks a scope's records for republication: the listed submissions when some are listed, else one form's when a
     * form is named, else every record. Only records the feed holds are marked (a head it lacks is reconcile's), and a
     * submission marked already keeps its mark. How many marks it added.
     */
    int mark(NamedParameterJdbcTemplate jdbc, String formId, List<String> submissionIds) {
        if (submissionIds != null && !submissionIds.isEmpty()) {
            return jdbc.update(MARK_LISTED, Map.of("submissionIds", submissionIds));
        }
        if (formId != null) {
            return jdbc.update(MARK_FORM, Map.of("formId", formId));
        }
        return jdbc.update(MARK_ALL, Map.of());
    }

    /** How many marks wait, due or not. */
    long waiting(NamedParameterJdbcTemplate jdbc) {
        Long waiting = jdbc.queryForObject(WAITING, Map.of(), Long.class);
        return waiting == null ? 0 : waiting;
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
