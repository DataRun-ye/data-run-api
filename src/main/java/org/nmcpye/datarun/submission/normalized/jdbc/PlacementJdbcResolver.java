package org.nmcpye.datarun.submission.normalized.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns the uids a stored submission names its placement by into the ids of the rows they name ({@link Placement}):
 * the one place that knows which stored values make up a placement and how they resolve. A revision's metadata stores
 * them as {@code assignment_uid}, {@code team_uid}, {@code org_unit_uid} and {@code activity_uid}
 * ({@code journal.submission_revision_metadata()}). It reads in the caller's transaction, through the template it is
 * given, and keeps each resolution for its own life, which is one read of the caller's.
 */
public final class PlacementJdbcResolver {

    private static final String RESOLVE = """
        SELECT (SELECT a.id FROM assignment a WHERE a.uid = :assignmentUid) AS assignment_id,
               (SELECT t.id FROM team t WHERE t.uid = :teamUid) AS team_id,
               (SELECT o.id FROM org_unit o WHERE o.uid = :orgUnitUid) AS org_unit_id,
               (SELECT c.id FROM activity c WHERE c.uid = :activityUid) AS activity_id""";

    /** The uids a placement is stored by. */
    private record StoredPlacement(String assignmentUid, String teamUid, String orgUnitUid, String activityUid) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final Map<StoredPlacement, Placement> resolved = new HashMap<>();

    PlacementJdbcResolver(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The placement a revision's metadata names; a uid that names nothing, or none stored, leaves its id missing. */
    Placement ofRevision(JsonNode metadata) {
        StoredPlacement stored = new StoredPlacement(text(metadata, "assignment_uid"), text(metadata, "team_uid"),
            text(metadata, "org_unit_uid"), text(metadata, "activity_uid"));
        return resolved.computeIfAbsent(stored, this::resolve);
    }

    private Placement resolve(StoredPlacement stored) {
        return jdbc.queryForObject(RESOLVE, new MapSqlParameterSource()
                .addValue("assignmentUid", stored.assignmentUid())
                .addValue("teamUid", stored.teamUid())
                .addValue("orgUnitUid", stored.orgUnitUid())
                .addValue("activityUid", stored.activityUid()),
            (rs, n) -> new Placement(rs.getString("assignment_id"), rs.getString("team_id"),
                rs.getString("org_unit_id"), rs.getString("activity_id")));
    }

    private static String text(JsonNode metadata, String key) {
        JsonNode value = metadata == null ? null : metadata.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }
}
