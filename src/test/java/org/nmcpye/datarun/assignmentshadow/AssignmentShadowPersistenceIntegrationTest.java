package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.eventjournal.AppendJournalEvent;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
import org.nmcpye.datarun.eventjournal.JournalEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class AssignmentShadowPersistenceIntegrationTest {

    private static final String ACTIVITY_UID = "Act00000001";
    private static final String BASELINE_USER_UID = "Usr00000001";
    private static final String BASELINE_ORG_UNIT_UID = "Org00000001";
    private static final String FORM_UID_1 = "Frm00000001";
    private static final String FORM_UID_2 = "Frm00000002";
    private static final UUID TARGET_ACTOR_ID = UUID.fromString("bb1dbdd3-4f1d-45c7-949f-e2e622886f68");
    private static final UUID ORG_UNIT_ID = UUID.fromString("3ba109ab-e47d-4a0d-99c8-08edbfd55d4a");
    private static final Instant T0 = Instant.parse("2026-07-28T10:00:00Z");
    private static final Instant T1 = Instant.parse("2026-07-28T11:00:00Z");

    @Autowired
    private ActorIdentityLinkPort actorIdentityLinks;

    @Autowired
    private OrgUnitIdentityLinkPort orgUnitIdentityLinks;

    @Autowired
    private AssignmentRoleDefinitionPort roleDefinitions;

    @Autowired
    private AssignmentIdentityLinkPort identityLinks;

    @Autowired
    private AssignmentGrantProjectionPort grantProjections;

    @Autowired
    private AssignmentAccessProjectionPort accessProjection;

    @Autowired
    private EventJournalPort eventJournal;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanShadowTables() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
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
                """)
        );
    }

    @Test
    void grantProjectionStoreIsPackageOwned() {
        assertThat(Modifier.isPublic(AssignmentGrantProjectionPort.class.getModifiers())).isFalse();
        assertThat(Modifier.isPublic(JdbcAssignmentGrantProjection.class.getModifiers())).isFalse();
    }

    @Test
    void liquibaseCreatesTheCompleteMinimalFoundationSchema() {
        List<String> tables = jdbc.queryForList(
            """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                  AND table_name IN (
                      'event_journal',
                      'transition_checkpoint',
                      'actor_identity_link',
                      'org_unit_identity_link',
                      'assignment_role_definition',
                      'assignment_identity_link',
                      'assignment_grant_projection'
                  )
                ORDER BY table_name
                """,
            String.class
        );
        Map<String, List<String>> columns = jdbc.query(
            """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name IN (
                      'event_journal',
                      'transition_checkpoint',
                      'actor_identity_link',
                      'org_unit_identity_link',
                      'assignment_role_definition',
                      'assignment_identity_link',
                      'assignment_grant_projection'
                  )
                ORDER BY table_name, ordinal_position
                """,
            resultSet -> {
                Map<String, List<String>> values = new LinkedHashMap<>();
                while (resultSet.next()) {
                    values.computeIfAbsent(resultSet.getString("table_name"), ignored -> new java.util.ArrayList<>())
                        .add(resultSet.getString("column_name"));
                }
                return values;
            }
        );
        Set<String> constraints = Set.copyOf(jdbc.queryForList(
            """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public'
                  AND table_name IN (
                      'event_journal',
                      'transition_checkpoint',
                      'actor_identity_link',
                      'org_unit_identity_link',
                      'assignment_role_definition',
                      'assignment_identity_link',
                      'assignment_grant_projection'
                  )
                  AND constraint_name NOT LIKE '%not_null'
                """,
            String.class
        ));
        Map<String, String> indexes = jdbc.query(
            """
                SELECT indexname, indexdef
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND tablename IN (
                      'event_journal',
                      'transition_checkpoint',
                      'actor_identity_link',
                      'org_unit_identity_link',
                      'assignment_role_definition',
                      'assignment_identity_link',
                      'assignment_grant_projection'
                  )
                """,
            resultSet -> {
                Map<String, String> values = new LinkedHashMap<>();
                while (resultSet.next()) {
                    values.put(resultSet.getString("indexname"), resultSet.getString("indexdef"));
                }
                return values;
            }
        );
        List<String> triggerEvents = jdbc.queryForList(
            """
                SELECT event_object_table || ':' || event_manipulation
                FROM information_schema.triggers
                WHERE trigger_schema = 'public'
                  AND trigger_name IN (
                      'trg_event_journal_immutable',
                      'trg_transition_checkpoint_immutable',
                      'trg_actor_identity_link_immutable',
                      'trg_org_unit_identity_link_immutable',
                      'trg_assignment_role_definition_immutable',
                      'trg_assignment_identity_link_immutable'
                  )
                ORDER BY event_object_table, event_manipulation
                """,
            String.class
        );
        Integer immutableFunctionCount = jdbc.queryForObject(
            """
                SELECT count(*)
                FROM pg_proc
                WHERE pronamespace = 'public'::regnamespace
                  AND proname = 'reject_immutable_table_mutation'
                """,
            Integer.class
        );
        String viewDefinition = jdbc.queryForObject(
            "SELECT pg_get_viewdef('assignment_access_projection'::regclass, true)",
            String.class
        );

        assertThat(tables).containsExactly(
            "actor_identity_link",
            "assignment_grant_projection",
            "assignment_identity_link",
            "assignment_role_definition",
            "event_journal",
            "org_unit_identity_link",
            "transition_checkpoint"
        );
        assertThat(columns)
            .containsEntry("event_journal", List.of(
                "journal_position",
                "event_id",
                "event_type",
                "shape_ref",
                "activity_ref",
                "subject_type",
                "subject_id",
                "actor_id",
                "recorded_at",
                "payload"
            ))
            .containsEntry("transition_checkpoint", List.of(
                "checkpoint_key",
                "recorded_at",
                "payload"
            ))
            .containsEntry("actor_identity_link", List.of("actor_id", "baseline_user_uid"))
            .containsEntry("org_unit_identity_link", List.of("org_unit_id", "baseline_org_unit_uid"))
            .containsEntry("assignment_role_definition", List.of("role_key", "activity_uid", "form_uids"))
            .containsEntry("assignment_identity_link", List.of(
                "assignment_id",
                "baseline_assignment_uid",
                "target_actor_id",
                "generation"
            ))
            .containsEntry("assignment_grant_projection", List.of(
                "assignment_id",
                "source_event_id",
                "role_key",
                "org_unit_id",
                "lifecycle_state"
            ));
        assertThat(constraints).containsExactlyInAnyOrder(
            "pk_event_journal",
            "uq_event_journal_event_id",
            "ck_event_journal_payload_object",
            "ck_event_journal_event_type",
            "ck_event_journal_shape_ref",
            "ck_event_journal_subject_type",
            "ck_event_journal_known_shape_envelope",
            "pk_transition_checkpoint",
            "ck_transition_checkpoint_payload_object",
            "pk_actor_identity_link",
            "uq_actor_identity_baseline_user",
            "pk_org_unit_identity_link",
            "uq_org_unit_identity_baseline_org_unit",
            "pk_assignment_role_definition",
            "uq_assignment_role_content",
            "ck_assignment_role_form_uids_array",
            "pk_assignment_identity_link",
            "uq_assignment_identity_generation",
            "fk_assignment_identity_actor",
            "ck_assignment_identity_generation",
            "pk_assignment_grant_projection",
            "uq_assignment_grant_source_event",
            "fk_assignment_grant_identity",
            "fk_assignment_grant_source_event",
            "fk_assignment_grant_role",
            "fk_assignment_grant_org_unit",
            "ck_assignment_grant_lifecycle_state"
        );
        assertThat(indexes).containsOnlyKeys(
            "pk_event_journal",
            "uq_event_journal_event_id",
            "idx_event_journal_subject_position",
            "idx_event_journal_type_position",
            "pk_transition_checkpoint",
            "pk_actor_identity_link",
            "uq_actor_identity_baseline_user",
            "pk_org_unit_identity_link",
            "uq_org_unit_identity_baseline_org_unit",
            "pk_assignment_role_definition",
            "uq_assignment_role_content",
            "pk_assignment_identity_link",
            "uq_assignment_identity_generation",
            "idx_assignment_identity_target_actor",
            "pk_assignment_grant_projection",
            "uq_assignment_grant_source_event"
        );
        assertThat(indexes.get("idx_event_journal_subject_position"))
            .contains("(subject_type, subject_id, journal_position)");
        assertThat(indexes.get("idx_event_journal_type_position"))
            .contains("(event_type, journal_position)");
        assertThat(indexes.get("idx_assignment_identity_target_actor"))
            .contains("(target_actor_id)");
        assertThat(triggerEvents).containsExactly(
            "actor_identity_link:DELETE",
            "actor_identity_link:UPDATE",
            "assignment_identity_link:DELETE",
            "assignment_identity_link:UPDATE",
            "assignment_role_definition:DELETE",
            "assignment_role_definition:UPDATE",
            "event_journal:DELETE",
            "event_journal:UPDATE",
            "org_unit_identity_link:DELETE",
            "org_unit_identity_link:UPDATE",
            "transition_checkpoint:DELETE",
            "transition_checkpoint:UPDATE"
        );
        assertThat(immutableFunctionCount).isEqualTo(1);
        assertThat(viewDefinition)
            .contains("SELECT DISTINCT")
            .contains("assignment_grant_projection")
            .contains("assignment_identity_link")
            .contains("assignment_role_definition")
            .contains("target_actor_id")
            .contains("activity_uid")
            .contains("org_unit_id")
            .contains("role_key")
            .contains("'ACTIVE'");
    }

    @Test
    void databaseRejectsUpdatesAndDeletesForEveryImmutableTable() {
        insertTargetActor();
        insertOrgUnit();
        AssignmentRoleDefinition role = insertRoleDefinition();
        UUID assignmentId = UUID.randomUUID();
        UUID eventId = appendEvent(assignmentId, "assignment_created/v1", T0);
        identityLinks.insert(identity("Asg00000001", 0, assignmentId));
        jdbc.update(
            """
                INSERT INTO transition_checkpoint (
                    checkpoint_key, recorded_at, payload
                ) VALUES ('test_checkpoint/v1', now(), '{}'::jsonb)
                """
        );

        List<ImmutableTable> immutableTables = List.of(
            new ImmutableTable("event_journal", "event_type"),
            new ImmutableTable("transition_checkpoint", "recorded_at"),
            new ImmutableTable("actor_identity_link", "baseline_user_uid"),
            new ImmutableTable("org_unit_identity_link", "baseline_org_unit_uid"),
            new ImmutableTable("assignment_role_definition", "activity_uid"),
            new ImmutableTable("assignment_identity_link", "generation")
        );

        immutableTables.forEach(table -> {
            assertThatThrownBy(() ->
                jdbc.update("UPDATE " + table.tableName() + " SET " + table.mutableProbeColumn()
                    + " = " + table.mutableProbeColumn())
            ).isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining(table.tableName() + " is immutable");

            assertThatThrownBy(() ->
                jdbc.update("DELETE FROM " + table.tableName())
            ).isInstanceOf(DataAccessException.class)
                .hasStackTraceContaining(table.tableName() + " is immutable");

            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table.tableName(), Integer.class))
                .isEqualTo(1);
        });

        assertThat(eventJournal.findByEventId(eventId)).isPresent();
        assertThat(roleDefinitions.findByRoleKey(role.roleKey())).contains(role);
    }

    @Test
    void actorAliasesRoundTripAndRejectDuplicateActorOrBaselineIdentity() {
        ActorIdentityLink alias = insertTargetActor();

        assertThat(actorIdentityLinks.findByActorId(TARGET_ACTOR_ID)).contains(alias);
        assertThat(actorIdentityLinks.findByBaselineUserUid(BASELINE_USER_UID)).contains(alias);

        assertThatThrownBy(() ->
            actorIdentityLinks.insert(new ActorIdentityLink(TARGET_ACTOR_ID, "Usr00000002"))
        ).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
            actorIdentityLinks.insert(new ActorIdentityLink(UUID.randomUUID(), BASELINE_USER_UID))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void orgUnitAliasesRoundTripAndRejectDuplicateOrgUnitOrBaselineIdentity() {
        OrgUnitIdentityLink alias = insertOrgUnit();

        assertThat(orgUnitIdentityLinks.findByOrgUnitId(ORG_UNIT_ID)).contains(alias);
        assertThat(orgUnitIdentityLinks.findByBaselineOrgUnitUid(BASELINE_ORG_UNIT_UID)).contains(alias);

        assertThatThrownBy(() ->
            orgUnitIdentityLinks.insert(new OrgUnitIdentityLink(ORG_UNIT_ID, "Org00000002"))
        ).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() ->
            orgUnitIdentityLinks.insert(new OrgUnitIdentityLink(UUID.randomUUID(), BASELINE_ORG_UNIT_UID))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleOwnerCanonicalizesAndRejectsConflictingStoredContent() {
        AssignmentRoleDefinition role = roleDefinitions.resolveOrInsert(
            ACTIVITY_UID,
            List.of(FORM_UID_2, FORM_UID_1, FORM_UID_2)
        );

        assertThat(role.activityUid()).isEqualTo(ACTIVITY_UID);
        assertThat(role.formUids()).containsExactly(FORM_UID_1, FORM_UID_2);
        assertThat(roleDefinitions.resolveOrInsert(ACTIVITY_UID, List.of(FORM_UID_1, FORM_UID_2)))
            .isEqualTo(role);
        assertThatThrownBy(() -> roleDefinitions.resolveOrInsert(ACTIVITY_UID, List.of()))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasRootCauseInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be empty");
        assertThatThrownBy(() ->
            jdbc.update(
                """
                    INSERT INTO assignment_role_definition (role_key, activity_uid, form_uids)
                    VALUES (?, ?, CAST(? AS jsonb))
                    """,
                "different-role-key",
                ACTIVITY_UID,
                "[\"Frm00000001\", \"Frm00000002\"]"
            )
        ).isInstanceOf(DataIntegrityViolationException.class);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.execute("TRUNCATE TABLE assignment_role_definition CASCADE");
            jdbc.update(
                """
                    INSERT INTO assignment_role_definition (role_key, activity_uid, form_uids)
                    VALUES (?, ?, CAST(? AS jsonb))
                    """,
                role.roleKey(),
                ACTIVITY_UID,
                "[\"Frm00000003\"]"
            );
        });

        assertThat(roleDefinitions.findByRoleKey(role.roleKey()))
            .get()
            .extracting(AssignmentRoleDefinition::formUids)
            .isEqualTo(List.of("Frm00000003"));
        assertThatThrownBy(() ->
            roleDefinitions.resolveOrInsert(ACTIVITY_UID, List.of(FORM_UID_1, FORM_UID_2))
        ).isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("already bound to different role content");
    }

    @Test
    void independentBaselineAssignmentAndActorGenerationsRemainDistinct() {
        insertTargetActor();
        UUID secondActorId = UUID.randomUUID();
        actorIdentityLinks.insert(new ActorIdentityLink(secondActorId, "Usr00000002"));
        UUID firstGenerationId = UUID.randomUUID();
        UUID secondGenerationId = UUID.randomUUID();
        UUID independentBaselineId = UUID.randomUUID();
        UUID independentActorId = UUID.randomUUID();

        identityLinks.insert(identity("Asg00000001", 0, firstGenerationId));
        identityLinks.insert(identity("Asg00000001", 1, secondGenerationId));
        identityLinks.insert(identity("Asg00000002", 0, independentBaselineId));
        identityLinks.insert(new AssignmentIdentityLink(
            independentActorId,
            "Asg00000001",
            secondActorId,
            0
        ));

        assertThat(identityLinks.findGenerations("Asg00000001", TARGET_ACTOR_ID))
            .extracting(AssignmentIdentityLink::assignmentId)
            .containsExactly(firstGenerationId, secondGenerationId);
        assertThat(identityLinks.findByAssignmentId(independentBaselineId))
            .get()
            .extracting(
                AssignmentIdentityLink::baselineAssignmentUid,
                AssignmentIdentityLink::targetActorId,
                AssignmentIdentityLink::generation
            )
            .containsExactly("Asg00000002", TARGET_ACTOR_ID, 0);
        assertThat(identityLinks.findGenerations("Asg00000001", secondActorId))
            .extracting(AssignmentIdentityLink::assignmentId)
            .containsExactly(independentActorId);

        assertThatThrownBy(() ->
            identityLinks.insert(identity("Asg00000001", 1, UUID.randomUUID()))
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateActiveGrantsCollapseOnlyInTheJoinedAccessProjection() {
        insertTargetActor();
        insertOrgUnit();
        AssignmentRoleDefinition role = insertRoleDefinition();
        UUID firstAssignmentId = UUID.randomUUID();
        UUID secondAssignmentId = UUID.randomUUID();
        identityLinks.insert(identity("Asg00000001", 0, firstAssignmentId));
        identityLinks.insert(identity("Asg00000002", 0, secondAssignmentId));

        UUID firstEventId = appendEvent(firstAssignmentId, "assignment_created/v1", T0);
        UUID secondEventId = appendEvent(secondAssignmentId, "assignment_created/v1", T0);
        grantProjections.insert(activeGrant(firstAssignmentId, firstEventId, role.roleKey()));
        grantProjections.insert(activeGrant(secondAssignmentId, secondEventId, role.roleKey()));

        assertThat(identityLinks.findByAssignmentId(firstAssignmentId)).isPresent();
        assertThat(identityLinks.findByAssignmentId(secondAssignmentId)).isPresent();
        assertThat(grantProjections.findByAssignmentId(firstAssignmentId)).isPresent();
        assertThat(grantProjections.findByAssignmentId(secondAssignmentId)).isPresent();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM assignment_grant_projection", Integer.class))
            .isEqualTo(2);
        assertThat(accessProjection.findByTargetActorId(TARGET_ACTOR_ID))
            .containsExactly(new AssignmentAccess(TARGET_ACTOR_ID, ACTIVITY_UID, ORG_UNIT_ID, role.roleKey()));
    }

    @Test
    void endedGrantLeavesJournalHistoryButDisappearsFromEffectiveAccess() {
        insertTargetActor();
        insertOrgUnit();
        AssignmentRoleDefinition role = insertRoleDefinition();
        UUID assignmentId = UUID.randomUUID();
        identityLinks.insert(identity("Asg00000001", 0, assignmentId));
        UUID startedEventId = appendEvent(assignmentId, "assignment_created/v1", T0);
        grantProjections.insert(activeGrant(assignmentId, startedEventId, role.roleKey()));

        UUID endedEventId = appendEvent(assignmentId, "assignment_ended/v1", T1);
        AssignmentGrantProjection ended = grantProjections.update(
            startedEventId,
            new AssignmentGrantProjection(
                assignmentId,
                endedEventId,
                role.roleKey(),
                ORG_UNIT_ID,
                AssignmentLifecycleState.ENDED
            )
        );

        assertThat(accessProjection.findByTargetActorId(TARGET_ACTOR_ID)).isEmpty();
        assertThat(identityLinks.findByAssignmentId(assignmentId)).isPresent();
        assertThat(eventJournal.findBySubject("assignment", assignmentId))
            .extracting(JournalEvent::eventId)
            .containsExactly(startedEventId, endedEventId);
        assertThat(grantProjections.findByAssignmentId(assignmentId)).contains(ended);
        assertThat(ended.lifecycleState()).isEqualTo(AssignmentLifecycleState.ENDED);
    }

    @Test
    void staleExpectedSourceEventRollsBackItsJournalAppend() {
        insertTargetActor();
        insertOrgUnit();
        AssignmentRoleDefinition role = insertRoleDefinition();
        UUID assignmentId = UUID.randomUUID();
        identityLinks.insert(identity("Asg00000001", 0, assignmentId));
        UUID startedEventId = appendEvent(assignmentId, "assignment_created/v1", T0);
        grantProjections.insert(activeGrant(assignmentId, startedEventId, role.roleKey()));

        UUID currentEventId = appendEvent(assignmentId, "assignment_ended/v1", T1);
        AssignmentGrantProjection current = grantProjections.update(
            startedEventId,
            new AssignmentGrantProjection(
                assignmentId,
                currentEventId,
                role.roleKey(),
                ORG_UNIT_ID,
                AssignmentLifecycleState.ENDED
            )
        );
        UUID staleAttemptEventId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() ->
            transaction.executeWithoutResult(status -> {
                appendEvent(staleAttemptEventId, assignmentId, "assignment_ended/v1", T1.plusSeconds(1));
                grantProjections.update(
                    startedEventId,
                    new AssignmentGrantProjection(
                        assignmentId,
                        staleAttemptEventId,
                        role.roleKey(),
                        ORG_UNIT_ID,
                        AssignmentLifecycleState.ENDED
                    )
                );
            })
        ).isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(eventJournal.findByEventId(staleAttemptEventId)).isEmpty();
        assertThat(eventJournal.findBySubject("assignment", assignmentId))
            .extracting(JournalEvent::eventId)
            .containsExactly(startedEventId, currentEventId);
        assertThat(grantProjections.findByAssignmentId(assignmentId)).contains(current);
    }

    private ActorIdentityLink insertTargetActor() {
        return actorIdentityLinks.insert(new ActorIdentityLink(TARGET_ACTOR_ID, BASELINE_USER_UID));
    }

    private OrgUnitIdentityLink insertOrgUnit() {
        return orgUnitIdentityLinks.insert(new OrgUnitIdentityLink(ORG_UNIT_ID, BASELINE_ORG_UNIT_UID));
    }

    private AssignmentRoleDefinition insertRoleDefinition() {
        return roleDefinitions.resolveOrInsert(ACTIVITY_UID, List.of(FORM_UID_1));
    }

    private AssignmentIdentityLink identity(
        String baselineAssignmentUid,
        int generation,
        UUID assignmentId
    ) {
        return new AssignmentIdentityLink(
            assignmentId,
            baselineAssignmentUid,
            TARGET_ACTOR_ID,
            generation
        );
    }

    private UUID appendEvent(UUID assignmentId, String shapeRef, Instant recordedAt) {
        UUID eventId = UUID.randomUUID();
        appendEvent(eventId, assignmentId, shapeRef, recordedAt);
        return eventId;
    }

    private void appendEvent(UUID eventId, UUID assignmentId, String shapeRef, Instant recordedAt) {
        eventJournal.append(
            new AppendJournalEvent(
                eventId,
                "assignment_changed",
                shapeRef,
                ACTIVITY_UID,
                "assignment",
                assignmentId,
                TARGET_ACTOR_ID.toString(),
                recordedAt,
                assignmentPayload(shapeRef, recordedAt)
            )
        );
    }

    private JsonNode assignmentPayload(String shapeRef, Instant recordedAt) {
        if ("assignment_ended/v1".equals(shapeRef)) {
            return objectMapper.createObjectNode().putNull("reason");
        }
        if (!"assignment_created/v1".equals(shapeRef)) {
            throw new IllegalArgumentException("Unsupported assignment test shape: " + shapeRef);
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.putObject("target_actor")
            .put("type", "actor")
            .put("id", TARGET_ACTOR_ID.toString());
        payload.put("role", "test-role");
        ObjectNode scope = payload.putObject("scope");
        scope.put("geographic", ORG_UNIT_ID.toString());
        scope.putNull("subject_list");
        scope.putArray("activity").add(ACTIVITY_UID);
        payload.put("valid_from", recordedAt.toString());
        payload.putNull("valid_to");
        return payload;
    }

    private AssignmentGrantProjection activeGrant(UUID assignmentId, UUID sourceEventId, String roleKey) {
        return new AssignmentGrantProjection(
            assignmentId,
            sourceEventId,
            roleKey,
            ORG_UNIT_ID,
            AssignmentLifecycleState.ACTIVE
        );
    }

    private record ImmutableTable(String tableName, String mutableProbeColumn) {
    }
}
