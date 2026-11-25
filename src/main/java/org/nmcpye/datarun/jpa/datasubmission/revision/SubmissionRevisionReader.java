package org.nmcpye.datarun.jpa.datasubmission.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Reads the journal, and the submission it records, for conflict detection: which revisions a submission has, and the
 * state the comparison reads at one of them or now. Stored documents come back as the database prints them
 * ({@code form_data::text}), so a reader can parse their numbers exactly. Called under the submission lock, in the
 * upload item's transaction, so the submission cannot change while it reads.
 */
@Repository
@RequiredArgsConstructor
public class SubmissionRevisionReader {

    /**
     * One revision, without its state.
     *
     * @param kind             {@code CREATE}, {@code UPDATE}, {@code DELETE} or {@code BASELINE}
     * @param command          the command that made it, as stored
     * @param actorUserId      the user who made it, or null for a system command
     * @param onBehalfOfUserId the uploader a release or an accept acted for, or null
     * @param deleted          whether the submission was deleted after it
     */
    public record Revision(long version, String kind, String command, String actorUserId, String onBehalfOfUserId,
                           boolean deleted) {
    }

    /**
     * What the conflict comparison reads of a submission's state.
     *
     * @param document    {@code form_data} as text, or null when the state holds no document
     * @param deleted     whether the submission is deleted
     * @param assignment  the assignment's uid
     * @param formVersion the pinned form version's uid
     */
    public record State(String document, boolean deleted, String assignment, String formVersion) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    /** The submission's revisions, oldest first. */
    public List<Revision> revisions(String submissionId) {
        return jdbc.query("""
                SELECT version, kind, command, actor_user_id, on_behalf_of_user_id,
                       coalesce((metadata ->> 'deleted')::boolean, false) AS deleted
                FROM journal.submission_revision
                WHERE submission_id = :submissionId
                ORDER BY version""",
            Map.of("submissionId", submissionId),
            (row, n) -> new Revision(row.getLong("version"), row.getString("kind"), row.getString("command"),
                row.getString("actor_user_id"), row.getString("on_behalf_of_user_id"), row.getBoolean("deleted")));
    }

    /** The submission's state after the given revision. */
    public State stateAt(String submissionId, long version) {
        return jdbc.queryForObject("""
                SELECT form_data::text AS document, coalesce((metadata ->> 'deleted')::boolean, false) AS deleted,
                       metadata ->> 'assignment_uid' AS assignment, metadata ->> 'template_version_uid' AS form_version
                FROM journal.submission_revision
                WHERE submission_id = :submissionId AND version = :version""",
            new MapSqlParameterSource().addValue("submissionId", submissionId).addValue("version", version),
            (row, n) -> state(row));
    }

    /** The submission's state now, as stored. */
    public State current(String submissionId) {
        return jdbc.queryForObject("""
                SELECT form_data::text AS document, coalesce(deleted, false) AS deleted,
                       assignment_uid AS assignment, template_version_uid AS form_version
                FROM data_submission
                WHERE id = :submissionId""",
            Map.of("submissionId", submissionId),
            (row, n) -> state(row));
    }

    private static State state(java.sql.ResultSet row) throws java.sql.SQLException {
        return new State(row.getString("document"), row.getBoolean("deleted"), row.getString("assignment"),
            row.getString("form_version"));
    }
}
