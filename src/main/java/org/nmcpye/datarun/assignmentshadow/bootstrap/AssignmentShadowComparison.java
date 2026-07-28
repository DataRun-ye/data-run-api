package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrapReport.ItemCount;
import static org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrapReport.MismatchSample;

@Component
final class AssignmentShadowComparison {

    private static final String CREATE_BASELINE_TUPLES_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_baseline_tuple ON COMMIT DROP AS
        SELECT DISTINCT
            eligible.user_uid,
            eligible.activity_uid,
            eligible.org_unit_uid,
            form_uid.value AS form_uid
        FROM assignment_shadow_bootstrap_eligible eligible
        CROSS JOIN LATERAL jsonb_array_elements_text(eligible.form_uids) form_uid(value)
        WHERE eligible.lifecycle_state = 'ACTIVE'
        """;

    private static final String CREATE_SHADOW_TUPLES_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_shadow_tuple ON COMMIT DROP AS
        SELECT DISTINCT
            actor_alias.baseline_user_uid AS user_uid,
            role.activity_uid,
            org_unit_alias.baseline_org_unit_uid AS org_unit_uid,
            form_uid.value AS form_uid
        FROM assignment_access_projection access_projection
        JOIN actor_identity_link actor_alias
            ON actor_alias.actor_id = access_projection.target_actor_id
        JOIN org_unit_identity_link org_unit_alias
            ON org_unit_alias.org_unit_id = access_projection.org_unit_id
        JOIN assignment_role_definition role
            ON role.role_key = access_projection.role_key
        CROSS JOIN LATERAL jsonb_array_elements_text(role.form_uids) form_uid(value)
        """;

    private final JdbcTemplate jdbc;

    AssignmentShadowComparison(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    AssignmentShadowBootstrapReport compare(AssignmentShadowBootstrapMetrics metrics) {
        return compare(metrics, true);
    }

    AssignmentShadowBootstrapReport compareCurrent(AssignmentShadowBootstrapMetrics metrics) {
        requireCurrentStreamParity();
        return compare(metrics, false);
    }

    private void requireCurrentStreamParity() {
        long streamMismatches = count("""
            WITH desired AS (
                SELECT assignment_uid, user_uid, activity_uid, org_unit_uid, form_uids
                FROM assignment_shadow_bootstrap_desired
                WHERE lifecycle_state = 'ACTIVE'
            ), actual AS (
                SELECT
                    identity.baseline_assignment_uid AS assignment_uid,
                    actor.baseline_user_uid AS user_uid,
                    role.activity_uid,
                    org_unit.baseline_org_unit_uid AS org_unit_uid,
                    role.form_uids
                FROM assignment_identity_link identity
                JOIN assignment_grant_projection grant_projection
                  ON grant_projection.assignment_id = identity.assignment_id
                 AND grant_projection.lifecycle_state = 'ACTIVE'
                JOIN actor_identity_link actor
                  ON actor.actor_id = identity.target_actor_id
                JOIN org_unit_identity_link org_unit
                  ON org_unit.org_unit_id = grant_projection.org_unit_id
                JOIN assignment_role_definition role
                  ON role.role_key = grant_projection.role_key
            )
            SELECT count(*)
            FROM (
                SELECT *
                FROM (
                    SELECT * FROM desired
                    EXCEPT
                    SELECT * FROM actual
                ) desired_only
                UNION ALL
                SELECT *
                FROM (
                    SELECT * FROM actual
                    EXCEPT
                    SELECT * FROM desired
                ) actual_only
            ) mismatch
            """);
        long duplicateActiveStreams = count("""
            SELECT count(*)
            FROM (
                SELECT identity.baseline_assignment_uid, identity.target_actor_id
                FROM assignment_identity_link identity
                JOIN assignment_grant_projection grant_projection
                  ON grant_projection.assignment_id = identity.assignment_id
                 AND grant_projection.lifecycle_state = 'ACTIVE'
                GROUP BY identity.baseline_assignment_uid, identity.target_actor_id
                HAVING count(*) > 1
            ) duplicate_stream
            """);
        if (streamMismatches != 0 || duplicateActiveStreams != 0) {
            throw new AssignmentShadowBootstrapConflictException(
                "Current assignment stream comparison failed: mismatches=" + streamMismatches
                    + ", duplicateActiveStreams=" + duplicateActiveStreams
            );
        }
    }

    private AssignmentShadowBootstrapReport compare(
        AssignmentShadowBootstrapMetrics metrics,
        boolean verifyBootstrapRetiredGrants
    ) {
        jdbc.execute(CREATE_BASELINE_TUPLES_SQL);
        jdbc.execute(CREATE_SHADOW_TUPLES_SQL);

        long baselineTupleCount = count(
            "SELECT count(*) FROM assignment_shadow_bootstrap_baseline_tuple"
        );
        long shadowTupleCount = count(
            "SELECT count(*) FROM assignment_shadow_bootstrap_shadow_tuple"
        );
        long baselineOnlyCount = count("""
            SELECT count(*)
            FROM (
                SELECT user_uid, activity_uid, org_unit_uid, form_uid
                FROM assignment_shadow_bootstrap_baseline_tuple
                EXCEPT
                SELECT user_uid, activity_uid, org_unit_uid, form_uid
                FROM assignment_shadow_bootstrap_shadow_tuple
            ) difference
            """);
        long shadowOnlyCount = count("""
            SELECT count(*)
            FROM (
                SELECT user_uid, activity_uid, org_unit_uid, form_uid
                FROM assignment_shadow_bootstrap_shadow_tuple
                EXCEPT
                SELECT user_uid, activity_uid, org_unit_uid, form_uid
                FROM assignment_shadow_bootstrap_baseline_tuple
            ) difference
            """);
        List<MismatchSample> samples = mismatchSamples();
        long rawActiveGrants = count(
            "SELECT count(*) FROM assignment_grant_projection WHERE lifecycle_state = 'ACTIVE'"
        );
        long effectiveAccess = count("SELECT count(*) FROM assignment_access_projection");
        long retiredGrantMismatches = verifyBootstrapRetiredGrants ? count("""
            SELECT count(*)
            FROM assignment_shadow_bootstrap_desired desired
            LEFT JOIN assignment_grant_projection actual
              ON actual.assignment_id = desired.assignment_id
             AND actual.source_event_id = desired.event_id
             AND actual.role_key = desired.role_key
             AND actual.org_unit_id = desired.org_unit_id
             AND actual.lifecycle_state = 'ENDED'
            WHERE desired.lifecycle_state = 'ENDED'
              AND actual.assignment_id IS NULL
            """) : 0;

        return new AssignmentShadowBootstrapReport(
            baselineTupleCount,
            shadowTupleCount,
            baselineOnlyCount,
            shadowOnlyCount,
            samples,
            metrics.actorAliases,
            metrics.orgUnitAliases,
            new ItemCount(metrics.rolesCreated, metrics.rolesExisting),
            metrics.identities,
            metrics.events,
            metrics.activeGrants,
            metrics.endedGrants,
            metrics.retiredRows,
            metrics.disabledRows,
            metrics.noActorRows,
            metrics.emptyFormSetRows,
            metrics.nullScopeRows,
            metrics.malformedRows,
            rawActiveGrants,
            effectiveAccess,
            rawActiveGrants - effectiveAccess,
            retiredGrantMismatches
        );
    }

    private List<MismatchSample> mismatchSamples() {
        return jdbc.query(
            """
                WITH baseline_only AS (
                    SELECT user_uid, activity_uid, org_unit_uid, form_uid
                    FROM assignment_shadow_bootstrap_baseline_tuple
                    EXCEPT
                    SELECT user_uid, activity_uid, org_unit_uid, form_uid
                    FROM assignment_shadow_bootstrap_shadow_tuple
                ),
                shadow_only AS (
                    SELECT user_uid, activity_uid, org_unit_uid, form_uid
                    FROM assignment_shadow_bootstrap_shadow_tuple
                    EXCEPT
                    SELECT user_uid, activity_uid, org_unit_uid, form_uid
                    FROM assignment_shadow_bootstrap_baseline_tuple
                )
                SELECT side, user_uid, activity_uid, org_unit_uid, form_uid
                FROM (
                    SELECT 'BASELINE_ONLY' AS side, baseline_only.* FROM baseline_only
                    UNION ALL
                    SELECT 'SHADOW_ONLY' AS side, shadow_only.* FROM shadow_only
                ) difference
                ORDER BY side, user_uid, activity_uid, org_unit_uid, form_uid
                LIMIT 10
                """,
            (resultSet, rowNumber) -> new MismatchSample(
                resultSet.getString("side"),
                resultSet.getString("user_uid"),
                resultSet.getString("activity_uid"),
                resultSet.getString("org_unit_uid"),
                resultSet.getString("form_uid")
            )
        );
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return Objects.requireNonNull(value, "Count query returned null");
    }
}
