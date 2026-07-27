package org.nmcpye.datarun.security;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.user.User;
import org.nmcpye.datarun.jpa.userauthority.Authority;
import org.nmcpye.datarun.userdetail.CurrentUserActivityInfo;
import org.nmcpye.datarun.userdetail.CurrentUserGroupInfo;
import org.nmcpye.datarun.userdetail.CurrentUserTeamInfo;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreatUserDetailServiceTest {

    @Test
    void usesUserAuthoritiesAsTheAuthenticationAuthoritySource() {
        CurrentUserInfoService currentUserInfoService = mock(CurrentUserInfoService.class);
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

        when(currentUserInfoService.getUserTeamInfo("worker")).thenReturn(
            CurrentUserTeamInfo.builder()
                .teamIds(Set.of())
                .teamUIDs(Set.of())
                .managedTeamIds(Set.of())
                .managedTeamUIDs(Set.of())
                .build()
        );
        when(currentUserInfoService.getUserGroupIds("worker")).thenReturn(
            CurrentUserGroupInfo.builder()
                .userGroupIds(Set.of())
                .userGroupUIDs(Set.of())
                .build()
        );
        when(currentUserInfoService.getUserActivityInfo("worker")).thenReturn(
            CurrentUserActivityInfo.builder().activityUIDs(Set.of()).build()
        );
        when(currentUserInfoService.getUserFormAccess("worker", Set.of())).thenReturn(List.of());

        CurrentUserDetails details =
            new CreatUserDetailService(currentUserInfoService).createUserDetails(user);

        assertThat(details.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder(AuthoritiesConstants.USER, AuthoritiesConstants.ADMIN);
        assertThat(details.isSuper()).isTrue();
    }
}
