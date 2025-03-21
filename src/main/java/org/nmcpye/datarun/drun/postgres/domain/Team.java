package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.NameableObject;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Team.
 */
@Entity
@Table(name = "team", indexes = {
    @Index(name = "idx_team_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_team_code_activity_id", columnNames = {"code", "activity_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Team extends JpaBaseIdentifiableObject {
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "delete_client_data")
    private Boolean deleteClientData;

    @ManyToOne//fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"project", "translations"}, allowSetters = true)
    @JsonSerialize(contentAs = NameableObject.class)
    private Activity activity;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "team_user", joinColumns = @JoinColumn(name = "team_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
//    @JsonSerialize(as = UserDTO.class)
    @JsonIgnoreProperties(value = {"teams", "password", "authorities", "translations"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users = new HashSet<>();

    /////
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "team_managed_teams",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "managed_team_id")
    )
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Team> managedTeams = new HashSet<>();

    @ManyToMany(mappedBy = "managedTeams")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity"}, allowSetters = true)
    private Set<Team> managedByTeams = new HashSet<>();

    @Column(name = "enabled_from")
    private Instant enabledFrom;

    @Column(name = "enabled_to")
    private Instant enabledTo;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private Set<Assignment> assignments = new HashSet<>();

    @Type(JsonType.class)
    @Column(name = "form_permissions", columnDefinition = "jsonb")
    private Set<TeamFormPermissions> formPermissions = new HashSet<>();

    @Transient
    private EntityScope entityScope;

    public Set<String> getFormWithPermission(FormPermission permission) {
        return formPermissions.stream()
            .filter(p -> p.getPermissions().contains(permission))
            .map(TeamFormPermissions::getForm)
            .collect(Collectors.toSet());
    }

    public Set<String> getFormWithAnyPermission(List<FormPermission> permissionList) {
        return formPermissions.stream()
            .filter(fp -> fp.getPermissions().stream().anyMatch((permissionList::contains)))
            .map(TeamFormPermissions::getForm)
            .collect(Collectors.toSet());
    }

    public boolean hasFormPermission(String formId, FormPermission permission) {
        var permissions = this.formPermissions.stream().filter(teamFormPermissions ->
            Objects.equals(teamFormPermissions.getForm(), formId)).findFirst();
        return permissions.isPresent() && permissions.get().getPermissions().contains(permission);
    }

    public boolean hasAnyOfFormPermission(String formId, List<FormPermission> permissionsList) {
        var permissions = this.formPermissions.stream().filter(teamFormPermissions ->
            Objects.equals(teamFormPermissions.getForm(), formId)).findFirst();
        return permissions.isPresent() && permissions.get().getPermissions()
            .stream().anyMatch(permissionsList::contains);
    }

    public Set<String> getFormsWithPermission(FormPermission permission) {
        // for old saved and not migrated teams, who have formPermissions == null
        if (this.formPermissions != null) {
            return this.formPermissions.stream()
                .filter(entry -> entry.getPermissions().contains(permission))
                .map(TeamFormPermissions::getForm)
                .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public Team addUser(User user) {
        this.users.add(user);
        user.getTeams().add(this);
        return this;
    }

    public Team removeUser(User user) {
        this.users.remove(user);
        user.getTeams().remove(this);
        return this;
    }

    public Team addManagedTeam(Team userGroup) {
        this.managedTeams.add(userGroup);
        userGroup.getManagedByTeams().add(this);
        return this;
    }

    public Team removeManagedGroup(Team userGroup) {
        this.managedTeams.remove(userGroup);
        userGroup.getManagedByTeams().remove(this);
        return this;
    }


    public Team managedByTeams(Set<Team> teams) {
        this.setManagedByTeams(teams);
        return this;
    }

    public Team addManagedByTeam(Team team) {
        this.managedByTeams.add(team);
        team.getManagedTeams().add(this);
        return this;
    }

    public Team removeManagedByTeam(Team team) {
        this.managedByTeams.remove(team);
        team.getManagedTeams().remove(this);
        return this;
    }

    public void setAssignments(Set<Assignment> assignments) {
        if (this.assignments != null) {
            this.assignments.forEach(i -> i.setTeam(null));
        }
        if (assignments != null) {
            assignments.forEach(i -> i.setTeam(this));
        }
        this.assignments = assignments;
    }

    public Team assignments(Set<Assignment> assignments) {
        this.setAssignments(assignments);
        return this;
    }

    public Team addAssignment(Assignment assignment) {
        this.assignments.add(assignment);
        assignment.setTeam(this);
        return this;
    }

    public Team removeAssignment(Assignment assignment) {
        this.assignments.remove(assignment);
        assignment.setTeam(null);
        return this;
    }
}
