package org.nmcpye.datarun.assignmentshadow.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class AssignmentShadowBootstrapIntegrationTest {

    private static final String PROJECT_ID = "bst-project";
    private static final String PROJECT_UID = "P0000000001";
    private static final String ACTIVITY_ID = "bst-activity";
    private static final String ACTIVITY_UID = "A0000000001";
    private static final String USER_ID = "bst-user";
    private static final String USER_UID = "U0000000001";
    private static final String FORM_UID_1 = "F0000000001";
    private static final String FORM_UID_2 = "F0000000002";
    private static final String FORM_UID_3 = "F0000000003";
    private static final String FORM_UID_4 = "F0000000004";

    @Autowired
    private AssignmentShadowBootstrap bootstrap;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        cleanFixtures();
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    @Test
    void activeRetiredAndExcludedRowsFollowBaselineCaptureRules() throws JsonProcessingException {
        insertProject();
        insertActivity(ACTIVITY_ID, ACTIVITY_UID, false);
        insertActivity("bst-disabled-activity", "A0000000002", true);
        insertUser();

        insertTeam(
            "bst-team-active",
            "T0000000001",
            ACTIVITY_ID,
            false,
            """
                [
                  {"form":"F0000000001","permissions":["VIEW_SUBMISSIONS","ADD_SUBMISSIONS"]},
                  {"form":"F0000000002","permissions":["EDIT_SUBMISSIONS"]},
                  {"form":"F0000000003","permissions":["VIEW_SUBMISSIONS"]},
                  {"form":"F0000000004","permissions":["DELETE_SUBMISSIONS"]}
                ]
                """
        );
        addTeamUser("bst-team-active");
        insertOrgUnit("bst-org-active", "O0000000001");
        insertAssignment(
            "bst-assignment-active",
            "S0000000001",
            ACTIVITY_ID,
            "bst-team-active",
            "bst-org-active",
            false,
            "[\"F0000000004\",\"F0000000002\",\"F0000000001\",\"F0000000003\"]"
        );

        insertTeam(
            "bst-team-retired",
            "T0000000002",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-retired");
        insertOrgUnit("bst-org-retired", "O0000000002");
        insertAssignment(
            "bst-assignment-retired",
            "S0000000002",
            ACTIVITY_ID,
            "bst-team-retired",
            "bst-org-retired",
            true,
            forms(FORM_UID_1)
        );

        insertExcludedFixtures();

        AssignmentShadowBootstrapReport report = bootstrap.run();

        assertThat(report.successful()).isTrue();
        assertThat(report.baselineTupleCount()).isEqualTo(2);
        assertThat(report.shadowTupleCount()).isEqualTo(2);
        assertThat(report.activeGrants()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(1, 0));
        assertThat(report.endedGrants()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(1, 0));
        assertThat(report.retiredRows()).isEqualTo(1);
        assertThat(report.disabledRows()).isEqualTo(2);
        assertThat(report.noActorRows()).isEqualTo(1);
        assertThat(report.emptyFormSetRows()).isEqualTo(2);
        assertThat(report.nullScopeRows()).isZero();
        assertThat(report.malformedRows()).isZero();
        assertThat(report.rawActiveGrantCount()).isEqualTo(1);
        assertThat(report.distinctEffectiveAccessCount()).isEqualTo(1);
        assertThat(report.overlapCount()).isZero();

        assertThat(jdbc.queryForList(
            "SELECT lifecycle_state FROM assignment_grant_projection ORDER BY lifecycle_state",
            String.class
        )).containsExactly("ACTIVE", "ENDED");
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM assignment_identity_link",
            Integer.class
        )).isEqualTo(2);

        String activeForms = jdbc.queryForObject(
            """
                SELECT role.form_uids::text
                FROM assignment_grant_projection grant_projection
                JOIN assignment_role_definition role
                  ON role.role_key = grant_projection.role_key
                WHERE grant_projection.lifecycle_state = 'ACTIVE'
                """,
            String.class
        );
        assertThat(objectMapper.readTree(activeForms))
            .isEqualTo(objectMapper.readTree("[\"F0000000001\",\"F0000000002\"]"));

        Map<String, Object> event = jdbc.queryForMap("""
            SELECT
                event_type,
                shape_ref,
                activity_ref,
                subject_type,
                actor_id,
                payload::text AS payload
            FROM event_journal
            JOIN assignment_grant_projection grant_projection
              ON grant_projection.source_event_id = event_journal.event_id
            WHERE grant_projection.lifecycle_state = 'ACTIVE'
            """);
        assertThat(event)
            .containsEntry("event_type", "assignment_changed")
            .containsEntry("shape_ref", "baseline_assignment_observed/v1")
            .containsEntry("activity_ref", ACTIVITY_UID)
            .containsEntry("subject_type", "assignment")
            .containsEntry("actor_id", "system:migration/datarun-baseline-assignment-bootstrap");
        JsonNode payload = objectMapper.readTree((String) event.get("payload"));
        assertThat(payload.fieldNames()).toIterable()
            .containsExactlyInAnyOrder("role", "org_unit_id", "lifecycle_state");
        assertThat(payload.path("lifecycle_state").textValue()).isEqualTo("ACTIVE");
        assertThat(report.toOperatorText())
            .contains("comparison_scope=direct_team_assignment_capture_authority")
            .contains("comparison_excludes=administrator_bypass,managed_team,user_group,view_only,delete_only")
            .contains("observed_retired_rows=1")
            .doesNotContain("excluded_retired_rows");
    }

    @Test
    void overlappingAssignmentsKeepStreamsAndRerunExactly() {
        insertBaseFixture();
        insertTeam(
            "bst-team-overlap-2",
            "T0000000002",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-overlap-2");
        insertAssignment(
            "bst-assignment-overlap-2",
            "S0000000002",
            ACTIVITY_ID,
            "bst-team-overlap-2",
            "bst-org-base",
            false,
            forms(FORM_UID_1)
        );

        AssignmentShadowBootstrapReport first = bootstrap.run();
        List<String> firstRecordedAt = recordedAtValues();
        AssignmentShadowBootstrapReport second = bootstrap.run();

        assertThat(first.baselineTupleCount()).isEqualTo(1);
        assertThat(first.shadowTupleCount()).isEqualTo(1);
        assertThat(first.identities()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(2, 0));
        assertThat(first.events()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(2, 0));
        assertThat(first.activeGrants()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(2, 0));
        assertThat(first.rawActiveGrantCount()).isEqualTo(2);
        assertThat(first.distinctEffectiveAccessCount()).isEqualTo(1);
        assertThat(first.overlapCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM assignment_identity_link",
            Integer.class
        )).isEqualTo(2);

        assertThat(second.actorAliases()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1));
        assertThat(second.orgUnitAliases()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1));
        assertThat(second.roles()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1));
        assertThat(second.identities()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 2));
        assertThat(second.events()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 2));
        assertThat(second.activeGrants()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 2));
        assertThat(recordedAtValues()).isEqualTo(firstRecordedAt);

        UUID expectedAssignmentId = namespacedUuid(
            "datarun-baseline/assignment/S0000000001/actor/U0000000001/generation/0"
        );
        assertThat(jdbc.queryForObject(
            "SELECT assignment_id FROM assignment_identity_link WHERE baseline_assignment_uid = ?",
            UUID.class,
            "S0000000001"
        )).isEqualTo(expectedAssignmentId);
    }

    @Test
    void conflictingImmutableAliasAbortsWithoutOverwrite() {
        insertBaseFixture();
        UUID expectedActorId = namespacedUuid("datarun-baseline/actor/" + USER_UID);
        jdbc.update(
            "INSERT INTO actor_identity_link (actor_id, baseline_user_uid) VALUES (?, ?)",
            expectedActorId,
            "U9999999999"
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(AssignmentShadowBootstrapConflictException.class)
            .hasMessageContaining("Conflicting immutable actor alias");

        assertThat(jdbc.queryForObject(
            "SELECT baseline_user_uid FROM actor_identity_link WHERE actor_id = ?",
            String.class,
            expectedActorId
        )).isEqualTo("U9999999999");
        assertThat(shadowCount("assignment_role_definition")).isZero();
        assertThat(shadowCount("assignment_identity_link")).isZero();
        assertThat(shadowCount("event_journal")).isZero();
        assertThat(shadowCount("assignment_grant_projection")).isZero();
    }

    @Test
    void comparisonMismatchRollsBackEveryInsertionFromTheRun() {
        insertBaseFixture();
        insertExtraShadowAuthority();
        UUID expectedActorId = namespacedUuid("datarun-baseline/actor/" + USER_UID);

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(AssignmentShadowBootstrapMismatchException.class)
            .satisfies(exception -> {
                AssignmentShadowBootstrapMismatchException mismatch =
                    (AssignmentShadowBootstrapMismatchException) exception;
                assertThat(mismatch.report().shadowOnlyCount()).isEqualTo(1);
                assertThat(mismatch.report().mismatchSamples()).hasSize(1);
            });

        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM actor_identity_link WHERE actor_id = ?",
            Integer.class,
            expectedActorId
        )).isZero();
        assertThat(shadowCount("assignment_identity_link")).isEqualTo(1);
        assertThat(shadowCount("event_journal")).isEqualTo(1);
        assertThat(shadowCount("assignment_grant_projection")).isEqualTo(1);
    }

    @Test
    void fixtureLargerThanOneBatchCompletesAndRerunsExactly() {
        insertProject();
        insertActivity(ACTIVITY_ID, ACTIVITY_UID, false);
        insertUser();
        insertTeam(
            "bst-team-large",
            "T0000000001",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-large");
        insertLargeOrgUnitsAndAssignments(AssignmentShadowBootstrap.BATCH_SIZE + 1);

        AssignmentShadowBootstrapReport first = bootstrap.run();
        AssignmentShadowBootstrapReport second = bootstrap.run();

        assertThat(first.baselineTupleCount()).isEqualTo(1_001);
        assertThat(first.shadowTupleCount()).isEqualTo(1_001);
        assertThat(first.orgUnitAliases()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(1_001, 0));
        assertThat(first.identities()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(1_001, 0));
        assertThat(first.events()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(1_001, 0));
        assertThat(first.activeGrants()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(1_001, 0));
        assertThat(second.orgUnitAliases()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1_001));
        assertThat(second.identities()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1_001));
        assertThat(second.events()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1_001));
        assertThat(second.activeGrants()).isEqualTo(new AssignmentShadowBootstrapReport.ItemCount(0, 1_001));
    }

    @Test
    void concurrentWaitersRetryWithAFreshSnapshotAfterTheAdvisoryLock() throws Exception {
        insertBaseFixture();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        try {
            Future<?> locker = executor.submit(() -> {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.executeWithoutResult(status -> {
                    jdbc.execute(
                        "SELECT pg_advisory_xact_lock("
                            + AssignmentShadowBootstrapTransaction.ADVISORY_LOCK_KEY + ")"
                    );
                    lockHeld.countDown();
                    await(releaseLock);
                });
            });
            assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();

            Future<AssignmentShadowBootstrapReport> first = executor.submit(bootstrap::run);
            Future<AssignmentShadowBootstrapReport> second = executor.submit(bootstrap::run);
            awaitAdvisoryWaiters(2);
            releaseLock.countDown();

            locker.get(30, TimeUnit.SECONDS);
            AssignmentShadowBootstrapReport firstReport = first.get(30, TimeUnit.SECONDS);
            AssignmentShadowBootstrapReport secondReport = second.get(30, TimeUnit.SECONDS);

            assertThat(firstReport.successful()).isTrue();
            assertThat(secondReport.successful()).isTrue();
            assertThat(firstReport.identities().created() + secondReport.identities().created())
                .isEqualTo(1);
            assertThat(shadowCount("assignment_identity_link")).isEqualTo(1);
        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void nullScopeAndMalformedPermissionDataAreHardFailures() {
        insertProject();
        insertActivity(ACTIVITY_ID, ACTIVITY_UID, false);
        insertUser();
        insertTeam(
            "bst-team-null-scope",
            "T0000000001",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-null-scope");
        insertAssignment(
            "bst-assignment-null-scope",
            "S0000000001",
            ACTIVITY_ID,
            "bst-team-null-scope",
            null,
            false,
            forms(FORM_UID_1)
        );

        insertTeam(
            "bst-team-malformed",
            "T0000000002",
            ACTIVITY_ID,
            false,
            "[{\"form\":\"F0000000001\",\"permissions\":[\"NOT_A_PERMISSION\"]}]"
        );
        addTeamUser("bst-team-malformed");
        insertOrgUnit("bst-org-malformed", "O0000000002");
        insertAssignment(
            "bst-assignment-malformed",
            "S0000000002",
            ACTIVITY_ID,
            "bst-team-malformed",
            "bst-org-malformed",
            false,
            forms(FORM_UID_1)
        );

        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(AssignmentShadowBootstrapConflictException.class)
            .hasMessageContaining("nullScopeRows=1")
            .hasMessageContaining("malformedRows=1");
        assertThat(shadowCount("actor_identity_link")).isZero();
        assertThat(shadowCount("assignment_role_definition")).isZero();
        assertThat(shadowCount("event_journal")).isZero();
    }

    @Test
    void operatorCommandIsDisabledByDefault() {
        assertThat(applicationContext.getBeansOfType(AssignmentShadowBootstrapCommand.class)).isEmpty();
    }

    private void insertBaseFixture() {
        insertProject();
        insertActivity(ACTIVITY_ID, ACTIVITY_UID, false);
        insertUser();
        insertTeam(
            "bst-team-base",
            "T0000000001",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-base");
        insertOrgUnit("bst-org-base", "O0000000001");
        insertAssignment(
            "bst-assignment-base",
            "S0000000001",
            ACTIVITY_ID,
            "bst-team-base",
            "bst-org-base",
            false,
            forms(FORM_UID_1)
        );
    }

    private void insertExcludedFixtures() {
        insertTeam(
            "bst-team-disabled",
            "T0000000003",
            ACTIVITY_ID,
            true,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-disabled");
        insertAssignment(
            "bst-asg-disabled-team",
            "S0000000003",
            ACTIVITY_ID,
            "bst-team-disabled",
            null,
            false,
            forms(FORM_UID_1)
        );

        insertTeam(
            "bst-team-disabled-activity",
            "T0000000004",
            "bst-disabled-activity",
            false,
            capturePermissions(FORM_UID_1)
        );
        addTeamUser("bst-team-disabled-activity");
        insertOrgUnit("bst-org-disabled-activity", "O0000000004");
        insertAssignment(
            "bst-asg-disabled-activity",
            "S0000000004",
            "bst-disabled-activity",
            "bst-team-disabled-activity",
            "bst-org-disabled-activity",
            false,
            forms(FORM_UID_1)
        );

        insertTeam(
            "bst-team-no-actor",
            "T0000000005",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_1)
        );
        insertOrgUnit("bst-org-no-actor", "O0000000005");
        insertAssignment(
            "bst-assignment-no-actor",
            "S0000000005",
            ACTIVITY_ID,
            "bst-team-no-actor",
            "bst-org-no-actor",
            false,
            forms(FORM_UID_1)
        );

        insertTeam(
            "bst-team-empty",
            "T0000000006",
            ACTIVITY_ID,
            false,
            "[{\"form\":\"F0000000001\",\"permissions\":[\"VIEW_SUBMISSIONS\"]}]"
        );
        addTeamUser("bst-team-empty");
        insertOrgUnit("bst-org-empty", "O0000000006");
        insertAssignment(
            "bst-assignment-empty",
            "S0000000006",
            ACTIVITY_ID,
            "bst-team-empty",
            "bst-org-empty",
            false,
            forms(FORM_UID_1)
        );

        insertTeam(
            "bst-team-unrelated",
            "T0000000007",
            ACTIVITY_ID,
            false,
            capturePermissions(FORM_UID_4)
        );
        addTeamUser("bst-team-unrelated");

        insertTeam(
            "bst-team-empty-array",
            "T0000000008",
            ACTIVITY_ID,
            false,
            "[]"
        );
        addTeamUser("bst-team-empty-array");
        insertOrgUnit("bst-org-empty-array", "O0000000008");
        insertAssignment(
            "bst-assignment-empty-array",
            "S0000000008",
            ACTIVITY_ID,
            "bst-team-empty-array",
            "bst-org-empty-array",
            false,
            forms(FORM_UID_1)
        );
    }

    private void insertProject() {
        jdbc.update(
            """
                INSERT INTO project (id, uid, code, name, disabled, created_by)
                VALUES (?, ?, ?, ?, FALSE, 'bootstrap-test')
                """,
            PROJECT_ID,
            PROJECT_UID,
            "bst-project",
            "Bootstrap project"
        );
    }

    private void insertActivity(String id, String uid, boolean disabled) {
        jdbc.update(
            """
                INSERT INTO activity (id, uid, code, name, disabled, project_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 'bootstrap-test')
                """,
            id,
            uid,
            id,
            id,
            disabled,
            PROJECT_ID
        );
    }

    private void insertUser() {
        jdbc.update(
            """
                INSERT INTO app_user (
                    id, uid, mobile, login, password_hash, activated, created_by
                ) VALUES (?, ?, ?, ?, ?, TRUE, 'bootstrap-test')
                """,
            USER_ID,
            USER_UID,
            "7000000001",
            "bootstrap-user",
            "x".repeat(60)
        );
    }

    private void insertTeam(
        String id,
        String uid,
        String activityId,
        boolean disabled,
        String formPermissions
    ) {
        jdbc.update(
            """
                INSERT INTO team (
                    id, uid, code, name, disabled, activity_id, form_permissions, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'bootstrap-test')
                """,
            id,
            uid,
            id,
            id,
            disabled,
            activityId,
            formPermissions
        );
    }

    private void addTeamUser(String teamId) {
        jdbc.update(
            "INSERT INTO team_user (team_id, user_id) VALUES (?, ?)",
            teamId,
            USER_ID
        );
    }

    private void insertOrgUnit(String id, String uid) {
        jdbc.update(
            """
                INSERT INTO org_unit (id, uid, code, name, created_by)
                VALUES (?, ?, ?, ?, 'bootstrap-test')
                """,
            id,
            uid,
            id,
            id
        );
    }

    private void insertAssignment(
        String id,
        String uid,
        String activityId,
        String teamId,
        String orgUnitId,
        boolean deleted,
        String forms
    ) {
        jdbc.update(
            """
                INSERT INTO assignment (
                    id, uid, deleted, activity_id, team_id, org_unit_id, forms, created_by
                ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'bootstrap-test')
                """,
            id,
            uid,
            deleted,
            activityId,
            teamId,
            orgUnitId,
            forms
        );
    }

    private void insertLargeOrgUnitsAndAssignments(int count) {
        jdbc.update("""
            INSERT INTO org_unit (id, uid, code, name, created_by)
            SELECT
                'bst-org-' || lpad(value::text, 10, '0'),
                'O' || lpad(value::text, 10, '0'),
                'bst-org-code-' || value,
                'Bootstrap org ' || value,
                'bootstrap-test'
            FROM generate_series(1, ?) value
            """, count);
        jdbc.update("""
            INSERT INTO assignment (
                id, uid, deleted, activity_id, team_id, org_unit_id, forms, created_by
            )
            SELECT
                'bst-assignment-' || lpad(value::text, 10, '0'),
                'S' || lpad(value::text, 10, '0'),
                FALSE,
                ?,
                'bst-team-large',
                'bst-org-' || lpad(value::text, 10, '0'),
                CAST('["F0000000001"]' AS jsonb),
                'bootstrap-test'
            FROM generate_series(1, ?) value
            """, ACTIVITY_ID, count);
    }

    private void insertExtraShadowAuthority() {
        UUID actorId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID orgUnitId = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID assignmentId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID eventId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        jdbc.update(
            "INSERT INTO actor_identity_link (actor_id, baseline_user_uid) VALUES (?, ?)",
            actorId,
            "U9999999998"
        );
        jdbc.update(
            "INSERT INTO org_unit_identity_link (org_unit_id, baseline_org_unit_uid) VALUES (?, ?)",
            orgUnitId,
            "O9999999998"
        );
        jdbc.update(
            """
                INSERT INTO assignment_role_definition (role_key, activity_uid, form_uids)
                VALUES ('bootstrap-extra-role', ?, CAST('["F9999999998"]' AS jsonb))
                """,
            ACTIVITY_UID
        );
        jdbc.update(
            """
                INSERT INTO assignment_identity_link (
                    assignment_id, baseline_assignment_uid, target_actor_id, generation
                ) VALUES (?, 'S9999999998', ?, 0)
                """,
            assignmentId,
            actorId
        );
        jdbc.update(
            """
                INSERT INTO event_journal (
                    event_id, event_type, shape_ref, activity_ref, subject_type,
                    subject_id, actor_id, recorded_at, payload
                ) VALUES (?, 'assignment_changed', 'test_extra/v1', ?, 'assignment', ?,
                    'bootstrap-test', now(), '{}'::jsonb)
                """,
            eventId,
            ACTIVITY_UID,
            assignmentId
        );
        jdbc.update(
            """
                INSERT INTO assignment_grant_projection (
                    assignment_id, source_event_id, role_key, org_unit_id, lifecycle_state
                ) VALUES (?, ?, 'bootstrap-extra-role', ?, 'ACTIVE')
                """,
            assignmentId,
            eventId,
            orgUnitId
        );
    }

    private List<String> recordedAtValues() {
        return jdbc.queryForList(
            "SELECT recorded_at::text FROM event_journal ORDER BY event_id",
            String.class
        );
    }

    private int shadowCount(String tableName) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tableName, Integer.class);
    }

    private void awaitAdvisoryWaiters(int expectedCount) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject(
                "SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted",
                Integer.class
            );
            if (waiting != null && waiting >= expectedCount) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out waiting for advisory-lock contenders");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted during test coordination", exception);
        }
    }

    private void cleanFixtures() {
        jdbc.execute("""
            TRUNCATE TABLE
                transition_checkpoint,
                assignment_grant_projection,
                assignment_identity_link,
                assignment_role_definition,
                org_unit_identity_link,
                actor_identity_link,
                event_journal
            RESTART IDENTITY CASCADE
            """);
        jdbc.update("DELETE FROM assignment WHERE id LIKE 'bst-%'");
        jdbc.update("DELETE FROM team_user WHERE team_id LIKE 'bst-%' OR user_id LIKE 'bst-%'");
        jdbc.update("DELETE FROM team WHERE id LIKE 'bst-%'");
        jdbc.update("DELETE FROM org_unit WHERE id LIKE 'bst-%'");
        jdbc.update("DELETE FROM activity WHERE id LIKE 'bst-%'");
        jdbc.update("DELETE FROM project WHERE id LIKE 'bst-%'");
        jdbc.update("DELETE FROM app_user WHERE id LIKE 'bst-%'");
    }

    private static String capturePermissions(String formUid) {
        return "[{\"form\":\"" + formUid + "\",\"permissions\":[\"ADD_SUBMISSIONS\"]}]";
    }

    private static String forms(String formUid) {
        return "[\"" + formUid + "\"]";
    }

    private static UUID namespacedUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }
}
