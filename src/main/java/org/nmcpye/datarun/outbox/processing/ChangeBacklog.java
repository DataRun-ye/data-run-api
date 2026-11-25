package org.nmcpye.datarun.outbox.processing;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * The changes the queue still holds open, as references an output compares with what it holds: each change's
 * submission, the version it made and when it was recorded. Open means not completed by the runtime: waiting for its
 * first try, failed and waiting to be tried again, or given up on. An output counts the open changes whose version it
 * lacks; what the runtime's own outputs did with a change is no part of that count. The queue is the outbox today; this
 * query is the one place an output reads it, so a change of queue touches only this class. It reads in the caller's
 * transaction, through the template it is given.
 */
@Component
public class ChangeBacklog {

    /** The outbox's created_at is the writer's transaction time in UTC; a row written before the journal has no version. */
    private static final String OPEN = """
        SELECT o.submission_id, o.submission_version, o.created_at AT TIME ZONE 'UTC' AS recorded_at
        FROM outbox o
        WHERE o.status IN ('pending', 'failed', 'dead') AND o.submission_version IS NOT NULL""";

    /**
     * One open change.
     *
     * @param submissionId the changed submission's id
     * @param version      the version the change made
     * @param recordedAt   when the change was recorded
     */
    public record OpenChange(String submissionId, long version, OffsetDateTime recordedAt) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public ChangeBacklog(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every change the queue holds open. */
    public List<OpenChange> openChanges() {
        return jdbc.query(OPEN, Map.of(), (rs, n) -> new OpenChange(rs.getString("submission_id"),
            rs.getLong("submission_version"), rs.getObject("recorded_at", OffsetDateTime.class)));
    }
}
