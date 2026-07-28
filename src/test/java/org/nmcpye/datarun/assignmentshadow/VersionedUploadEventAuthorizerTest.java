package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VersionedUploadEventAuthorizerTest {

    private static final String USER_UID = "Usr00000001";
    private static final String TEAM_UID = "Tem00000001";
    private static final String ACTIVITY_UID = "Act00000001";
    private static final String FORM_UID = "Frm00000001";
    private static final String ASSIGNMENT_UID = "Asg00000001";
    private static final String ORG_UNIT_UID = "Org00000001";

    private AssignmentCaptureEventReadPort eventReader;
    private AssignmentFormAccessService formAccessService;
    private CurrentUserDetails user;
    private VersionedUploadEventAuthorizer authorizer;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        eventReader = mock(AssignmentCaptureEventReadPort.class);
        formAccessService = mock(AssignmentFormAccessService.class);
        CanonicalCaptureFormResolver captureForms =
            new CanonicalCaptureFormResolver(new ObjectMapper());
        Clock clock = Clock.fixed(
            Instant.parse("2026-07-28T12:00:00Z"),
            ZoneOffset.UTC
        );
        AssignmentCaptureScopeFactory scopeFactory =
            new AssignmentCaptureScopeFactory();
        BaselineVersionedUploadCompatibilityAdapter compatibility =
            new BaselineVersionedUploadCompatibilityAdapter(
                scopeFactory,
                captureForms,
                formAccessService,
                clock
            );
        authorizer = new VersionedUploadEventAuthorizer(
            eventReader,
            scopeFactory,
            compatibility
        );

        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn(USER_UID);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(user.getFormAccess()).thenReturn(List.of(captureAccess()));
        assignment = assignment(ASSIGNMENT_UID, ORG_UNIT_UID);
    }

    @Test
    void oneBulkReadAuthorizesActiveGrantsAcrossDistinctAssignments() {
        Assignment second = assignment("Asg00000002", "Org00000002");
        when(user.getUserTeamsUIDs()).thenReturn(Set.of());
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(
                grant(assignment, AssignmentLifecycleState.ACTIVE),
                grant(second, AssignmentLifecycleState.ACTIVE)
            ));
        VersionedUploadEventAuthorizer.Session session = authorizer.openSession(
            user,
            Arrays.asList(
                ASSIGNMENT_UID,
                second.getUid(),
                ASSIGNMENT_UID,
                null
            )
        );

        assertDoesNotThrow(() ->
            session.authorize(assignment, FORM_UID, "Sub00000001")
        );
        assertDoesNotThrow(() ->
            session.authorize(second, FORM_UID, "Sub00000002")
        );

        ArgumentCaptor<Collection<String>> assignmentUids =
            ArgumentCaptor.forClass(Collection.class);
        verify(eventReader, times(1)).readAssignments(
            eq(USER_UID),
            assignmentUids.capture()
        );
        assertThat(assignmentUids.getValue()).containsExactlyInAnyOrder(
            ASSIGNMENT_UID,
            second.getUid()
        );
        verify(formAccessService, never()).canSubmitData(
            user,
            assignment,
            FORM_UID
        );
    }

    @Test
    void administratorBypassesWithoutReadingEvents() {
        when(user.isSuper()).thenReturn(true);
        VersionedUploadEventAuthorizer.Session session =
            authorizer.openSession(user, List.of(ASSIGNMENT_UID));

        assertDoesNotThrow(() ->
            session.authorize(assignment, FORM_UID, "Sub00000001")
        );

        verify(eventReader, never()).readAssignments(
            eq(USER_UID),
            anyCollection()
        );
        verify(formAccessService, never()).canSubmitData(
            user,
            assignment,
            FORM_UID
        );
    }

    @Test
    void eventDenialPreservesMembershipAndPermissionErrors() {
        assignment.setDeleted(true);
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                assignment,
                AssignmentLifecycleState.ENDED
            )));
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("Tem00000009"));

        IllegalQueryException membership = assertThrows(
            IllegalQueryException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );
        assertThat(membership.getErrorCode()).isEqualTo(ErrorCode.E4114);

        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(user.getFormAccess()).thenReturn(List.of());
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(false);
        IllegalQueryException permission = assertThrows(
            IllegalQueryException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000002"
            )
        );
        assertThat(permission.getErrorCode()).isEqualTo(ErrorCode.E1112);
    }

    @Test
    void baselineAllowedCannotOverrideMissingOrContraryEventState() {
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot());

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );

        Assignment previousScope = assignment(
            ASSIGNMENT_UID,
            "Org00000009"
        );
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                previousScope,
                AssignmentLifecycleState.ACTIVE
            )));
        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000002"
            )
        );
    }

    @Test
    void unavailableSnapshotAndReadFailureFailClosed() {
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(AssignmentCaptureEventSnapshot.unavailable())
            .thenThrow(new IllegalStateException("event read failed"));

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );
        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000002"
            )
        );
    }

    @Test
    void unavailableAuthorityMapsToServiceUnavailable() {
        ResponseStatus responseStatus =
            AssignmentCaptureAuthorityUnavailableException.class
                .getAnnotation(ResponseStatus.class);

        assertThat(responseStatus).isNotNull();
        assertThat(responseStatus.value())
            .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void matchingEndedGenerationAcceptsOnlyRetiredCompatibility() {
        assignment.setDeleted(true);
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                assignment,
                AssignmentLifecycleState.ENDED
            )));

        assertDoesNotThrow(() ->
            session().authorize(assignment, FORM_UID, "Sub00000001")
        );

        assignment.setDeleted(false);
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);
        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000002"
            )
        );
    }

    @Test
    void activeGrantCannotAuthorizeSoftDeletedAssignment() {
        assignment.setDeleted(true);
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                assignment,
                AssignmentLifecycleState.ACTIVE
            )));

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );
    }

    @Test
    void olderMatchingGenerationCannotOverrideLatestScope() {
        assignment.setDeleted(true);
        AssignmentCaptureEventGrant olderMatching = grant(
            assignment,
            0,
            AssignmentLifecycleState.ENDED
        );
        Assignment latestScope = assignment(
            ASSIGNMENT_UID,
            "Org00000009"
        );
        AssignmentCaptureEventGrant latestChanged = grant(
            latestScope,
            1,
            AssignmentLifecycleState.ENDED
        );
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(olderMatching, latestChanged));
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );
    }

    @Test
    void retiredGrantCannotAuthorizeFormRemovedFromCurrentScope() {
        assignment.setDeleted(true);
        assignment.setForms(Set.of(FORM_UID, "Frm00000002"));
        when(user.getFormAccess()).thenReturn(List.of(
            UserFormAccess.builder()
                .user(USER_UID)
                .team(TEAM_UID)
                .form("Frm00000002")
                .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
                .build()
        ));
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                assignment,
                0,
                List.of(FORM_UID, "Frm00000002"),
                AssignmentLifecycleState.ENDED
            )));
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(false);

        IllegalQueryException denial = assertThrows(
            IllegalQueryException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );

        assertThat(denial.getErrorCode()).isEqualTo(ErrorCode.E1112);
    }

    @Test
    void retiredGrantCannotAuthorizeAfterCurrentFormScopeNarrows() {
        assignment.setDeleted(true);
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                assignment,
                0,
                List.of(FORM_UID, "Frm00000002"),
                AssignmentLifecycleState.ENDED
            )));
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);

        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );
    }

    @Test
    void retiredStatusFormRoleAndScopeChangesDenyEndedHistory() {
        assignment.setDeleted(true);
        AssignmentCaptureEventGrant ended = grant(
            assignment,
            AssignmentLifecycleState.ENDED
        );
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(ended));
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);

        assignment.getTeam().setDisabled(true);
        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000001"
            )
        );
        assignment.getTeam().setDisabled(false);

        assignment.setForms(Set.of("Frm00000009"));
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(false);
        IllegalQueryException formRole = assertThrows(
            IllegalQueryException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000002"
            )
        );
        assertThat(formRole.getErrorCode()).isEqualTo(ErrorCode.E1112);

        assignment.setForms(Set.of(FORM_UID));
        assignment.getOrgUnit().setUid("Org00000009");
        when(formAccessService.canSubmitData(user, assignment, FORM_UID))
            .thenReturn(true);
        assertThrows(
            AssignmentCaptureAuthorityUnavailableException.class,
            () -> session().authorize(
                assignment,
                FORM_UID,
                "Sub00000003"
            )
        );
    }

    private VersionedUploadEventAuthorizer.Session session() {
        return authorizer.openSession(user, List.of(ASSIGNMENT_UID));
    }

    private AssignmentCaptureEventSnapshot snapshot(
        AssignmentCaptureEventGrant... grants
    ) {
        return AssignmentCaptureEventSnapshot.available(
            AssignmentShadowIdentities.actorId(USER_UID),
            List.of(grants)
        );
    }

    private AssignmentCaptureEventGrant grant(
        Assignment value,
        AssignmentLifecycleState lifecycle
    ) {
        return grant(value, 0, lifecycle);
    }

    private AssignmentCaptureEventGrant grant(
        Assignment value,
        int generation,
        AssignmentLifecycleState lifecycle
    ) {
        return grant(value, generation, List.of(FORM_UID), lifecycle);
    }

    private AssignmentCaptureEventGrant grant(
        Assignment value,
        int generation,
        List<String> formUids,
        AssignmentLifecycleState lifecycle
    ) {
        return new AssignmentCaptureEventGrant(
            value.getUid(),
            AssignmentShadowIdentities.actorId(USER_UID),
            generation,
            value.getActivity().getUid(),
            AssignmentShadowIdentities.orgUnitId(
                value.getOrgUnit().getUid()
            ),
            value.getOrgUnit().getUid(),
            formUids,
            lifecycle
        );
    }

    private UserFormAccess captureAccess() {
        return UserFormAccess.builder()
            .user(USER_UID)
            .team(TEAM_UID)
            .form(FORM_UID)
            .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
            .build();
    }

    private Assignment assignment(
        String assignmentUid,
        String orgUnitUid
    ) {
        Activity activity = new Activity();
        activity.setUid(ACTIVITY_UID);
        activity.setDisabled(false);

        Activity teamActivity = new Activity();
        teamActivity.setUid(ACTIVITY_UID);
        teamActivity.setDisabled(false);

        Team team = new Team();
        team.setUid(TEAM_UID);
        team.setDisabled(false);
        team.setActivity(teamActivity);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setUid(orgUnitUid);

        Assignment value = new Assignment();
        value.setUid(assignmentUid);
        value.setDeleted(false);
        value.setActivity(activity);
        value.setTeam(team);
        value.setOrgUnit(orgUnit);
        value.setForms(Set.of(FORM_UID));
        return value;
    }
}
