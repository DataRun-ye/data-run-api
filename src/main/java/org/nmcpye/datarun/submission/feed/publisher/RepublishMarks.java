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
 * feed holds, of every form, of one form, or of listed submissions. For every form or one form, the publisher reads the
 * records the feed holds a page of the primary key at a time, outside the feed's lock ({@link #heldAfter}): no index
 * serves one form's records, and a page is bounded. Listed submissions go a batch of the list at a time. Each batch's
 * records of the scope are marked in a short transaction of their own under the lock, so live changes go on between
 * them.</p>
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

    /** The records the feed holds after a submission id, in id order, with their form: a page of the primary key. */
    private static final String HELD_AFTER = """
        SELECT id, form_id FROM feed.submission WHERE id > :after ORDER BY id LIMIT :limit""";

    /** A mark for each listed submission the feed holds; a submission marked already keeps its mark. */
    private static final String MARK = """
        INSERT INTO feed_ops.republish (submission_id, marked_at)
        SELECT f.id, now() FROM feed.submission f WHERE f.id IN (:submissionIds)
        ON CONFLICT (submission_id) DO NOTHING""";

    private static final String WAITING = "SELECT count(*) FROM feed_ops.republish";

    private final FeedPublisherProperties properties;

    RepublishMarks(FeedPublisherProperties properties) {
        this.properties = properties;
    }

    /** A record the feed holds, as a republication pages through them: its submission and its form. */
    record Held(String submissionId, String formId) {
    }

    /**
     * The records the feed holds after the submission {@code after} (empty: from the first), in id order, at most
     * {@code limit}, with their form.
     */
    List<Held> heldAfter(NamedParameterJdbcTemplate jdbc, String after, int limit) {
        return jdbc.query(HELD_AFTER, Map.of("after", after, "limit", limit),
            (rs, n) -> new Held(rs.getString("id"), rs.getString("form_id")));
    }

    /**
     * Marks the listed submissions for republication, at least one. Only records the feed holds are marked: a head it
     * lacks is the first publication's, or reconcile's once that has completed. A submission marked already keeps its
     * mark. How many marks it added.
     */
    int mark(NamedParameterJdbcTemplate jdbc, List<String> submissionIds) {
        return jdbc.update(MARK, Map.of("submissionIds", submissionIds));
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
