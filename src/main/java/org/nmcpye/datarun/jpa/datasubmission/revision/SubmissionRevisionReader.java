package org.nmcpye.datarun.jpa.datasubmission.revision;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.submission.normalized.StoredJson;
import org.nmcpye.datarun.submission.normalized.jdbc.PlacementJdbcResolver;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Reads the journal for conflict detection: which revisions a submission has, and the state the comparison reads at
 * one of them. It keys by ids only: the submission's, the users', and the assignment's, which the one placement
 * resolver ({@link PlacementJdbcResolver}) finds for the placement a revision stores. Stored documents come back as the
 * database prints them ({@code form_data::text}), so a reader can parse their numbers exactly. Called under the
 * submission lock, in the upload item's transaction, so the submission cannot change while it reads.
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
     * @param document     {@code form_data} as text, or null when the state holds no document
     * @param deleted      whether the submission is deleted
     * @param assignmentId the assignment's id, or null when the stored placement names none
     * @param status       the progress status, or null when there is none
     */
    public record State(String document, boolean deleted, String assignmentId, String status) {
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

    /** The submission's state after the given revision, its assignment by id. */
    public State stateAt(String submissionId, long version) {
        PlacementJdbcResolver placements = new PlacementJdbcResolver(jdbc);
        return jdbc.queryForObject("""
                SELECT form_data::text AS document, coalesce((metadata ->> 'deleted')::boolean, false) AS deleted,
                       metadata ->> 'status' AS status, metadata::text AS metadata
                FROM journal.submission_revision
                WHERE submission_id = :submissionId AND version = :version""",
            new MapSqlParameterSource().addValue("submissionId", submissionId).addValue("version", version),
            (row, n) -> new State(row.getString("document"), row.getBoolean("deleted"),
                placements.ofRevision(metadata(row.getString("metadata"))).assignmentId(), row.getString("status")));
    }

    private static com.fasterxml.jackson.databind.JsonNode metadata(String text) {
        try {
            return text == null ? null : StoredJson.parse(text);
        } catch (JsonProcessingException e) {
            // StoredJson's message names only the kind of failure, never the text
            throw new IllegalStateException("A revision's metadata is not readable JSON: " + e.getOriginalMessage());
        }
    }
}
