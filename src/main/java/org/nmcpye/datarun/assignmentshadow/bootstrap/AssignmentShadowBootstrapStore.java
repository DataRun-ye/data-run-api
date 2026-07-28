package org.nmcpye.datarun.assignmentshadow.bootstrap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrapReport.ItemCount;

@Component
final class AssignmentShadowBootstrapStore {

    private static final String EVENT_TYPE = "assignment_changed";
    private static final String SHAPE_REF = "baseline_assignment_observed/v1";
    private static final String SUBJECT_TYPE = "assignment";
    private static final String SYSTEM_ACTOR =
        "system:migration/datarun-baseline-assignment-bootstrap";

    private static final String CREATE_ACTOR_CANDIDATES_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_actor ON COMMIT DROP AS
        SELECT
            row_number() OVER (ORDER BY user_uid) AS candidate_order,
            actor_id,
            user_uid
        FROM (
            SELECT DISTINCT actor_id, user_uid
            FROM assignment_shadow_bootstrap_desired
        ) candidate
        """;

    private static final String CREATE_ORG_UNIT_CANDIDATES_SQL = """
        CREATE TEMP TABLE assignment_shadow_bootstrap_org_unit ON COMMIT DROP AS
        SELECT
            row_number() OVER (ORDER BY org_unit_uid) AS candidate_order,
            org_unit_id,
            org_unit_uid
        FROM (
            SELECT DISTINCT org_unit_id, org_unit_uid
            FROM assignment_shadow_bootstrap_desired
        ) candidate
        """;

    private static final String INSERT_ACTORS_SQL = """
        INSERT INTO actor_identity_link (actor_id, baseline_user_uid)
        SELECT actor_id, user_uid
        FROM assignment_shadow_bootstrap_actor
        WHERE candidate_order > ? AND candidate_order <= ?
        ORDER BY candidate_order
        ON CONFLICT DO NOTHING
        """;

    private static final String INSERT_ORG_UNITS_SQL = """
        INSERT INTO org_unit_identity_link (org_unit_id, baseline_org_unit_uid)
        SELECT org_unit_id, org_unit_uid
        FROM assignment_shadow_bootstrap_org_unit
        WHERE candidate_order > ? AND candidate_order <= ?
        ORDER BY candidate_order
        ON CONFLICT DO NOTHING
        """;

    private static final String INSERT_IDENTITIES_SQL = """
        INSERT INTO assignment_identity_link (
            assignment_id,
            baseline_assignment_uid,
            target_actor_id,
            generation
        )
        SELECT assignment_id, assignment_uid, actor_id, 0
        FROM assignment_shadow_bootstrap_desired
        WHERE desired_order > ? AND desired_order <= ?
        ORDER BY desired_order
        ON CONFLICT DO NOTHING
        """;

    private static final String INSERT_EVENTS_SQL = """
        INSERT INTO event_journal (
            event_id,
            event_type,
            shape_ref,
            activity_ref,
            subject_type,
            subject_id,
            actor_id,
            recorded_at,
            payload
        )
        SELECT
            event_id,
            'assignment_changed',
            'baseline_assignment_observed/v1',
            activity_uid,
            'assignment',
            assignment_id,
            'system:migration/datarun-baseline-assignment-bootstrap',
            ?,
            jsonb_build_object(
                'role', role_key,
                'org_unit_id', org_unit_id::text,
                'lifecycle_state', lifecycle_state
            )
        FROM assignment_shadow_bootstrap_desired
        WHERE desired_order > ? AND desired_order <= ?
        ORDER BY desired_order
        ON CONFLICT (event_id) DO NOTHING
        """;

    private static final String INSERT_GRANTS_SQL = """
        INSERT INTO assignment_grant_projection (
            assignment_id,
            source_event_id,
            role_key,
            org_unit_id,
            lifecycle_state
        )
        SELECT assignment_id, event_id, role_key, org_unit_id, lifecycle_state
        FROM assignment_shadow_bootstrap_desired
        WHERE desired_order > ? AND desired_order <= ?
        ORDER BY desired_order
        ON CONFLICT DO NOTHING
        """;

    private final JdbcTemplate jdbc;

    AssignmentShadowBootstrapStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void persistAndVerify(AssignmentShadowBootstrapMetrics metrics, Instant recordedAt) {
        jdbc.execute(CREATE_ACTOR_CANDIDATES_SQL);
        jdbc.execute(
            "ALTER TABLE assignment_shadow_bootstrap_actor ADD PRIMARY KEY (candidate_order)"
        );
        jdbc.execute(CREATE_ORG_UNIT_CANDIDATES_SQL);
        jdbc.execute(
            "ALTER TABLE assignment_shadow_bootstrap_org_unit ADD PRIMARY KEY (candidate_order)"
        );

        metrics.actorAliases = existingAndCreated(
            "assignment_shadow_bootstrap_actor",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_actor candidate
                JOIN actor_identity_link actual
                  ON actual.actor_id = candidate.actor_id
                 AND actual.baseline_user_uid = candidate.user_uid
                """
        );
        metrics.orgUnitAliases = existingAndCreated(
            "assignment_shadow_bootstrap_org_unit",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_org_unit candidate
                JOIN org_unit_identity_link actual
                  ON actual.org_unit_id = candidate.org_unit_id
                 AND actual.baseline_org_unit_uid = candidate.org_unit_uid
                """
        );
        metrics.identities = existingAndCreated(
            "assignment_shadow_bootstrap_desired",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_desired desired
                JOIN assignment_identity_link actual
                  ON actual.assignment_id = desired.assignment_id
                 AND actual.baseline_assignment_uid = desired.assignment_uid
                 AND actual.target_actor_id = desired.actor_id
                 AND actual.generation = 0
                """
        );
        metrics.events = existingAndCreated(
            "assignment_shadow_bootstrap_desired",
            exactEventCountSql()
        );
        metrics.activeGrants = existingAndCreatedGrants("ACTIVE");
        metrics.endedGrants = existingAndCreatedGrants("ENDED");

        batchInsert("assignment_shadow_bootstrap_actor", "candidate_order", INSERT_ACTORS_SQL);
        verifyActorAliases();
        batchInsert(
            "assignment_shadow_bootstrap_org_unit",
            "candidate_order",
            INSERT_ORG_UNITS_SQL
        );
        verifyOrgUnitAliases();
        batchInsert("assignment_shadow_bootstrap_desired", "desired_order", INSERT_IDENTITIES_SQL);
        verifyIdentities();
        batchInsertEvents(recordedAt);
        verifyEvents();
        batchInsert("assignment_shadow_bootstrap_desired", "desired_order", INSERT_GRANTS_SQL);
        verifyGrants();
    }

    private ItemCount existingAndCreated(String tableName, String exactExistingSql) {
        long total = count("SELECT count(*) FROM " + tableName);
        long existing = count(exactExistingSql);
        return new ItemCount(total - existing, existing);
    }

    private ItemCount existingAndCreatedGrants(String lifecycleState) {
        long total = count(
            "SELECT count(*) FROM assignment_shadow_bootstrap_desired WHERE lifecycle_state = ?",
            lifecycleState
        );
        long existing = count(
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_desired desired
                JOIN assignment_grant_projection actual
                  ON actual.assignment_id = desired.assignment_id
                 AND actual.source_event_id = desired.event_id
                 AND actual.role_key = desired.role_key
                 AND actual.org_unit_id = desired.org_unit_id
                 AND actual.lifecycle_state = desired.lifecycle_state
                WHERE desired.lifecycle_state = ?
                """,
            lifecycleState
        );
        return new ItemCount(total - existing, existing);
    }

    private void batchInsert(String tableName, String orderColumn, String insertSql) {
        long maximum = count("SELECT COALESCE(max(" + orderColumn + "), 0) FROM " + tableName);
        for (
            long lowerBound = 0;
            lowerBound < maximum;
            lowerBound += AssignmentShadowBootstrap.BATCH_SIZE
        ) {
            jdbc.update(insertSql, lowerBound, lowerBound + AssignmentShadowBootstrap.BATCH_SIZE);
        }
    }

    private void batchInsertEvents(Instant recordedAt) {
        long maximum = count(
            "SELECT COALESCE(max(desired_order), 0) FROM assignment_shadow_bootstrap_desired"
        );
        for (
            long lowerBound = 0;
            lowerBound < maximum;
            lowerBound += AssignmentShadowBootstrap.BATCH_SIZE
        ) {
            jdbc.update(
                INSERT_EVENTS_SQL,
                Timestamp.from(recordedAt),
                lowerBound,
                lowerBound + AssignmentShadowBootstrap.BATCH_SIZE
            );
        }
    }

    private void verifyActorAliases() {
        verifyExact(
            "actor alias",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_actor candidate
                LEFT JOIN actor_identity_link actual
                  ON actual.actor_id = candidate.actor_id
                 AND actual.baseline_user_uid = candidate.user_uid
                WHERE actual.actor_id IS NULL
                """,
            """
                SELECT candidate.user_uid
                FROM assignment_shadow_bootstrap_actor candidate
                LEFT JOIN actor_identity_link actual
                  ON actual.actor_id = candidate.actor_id
                 AND actual.baseline_user_uid = candidate.user_uid
                WHERE actual.actor_id IS NULL
                ORDER BY candidate.user_uid
                LIMIT 10
                """
        );
    }

    private void verifyOrgUnitAliases() {
        verifyExact(
            "organization-unit alias",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_org_unit candidate
                LEFT JOIN org_unit_identity_link actual
                  ON actual.org_unit_id = candidate.org_unit_id
                 AND actual.baseline_org_unit_uid = candidate.org_unit_uid
                WHERE actual.org_unit_id IS NULL
                """,
            """
                SELECT candidate.org_unit_uid
                FROM assignment_shadow_bootstrap_org_unit candidate
                LEFT JOIN org_unit_identity_link actual
                  ON actual.org_unit_id = candidate.org_unit_id
                 AND actual.baseline_org_unit_uid = candidate.org_unit_uid
                WHERE actual.org_unit_id IS NULL
                ORDER BY candidate.org_unit_uid
                LIMIT 10
                """
        );
    }

    private void verifyIdentities() {
        verifyExact(
            "assignment identity",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_desired desired
                LEFT JOIN assignment_identity_link actual
                  ON actual.assignment_id = desired.assignment_id
                 AND actual.baseline_assignment_uid = desired.assignment_uid
                 AND actual.target_actor_id = desired.actor_id
                 AND actual.generation = 0
                WHERE actual.assignment_id IS NULL
                """,
            """
                SELECT desired.assignment_uid || '/' || desired.user_uid
                FROM assignment_shadow_bootstrap_desired desired
                LEFT JOIN assignment_identity_link actual
                  ON actual.assignment_id = desired.assignment_id
                 AND actual.baseline_assignment_uid = desired.assignment_uid
                 AND actual.target_actor_id = desired.actor_id
                 AND actual.generation = 0
                WHERE actual.assignment_id IS NULL
                ORDER BY desired.assignment_uid, desired.user_uid
                LIMIT 10
                """
        );
    }

    private void verifyEvents() {
        verifyExact(
            "bootstrap event",
            "SELECT count(*) FROM (" + missingEventSql() + ") conflicts",
            "SELECT assignment_uid || '/' || user_uid FROM (" + missingEventSql()
                + ") conflicts ORDER BY assignment_uid, user_uid LIMIT 10"
        );
    }

    private void verifyGrants() {
        verifyExact(
            "assignment grant",
            """
                SELECT count(*)
                FROM assignment_shadow_bootstrap_desired desired
                LEFT JOIN assignment_grant_projection actual
                  ON actual.assignment_id = desired.assignment_id
                 AND actual.source_event_id = desired.event_id
                 AND actual.role_key = desired.role_key
                 AND actual.org_unit_id = desired.org_unit_id
                 AND actual.lifecycle_state = desired.lifecycle_state
                WHERE actual.assignment_id IS NULL
                """,
            """
                SELECT desired.assignment_uid || '/' || desired.user_uid
                FROM assignment_shadow_bootstrap_desired desired
                LEFT JOIN assignment_grant_projection actual
                  ON actual.assignment_id = desired.assignment_id
                 AND actual.source_event_id = desired.event_id
                 AND actual.role_key = desired.role_key
                 AND actual.org_unit_id = desired.org_unit_id
                 AND actual.lifecycle_state = desired.lifecycle_state
                WHERE actual.assignment_id IS NULL
                ORDER BY desired.assignment_uid, desired.user_uid
                LIMIT 10
                """
        );
    }

    private String exactEventCountSql() {
        return "SELECT count(*) FROM assignment_shadow_bootstrap_desired desired "
            + "JOIN event_journal actual ON " + exactEventPredicate();
    }

    private String missingEventSql() {
        return "SELECT desired.assignment_uid, desired.user_uid "
            + "FROM assignment_shadow_bootstrap_desired desired "
            + "LEFT JOIN event_journal actual ON " + exactEventPredicate() + " "
            + "WHERE actual.event_id IS NULL";
    }

    private String exactEventPredicate() {
        return "actual.event_id = desired.event_id "
            + "AND actual.event_type = '" + EVENT_TYPE + "' "
            + "AND actual.shape_ref = '" + SHAPE_REF + "' "
            + "AND actual.activity_ref = desired.activity_uid "
            + "AND actual.subject_type = '" + SUBJECT_TYPE + "' "
            + "AND actual.subject_id = desired.assignment_id "
            + "AND actual.actor_id = '" + SYSTEM_ACTOR + "' "
            + "AND actual.payload = jsonb_build_object("
            + "'role', desired.role_key, "
            + "'org_unit_id', desired.org_unit_id::text, "
            + "'lifecycle_state', desired.lifecycle_state)";
    }

    private void verifyExact(String itemName, String conflictCountSql, String sampleSql) {
        long conflicts = count(conflictCountSql);
        if (conflicts == 0) {
            return;
        }
        List<String> samples = jdbc.queryForList(sampleSql, String.class);
        throw new AssignmentShadowBootstrapConflictException(
            "Conflicting immutable " + itemName + " rows=" + conflicts + ", samples=" + samples
        );
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return Objects.requireNonNull(value, "Count query returned null");
    }
}
