package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.outbox.processing.ChangeBacklog;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Types;
import java.time.Duration;

/**
 * The feed's freshness record ({@code feed.freshness}, key {@code submissions}; contract 7), written in the caller's
 * transaction, which holds the feed's lock. Each publication moves its last position, last publication and newest
 * change forward ({@link FeedWriter}); this class writes the rest.
 */
final class Freshness {

    /**
     * What waits, at each poll: the changes recorded but not processed yet and the oldest of them, and the changes
     * given up on, read from the change queue ({@link ChangeBacklog}), with the pending changes and the oldest one's
     * first failure, and the pending changes past the cap, from the feed's own table.
     */
    private static final String REFRESH = """
        WITH pending_counts AS (
            SELECT count(*) FILTER (WHERE p.dead_at IS NULL) AS pending,
                   min(p.first_failed_at) FILTER (WHERE p.dead_at IS NULL) AS oldest,
                   count(*) FILTER (WHERE p.dead_at IS NOT NULL) AS dead
            FROM feed_ops.pending p
        )
        INSERT INTO feed.freshness AS f (feed, backlog, oldest_unpublished_at, pending, oldest_pending_at, dead,
                                         checked_at)
        SELECT 'submissions', :waiting, :oldestWaiting, p.pending, p.oldest, :givenUp + p.dead, now()
        FROM pending_counts p
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
     * @param backlog            changes recorded but not processed yet
     * @param oldestBacklogAge   how long ago the oldest of them was recorded, in seconds, or {@code null} when none
     * @param pending            pending changes
     * @param oldestPendingAge   how long ago the oldest pending change first failed, in seconds, or {@code null}
     * @param dead               changes given up on: by the runtime, and pending changes past the cap
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
    Reading refresh(NamedParameterJdbcTemplate jdbc, ChangeBacklog.Reading backlog) {
        return jdbc.queryForObject(REFRESH, new MapSqlParameterSource()
            .addValue("waiting", backlog.waiting())
            .addValue("oldestWaiting", backlog.oldest(), Types.TIMESTAMP_WITH_TIMEZONE)
            .addValue("givenUp", backlog.givenUp()), (rs, n) -> new Reading(
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
