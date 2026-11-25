package org.nmcpye.datarun.outbox.processing;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * What waits in the change queue, as the outputs report it: changes recorded but not processed yet, and changes given
 * up on. The queue is the outbox today; this query is the one place an output reads it, so a change of queue touches
 * only this class. It reads in the caller's transaction, through the template it is given.
 */
@Component
public class ChangeBacklog {

    /** The outbox's created_at is the writer's transaction time in UTC. */
    private static final String READ = """
        SELECT count(*) FILTER (WHERE o.status IN ('pending', 'failed')) AS waiting,
               (min(o.created_at) FILTER (WHERE o.status IN ('pending', 'failed'))) AT TIME ZONE 'UTC' AS oldest,
               count(*) FILTER (WHERE o.status = 'dead') AS dead
        FROM outbox o
        WHERE o.status IN ('pending', 'failed', 'dead')""";

    /**
     * The queue at one moment.
     *
     * @param waiting  changes recorded but not processed yet
     * @param oldest   when the oldest of them was recorded, or {@code null} when none waits
     * @param givenUp  changes the runtime gave up on (dead)
     */
    public record Reading(long waiting, OffsetDateTime oldest, long givenUp) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ChangeBacklog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Reading read() {
        return jdbc.queryForObject(READ, Map.of(), (rs, n) -> new Reading(rs.getLong("waiting"),
            rs.getObject("oldest", OffsetDateTime.class), rs.getLong("dead")));
    }
}
