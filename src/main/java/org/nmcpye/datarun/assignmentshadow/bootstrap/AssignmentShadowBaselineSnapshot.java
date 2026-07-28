package org.nmcpye.datarun.assignmentshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.assignmentshadow.AssignmentRoleDefinition;
import org.nmcpye.datarun.assignmentshadow.AssignmentRoleDefinitionPort;
import org.nmcpye.datarun.assignmentshadow.AssignmentShadowIdentities;
import org.nmcpye.datarun.assignmentshadow.CanonicalCaptureFormResolver;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
final class AssignmentShadowBaselineSnapshot {

    private static final String CREATE_SOURCE_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_source ON COMMIT DROP AS
        SELECT
            row_number() OVER (
                ORDER BY assignment.uid NULLS FIRST,
                    actor.user_uid NULLS FIRST,
                    assignment.id,
                    actor.user_db_id
            ) AS source_order,
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
        LEFT JOIN activity assignment_activity
            ON assignment_activity.id = assignment.activity_id
        LEFT JOIN team
            ON team.id = assignment.team_id
        LEFT JOIN activity team_activity
            ON team_activity.id = team.activity_id
        LEFT JOIN org_unit
            ON org_unit.id = assignment.org_unit_id
        LEFT JOIN (
            SELECT
                team_user.team_id,
                app_user.id AS user_db_id,
                app_user.uid AS user_uid
            FROM team_user
            JOIN app_user
                ON app_user.id = team_user.user_id
        ) actor
            ON actor.team_id = team.id
        """;

    private static final String CREATE_ELIGIBLE_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_eligible (
            eligible_order BIGSERIAL PRIMARY KEY,
            assignment_uid VARCHAR(11) NOT NULL,
            user_uid VARCHAR(11) NOT NULL,
            activity_uid VARCHAR(11) NOT NULL,
            org_unit_uid VARCHAR(11) NOT NULL,
            lifecycle_state VARCHAR(16) NOT NULL,
            form_uids JSONB NOT NULL,
            actor_id UUID NOT NULL,
            org_unit_id UUID NOT NULL,
            assignment_id UUID NOT NULL,
            event_id UUID NOT NULL,
            UNIQUE (assignment_uid, user_uid)
        ) ON COMMIT DROP
        """;

    private static final String SOURCE_PAGE_SQL = """
        SELECT *
        FROM assignment_shadow_bootstrap_source
        WHERE source_order > ?
        ORDER BY source_order
        LIMIT ?
        """;

    private static final String INSERT_ELIGIBLE_SQL = """
        INSERT INTO assignment_shadow_bootstrap_eligible (
            assignment_uid,
            user_uid,
            activity_uid,
            org_unit_uid,
            lifecycle_state,
            form_uids,
            actor_id,
            org_unit_id,
            assignment_id,
            event_id
        ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
        """;

    private static final String CREATE_ROLE_CANDIDATES_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_role_candidate ON COMMIT DROP AS
        SELECT
            row_number() OVER (ORDER BY activity_uid, form_uids::text) AS candidate_order,
            activity_uid,
            form_uids
        FROM (
            SELECT DISTINCT activity_uid, form_uids
            FROM assignment_shadow_bootstrap_eligible
        ) candidate
        """;

    private static final String CREATE_RESOLVED_ROLES_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_role (
            activity_uid VARCHAR(11) NOT NULL,
            form_uids JSONB NOT NULL,
            role_key VARCHAR(128) NOT NULL,
            PRIMARY KEY (activity_uid, form_uids)
        ) ON COMMIT DROP
        """;

    private static final String CREATE_DESIRED_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_desired ON COMMIT DROP AS
        SELECT
            row_number() OVER (
                ORDER BY eligible.assignment_uid, eligible.user_uid
            ) AS desired_order,
            eligible.*,
            role.role_key
        FROM assignment_shadow_bootstrap_eligible eligible
        JOIN assignment_shadow_bootstrap_role role
            ON role.activity_uid = eligible.activity_uid
           AND role.form_uids = eligible.form_uids
        """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AssignmentRoleDefinitionPort roleDefinitions;
    private final CanonicalCaptureFormResolver captureForms;

    AssignmentShadowBaselineSnapshot(
        JdbcTemplate jdbc,
        ObjectMapper objectMapper,
        AssignmentRoleDefinitionPort roleDefinitions,
        CanonicalCaptureFormResolver captureForms
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.roleDefinitions = roleDefinitions;
        this.captureForms = captureForms;
    }

    void stage(AssignmentShadowBootstrapMetrics metrics) {
        jdbc.execute(CREATE_SOURCE_SQL);
        jdbc.execute(
            "ALTER TABLE assignment_shadow_bootstrap_source ADD PRIMARY KEY (source_order)"
        );
        jdbc.execute(CREATE_ELIGIBLE_SQL);
        stageEligibleRows(metrics);
        failOnInvalidBaseline(metrics);
        jdbc.execute(CREATE_ROLE_CANDIDATES_SQL);
        jdbc.execute(
            "ALTER TABLE assignment_shadow_bootstrap_role_candidate "
                + "ADD PRIMARY KEY (candidate_order)"
        );
        jdbc.execute(CREATE_RESOLVED_ROLES_SQL);
        resolveRoles(metrics);
        jdbc.execute(CREATE_DESIRED_SQL);
        jdbc.execute(
            "ALTER TABLE assignment_shadow_bootstrap_desired ADD PRIMARY KEY (desired_order)"
        );
    }

    private void stageEligibleRows(AssignmentShadowBootstrapMetrics metrics) {
        long lastSourceOrder = 0;
        while (true) {
            List<SourceRow> sourceRows = jdbc.query(
                SOURCE_PAGE_SQL,
                this::mapSourceRow,
                lastSourceOrder,
                AssignmentShadowBootstrap.BATCH_SIZE
            );
            if (sourceRows.isEmpty()) {
                return;
            }

            List<Object[]> eligibleRows = new ArrayList<>(sourceRows.size());
            for (SourceRow source : sourceRows) {
                stageSourceRow(source, eligibleRows, metrics);
            }
            if (!eligibleRows.isEmpty()) {
                jdbc.batchUpdate(INSERT_ELIGIBLE_SQL, eligibleRows);
            }
            lastSourceOrder = sourceRows.get(sourceRows.size() - 1).sourceOrder();
        }
    }

    private void stageSourceRow(
        SourceRow source,
        List<Object[]> eligibleRows,
        AssignmentShadowBootstrapMetrics metrics
    ) {
        List<String> malformedReasons = new ArrayList<>();
        requireUid("assignment", source.assignmentUid(), malformedReasons);
        requireUid("activity", source.activityUid(), malformedReasons);
        requireUid("team", source.teamUid(), malformedReasons);
        if (source.userDbId() != null) {
            requireUid("user", source.userUid(), malformedReasons);
        }

        List<String> captureFormUids = List.of();
        try {
            captureFormUids = canonicalCaptureForms(source.assignmentForms(), source.formPermissions());
        } catch (IllegalArgumentException exception) {
            malformedReasons.add(exception.getMessage());
        }

        if (!malformedReasons.isEmpty()) {
            metrics.malformedRows++;
            metrics.addValidationSample(
                source.assignmentDbId(),
                source.userDbId(),
                String.join(", ", malformedReasons)
            );
            return;
        }

        if (!Boolean.FALSE.equals(source.teamDisabled())
            || !Boolean.FALSE.equals(source.activityDisabled())
            || !Boolean.FALSE.equals(source.teamActivityDisabled())) {
            metrics.disabledRows++;
            return;
        }
        if (source.userDbId() == null) {
            metrics.noActorRows++;
            return;
        }
        if (captureFormUids.isEmpty()) {
            metrics.emptyFormSetRows++;
            return;
        }
        if (source.orgUnitUid() == null) {
            metrics.nullScopeRows++;
            metrics.addValidationSample(
                source.assignmentDbId(),
                source.userDbId(),
                "null organization-unit scope"
            );
            return;
        }
        requireUid("organization-unit", source.orgUnitUid(), malformedReasons);
        if (!malformedReasons.isEmpty()) {
            metrics.malformedRows++;
            metrics.addValidationSample(
                source.assignmentDbId(),
                source.userDbId(),
                String.join(", ", malformedReasons)
            );
            return;
        }

        String lifecycleState = Boolean.TRUE.equals(source.deleted()) ? "ENDED" : "ACTIVE";
        if ("ENDED".equals(lifecycleState)) {
            metrics.retiredRows++;
        }

        UUID actorId = AssignmentShadowIdentities.actorId(source.userUid());
        UUID orgUnitId = AssignmentShadowIdentities.orgUnitId(source.orgUnitUid());
        UUID assignmentId = AssignmentShadowIdentities.assignmentId(
            source.assignmentUid(), source.userUid(), 0
        );
        UUID eventId = AssignmentShadowIdentities.namespacedUuid(
            "datarun-baseline/event/assignment-observed/" + assignmentId
        );
        eligibleRows.add(new Object[]{
            source.assignmentUid(),
            source.userUid(),
            source.activityUid(),
            source.orgUnitUid(),
            lifecycleState,
            writeJson(captureFormUids),
            actorId,
            orgUnitId,
            assignmentId,
            eventId
        });
    }

    private void failOnInvalidBaseline(AssignmentShadowBootstrapMetrics metrics) {
        if (metrics.nullScopeRows == 0 && metrics.malformedRows == 0) {
            return;
        }
        throw new AssignmentShadowBootstrapConflictException(
            "Baseline validation failed: nullScopeRows=" + metrics.nullScopeRows
                + ", malformedRows=" + metrics.malformedRows
                + ", samples=" + metrics.validationSamples
        );
    }

    private List<String> canonicalCaptureForms(String assignmentForms, String formPermissions) {
        return captureForms.resolve(assignmentForms, formPermissions);
    }

    private void requireUid(String fieldName, String value, List<String> errors) {
        if (!CodeGenerator.isValidUid(value)) {
            errors.add(fieldName + " UID is invalid");
        }
    }

    private void resolveRoles(AssignmentShadowBootstrapMetrics metrics) {
        long lastCandidate = 0;
        while (true) {
            List<RoleCandidate> candidates = jdbc.query(
                """
                    SELECT candidate_order, activity_uid, form_uids::text
                    FROM assignment_shadow_bootstrap_role_candidate
                    WHERE candidate_order > ?
                    ORDER BY candidate_order
                    LIMIT ?
                    """,
                (resultSet, rowNumber) -> new RoleCandidate(
                    resultSet.getLong("candidate_order"),
                    resultSet.getString("activity_uid"),
                    readFormUidList(resultSet.getString("form_uids"))
                ),
                lastCandidate,
                AssignmentShadowBootstrap.BATCH_SIZE
            );
            if (candidates.isEmpty()) {
                return;
            }
            for (RoleCandidate candidate : candidates) {
                String formUids = writeJson(candidate.formUids());
                Boolean existed = jdbc.queryForObject(
                    """
                        SELECT EXISTS (
                            SELECT 1
                            FROM assignment_role_definition
                            WHERE activity_uid = ?
                              AND form_uids = CAST(? AS jsonb)
                        )
                        """,
                    Boolean.class,
                    candidate.activityUid(),
                    formUids
                );
                AssignmentRoleDefinition role;
                try {
                    role = roleDefinitions.resolveOrInsert(candidate.activityUid(), candidate.formUids());
                } catch (PessimisticLockingFailureException exception) {
                    throw exception;
                } catch (DataAccessException | IllegalArgumentException exception) {
                    throw new AssignmentShadowBootstrapConflictException(
                        "Role resolution conflicted for activity " + candidate.activityUid(),
                        exception
                    );
                }
                if (!role.activityUid().equals(candidate.activityUid())
                    || !role.formUids().equals(candidate.formUids())) {
                    throw new AssignmentShadowBootstrapConflictException(
                        "Role owner returned noncanonical content for " + role.roleKey()
                    );
                }
                jdbc.update(
                    """
                        INSERT INTO assignment_shadow_bootstrap_role (
                            activity_uid,
                            form_uids,
                            role_key
                        ) VALUES (?, CAST(? AS jsonb), ?)
                        """,
                    candidate.activityUid(),
                    formUids,
                    role.roleKey()
                );
                if (Boolean.TRUE.equals(existed)) {
                    metrics.rolesExisting++;
                } else {
                    metrics.rolesCreated++;
                }
            }
            lastCandidate = candidates.get(candidates.size() - 1).candidateOrder();
        }
    }

    private SourceRow mapSourceRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SourceRow(
            resultSet.getLong("source_order"),
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

    private List<String> readFormUidList(String value) {
        return captureForms.readCanonicalFormUidList(value, "staged role form_uids");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new AssignmentShadowBootstrapConflictException(
                "Canonical bootstrap JSON could not be serialized",
                exception
            );
        }
    }

    private record SourceRow(
        long sourceOrder,
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

    private record RoleCandidate(long candidateOrder, String activityUid, List<String> formUids) {
    }
}
