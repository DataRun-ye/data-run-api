package org.nmcpye.datarun.assignmentshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.COMPATIBILITY_ONLY_EMPTY_CAPTURE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.EXACT;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.SHADOW_UNAVAILABLE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.UNEXPLAINED_BASELINE_SCOPE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureComparisonCategory.UNEXPLAINED_EVENT_SCOPE;
import static org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowResult.ACTIVE_GRANT;

class AssignmentCaptureShadowComparatorTest {

    private static final String USER_UID = "Usr00000001";
    private static final String ACTIVITY_UID = "Act00000001";
    private static final String TEAM_UID = "Tem00000001";
    private static final String FORM_UID_1 = "Frm00000001";
    private static final String FORM_UID_2 = "Frm00000002";

    private AssignmentCaptureEventReadPort eventReader;
    private CurrentUserDetails user;
    private AssignmentCaptureShadowComparator comparator;
    private Assignment active;
    private SimpleMeterRegistry meters;

    @BeforeEach
    void setUp() {
        eventReader = mock(AssignmentCaptureEventReadPort.class);
        user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn(USER_UID);
        when(user.getUserTeamsUIDs()).thenReturn(Set.of(TEAM_UID));
        when(user.getFormAccess()).thenReturn(List.of(
            access(FORM_UID_1),
            access(FORM_UID_2)
        ));

        BaselineAssignmentCaptureAdapter baseline =
            new BaselineAssignmentCaptureAdapter(
                new CanonicalCaptureFormResolver(new ObjectMapper()),
                new AssignmentCaptureScopeFactory(),
                Clock.fixed(
                    Instant.parse("2026-07-28T12:00:00Z"),
                    ZoneOffset.UTC
                )
            );
        meters = new SimpleMeterRegistry();
        comparator = new AssignmentCaptureShadowComparator(
            eventReader,
            baseline,
            meters
        );
        active = assignment("Asg00000001", "Org00000001", Set.of(FORM_UID_1));
    }

    @Test
    void assignmentAndFormComparisonsKeepEmptyCaptureAsCompatibility() {
        Assignment compatibility = assignment(
            "Asg00000002",
            "Org00000002",
            Set.of("Frm00000009")
        );
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                active,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_1)
            )));

        AssignmentCaptureComparisonReport list =
            comparator.compareAssignmentList(
                user,
                List.of(active, compatibility),
                false
            );
        AssignmentCaptureComparisonReport forms =
            comparator.compareAssignmentForms(
                user,
                List.of(active, compatibility),
                false
            );

        assertThat(list.count(EXACT)).isEqualTo(1);
        assertThat(list.count(COMPATIBILITY_ONLY_EMPTY_CAPTURE)).isEqualTo(1);
        assertThat(forms.count(EXACT)).isEqualTo(1);
        assertThat(forms.count(COMPATIBILITY_ONLY_EMPTY_CAPTURE)).isEqualTo(1);
        assertThat(meters.get(
            "datarun.assignment.capture.shadow.comparisons"
        ).tags(
            "surface",
            "assignment_list",
            "result",
            "exact"
        ).counter().count()).isEqualTo(1);
        verify(eventReader, org.mockito.Mockito.times(2))
            .readAssignments(eq(USER_UID), anyCollection());
    }

    @Test
    void completeAssignmentComparisonReportsBothMismatchDirections() {
        Assignment eventOnly = assignment(
            "Asg00000003",
            "Org00000003",
            Set.of(FORM_UID_1)
        );
        when(eventReader.readAllForActor(USER_UID)).thenReturn(snapshot(
            grant(
                active,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_2)
            ),
            grant(
                eventOnly,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_1)
            )
        ));

        AssignmentCaptureComparisonReport report =
            comparator.compareAssignmentList(user, List.of(active), true);

        assertThat(report.count(UNEXPLAINED_BASELINE_SCOPE)).isEqualTo(1);
        assertThat(report.count(UNEXPLAINED_EVENT_SCOPE)).isEqualTo(1);
        verify(eventReader).readAllForActor(USER_UID);
    }

    @Test
    void aliasAbsenceIsEquivalentOnlyForEmptyBaselineScope() {
        Assignment compatibility = assignment(
            "Asg00000002",
            "Org00000002",
            Set.of("Frm00000009")
        );
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(AssignmentCaptureEventSnapshot.actorAliasAbsent());

        AssignmentCaptureComparisonReport empty =
            comparator.compareAssignmentList(
                user,
                List.of(compatibility),
                false
            );
        AssignmentCaptureComparisonReport nonEmpty =
            comparator.compareAssignmentList(user, List.of(active), false);

        assertThat(empty.count(COMPATIBILITY_ONLY_EMPTY_CAPTURE)).isEqualTo(1);
        assertThat(nonEmpty.count(UNEXPLAINED_BASELINE_SCOPE)).isEqualTo(1);
    }

    @Test
    void directOrgUnitsCompareGrantsWithoutTreatingAncestorsAsScope() {
        Assignment compatibility = assignment(
            "Asg00000002",
            "Org00000002",
            Set.of("Frm00000009")
        );
        Assignment eventOnly = assignment(
            "Asg00000003",
            "Org00000003",
            Set.of(FORM_UID_1)
        );
        when(eventReader.readAllForActor(USER_UID)).thenReturn(snapshot(
            grant(
                active,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_1)
            ),
            grant(
                eventOnly,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_1)
            )
        ));

        AssignmentCaptureComparisonReport report =
            comparator.compareDirectOrgUnits(
                user,
                List.of(active, compatibility)
            );

        assertThat(report.count(EXACT)).isEqualTo(1);
        assertThat(report.count(COMPATIBILITY_ONLY_EMPTY_CAPTURE)).isEqualTo(1);
        assertThat(report.count(UNEXPLAINED_EVENT_SCOPE)).isEqualTo(1);
    }

    @Test
    void referenceComparisonIsDirectionallyNarrowedByBaselineAddAccess() {
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                active,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_1, FORM_UID_2)
            )));

        AssignmentCaptureComparisonReport report =
            comparator.compareReferenceCatalog(
                user,
                active,
                Set.of(FORM_UID_1)
            );

        assertThat(report.shadowResults()).containsExactly(ACTIVE_GRANT);
        assertThat(report.count(EXACT)).isEqualTo(1);
    }

    @Test
    void administratorsAreExcludedFromReadSurfaceComparison() {
        CurrentUserDetails administrator = mock(CurrentUserDetails.class);
        when(administrator.isSuper()).thenReturn(true);
        AssignmentCaptureComparisonReport skipped =
            comparator.compareAssignmentList(
                administrator,
                List.of(active),
                true
            );
        assertThat(skipped.categories()).isEmpty();
        verify(eventReader, never()).readAllForActor(null);
    }

    @Test
    void comparisonFailureBecomesUnavailableInsteadOfEscaping() {
        BaselineAssignmentCaptureAdapter failingBaseline =
            mock(BaselineAssignmentCaptureAdapter.class);
        AssignmentCaptureShadowComparator failOpenComparator =
            new AssignmentCaptureShadowComparator(
                eventReader,
                failingBaseline,
                meters
            );
        when(eventReader.readAssignments(eq(USER_UID), anyCollection()))
            .thenReturn(snapshot(grant(
                active,
                AssignmentLifecycleState.ACTIVE,
                0,
                List.of(FORM_UID_1)
            )));
        when(failingBaseline.activeScope(user, active))
            .thenThrow(new IllegalStateException("malformed baseline"));

        AssignmentCaptureComparisonReport report =
            failOpenComparator.compareAssignmentList(
                user,
                List.of(active),
                false
            );

        assertThat(report.count(SHADOW_UNAVAILABLE)).isEqualTo(1);
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
        Assignment assignment,
        AssignmentLifecycleState state,
        int generation,
        List<String> forms
    ) {
        return new AssignmentCaptureEventGrant(
            assignment.getUid(),
            AssignmentShadowIdentities.actorId(USER_UID),
            generation,
            assignment.getActivity().getUid(),
            AssignmentShadowIdentities.orgUnitId(
                assignment.getOrgUnit().getUid()
            ),
            assignment.getOrgUnit().getUid(),
            forms,
            state
        );
    }

    private UserFormAccess access(String formUid) {
        return UserFormAccess.builder()
            .user(USER_UID)
            .team(TEAM_UID)
            .form(formUid)
            .permissions(Set.of(FormPermission.ADD_SUBMISSIONS))
            .build();
    }

    private Assignment assignment(
        String assignmentUid,
        String orgUnitUid,
        Set<String> forms
    ) {
        Activity assignmentActivity = new Activity();
        assignmentActivity.setUid(ACTIVITY_UID);
        assignmentActivity.setDisabled(false);

        Activity teamActivity = new Activity();
        teamActivity.setUid(ACTIVITY_UID);
        teamActivity.setDisabled(false);

        Team team = new Team();
        team.setUid(TEAM_UID);
        team.setDisabled(false);
        team.setActivity(teamActivity);

        OrgUnit orgUnit = new OrgUnit();
        orgUnit.setUid(orgUnitUid);

        Assignment assignment = new Assignment();
        assignment.setUid(assignmentUid);
        assignment.setDeleted(false);
        assignment.setActivity(assignmentActivity);
        assignment.setTeam(team);
        assignment.setOrgUnit(orgUnit);
        assignment.setForms(forms);
        return assignment;
    }
}
