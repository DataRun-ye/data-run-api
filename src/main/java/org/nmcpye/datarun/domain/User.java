package org.nmcpye.datarun.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.config.Constants;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.nmcpye.datarun.security.AuthoritiesConstants;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A user.
 */
@Entity
@Table(name = "app_user")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Getter
@Setter
public class User extends JpaAuditableObject {

    @Size(max = 20)
    @Column(name = "mobile", length = 20, unique = true)
    private String mobile;

    @NotNull
    @Pattern(regexp = Constants.LOGIN_REGEX)
    @Size(min = 1, max = 50)
    @Column(length = 50, unique = true, nullable = false)
    private String login;

    @JsonIgnore
    @NotNull
    @Size(min = 60, max = 60)
    @Column(name = "password_hash", length = 60, nullable = false)
    private String password;

    @Size(max = 50)
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Size(max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Email
    @Size(min = 5, max = 254)
    @Column(length = 254, unique = true)
    private String email;

    @NotNull
    @Column(nullable = false)
    private boolean activated = false;

    @Size(min = 2, max = 10)
    @Column(name = "lang_key", length = 10)
    private String langKey;

    @Size(max = 256)
    @Column(name = "image_url", length = 256)
    private String imageUrl;

    @Size(max = 20)
    @Column(name = "activation_key", length = 20)
    @JsonIgnore
    private String activationKey;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    @JsonIgnore
    private String resetKey;

    @Column(name = "reset_date")
    private Instant resetDate = null;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "app_user_authority",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
        inverseJoinColumns = {@JoinColumn(name = "authority_name", referencedColumnName = "name")}
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @BatchSize(size = 20)
    private Set<Authority> authorities = new HashSet<>();

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "app_user_role_members",
        joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
        inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "id")}
    )
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    Set<Role> roles;

    @ManyToMany(mappedBy = "users")
    @JsonIgnoreProperties(value = {"managedTeams", "managedTeams", "users", "assignments",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Team> teams = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "users", fetch = FetchType.EAGER)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"users", "managedGroups", "managedByGroups"}, allowSetters = true)
    private Set<UserGroup> groups = new HashSet<>();

    // Lowercase the login before saving it in database
    public void setLogin(String login) {
        this.login = StringUtils.lowerCase(login, Locale.ENGLISH);
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getPassword() {
        return password;
    }

    public void setGroups(Set<UserGroup> userGroups) {
        if (this.groups != null) {
            this.groups.forEach(i -> i.removeMember(this));
        }
        if (userGroups != null) {
            userGroups.forEach(i -> i.addMember(this));
        }
        this.groups = userGroups;
    }

    public User groups(Set<UserGroup> userGroups) {
        this.setGroups(userGroups);
        return this;
    }

    public User addGroup(UserGroup userGroup) {
        this.groups.add(userGroup);
        userGroup.getUsers().add(this);
        return this;
    }

    public User removeGroup(UserGroup userGroup) {
        this.groups.remove(userGroup);
        userGroup.getUsers().remove(this);
        return this;
    }

    @JsonIgnore
    public Set<Team> getManagedTeams() {
        Set<Team> managedTeams = new HashSet<>();

        for (Team team : teams) {
            managedTeams.addAll(team.getManagedTeams());
        }

        return managedTeams;
    }

    public boolean hasManagedTeams() {
        for (Team team : teams) {
            if (team != null && team.getManagedTeams() != null && !team.getManagedTeams().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates whether this user can manage the given user group.
     *
     * @param team the user group to test.
     * @return true if the given user group can be managed by this user, false
     * if not.
     */
    public boolean canManage(Team team) {
        return team != null && CollectionUtils.containsAny(teams, team.getManagedByTeams());
    }

    /**
     * Indicates whether this user is managed by the given user group.
     *
     * @param team the user group to test.
     * @return true if the given user group is managed by this user, false if
     * not.
     */
    public boolean isManagedBy(Team team) {
        return team != null && CollectionUtils.containsAny(teams, team.getManagedTeams());
    }

    /**
     * Indicates whether this user is managed by the given user.
     *
     * @param user the user to test.
     * @return true if the given user is managed by this user, false if not.
     */
    /**
     * Indicates whether this user is managed by the given user.
     *
     * @param user the user to test.
     * @return true if the given user is managed by this user, false if not.
     */
    public boolean isManagedBy(User user) {
        if (user == null || user.getGroups() == null) {
            return false;
        }

        for (UserGroup group : user.getGroups()) {
            if (isManagedBy(group)) {
                return true;
            }
        }

        for (Team team : user.getTeams()) {
            if (isManagedBy(team)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates whether this user is managed by the given user group.
     *
     * @param userGroup the user group to test.
     * @return true if the given user group is managed by this user, false if
     * not.
     */
    public boolean isManagedBy(UserGroup userGroup) {
        return userGroup != null && CollectionUtils.containsAny(groups, userGroup.getManagedGroups());
    }

    public Set<UserGroup> getManagedGroups() {
        Set<UserGroup> managedGroups = new HashSet<>();

        for (UserGroup group : groups) {
            managedGroups.addAll(group.getManagedGroups());
        }

        return managedGroups;
    }

    public boolean hasManagedGroups() {
        for (UserGroup group : groups) {
            if (group != null && group.getManagedGroups() != null && !group.getManagedGroups().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Indicates whether this user can manage the given user group.
     *
     * @param userGroup the user group to test.
     * @return true if the given user group can be managed by this user, false
     * if not.
     */
    public boolean canManage(UserGroup userGroup) {
        return userGroup != null && CollectionUtils.containsAny(groups, userGroup.getManagedByGroups());
    }

    /**
     * Indicates whether this user can manage the given user.
     *
     * @param user the user to test.
     * @return true if the given user can be managed by this user, false if not.
     */
    public boolean canManage(User user) {
        if (user == null || user.getGroups() == null) {
            return false;
        }

        for (UserGroup group : user.getGroups()) {
            if (canManage(group)) {
                return true;
            }
        }

        for (Team team : user.getTeams()) {
            if (canManage(team)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getAllAuthorities() {
        return getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet());
    }

    /**
     * Tests whether this user has any of the authorities in the given set.
     *
     * @param auths the authorities to compare with.
     * @return true or false.
     */
    public boolean hasAnyAuthority(Collection<String> auths) {
        return getAllAuthorities().stream().anyMatch(auths::contains);
    }

    /**
     * Tests whether the user has the given authority. Returns true in any case
     * if the user has the ALL authority.
     */
    public boolean isAuthorized(String auth) {
        if (auth == null) {
            return false;
        }

        final Set<String> auths = getAllAuthorities();

        return CollectionUtils.containsAny(auths,
            Sets.newHashSet(AuthoritiesConstants.ADMIN)) || auths.contains(auth);
    }

    /**
     * Indicates whether this user is a super user, implying that the ALL
     * authority is present in at least one of the user authority groups of this
     * user.
     */
    @JsonProperty
    public boolean isSuper() {
        final var superAuths = Sets.newHashSet(AuthoritiesConstants.ADMIN);
        return getAllAuthorities().stream().anyMatch(superAuths::contains);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "User{" +
            "login='" + login + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", imageUrl='" + imageUrl + '\'' +
            ", activated='" + activated + '\'' +
            ", langKey='" + langKey + '\'' +
            ", activationKey='" + activationKey + '\'' +
            "}";
    }
}
