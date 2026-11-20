package org.nmcpye.datarun.jpa.accessfilter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.repository.ManagedAssignmentRef;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.userdetail.UserFormAccess;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManagedAssignmentScopeTest {

    private static final String MANAGED = "managed0001";
    private static final String OTHER_MANAGED = "managed0002";
    private static final String SUPERVISION = "form0000001";
    private static final String HOUSEHOLD = "form0000002";
    private static final String MOVEMENT = "form0000003";

    private final AssignmentRepository repository = mock(AssignmentRepository.class);
    private final ManagedAssignmentScope scope = new ManagedAssignmentScope(repository);

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void asksForTheManagedTeamsAssignmentsCarryingAFormTheUserMaySubmit() {
        fromApp("6.1.2+57");
        CurrentUserDetails supervisor = user(Set.of(MANAGED), List.of(
            access(MANAGED, SUPERVISION, FormPermission.ADD_SUBMISSIONS),
            access(MANAGED, HOUSEHOLD, FormPermission.VIEW_SUBMISSIONS)));
        ManagedAssignmentRef village = new Ref("assign00001", "village0001", ",district001,village0001");
        when(repository.findManagedScope(Set.of(MANAGED), Set.of(SUPERVISION), false))
            .thenReturn(List.of(village));

        assertThat(scope.findAccessible(supervisor, false)).containsExactly(village);
    }

    @Test
    void groupsManagedTeamsByTheirForms() {
        fromApp("6.1.2+57");
        CurrentUserDetails supervisor = user(Set.of(MANAGED, OTHER_MANAGED), List.of(
            access(MANAGED, SUPERVISION, FormPermission.ADD_SUBMISSIONS),
            access(OTHER_MANAGED, MOVEMENT, FormPermission.EDIT_SUBMISSIONS)));

        scope.findAccessible(supervisor, true);

        verify(repository).findManagedScope(Set.of(MANAGED), Set.of(SUPERVISION), true);
        verify(repository).findManagedScope(Set.of(OTHER_MANAGED), Set.of(MOVEMENT), true);
    }

    @Test
    void appsThatCannotListManagedAssignmentsReceiveNone() {
        CurrentUserDetails supervisor = user(Set.of(MANAGED), List.of(
            access(MANAGED, SUPERVISION, FormPermission.ADD_SUBMISSIONS)));

        assertThat(scope.findAccessible(supervisor, false)).isEmpty();
        fromApp("6.1.1+56");
        assertThat(scope.findAccessible(supervisor, false)).isEmpty();
        verify(repository, never()).findManagedScope(any(), any(), anyBoolean());
    }

    @Test
    void managersWithoutASubmittableGrantQueryNothing() {
        fromApp("6.1.2+57");
        CurrentUserDetails viewer = user(Set.of(MANAGED), List.of(
            access(MANAGED, HOUSEHOLD, FormPermission.VIEW_SUBMISSIONS)));
        CurrentUserDetails worker = user(Set.of(), List.of());

        assertThat(scope.findAccessible(viewer, false)).isEmpty();
        assertThat(scope.findAccessible(worker, false)).isEmpty();
        verify(repository, never()).findManagedScope(any(), any(), anyBoolean());
    }

    @Test
    void readsTheAppVersionFromItsHeader() {
        assertThat(ManagedAssignmentScope.listsManagedAssignments("6.1.2")).isTrue();
        assertThat(ManagedAssignmentScope.listsManagedAssignments("6.1.2+57")).isTrue();
        assertThat(ManagedAssignmentScope.listsManagedAssignments("6.2.0+60")).isTrue();
        assertThat(ManagedAssignmentScope.listsManagedAssignments("10.0.0")).isTrue();
        assertThat(ManagedAssignmentScope.listsManagedAssignments("6.1.1+56")).isFalse();
        assertThat(ManagedAssignmentScope.listsManagedAssignments("5.3.0")).isFalse();
        assertThat(ManagedAssignmentScope.listsManagedAssignments("next")).isFalse();
        assertThat(ManagedAssignmentScope.listsManagedAssignments(null)).isFalse();
    }

    @Test
    void readsTheOrgUnitAndItsAncestorsFromThePath() {
        assertThat(new Ref("a", "village0001", ",gov00000001,district001,village0001")
            .orgUnitAndAncestorUids())
            .containsExactly("gov00000001", "district001", "village0001");
        assertThat(new Ref("a", "village0001", null).orgUnitAndAncestorUids())
            .containsExactly("village0001");
    }

    private void fromApp(String version) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ManagedAssignmentScope.APP_VERSION_HEADER, version);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private CurrentUserDetails user(Set<String> managedTeams, List<UserFormAccess> formAccess) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getManagedTeamsUIDs()).thenReturn(managedTeams);
        when(user.getFormAccess()).thenReturn(formAccess);
        return user;
    }

    private UserFormAccess access(String team, String form, FormPermission permission) {
        return UserFormAccess.builder().team(team).form(form).permissions(Set.of(permission)).build();
    }

    private record Ref(String uid, String orgUnitUid, String orgUnitPath) implements ManagedAssignmentRef {
        @Override
        public String getUid() {
            return uid;
        }

        @Override
        public String getOrgUnitUid() {
            return orgUnitUid;
        }

        @Override
        public String getOrgUnitPath() {
            return orgUnitPath;
        }
    }
}
