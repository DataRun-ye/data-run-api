package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.outbox.processing.ChangeBacklog;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Types;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The feed's freshness record ({@code feed.freshness}, key {@code submissions}; contract 7), written in the caller's
 * transaction, which holds the feed's lock. Each publication moves its last position, last publication and newest
 * change forward ({@link FeedWriter}); this class writes the rest. Freshness counts only the feed's own state: its
 * backlog is the open changes whose version the feed lacks, whatever the runtime's other outputs did with them, and
 * its dead changes are its own.
 */
final class Freshness {

    /**
     * What waits, at each poll: the backlog and its oldest change, given; the pending changes, the oldest one's first
     * failure, and the pending changes past the cap (the feed's dead), from the feed's own table.
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
        SELECT 'submissions', :backlog, :oldestUnpublished, p.pending, p.oldest, p.dead, now()
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
     * @param backlog            open changes whose version the feed does not hold yet
     * @param oldestBacklogAge   how long ago the oldest of them was recorded, in seconds, or {@code null} when none
     * @param pending            pending changes
     * @param oldestPendingAge   how long ago the oldest pending change first failed, in seconds, or {@code null}
     * @param dead               the feed's changes given up on: pending changes past the cap
     */
    record Reading(long backlog, Double oldestBacklogAge, long pending, Double oldestPendingAge, long dead) {

        /**
         * Whether the feed is within its freshness target (contract 12): the oldest change of its backlog and its oldest
         * pending change are both younger than the target, and it has nothing dead.
         */
        boolean withinTarget(Duration target) {
            double seconds = target.toMillis() / 1000.0;
            return dead == 0
                && (oldestBacklogAge == null || oldestBacklogAge < seconds)
                && (oldestPendingAge == null || oldestPendingAge < seconds);
        }
    }

    private static final String HELD = "SELECT id, version FROM feed.submission WHERE id IN (:ids)";

    /** Open changes compared with the feed's versions at a time, so a burst never makes one huge statement. */
    private static final int CHUNK = 5000;

    /**
     * Writes what waits into the freshness record, also when nothing does ({@code checked_at}); what it wrote. The
     * backlog is the open changes (from the change queue's seam) whose version the feed does not hold yet.
     */
    Reading refresh(NamedParameterJdbcTemplate jdbc, List<ChangeBacklog.OpenChange> open) {
        long backlog = 0;
        OffsetDateTime oldestUnpublished = null;
        for (int from = 0; from < open.size(); from += CHUNK) {
            List<ChangeBacklog.OpenChange> chunk = open.subList(from, Math.min(open.size(), from + CHUNK));
            List<String> ids = new ArrayList<>(chunk.stream().map(ChangeBacklog.OpenChange::submissionId).distinct()
                .toList());
            Map<String, Long> held = new HashMap<>();
            jdbc.query(HELD, Map.of("ids", ids), rs -> {
                held.put(rs.getString("id"), rs.getLong("version"));
            });
            for (ChangeBacklog.OpenChange change : chunk) {
                Long version = held.get(change.submissionId());
                if (version == null || version < change.version()) {
                    backlog++;
                    if (oldestUnpublished == null || change.recordedAt().isBefore(oldestUnpublished)) {
                        oldestUnpublished = change.recordedAt();
                    }
                }
            }
        }
        return jdbc.queryForObject(REFRESH, new MapSqlParameterSource()
            .addValue("backlog", backlog)
            .addValue("oldestUnpublished", oldestUnpublished, Types.TIMESTAMP_WITH_TIMEZONE), (rs, n) -> new Reading(
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
