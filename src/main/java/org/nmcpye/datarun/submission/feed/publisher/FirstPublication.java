package org.nmcpye.datarun.submission.feed.publisher;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The first publication's mark ({@code feed_ops.first_publication}, contract 11, feed build step F4): one row at most,
 * written in the caller's transaction, which holds the feed's lock.
 *
 * <p>An admin starts it; the publisher then writes every submission's head, in id order and in batches behind the live
 * backlog, and moves the mark past each batch in that batch's transaction, so a restart goes on from the mark and
 * repeats at most the batch it lost. A new start is accepted only once the last one finished.</p>
 */
final class FirstPublication {

    /**
     * Where the first publication stands.
     *
     * @param requestedAt when an admin started it
     * @param mark        the last submission id it has passed, in id order ({@code ""} before the first)
     * @param finishedAt  when it passed the last submission, or {@code null} while it goes on
     * @param written     heads it wrote
     * @param skipped     heads the feed held already
     * @param failed      heads it could not write, each now a pending change
     */
    record State(OffsetDateTime requestedAt, String mark, OffsetDateTime finishedAt, long written, long skipped,
                 long failed) {

        boolean running() {
            return finishedAt == null;
        }
    }

    private static final String COLUMNS = "requested_at, mark, finished_at, written, skipped, failed";

    /** Starts one, unless one is under way: then nothing changes and nothing is returned. */
    private static final String START = """
        INSERT INTO feed_ops.first_publication AS p (id, requested_at, mark) VALUES (true, now(), '')
        ON CONFLICT (id) DO UPDATE SET requested_at = now(), mark = '', finished_at = NULL, written = 0, skipped = 0,
                                       failed = 0
        WHERE p.finished_at IS NOT NULL
        RETURNING\s""" + COLUMNS;

    private static final String READ = "SELECT " + COLUMNS + " FROM feed_ops.first_publication";

    private static final String ADVANCE = """
        UPDATE feed_ops.first_publication
        SET mark = :mark, written = written + :written, skipped = skipped + :skipped, failed = failed + :failed,
            finished_at = CASE WHEN :finished THEN now() END
        WHERE finished_at IS NULL
        RETURNING\s""" + COLUMNS;

    private static final RowMapper<State> STATE = (rs, n) -> new State(
        rs.getObject("requested_at", OffsetDateTime.class),
        rs.getString("mark"),
        rs.getObject("finished_at", OffsetDateTime.class),
        rs.getLong("written"),
        rs.getLong("skipped"),
        rs.getLong("failed"));

    /** Starts the first publication from the beginning; empty when one is under way. */
    Optional<State> start(NamedParameterJdbcTemplate jdbc) {
        return first(jdbc.query(START, Map.of(), STATE));
    }

    /** Where it stands; empty when none was ever started. */
    Optional<State> read(NamedParameterJdbcTemplate jdbc) {
        return first(jdbc.query(READ, Map.of(), STATE));
    }

    /** Moves the mark past a batch and adds its counts; finished once the batch reached the last submission. */
    State advance(NamedParameterJdbcTemplate jdbc, String mark, long written, long skipped, long failed,
                  boolean finished) {
        return jdbc.queryForObject(ADVANCE, new MapSqlParameterSource()
            .addValue("mark", mark)
            .addValue("written", written)
            .addValue("skipped", skipped)
            .addValue("failed", failed)
            .addValue("finished", finished), STATE);
    }

    private static Optional<State> first(List<State> states) {
        return states.isEmpty() ? Optional.empty() : Optional.of(states.get(0));
    }
}
