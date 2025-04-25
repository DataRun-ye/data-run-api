package org.nmcpye.datarun.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonIgnore
    @Override
    boolean isAccountNonExpired();

    @JsonIgnore
    @Override
    boolean isAccountNonLocked();

    @JsonIgnore
    @Override
    boolean isCredentialsNonExpired();

    @JsonProperty(value = "activated")
    @Override
    boolean isEnabled();

    /// Data run user's attributes
    @JsonProperty(value = "id")
    String getUid();

    String getMobile();

    String getFirstName();

    String getLastName();

    String getEmail();

    String getImageUrl();

    @JsonIgnore
    boolean isSuper();

    String getLangKey();

    /**
     * Set of UserTeam UID which current User belongs to.
     *
     * @return user Teams uids
     */
    Set<String> getUserTeams();

    Set<String> getUserActivities();

    Set<String> getManagedTeams();

    Set<String> getUserGroups();

    Set<String> getUserForms();

    List<UserFormAccess> getFormAccess();
}
