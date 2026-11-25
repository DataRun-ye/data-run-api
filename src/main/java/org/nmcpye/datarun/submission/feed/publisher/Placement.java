package org.nmcpye.datarun.submission.feed.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * A submission's placement as the feed publishes it (contract 2): the ids of the assignment, team, org unit and
 * activity that its head names by uid. The only place the publisher resolves placement, so that a change to the
 * placement columns (the placement model is under review, and the columns may change before the feed's first
 * publication) touches this class, the record's columns and their tests.
 *
 * @param assignmentId the assignment's id, or {@code null} when its uid names no assignment
 * @param teamId       the team's id, or {@code null}
 * @param orgUnitId    the org unit's id, or {@code null}
 * @param activityId   the activity's id, or {@code null}
 */
record Placement(String assignmentId, String teamId, String orgUnitId, String activityId) {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String RESOLVE = """
        SELECT (SELECT a.id FROM assignment a WHERE a.uid = :assignmentUid) AS assignment_id,
               (SELECT t.id FROM team t WHERE t.uid = :teamUid) AS team_id,
               (SELECT o.id FROM org_unit o WHERE o.uid = :orgUnitUid) AS org_unit_id,
               (SELECT c.id FROM activity c WHERE c.uid = :activityUid) AS activity_id""";

    /**
     * The placement a head names: the uids its revision's metadata stores
     * ({@code journal.submission_revision_metadata()}), each resolved to its row's id in the caller's transaction.
     *
     * @param metadata the head's metadata, as JSON text
     */
    static Placement of(NamedParameterJdbcTemplate jdbc, String metadata) {
        JsonNode stored;
        try {
            stored = metadata == null ? JSON.createObjectNode() : JSON.readTree(metadata);
        } catch (JsonProcessingException e) {
            // the column is jsonb, which always prints as JSON
            throw new IllegalStateException("a revision's metadata is not JSON", e);
        }
        return jdbc.queryForObject(RESOLVE, new MapSqlParameterSource()
                .addValue("assignmentUid", text(stored, "assignment_uid"))
                .addValue("teamUid", text(stored, "team_uid"))
                .addValue("orgUnitUid", text(stored, "org_unit_uid"))
                .addValue("activityUid", text(stored, "activity_uid")),
            (rs, n) -> new Placement(rs.getString("assignment_id"), rs.getString("team_id"),
                rs.getString("org_unit_id"), rs.getString("activity_id")));
    }

    private static String text(JsonNode stored, String key) {
        JsonNode value = stored.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Whether every uid named a row: the contract's placement columns are required. */
    boolean complete() {
        return assignmentId != null && teamId != null && orgUnitId != null && activityId != null;
    }
}
