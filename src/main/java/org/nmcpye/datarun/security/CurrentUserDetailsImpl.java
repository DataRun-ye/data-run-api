package org.nmcpye.datarun.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.nmcpye.datarun.common.security.UserFormAccess;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Hamza Assada, 20/03/2025
 */
@AllArgsConstructor
@Getter
@Builder
public class CurrentUserDetailsImpl implements CurrentUserDetails {
    private final String uid;

    private final String username;

    @JsonIgnore
    private final String password;

    private final boolean enabled;

    private final boolean accountNonExpired;

    private final boolean accountNonLocked;

    private final boolean credentialsNonExpired;

    private final Collection<GrantedAuthority> authorities;

    private final String mobile;
    private final String email;
    private final String imageUrl;
    private final String firstName;
    private final String lastName;
    private final String langKey;
    private final boolean isSuper;
    private final Set<String> userActivities;
    private final Set<String> userTeams;
    private final Set<String> managedTeams;
    private final Set<String> userGroups;
    private final Set<String> userForms;
    private final List<UserFormAccess> formAccess;
}
