package org.nmcpye.datarun.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.nmcpye.datarun.common.security.UserFormAccess;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CurrentUserDetails extends UserDetails {

    @Override
    Collection<? extends GrantedAuthority> getAuthorities();

    @JsonIgnore
    @Override
    String getPassword();

    @Override
    String getUsername();

    @Override
    boolean isAccountNonExpired();

    @Override
    boolean isAccountNonLocked();

    @Override
    boolean isCredentialsNonExpired();

    @Override
    boolean isEnabled();

    boolean isSuper();

    String getUid();

    String getLangKey();

    /**
     * Set of UserTeam UID which current User belongs to.
     *
     * @return user Teams uids
     */
    Set<String> getUserTeamIds();

    Set<String> getUserActivityIds();

    Set<String> getUserManagedTeamIds();

    Set<String> getUserGroupIds();

    List<UserFormAccess> getUserFormAccess();
}
