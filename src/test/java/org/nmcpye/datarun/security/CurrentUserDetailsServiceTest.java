package org.nmcpye.datarun.security;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.TeamFormPermissions;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.userauthority.Authority;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserDetailsServiceTest {

    @Test
    void buildsTheCurrentScopeFromEnabledDirectTeams() {
        TeamRepository teamRepository = mock(TeamRepository.class);
        User user = activeUser();
        Activity activity = activity("activity01", false);
        Team managedTeam = team("managed0001", activity, false);
        Team directTeam = team("direct00001", activity, false);
        directTeam.setManagedTeams(Set.of(managedTeam));
        directTeam.setFormPermissions(Set.of(
            new TeamFormPermissions("form0000001", Set.of(FormPermission.ADD_SUBMISSIONS))
        ));
        when(teamRepository.findAllByUserLogin("worker", false)).thenReturn(List.of(directTeam));

        CurrentUserDetails details = new CurrentUserDetailsService(teamRepository).createUserDetails(user);

        assertThat(details.getUserTeamsUIDs()).containsExactly("direct00001");
        assertThat(details.getManagedTeamsUIDs()).containsExactly("managed0001");
        assertThat(details.getActivityUIDs()).containsExactly("activity01");
        assertThat(details.getUserFormsUIDs()).containsExactly("form0000001");
        assertThat(details.getFormAccess()).singleElement().satisfies(access -> {
            assertThat(access.getUser()).isEqualTo("user0000001");
            assertThat(access.getTeam()).isEqualTo("direct00001");
            assertThat(access.getForm()).isEqualTo("form0000001");
        });
        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
        assertThat(details.isSuper()).isTrue();
    }

    @Test
    void reloadsScopeForEveryAuthenticationRequest() {
        TeamRepository teamRepository = mock(TeamRepository.class);
        User user = activeUser();
        Team first = team("team0000001", activity("activity01", false), false);
        Team second = team("team0000002", activity("activity02", false), false);
        when(teamRepository.findAllByUserLogin("worker", false))
            .thenReturn(List.of(first))
            .thenReturn(List.of(second));
        CurrentUserDetailsService service = new CurrentUserDetailsService(teamRepository);

        assertThat(service.createUserDetails(user).getUserTeamsUIDs()).containsExactly("team0000001");
        assertThat(service.createUserDetails(user).getUserTeamsUIDs()).containsExactly("team0000002");
    }

    @Test
    void excludesDisabledManagedTeamsAndToleratesLegacyNullPermissions() {
        TeamRepository teamRepository = mock(TeamRepository.class);
        User user = activeUser();
        Activity activity = activity("activity01", false);
        Team directTeam = team("direct00001", activity, false);
        directTeam.setFormPermissions(null);
        directTeam.setManagedTeams(Set.of(
            team("disabled001", activity, true),
            team("disabled002", activity("disabledAct", true), false)
        ));
        when(teamRepository.findAllByUserLogin("worker", false)).thenReturn(List.of(directTeam));

        CurrentUserDetails details = new CurrentUserDetailsService(teamRepository).createUserDetails(user);

        assertThat(details.getManagedTeamsUIDs()).isEmpty();
        assertThat(details.getFormAccess()).isEmpty();
    }

    private User activeUser() {
        User user = new User();
        user.setId("user-id");
        user.setUid("user0000001");
        user.setLogin("worker");
        user.setPassword("x".repeat(60));
        user.setActivated(true);
        user.setAuthorities(Set.of(
            new Authority().name(AuthoritiesConstants.USER),
            new Authority().name(AuthoritiesConstants.ADMIN)
        ));
        return user;
    }

    private Activity activity(String uid, boolean disabled) {
        Activity activity = new Activity();
        activity.setUid(uid);
        activity.setDisabled(disabled);
        return activity;
    }

    private Team team(String uid, Activity activity, boolean disabled) {
        Team team = new Team();
        team.setId(uid + "-id");
        team.setUid(uid);
        team.setActivity(activity);
        team.setDisabled(disabled);
        return team;
    }
}
