package org.nmcpye.datarun.jpa.team;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.stream.Streams;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.common.NameableObject;
import org.nmcpye.datarun.common.enumeration.EntityScope;
import org.nmcpye.datarun.common.enumeration.FormPermission;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.user.User;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Team.
 */
@Entity
@Table(name = "team", uniqueConstraints = {
    @UniqueConstraint(name = "uc_team_code_activity_id", columnNames = {"code", "activity_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class Team extends JpaBaseIdentifiableObject {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", nullable = false)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false)
    protected String name;

    @Column(name = "description")
    private String description;

    @Column(name = "disabled")
    private Boolean disabled = false;

    @Column(name = "delete_client_data")
    private Boolean deleteClientData = false;

    @ManyToOne
    @JsonIgnoreProperties(value = {"project", "translations", "assignments"}, allowSetters = true)
    @JsonSerialize(contentAs = NameableObject.class)
    private Activity activity;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "team_user", joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    @JsonIgnoreProperties(value = {"teams", "password", "authorities", "userGroups", "managedGroups", "managedByGroups"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users = new HashSet<>();

    /// //
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "team_managed_teams",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "managed_team_id")
    )
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity", "teamFormAccesses", "formPermissions"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Team> managedTeams = new HashSet<>();

    @ManyToMany(mappedBy = "managedTeams")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"managedTeams", "managedByTeams", "users", "assignments",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy", "activity", "teamFormAccesses", "formPermissions"}, allowSetters = true)
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

    public Set<FormPermission> getFormPermissions(String form) {
        return formPermissions.stream()
            .filter(p -> p.getForm().equals(form)).map(TeamFormPermissions::getPermissions)
            .flatMap(Streams::of).collect(Collectors.toSet());
    }

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
}
