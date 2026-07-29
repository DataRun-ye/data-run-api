package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrap;
import org.nmcpye.datarun.assignmentshadow.bootstrap.AssignmentShadowBootstrapConflictException;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.eventjournal.EventJournalPort;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.project.Project;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.TeamFormPermissions;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.team.service.TeamService;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.authorization.ResourceApiAuthorization;
import org.nmcpye.datarun.web.rest.postgres.assignment.AssignmentResource;
import org.nmcpye.datarun.web.rest.postgres.team.TeamResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@IntegrationTest
class AssignmentAuthorityCommandIntegrationTest {

    private static final String PROJECT_ID = "aat-project";
    private static final String PROJECT_UID = "P1000000001";
    private static final String ACTIVITY_ID = "aat-activity-1";
    private static final String ACTIVITY_UID = "A1000000001";
    private static final String ACTIVITY_ID_2 = "aat-activity-2";
    private static final String ACTIVITY_UID_2 = "A1000000002";
    private static final String TEAM_ID = "aat-team";
    private static final String TEAM_UID = "T1000000001";
    private static final String TEAM_ID_2 = "aat-team-2";
    private static final String TEAM_UID_2 = "T1000000002";
    private static final String USER_ID = "aat-user";
    private static final String USER_UID = "U1000000001";
    private static final String COMMAND_ACTOR_UID = "U1000000002";
    private static final String ORG_UNIT_ID = "aat-org";
    private static final String ORG_UNIT_UID = "O1000000001";
    private static final String ORG_UNIT_ID_2 = "aat-org-2";
    private static final String ORG_UNIT_UID_2 = "O1000000002";
    private static final String ASSIGNMENT_ID = "aat-assignment";
    private static final String ASSIGNMENT_UID = "S1000000001";
    private static final String ASSIGNMENT_ID_2 = "aat-assignment-2";
    private static final String ASSIGNMENT_UID_2 = "S1000000002";
    private static final String FORM_UID_1 = "F1000000001";
    private static final String FORM_UID_2 = "F1000000002";

    @Autowired
    private AssignmentAuthorityCommandService commands;

    @Autowired
    private AssignmentShadowBootstrap bootstrap;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private EventJournalPort journal;

    @MockitoSpyBean
    private AssignmentGrantProjectionPort grantProjection;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        insertFixture(false);
        authenticateCommandActor();
    }

    @AfterEach
    void tearDown() {
        reset(journal, grantProjection);
        SecurityContextHolder.clearContext();
        cleanFixtures();
    }

    @Test
    void inactiveDirectMemberIsBootstrappedAndLifecycleUsesSuccessorGenerations() {
        assertThat(jdbc.queryForObject(
            "SELECT activated FROM app_user WHERE id = ?",
            Boolean.class,
            USER_ID
        )).isFalse();

        bootstrap.run();

        assertThat(generations()).containsExactly(0);
        assertThat(activeGrantCount()).isEqualTo(1);
        assertThat(checkpointCount()).isEqualTo(1);

        Assignment nonAuthorityUpdate = assignment(ACTIVITY_UID, Set.of(FORM_UID_1), false);
        nonAuthorityUpdate.setName("renamed without authority change");
        commands.updateAssignment(nonAuthorityUpdate);
        assertThat(assignmentEventCount()).isEqualTo(1);

        commands.updateAssignment(assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false));
        assertThat(generations()).containsExactly(0, 1);
        assertThat(jdbc.queryForObject(
            "SELECT activity_ref FROM event_journal WHERE shape_ref = 'assignment_ended/v1'",
            String.class
        )).isEqualTo(ACTIVITY_UID);
        assertThat(jdbc.queryForObject(
            "SELECT activity_ref FROM event_journal WHERE shape_ref = 'assignment_created/v1'",
            String.class
        )).isEqualTo(ACTIVITY_UID_2);

        commands.deleteAssignment(assignmentReference());
        assertThat(activeGrantCount()).isZero();

        commands.updateAssignment(assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false));
        assertThat(generations()).containsExactly(0, 1, 2);
        int eventCount = assignmentEventCount();

        commands.updateAssignment(assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false));
        assertThat(generations()).containsExactly(0, 1, 2);
        assertThat(assignmentEventCount()).isEqualTo(eventCount);
        assertThat(activeGrantCount()).isEqualTo(1);
    }

    @Test
    void liveLifecycleFactsMatchAcceptedAssignmentPayloadShapes() {
        bootstrap.run();

        commands.updateAssignment(
            assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false)
        );

        JsonNode ended = eventPayload("assignment_ended/v1");
        assertThat(ended.fieldNames()).toIterable().containsExactly("reason");
        assertThat(ended.path("reason").isNull()).isTrue();

        JsonNode created = eventPayload("assignment_created/v1");
        assertThat(created.fieldNames()).toIterable().containsExactlyInAnyOrder(
            "target_actor",
            "role",
            "scope",
            "valid_from",
            "valid_to"
        );
        assertThat(created.path("target_actor").fieldNames())
            .toIterable()
            .containsExactlyInAnyOrder("type", "id");
        assertThat(created.path("target_actor").path("type").textValue())
            .isEqualTo("actor");
        assertThat(created.path("target_actor").path("id").textValue())
            .isEqualTo(jdbc.queryForObject(
                """
                    SELECT target_actor_id::text
                    FROM assignment_identity_link
                    WHERE baseline_assignment_uid = ?
                      AND generation = 1
                    """,
                String.class,
                ASSIGNMENT_UID
            ));
        assertThat(created.path("scope").fieldNames())
            .toIterable()
            .containsExactlyInAnyOrder("geographic", "subject_list", "activity");
        assertThat(created.path("scope").path("subject_list").isNull()).isTrue();
        assertThat(created.path("scope").path("activity"))
            .containsExactly(objectMapper.getNodeFactory().textNode(ACTIVITY_UID_2));
        assertThat(created.path("valid_to").isNull()).isTrue();
        Instant recordedAt = jdbc.queryForObject(
            """
                SELECT recorded_at
                FROM event_journal
                WHERE shape_ref = 'assignment_created/v1'
                """,
            java.sql.Timestamp.class
        ).toInstant();
        assertThat(Instant.parse(created.path("valid_from").textValue()))
            .isEqualTo(recordedAt);
    }

    @Test
    void teamStatusMembershipAndPermissionsReconcileEveryAffectedAssignment() {
        bootstrap.run();

        commands.updateTeam(team(true, Set.of(userReference()), capturePermissions()));
        assertThat(activeGrantCount()).isZero();

        commands.updateTeam(team(false, Set.of(userReference()), capturePermissions()));
        assertThat(generations()).containsExactly(0, 1);

        commands.updateTeam(team(false, Set.of(), capturePermissions()));
        assertThat(activeGrantCount()).isZero();

        commands.updateTeam(team(false, Set.of(userReference()), capturePermissions()));
        assertThat(generations()).containsExactly(0, 1, 2);

        commands.updateTeam(team(false, Set.of(userReference()), Set.of()));
        assertThat(activeGrantCount()).isZero();

        commands.updateTeam(team(false, Set.of(userReference()), capturePermissions()));
        assertThat(generations()).containsExactly(0, 1, 2, 3);
        assertThat(activeGrantCount()).isEqualTo(1);
    }

    @Test
    void firstLiveStreamStartsAtGenerationZeroAfterDisabledBootstrapRow() {
        jdbc.update("UPDATE team SET disabled = TRUE WHERE id = ?", TEAM_ID);
        bootstrap.run();
        assertThat(generations()).isEmpty();

        commands.updateTeam(team(false, Set.of(userReference()), capturePermissions()));

        assertThat(generations()).containsExactly(0);
        assertThat(activeGrantCount()).isEqualTo(1);
    }

    @Test
    void missingCheckpointOrContraryShadowBlocksBaselineMutation() {
        Assignment update = assignment(ACTIVITY_UID, Set.of(FORM_UID_1), false);
        update.setName("must roll back");
        assertThatThrownBy(() -> commands.updateAssignment(update))
            .isInstanceOf(AssignmentAuthorityConflictException.class)
            .hasMessageContaining("checkpoint is missing");
        assertThat(assignmentName()).isNull();

        bootstrap.run();
        jdbc.update("""
            INSERT INTO assignment_role_definition (role_key, activity_uid, form_uids)
            VALUES ('aat-contrary-role', ?, CAST(? AS jsonb))
            """, ACTIVITY_UID, formsJson(FORM_UID_2));
        jdbc.update("""
            UPDATE assignment_grant_projection
            SET role_key = 'aat-contrary-role'
            WHERE lifecycle_state = 'ACTIVE'
              AND assignment_id IN (
                SELECT assignment_id FROM assignment_identity_link
                WHERE baseline_assignment_uid = ?
              )
            """, ASSIGNMENT_UID);

        assertThatThrownBy(() -> commands.updateAssignment(update))
            .isInstanceOf(AssignmentAuthorityConflictException.class)
            .hasMessageContaining("baseline/shadow mismatch");
        assertThat(assignmentName()).isNull();
    }

    @Test
    void assignmentCreateFormAndScopeChangesUseIndependentSuccessorGenerations() {
        bootstrap.run();

        commands.updateAssignment(assignment(ACTIVITY_UID, Set.of(FORM_UID_2), false));
        assertThat(generations()).containsExactly(0, 1);
        assertThat(activeRoleForms(ASSIGNMENT_UID)).containsExactly(FORM_UID_2);

        Assignment changedScope = assignment(ACTIVITY_UID, Set.of(FORM_UID_2), false);
        changedScope.setOrgUnit(orgUnitReference(ORG_UNIT_UID_2));
        commands.updateAssignment(changedScope);
        assertThat(generations()).containsExactly(0, 1, 2);
        assertThat(activeOrgUnitUid(ASSIGNMENT_UID)).isEqualTo(ORG_UNIT_UID_2);

        Assignment created = new Assignment();
        created.setUid(ASSIGNMENT_UID_2);
        created.setActivity(activityReference(ACTIVITY_UID_2));
        created.setTeam(teamReference());
        created.setOrgUnit(orgUnitReference(ORG_UNIT_UID));
        created.setForms(new HashSet<>(Set.of(FORM_UID_1)));
        Assignment saved = commands.saveAssignment(created);

        assertThat(saved.getId()).isNotBlank();
        assertThat(generations(ASSIGNMENT_UID_2)).containsExactly(0);
        assertThat(activeGrantCount(ASSIGNMENT_UID_2)).isEqualTo(1);
    }

    @Test
    void createNormalizesMissingUidBeforeSnapshotAndRejectsInvalidUid() {
        bootstrap.run();

        Assignment created = new Assignment();
        created.setUid(null);
        created.setActivity(activityReference(ACTIVITY_UID));
        created.setTeam(teamReference());
        created.setOrgUnit(orgUnitReference(ORG_UNIT_UID_2));
        created.setForms(new HashSet<>(Set.of(FORM_UID_1)));

        Assignment saved = commands.saveAssignment(created);

        assertThat(CodeGenerator.isValidUid(saved.getUid())).isTrue();
        assertThat(generations(saved.getUid())).containsExactly(0);
        assertThat(activeGrantCount(saved.getUid())).isEqualTo(1);

        Assignment invalid = new Assignment();
        invalid.setUid("invalid");
        invalid.setActivity(activityReference(ACTIVITY_UID));
        invalid.setTeam(teamReference());
        invalid.setOrgUnit(orgUnitReference(ORG_UNIT_UID_2));
        invalid.setForms(new HashSet<>(Set.of(FORM_UID_1)));

        assertThatThrownBy(() -> commands.saveAssignment(invalid))
            .isInstanceOf(AssignmentAuthorityConflictException.class)
            .hasMessageContaining("UID is invalid");
    }

    @Test
    void conflictingDatabaseIdAndUidCannotSelectAnotherAuthorityRow() throws Exception {
        insertOverlappingAssignment();
        bootstrap.run();

        Assignment assignment = assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false);
        assignment.setId(ASSIGNMENT_ID_2);
        AssignmentResource assignmentResource = new AssignmentResource(
            assignmentService,
            assignmentRepository
        );
        CurrentUserDetails routeUser = mock(CurrentUserDetails.class);
        ResourceApiAuthorization authorization = mock(ResourceApiAuthorization.class);
        when(authorization.canRead(routeUser)).thenReturn(true);
        when(authorization.canManage(routeUser)).thenReturn(true);
        ReflectionTestUtils.setField(
            assignmentResource,
            "resourceApiAuthorization",
            authorization
        );

        assertThatThrownBy(() -> assignmentResource.updateEntity(
            ASSIGNMENT_UID,
            assignment,
            routeUser
        )).isInstanceOf(AssignmentAuthorityConflictException.class)
            .hasMessageContaining("identify different rows");

        Team team = team(false, Set.of(userReference()), capturePermissions());
        team.setId(TEAM_ID_2);
        TeamResource teamResource = new TeamResource(teamService, teamRepository);
        ReflectionTestUtils.setField(teamResource, "resourceApiAuthorization", authorization);
        assertThatThrownBy(() -> teamResource.partialUpdateTeam(TEAM_UID, team, routeUser))
            .isInstanceOf(AssignmentAuthorityConflictException.class)
            .hasMessageContaining("identify different rows");

        Activity activity = activity(ACTIVITY_UID, true);
        activity.setId(ACTIVITY_ID_2);
        assertThatThrownBy(() -> commands.updateActivity(activity))
            .isInstanceOf(AssignmentAuthorityConflictException.class)
            .hasMessageContaining("identify different rows");

        assertThat(currentAssignmentActivity()).isEqualTo(ACTIVITY_UID);
        assertThat(activeGrantCount()).isEqualTo(1);
        assertThat(activeGrantCount(ASSIGNMENT_UID_2)).isEqualTo(1);
    }

    @Test
    void activityDisableAndReenableReconcilesAssignmentAndTeamActivityDependents() {
        bootstrap.run();

        commands.updateActivity(activity(ACTIVITY_UID, true));
        assertThat(activeGrantCount()).isZero();

        commands.updateActivity(activity(ACTIVITY_UID, false));
        assertThat(generations()).containsExactly(0, 1);
        assertThat(activeGrantCount()).isEqualTo(1);

        Team teamWithIndependentActivity = team(false, Set.of(userReference()), capturePermissions());
        teamWithIndependentActivity.setActivity(activityReference(ACTIVITY_UID_2));
        commands.updateTeam(teamWithIndependentActivity);
        assertThat(generations()).containsExactly(0, 1);

        commands.updateActivity(activity(ACTIVITY_UID_2, true));
        assertThat(activeGrantCount()).isZero();
        commands.updateActivity(activity(ACTIVITY_UID_2, false));
        assertThat(generations()).containsExactly(0, 1, 2);
    }

    @Test
    void partialTeamUserResolutionAcceptsLoginIdAndUidAndRejectsUnknownUsers() {
        bootstrap.run();
        int initialEvents = assignmentEventCount();

        for (User reference : List.of(userByLogin(), userById(), userReference())) {
            Team patch = teamReference();
            patch.setUsers(new HashSet<>(Set.of(reference)));
            assertThat(commands.partialUpdateTeam(patch)).isPresent();
            assertThat(assignmentEventCount()).isEqualTo(initialEvents);
            assertThat(activeGrantCount()).isEqualTo(1);
        }

        Team unresolvedPatch = teamReference();
        User unresolved = new User();
        unresolved.setUid("U1999999999");
        unresolvedPatch.setUsers(new HashSet<>(Set.of(unresolved)));
        assertThatThrownBy(() -> commands.partialUpdateTeam(unresolvedPatch))
            .hasMessageContaining("User not found");
        assertThat(teamMemberCount()).isEqualTo(1);
        assertThat(activeGrantCount()).isEqualTo(1);
        assertThat(assignmentEventCount()).isEqualTo(initialEvents);
    }

    @Test
    void overlappingAssignmentsKeepIndependentStreamsAndDeduplicateEffectiveAccess() {
        insertOverlappingAssignment();
        bootstrap.run();

        assertThat(activeGrantCount()).isEqualTo(1);
        assertThat(activeGrantCount(ASSIGNMENT_UID_2)).isEqualTo(1);
        assertThat(effectiveAccessCount()).isEqualTo(1);

        commands.deleteAssignment(assignmentReference());

        assertThat(activeGrantCount()).isZero();
        assertThat(activeGrantCount(ASSIGNMENT_UID_2)).isEqualTo(1);
        assertThat(effectiveAccessCount()).isEqualTo(1);
    }

    @Test
    void checkpointRerunComparesGenerationAwareAssignmentStreamsBeforeEffectiveAccess() {
        insertOverlappingAssignment();
        bootstrap.run();
        jdbc.update("""
            UPDATE assignment_grant_projection
            SET lifecycle_state = 'ENDED'
            WHERE assignment_id IN (
                SELECT assignment_id
                FROM assignment_identity_link
                WHERE baseline_assignment_uid = ?
            )
            """, ASSIGNMENT_UID_2);

        assertThat(effectiveAccessCount()).isEqualTo(1);
        assertThatThrownBy(bootstrap::run)
            .isInstanceOf(AssignmentShadowBootstrapConflictException.class)
            .hasMessageContaining("Current assignment stream comparison failed");
    }

    @Test
    void journalAndProjectionFailuresRollBackBaselineAndShadowTogether() {
        bootstrap.run();
        int initialEvents = assignmentEventCount();

        doThrow(new IllegalStateException("journal unavailable"))
            .when(journal)
            .append(argThat(event -> "assignment_ended/v1".equals(event.shapeRef())));
        assertThatThrownBy(() -> commands.updateAssignment(
            assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false)
        )).hasRootCauseInstanceOf(IllegalStateException.class);
        assertAuthorityRollback(initialEvents);

        reset(journal);
        doThrow(new IllegalStateException("projection unavailable"))
            .when(grantProjection)
            .update(any(), argThat(projection ->
                projection.lifecycleState() == AssignmentLifecycleState.ENDED
            ));
        assertThatThrownBy(() -> commands.updateAssignment(
            assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false)
        )).hasRootCauseInstanceOf(IllegalStateException.class);
        assertAuthorityRollback(initialEvents);
    }

    @Test
    void concurrentCommandsCannotCreateDuplicateGenerations() {
        bootstrap.run();
        CompletableFuture<Void> first = concurrentUpdate(
            assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false)
        );
        CompletableFuture<Void> second = concurrentUpdate(
            assignment(ACTIVITY_UID, Set.of(FORM_UID_2), false)
        );

        CompletableFuture.allOf(first, second).join();

        assertThat(generations()).containsExactly(0, 1, 2);
        assertThat(jdbc.queryForObject("""
            SELECT count(*)
            FROM (
                SELECT generation
                FROM assignment_identity_link
                WHERE baseline_assignment_uid = ?
                GROUP BY generation
                HAVING count(*) > 1
            ) duplicate_generation
            """, Integer.class, ASSIGNMENT_UID)).isZero();
        assertThat(activeGrantCount()).isEqualTo(1);
    }

    @Test
    void inheritedBulkCommitsEachCommandInItsOwnTransaction() {
        bootstrap.run();
        AssignmentResource resource = new AssignmentResource(
            assignmentService,
            assignmentRepository
        );
        ResourceApiAuthorization authorization = mock(ResourceApiAuthorization.class);
        when(authorization.canRead(any())).thenReturn(true);
        when(authorization.canManage(any())).thenReturn(true);
        ReflectionTestUtils.setField(resource, "resourceApiAuthorization", authorization);

        Assignment invalidSecondItem = new Assignment();
        invalidSecondItem.setUid(ASSIGNMENT_UID_2);
        invalidSecondItem.setActivity(activityReference(ACTIVITY_UID));
        invalidSecondItem.setTeam(new Team());
        invalidSecondItem.getTeam().setUid("T1999999999");
        invalidSecondItem.setOrgUnit(orgUnitReference());
        invalidSecondItem.setForms(new HashSet<>(Set.of(FORM_UID_1)));

        assertThatThrownBy(() -> resource.saveAll(List.of(
            assignment(ACTIVITY_UID_2, Set.of(FORM_UID_1), false),
            invalidSecondItem
        ))).hasMessageContaining("Team not found");

        assertThat(currentAssignmentActivity()).isEqualTo(ACTIVITY_UID_2);
        assertThat(generations()).containsExactly(0, 1);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM assignment WHERE uid = ?",
            Integer.class,
            ASSIGNMENT_UID_2
        )).isZero();
    }

    private Assignment assignment(String activityUid, Set<String> forms, boolean deleted) {
        Assignment assignment = assignmentReference();
        assignment.setDeleted(deleted);
        assignment.setActivity(activityReference(activityUid));
        assignment.setTeam(teamReference());
        assignment.setOrgUnit(orgUnitReference());
        assignment.setForms(new HashSet<>(forms));
        return assignment;
    }

    private Assignment assignmentReference() {
        Assignment assignment = new Assignment();
        assignment.setUid(ASSIGNMENT_UID);
        return assignment;
    }

    private Team team(
        boolean disabled,
        Set<User> users,
        Set<TeamFormPermissions> formPermissions
    ) {
        Team team = teamReference();
        team.setCode("aat-team");
        team.setName("Authority team");
        team.setDisabled(disabled);
        team.setActivity(activityReference(ACTIVITY_UID));
        team.setUsers(new HashSet<>(users));
        team.setFormPermissions(new HashSet<>(formPermissions));
        return team;
    }

    private Team teamReference() {
        Team team = new Team();
        team.setUid(TEAM_UID);
        return team;
    }

    private Activity activityReference(String uid) {
        Activity activity = new Activity();
        activity.setUid(uid);
        return activity;
    }

    private org.nmcpye.datarun.jpa.orgunit.OrgUnit orgUnitReference() {
        return orgUnitReference(ORG_UNIT_UID);
    }

    private org.nmcpye.datarun.jpa.orgunit.OrgUnit orgUnitReference(String uid) {
        org.nmcpye.datarun.jpa.orgunit.OrgUnit orgUnit =
            new org.nmcpye.datarun.jpa.orgunit.OrgUnit();
        orgUnit.setUid(uid);
        return orgUnit;
    }

    private Activity activity(String uid, boolean disabled) {
        Activity activity = activityReference(uid);
        String suffix = uid.equals(ACTIVITY_UID) ? "1" : "2";
        activity.setCode("aat-activity-" + suffix);
        activity.setName("Authority activity " + suffix);
        activity.setDisabled(disabled);
        Project project = new Project();
        project.setUid(PROJECT_UID);
        activity.setProject(project);
        return activity;
    }

    private User userReference() {
        User user = new User();
        user.setUid(USER_UID);
        return user;
    }

    private User userByLogin() {
        User user = new User();
        user.setUid(null);
        user.setLogin("authority-user");
        return user;
    }

    private User userById() {
        User user = new User();
        user.setUid(null);
        user.setId(USER_ID);
        return user;
    }

    private Set<TeamFormPermissions> capturePermissions() {
        return Set.of(
            new TeamFormPermissions(FORM_UID_1, Set.of(FormPermission.ADD_SUBMISSIONS)),
            new TeamFormPermissions(FORM_UID_2, Set.of(FormPermission.EDIT_SUBMISSIONS))
        );
    }

    private List<Integer> generations() {
        return generations(ASSIGNMENT_UID);
    }

    private List<Integer> generations(String assignmentUid) {
        return jdbc.queryForList("""
            SELECT generation
            FROM assignment_identity_link
            WHERE baseline_assignment_uid = ?
            ORDER BY generation
            """, Integer.class, assignmentUid);
    }

    private int activeGrantCount() {
        return activeGrantCount(ASSIGNMENT_UID);
    }

    private int activeGrantCount(String assignmentUid) {
        return jdbc.queryForObject("""
            SELECT count(*)
            FROM assignment_grant_projection grant_projection
            JOIN assignment_identity_link identity
              ON identity.assignment_id = grant_projection.assignment_id
            WHERE identity.baseline_assignment_uid = ?
              AND grant_projection.lifecycle_state = 'ACTIVE'
            """, Integer.class, assignmentUid);
    }

    private List<String> activeRoleForms(String assignmentUid) {
        String json = jdbc.queryForObject("""
            SELECT role.form_uids::text
            FROM assignment_grant_projection grant_projection
            JOIN assignment_identity_link identity
              ON identity.assignment_id = grant_projection.assignment_id
            JOIN assignment_role_definition role ON role.role_key = grant_projection.role_key
            WHERE identity.baseline_assignment_uid = ?
              AND grant_projection.lifecycle_state = 'ACTIVE'
            """, String.class, assignmentUid);
        return json == null ? List.of() : List.of(json.replace("[", "")
            .replace("]", "").replace("\"", ""));
    }

    private String activeOrgUnitUid(String assignmentUid) {
        return jdbc.queryForObject("""
            SELECT org_unit.baseline_org_unit_uid
            FROM assignment_grant_projection grant_projection
            JOIN assignment_identity_link identity
              ON identity.assignment_id = grant_projection.assignment_id
            JOIN org_unit_identity_link org_unit
              ON org_unit.org_unit_id = grant_projection.org_unit_id
            WHERE identity.baseline_assignment_uid = ?
              AND grant_projection.lifecycle_state = 'ACTIVE'
            """, String.class, assignmentUid);
    }

    private int effectiveAccessCount() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM assignment_access_projection",
            Integer.class
        );
    }

    private int assignmentEventCount() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM event_journal WHERE event_type = 'assignment_changed'",
            Integer.class
        );
    }

    private JsonNode eventPayload(String shapeRef) {
        String payload = jdbc.queryForObject(
            "SELECT payload::text FROM event_journal WHERE shape_ref = ?",
            String.class,
            shapeRef
        );
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new AssertionError("Stored event payload is not JSON", exception);
        }
    }

    private int teamMemberCount() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM team_user WHERE team_id = ?",
            Integer.class,
            TEAM_ID
        );
    }

    private String currentAssignmentActivity() {
        return jdbc.queryForObject("""
            SELECT activity.uid
            FROM assignment
            JOIN activity ON activity.id = assignment.activity_id
            WHERE assignment.uid = ?
            """, String.class, ASSIGNMENT_UID);
    }

    private int checkpointCount() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM transition_checkpoint WHERE checkpoint_key = ?",
            Integer.class,
            AssignmentShadowCheckpoint.KEY
        );
    }

    private String assignmentName() {
        return jdbc.queryForObject(
            "SELECT name FROM assignment WHERE uid = ?",
            String.class,
            ASSIGNMENT_UID
        );
    }

    private void authenticateCommandActor() {
        CurrentUserDetails principal = mock(CurrentUserDetails.class);
        when(principal.getUid()).thenReturn(COMMAND_ACTOR_UID);
        when(principal.getUsername()).thenReturn("authority-admin");
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "n/a", List.of())
        );
    }

    private void insertFixture(boolean teamDisabled) {
        jdbc.update("""
            INSERT INTO project (id, uid, code, name, disabled, created_by)
            VALUES (?, ?, 'aat-project', 'Authority project', FALSE, 'authority-test')
            """, PROJECT_ID, PROJECT_UID);
        insertActivity(ACTIVITY_ID, ACTIVITY_UID, "Authority activity 1");
        insertActivity(ACTIVITY_ID_2, ACTIVITY_UID_2, "Authority activity 2");
        jdbc.update("""
            INSERT INTO app_user (
                id, uid, mobile, login, password_hash, activated, created_by
            ) VALUES (?, ?, '7111111111', 'authority-user', ?, FALSE, 'authority-test')
            """, USER_ID, USER_UID, "x".repeat(60));
        jdbc.update("""
            INSERT INTO team (
                id, uid, code, name, disabled, activity_id, form_permissions, created_by
            ) VALUES (?, ?, 'aat-team', 'Authority team', ?, ?, CAST(? AS jsonb), 'authority-test')
            """, TEAM_ID, TEAM_UID, teamDisabled, ACTIVITY_ID,
            "[{\"form\":\"" + FORM_UID_1 + "\",\"permissions\":[\"ADD_SUBMISSIONS\"]},"
                + "{\"form\":\"" + FORM_UID_2 + "\",\"permissions\":[\"EDIT_SUBMISSIONS\"]}]");
        jdbc.update(
            "INSERT INTO team_user (team_id, user_id) VALUES (?, ?)",
            TEAM_ID,
            USER_ID
        );
        jdbc.update("""
            INSERT INTO org_unit (id, uid, code, name, created_by)
            VALUES (?, ?, 'aat-org', 'Authority org unit', 'authority-test')
            """, ORG_UNIT_ID, ORG_UNIT_UID);
        jdbc.update("""
            INSERT INTO org_unit (id, uid, code, name, created_by)
            VALUES (?, ?, 'aat-org-2', 'Authority org unit 2', 'authority-test')
            """, ORG_UNIT_ID_2, ORG_UNIT_UID_2);
        jdbc.update("""
            INSERT INTO assignment (
                id, uid, deleted, activity_id, team_id, org_unit_id, forms, created_by
            ) VALUES (?, ?, FALSE, ?, ?, ?, CAST(? AS jsonb), 'authority-test')
            """, ASSIGNMENT_ID, ASSIGNMENT_UID, ACTIVITY_ID, TEAM_ID, ORG_UNIT_ID,
            formsJson(FORM_UID_1));
    }

    private void insertActivity(String id, String uid, String name) {
        jdbc.update("""
            INSERT INTO activity (id, uid, code, name, disabled, project_id, created_by)
            VALUES (?, ?, ?, ?, FALSE, ?, 'authority-test')
            """, id, uid, id, name, PROJECT_ID);
    }

    private void insertOverlappingAssignment() {
        jdbc.update("""
            INSERT INTO team (
                id, uid, code, name, disabled, activity_id, form_permissions, created_by
            ) VALUES (?, ?, 'aat-team-2', 'Authority team 2', FALSE, ?, CAST(? AS jsonb),
                'authority-test')
            """, TEAM_ID_2, TEAM_UID_2, ACTIVITY_ID,
            "[{\"form\":\"" + FORM_UID_1 + "\",\"permissions\":[\"ADD_SUBMISSIONS\"]}]");
        jdbc.update(
            "INSERT INTO team_user (team_id, user_id) VALUES (?, ?)",
            TEAM_ID_2,
            USER_ID
        );
        jdbc.update("""
            INSERT INTO assignment (
                id, uid, deleted, activity_id, team_id, org_unit_id, forms, created_by
            ) VALUES (?, ?, FALSE, ?, ?, ?, CAST(? AS jsonb), 'authority-test')
            """, ASSIGNMENT_ID_2, ASSIGNMENT_UID_2, ACTIVITY_ID, TEAM_ID_2, ORG_UNIT_ID,
            formsJson(FORM_UID_1));
    }

    private CompletableFuture<Void> concurrentUpdate(Assignment assignment) {
        return CompletableFuture.runAsync(() -> {
            authenticateCommandActor();
            try {
                commands.updateAssignment(assignment);
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    private void assertAuthorityRollback(int initialEvents) {
        assertThat(jdbc.queryForObject("""
            SELECT activity.uid
            FROM assignment
            JOIN activity ON activity.id = assignment.activity_id
            WHERE assignment.uid = ?
            """, String.class, ASSIGNMENT_UID)).isEqualTo(ACTIVITY_UID);
        assertThat(generations()).containsExactly(0);
        assertThat(activeGrantCount()).isEqualTo(1);
        assertThat(assignmentEventCount()).isEqualTo(initialEvents);
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
        jdbc.update(
            """
                DELETE FROM assignment
                WHERE id LIKE 'aat-%'
                   OR uid IN (?, ?)
                   OR team_id LIKE 'aat-%'
                   OR activity_id LIKE 'aat-%'
                   OR org_unit_id LIKE 'aat-%'
                """,
            ASSIGNMENT_UID,
            ASSIGNMENT_UID_2
        );
        jdbc.update("DELETE FROM team_user WHERE team_id LIKE 'aat-%' OR user_id LIKE 'aat-%'");
        jdbc.update("DELETE FROM team WHERE id LIKE 'aat-%'");
        jdbc.update("DELETE FROM org_unit WHERE id LIKE 'aat-%'");
        jdbc.update("DELETE FROM activity WHERE id LIKE 'aat-%'");
        jdbc.update("DELETE FROM project WHERE id LIKE 'aat-%'");
        jdbc.update("DELETE FROM app_user WHERE id LIKE 'aat-%'");
    }

    private static String formsJson(String formUid) {
        return "[\"" + formUid + "\"]";
    }
}
