package org.nmcpye.datarun.assignmentshadow;

import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class AssignmentAuthoritySnapshot {

    private static final String BASELINE_SQL = """
        SELECT
            assignment.id AS assignment_db_id,
            assignment.uid AS assignment_uid,
            assignment.deleted,
            assignment.forms::text AS assignment_forms,
            assignment_activity.uid AS activity_uid,
            assignment_activity.disabled AS activity_disabled,
            team.uid AS team_uid,
            team.disabled AS team_disabled,
            team.form_permissions::text AS form_permissions,
            team_activity.disabled AS team_activity_disabled,
            org_unit.uid AS org_unit_uid,
            actor.user_db_id,
            actor.user_uid
        FROM assignment
        LEFT JOIN activity assignment_activity ON assignment_activity.id = assignment.activity_id
        LEFT JOIN team ON team.id = assignment.team_id
        LEFT JOIN activity team_activity ON team_activity.id = team.activity_id
        LEFT JOIN org_unit ON org_unit.id = assignment.org_unit_id
        LEFT JOIN (
            SELECT team_user.team_id, app_user.id AS user_db_id, app_user.uid AS user_uid
            FROM team_user
            JOIN app_user ON app_user.id = team_user.user_id
        ) actor ON actor.team_id = team.id
        WHERE assignment.uid IN (:assignmentUids)
        ORDER BY assignment.uid, actor.user_uid NULLS FIRST
        """;

    private static final String SHADOW_SQL = """
        SELECT
            identity.baseline_assignment_uid AS assignment_uid,
            actor.baseline_user_uid AS user_uid,
            role.activity_uid,
            org_unit.baseline_org_unit_uid AS org_unit_uid,
            role.form_uids::text AS form_uids
        FROM assignment_identity_link identity
        JOIN assignment_grant_projection grant_projection
          ON grant_projection.assignment_id = identity.assignment_id
         AND grant_projection.lifecycle_state = 'ACTIVE'
        JOIN actor_identity_link actor ON actor.actor_id = identity.target_actor_id
        JOIN org_unit_identity_link org_unit ON org_unit.org_unit_id = grant_projection.org_unit_id
        JOIN assignment_role_definition role ON role.role_key = grant_projection.role_key
        WHERE identity.baseline_assignment_uid IN (:assignmentUids)
        ORDER BY identity.baseline_assignment_uid, actor.baseline_user_uid
        """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CanonicalCaptureFormResolver captureForms;

    public AssignmentAuthoritySnapshot(
        NamedParameterJdbcTemplate jdbc,
        CanonicalCaptureFormResolver captureForms
    ) {
        this.jdbc = jdbc;
        this.captureForms = captureForms;
    }

    public Set<String> assignmentUidsForTeam(String teamUid) {
        if (teamUid == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(jdbc.queryForList(
            "SELECT uid FROM assignment WHERE team_id = (SELECT id FROM team WHERE uid = :uid)",
            new MapSqlParameterSource("uid", teamUid),
            String.class
        ));
    }

    public Set<String> assignmentUidsForActivity(String activityUid) {
        if (activityUid == null) {
            return Set.of();
        }
        return new LinkedHashSet<>(jdbc.queryForList(
            """
                SELECT DISTINCT assignment.uid
                FROM assignment
                LEFT JOIN team ON team.id = assignment.team_id
                LEFT JOIN activity assignment_activity
                  ON assignment_activity.id = assignment.activity_id
                LEFT JOIN activity team_activity ON team_activity.id = team.activity_id
                WHERE assignment_activity.uid = :uid OR team_activity.uid = :uid
                """,
            new MapSqlParameterSource("uid", activityUid),
            String.class
        ));
    }

    public Map<IntentKey, CaptureIntent> baseline(Collection<String> assignmentUids) {
        if (assignmentUids.isEmpty()) {
            return Map.of();
        }
        Map<IntentKey, CaptureIntent> intents = new LinkedHashMap<>();
        List<SourceRow> rows = jdbc.query(
            BASELINE_SQL,
            new MapSqlParameterSource("assignmentUids", assignmentUids),
            this::mapSourceRow
        );
        for (SourceRow row : rows) {
            CaptureIntent intent = toIntent(row);
            if (intent != null) {
                IntentKey key = new IntentKey(row.assignmentUid(), row.userUid());
                if (intents.putIfAbsent(key, intent) != null) {
                    throw new AssignmentAuthorityConflictException(
                        "Duplicate baseline assignment actor stream " + key
                    );
                }
            }
        }
        return Map.copyOf(intents);
    }

    public Map<IntentKey, CaptureIntent> activeShadow(Collection<String> assignmentUids) {
        if (assignmentUids.isEmpty()) {
            return Map.of();
        }
        Map<IntentKey, CaptureIntent> intents = new LinkedHashMap<>();
        jdbc.query(
            SHADOW_SQL,
            new MapSqlParameterSource("assignmentUids", assignmentUids),
            resultSet -> {
                IntentKey key = new IntentKey(
                    resultSet.getString("assignment_uid"),
                    resultSet.getString("user_uid")
                );
                CaptureIntent intent = new CaptureIntent(
                    resultSet.getString("activity_uid"),
                    resultSet.getString("org_unit_uid"),
                    captureForms.readCanonicalFormUidList(
                        resultSet.getString("form_uids"),
                        "assignment_role_definition.form_uids"
                    )
                );
                if (intents.putIfAbsent(key, intent) != null) {
                    throw new AssignmentAuthorityConflictException(
                        "Multiple active shadow generations for " + key
                    );
                }
            }
        );
        return Map.copyOf(intents);
    }

    public void requireParity(
        Collection<String> assignmentUids,
        Map<IntentKey, CaptureIntent> baseline
    ) {
        Map<IntentKey, CaptureIntent> shadow = activeShadow(assignmentUids);
        if (!baseline.equals(shadow)) {
            throw new AssignmentAuthorityConflictException(
                "Assignment authority baseline/shadow mismatch for assignments " + assignmentUids
                    + "; baseline=" + baseline + "; shadow=" + shadow
            );
        }
    }

    private CaptureIntent toIntent(SourceRow row) {
        requireUid("assignment", row.assignmentUid());
        requireUid("activity", row.activityUid());
        requireUid("team", row.teamUid());
        if (row.userDbId() != null) {
            requireUid("user", row.userUid());
        }

        List<String> forms;
        try {
            forms = captureForms.resolve(row.assignmentForms(), row.formPermissions());
        } catch (IllegalArgumentException exception) {
            throw conflict(row, exception.getMessage(), exception);
        }

        if (!Boolean.FALSE.equals(row.teamDisabled())
            || !Boolean.FALSE.equals(row.activityDisabled())
            || !Boolean.FALSE.equals(row.teamActivityDisabled())
            || row.userDbId() == null
            || forms.isEmpty()) {
            return null;
        }

        requireUid("organization-unit", row.orgUnitUid());
        if (Boolean.TRUE.equals(row.deleted())) {
            return null;
        }
        return new CaptureIntent(row.activityUid(), row.orgUnitUid(), forms);
    }

    private void requireUid(String owner, String uid) {
        if (!CodeGenerator.isValidUid(uid)) {
            throw new AssignmentAuthorityConflictException(owner + " UID is invalid");
        }
    }

    private AssignmentAuthorityConflictException conflict(
        SourceRow row,
        String message,
        Throwable cause
    ) {
        return new AssignmentAuthorityConflictException(
            "Invalid assignment authority row " + row.assignmentDbId() + "/"
                + Objects.toString(row.userDbId(), "<no-actor>") + ": " + message,
            cause
        );
    }

    private SourceRow mapSourceRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SourceRow(
            resultSet.getString("assignment_db_id"),
            resultSet.getString("assignment_uid"),
            resultSet.getObject("deleted", Boolean.class),
            resultSet.getString("assignment_forms"),
            resultSet.getString("activity_uid"),
            resultSet.getObject("activity_disabled", Boolean.class),
            resultSet.getString("team_uid"),
            resultSet.getObject("team_disabled", Boolean.class),
            resultSet.getString("form_permissions"),
            resultSet.getObject("team_activity_disabled", Boolean.class),
            resultSet.getString("org_unit_uid"),
            resultSet.getString("user_db_id"),
            resultSet.getString("user_uid")
        );
    }

    public record IntentKey(String assignmentUid, String userUid) {
    }

    public record CaptureIntent(String activityUid, String orgUnitUid, List<String> formUids) {
        public CaptureIntent {
            formUids = List.copyOf(formUids);
        }
    }

    private record SourceRow(
        String assignmentDbId,
        String assignmentUid,
        Boolean deleted,
        String assignmentForms,
        String activityUid,
        Boolean activityDisabled,
        String teamUid,
        Boolean teamDisabled,
        String formPermissions,
        Boolean teamActivityDisabled,
        String orgUnitUid,
        String userDbId,
        String userUid
    ) {
    }
}
