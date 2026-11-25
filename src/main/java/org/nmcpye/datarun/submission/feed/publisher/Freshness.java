package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * The feed's freshness record ({@code feed.freshness}, key {@code submissions}; contract 7), written in the caller's
 * transaction, which holds the feed's lock. Each publication moves its last position, last publication and newest
 * change forward ({@link FeedWriter}); this class writes the rest.
 */
final class Freshness {

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
