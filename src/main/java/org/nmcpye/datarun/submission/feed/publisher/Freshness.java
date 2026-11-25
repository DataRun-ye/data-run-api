package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * The feed's freshness record ({@code feed.freshness}, key {@code submissions}; contract 7), written in the caller's
 * transaction, which holds the feed's lock. Each publication moves its last position, last publication and newest
 * change forward ({@link FeedWriter}); this class writes the rest.
 */
final class Freshness {

    /**
     * What waits, counted at each poll: the backlog (outbox rows not processed yet) and its oldest change, the pending
     * changes and the oldest one's first failure, and what is dead (dead outbox rows and pending changes past the cap).
     * The outbox's created_at is the writer's transaction time in UTC.
     */
    private static final String REFRESH = """
        WITH outbox_counts AS (
            SELECT count(*) FILTER (WHERE o.status IN ('pending', 'failed')) AS backlog,
                   min(o.created_at) FILTER (WHERE o.status IN ('pending', 'failed')) AS oldest,
                   count(*) FILTER (WHERE o.status = 'dead') AS dead
            FROM outbox o
            WHERE o.status IN ('pending', 'failed', 'dead')
        ),
        pending_counts AS (
            SELECT count(*) FILTER (WHERE p.dead_at IS NULL) AS pending,
                   min(p.first_failed_at) FILTER (WHERE p.dead_at IS NULL) AS oldest,
                   count(*) FILTER (WHERE p.dead_at IS NOT NULL) AS dead
            FROM feed_ops.pending p
        )
        INSERT INTO feed.freshness AS f (feed, backlog, oldest_unpublished_at, pending, oldest_pending_at, dead,
                                         checked_at)
        SELECT 'submissions', o.backlog, o.oldest AT TIME ZONE 'UTC', p.pending, p.oldest, o.dead + p.dead, now()
        FROM outbox_counts o CROSS JOIN pending_counts p
        ON CONFLICT (feed) DO UPDATE SET
            backlog = excluded.backlog,
            oldest_unpublished_at = excluded.oldest_unpublished_at,
            pending = excluded.pending,
            oldest_pending_at = excluded.oldest_pending_at,
            dead = excluded.dead,
            checked_at = excluded.checked_at
        RETURNING backlog, extract(epoch FROM checked_at - oldest_unpublished_at)::float8 AS backlog_age, pending,
                  extract(epoch FROM checked_at - oldest_pending_at)::float8 AS pending_age, dead""";

    /**
     * What the feed reported at a poll.
     *
     * @param backlog            outbox rows not processed yet
     * @param oldestBacklogAge   how long ago the oldest of them was written, in seconds, or {@code null} when none
     * @param pending            pending changes
     * @param oldestPendingAge   how long ago the oldest pending change first failed, in seconds, or {@code null}
     * @param dead               dead outbox rows and pending changes past the cap
     */
    record Reading(long backlog, Double oldestBacklogAge, long pending, Double oldestPendingAge, long dead) {

        /**
         * Whether the feed is within its freshness target (contract 12): the oldest unpublished outbox change and the
         * oldest pending change are both younger than the target, and nothing is dead.
         */
        boolean withinTarget(Duration target) {
            double seconds = target.toMillis() / 1000.0;
            return dead == 0
                && (oldestBacklogAge == null || oldestBacklogAge < seconds)
                && (oldestPendingAge == null || oldestPendingAge < seconds);
        }
    }

    /** Writes what waits into the freshness record, also when nothing does ({@code checked_at}); what it wrote. */
    Reading refresh(NamedParameterJdbcTemplate jdbc) {
        return jdbc.queryForObject(REFRESH, Map.of(), (rs, n) -> new Reading(
            rs.getLong("backlog"),
            rs.getObject("backlog_age") == null ? null : rs.getDouble("backlog_age"),
            rs.getLong("pending"),
            rs.getObject("pending_age") == null ? null : rs.getDouble("pending_age"),
            rs.getLong("dead")));
    }

    private static final String RECONCILED = """
        INSERT INTO feed.freshness AS f (feed, backlog, pending, dead, checked_at, unpublished, last_reconciled_at,
                                         first_publication_completed_at)
        VALUES ('submissions', 0, 0, 0, now(), :unpublished, now(), CASE WHEN :complete THEN now() END)
        ON CONFLICT (feed) DO UPDATE SET
            unpublished = excluded.unpublished,
            last_reconciled_at = excluded.last_reconciled_at,
            first_publication_completed_at = coalesce(f.first_publication_completed_at,
                                                      excluded.first_publication_completed_at),
            checked_at = excluded.checked_at""";

    /**
     * Records a finished reconcile: how many submissions it found unpublished, and when. The first pass that wrote
     * every head it found unpublished sets {@code first_publication_completed_at}, which stays from then on.
     */
    void reconciled(NamedParameterJdbcTemplate jdbc, Reconcile.Summary summary) {
        jdbc.update(RECONCILED, new MapSqlParameterSource()
            .addValue("unpublished", summary.differing())
            .addValue("complete", summary.complete()));
    }
}
