package org.nmcpye.datarun.security.authorization;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.security.CurrentUserDetails;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceApiAuthorizationTest {

    private final ResourceApiAuthorization authorization =
        new ResourceApiAuthorization();

    @Test
    void administratorsCanReadAndManageGenericResourcesWithoutTeamScope() {
        CurrentUserDetails user = user(true, Set.of());

        assertThat(authorization.canRead(user)).isTrue();
        assertThat(authorization.canManage(user)).isTrue();
    }

    @Test
    void teamMembersCanReadButCannotManageGenericResources() {
        CurrentUserDetails user = user(false, Set.of("team0000001"));

        assertThat(authorization.canRead(user)).isTrue();
        assertThat(authorization.canManage(user)).isFalse();
    }

    @Test
    void usersWithoutTeamScopeAndMissingUsersCannotAccessGenericResources() {
        CurrentUserDetails user = user(false, Set.of());

        assertThat(authorization.canRead(user)).isFalse();
        assertThat(authorization.canManage(user)).isFalse();
        assertThat(authorization.canRead(null)).isFalse();
        assertThat(authorization.canManage(null)).isFalse();
    }

    private CurrentUserDetails user(boolean isSuper, Set<String> teamUids) {
        CurrentUserDetails user = mock(CurrentUserDetails.class);
        when(user.isSuper()).thenReturn(isSuper);
        when(user.getUserTeamsUIDs()).thenReturn(teamUids);
        return user;
    }
}
