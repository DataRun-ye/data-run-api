package org.nmcpye.datarun.web.rest.v1.account;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserProfileV1Test {

    @Test
    void mapsTheReleasedMobileProfileWithoutExposingInternalScopeIds() {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.getUid()).thenReturn("user0000001");
        when(user.getUsername()).thenReturn("worker");
        when(user.isEnabled()).thenReturn(true);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(user).getAuthorities();
        when(user.getActivityUIDs()).thenReturn(Set.of("activity01"));
        when(user.getUserTeamsUIDs()).thenReturn(Set.of("team0000001"));
        when(user.getManagedTeamsUIDs()).thenReturn(Set.of("team0000002"));
        when(user.getUserFormsUIDs()).thenReturn(Set.of("form0000001"));

        CurrentUserProfileV1 profile = CurrentUserProfileV1.from(user);

        assertThat(profile.id()).isEqualTo("user0000001");
        assertThat(profile.userTeamsUIDs()).containsExactly("team0000001");
        assertThat(profile.managedTeamsUIDs()).containsExactly("team0000002");
        assertThat(profile.userGroupsUIDs()).isEmpty();
        assertThat(profile.userFormsUIDs()).containsExactly("form0000001");
    }
}
