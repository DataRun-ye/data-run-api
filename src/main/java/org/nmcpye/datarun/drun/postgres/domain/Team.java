package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.NameableObject;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A Team.
 */
@Entity
@Table(name = "team", uniqueConstraints = {
    @UniqueConstraint(name = "uc_team_code_activity_id",
        columnNames = {"code", "activity_id"})})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Team extends BaseIdentifiableObject<Long> implements Serializable, Persistable<Long> {

    private static final String PATH_SEP = ",";

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true)
    private String uid;

    @NotNull
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "disabled")
    private Boolean disabled;

    @Column(name = "delete_client_data")
    private Boolean deleteClientData;

    @Transient
    private boolean isPersisted;

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

    ///////////////////////////////////
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"activity", "team", "orgUnit", "parent", "children", "ancestors", "level", "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    private Set<Assignment> assignments = new HashSet<>();

    //    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
//    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//    @JsonIgnoreProperties(value = {"team"}, allowSetters = true)
//    private Set<TeamFormPermission> formPermissions = new HashSet<>();
    @Type(JsonType.class)
    @Column(name = "form_permissions", columnDefinition = "jsonb")
    private Set<TeamFormPermissions> formPermissions = new HashSet<>();
//    private Map<String, List<FormPermission>> formPermissions = new HashMap<>();

    @Transient
    private EntityScope entityScope;

    public Team() {
        this.setAutoFields();
    }

    public EntityScope getEntityScope() {
        return entityScope;
    }

    public void setEntityScope(EntityScope entityScope) {
        this.entityScope = entityScope;
    }

    //    public Set<TeamFormPermission> getFormPermissions() {
//        return formPermissions;
//    }
//
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

    public Set<TeamFormPermissions> getFormPermissions() {
        return formPermissions;
    }

    public void setFormPermissions(Set<TeamFormPermissions> formPermissions) {
        this.formPermissions = formPermissions;
    }

    public void addFormPermission(TeamFormPermissions permission) {
        this.formPermissions.add(permission);
    }

//    public void removeFormPermission(String formId, FormPermission permission) {
//        Set<FormPermission> permissions = this.formPermissions.stream().filter((t)-> Objects.equals(t.getForm(), formId));
//        if (permissions != null) {
//            permissions.remove(permission);
//            if (permissions.isEmpty()) {
//                this.formPermissions.remove(formId);
//            }
//        }
//    }

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

//    public void setFormPermissions(Set<TeamFormPermission> formPermissions) {
//        this.formPermissions = formPermissions;
//    }
//
//    public Team addFormPermission(TeamFormPermission formPermission) {
//        this.formPermissions.add(formPermission);
//        formPermission.setTeam(this);
//        return this;
//    }
//
//    public Team removeFormPermission(TeamFormPermission formPermission) {
//        this.formPermissions.remove(formPermission);
//        formPermission.setTeam(null);
//        return this;
//    }

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

    public Instant getEnabledFrom() {
        return enabledFrom;
    }

    public void setEnabledFrom(Instant enabledFrom) {
        this.enabledFrom = enabledFrom;
    }

    public Instant getEnabledTo() {
        return enabledTo;
    }

    public void setEnabledTo(Instant enabledTo) {
        this.enabledTo = enabledTo;
    }


    public Set<Team> getManagedTeams() {
        return this.managedTeams;
    }

    public void setManagedTeams(Set<Team> userGroups) {
        this.managedTeams = userGroups;
    }

    public Team managedTeams(Set<Team> userGroups) {
        this.setManagedTeams(userGroups);
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

    public Set<Team> getManagedByTeams() {
        return this.managedByTeams;
    }

    public void setManagedByTeams(Set<Team> teams) {
        this.managedByTeams = teams;
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

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Team users(Set<User> users) {
        this.setUsers(users);
        return this;
    }

    public Long getId() {
        return this.id;
    }

    public Team id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public Team uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public Team code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public Team name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Team description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Boolean getDisabled() {
        return this.disabled;
    }

    public Team disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getDeleteClientData() {
        return this.deleteClientData;
    }

    public Team deleteClientData(Boolean deleteClientData) {
        this.setDeleteClientData(deleteClientData);
        return this;
    }

    public void setDeleteClientData(Boolean deleteClientData) {
        this.deleteClientData = deleteClientData;
    }

    // Inherited createdBy methods
    public Team createdBy(String createdBy) {
        this.setCreatedBy(createdBy);
        return this;
    }

    // Inherited createdDate methods
    public Team createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    // Inherited lastModifiedBy methods
    public Team lastModifiedBy(String lastModifiedBy) {
        this.setLastModifiedBy(lastModifiedBy);
        return this;
    }

    // Inherited lastModifiedDate methods
    public Team lastModifiedDate(Instant lastModifiedDate) {
        this.setLastModifiedDate(lastModifiedDate);
        return this;
    }

    @PostLoad
    @PostPersist
    public void updateEntityState() {
        this.setIsPersisted();
    }

    @Transient
    @Override
    public boolean isNew() {
        return !this.isPersisted;
    }

    public Team setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    //    @JsonSerialize(contentAs = Identifiable.class)
    public Activity getActivity() {
        return this.activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Team activity(Activity activity) {
        this.setActivity(activity);
        return this;
    }

    public Set<Assignment> getAssignments() {
        return this.assignments;
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


//    /**
//     * Returns the list of ancestor team UIDs up to any of the given
//     * roots for this team. Does not include itself. The list is
//     * ordered by root first.
//     *
//     * @param rootUids the root teams, if null using real roots.
//     */
//    public List<String> getAncestorUids(Set<String> rootUids) {
//        if (path == null || path.isEmpty()) {
//            return Lists.newArrayList();
//        }
//
//        String[] ancestors = path.substring(1).split(PATH_SEP); // Skip first delimiter, root team first
//        int lastIndex = ancestors.length - 2; // Skip this team
//        List<String> uids = Lists.newArrayList();
//
//        for (int i = lastIndex; i >= 0; i--) {
//            String uid = ancestors[i];
//            uids.add(0, uid);
//
//            if (rootUids != null && rootUids.contains(uid)) {
//                break;
//            }
//        }
//
//        return uids;
//    }

//    /**
//     * Returns a string representing the graph of ancestors. The string is delimited
//     * by "/". The ancestors are ordered by root first and represented by UIDs.
//     *
//     * @param roots the root teams, if null using real roots.
//     */
//    public String getParentGraph(Collection<Team> roots) {
//        Set<String> rootUids = roots != null ? Sets.newHashSet(String.valueOf(IdentifiableObjectUtils.getUids(roots))) : null;
//        List<String> ancestors = getAncestorUids(rootUids);
//        return StringUtils.join(ancestors, PATH_SEP);
//    }
//
//    /**
//     * Returns a string representing the graph of ancestors. The string is delimited
//     * by ",". The ancestors are ordered by root first and represented by names.
//     *
//     * @param roots       the root teams, if null using real roots.
//     * @param includeThis whether to include this team in the graph.
//     */
//    public String getParentNameGraph(Collection<Team> roots, boolean includeThis) {
//        StringBuilder builder = new StringBuilder();
//
//        List<Team> ancestors = getAncestors(roots);
//
//        for (Team team : ancestors) {
//            builder.append(PATH_SEP).append(team.getName());
//        }
//
//        if (includeThis) {
//            builder.append(PATH_SEP).append(name);
//        }
//
//        return builder.toString();
//    }
//
//    /**
//     * Returns a mapping between the uid and the uid parent graph of the given
//     * teams.
//     */
//    public static Map<String, String> getParentGraphMap(List<Team> teams, Collection<Team> roots) {
//        Map<String, String> map = new HashMap<>();
//
//        if (teams != null) {
//            for (Team team : teams) {
//                map.put(team.getUid(), team.getParentGraph(roots));
//            }
//        }
//
//        return map;
//    }
//
//    public Set<Team> getChildren() {
//        return children;
//    }
//
//    public void setChildren(Set<Team> children) {
//        this.children = children;
//    }
//
//    //    @JsonProperty(value = "level", access = JsonProperty.Access.READ_ONLY)
//    public Integer getLevel() {
//        return StringUtils.countMatches(path, PATH_SEP);
//    }
//
//    // for Hibernate
//    public void setLevel(Integer ouLevel) {
//        //this.level = ouLevel;
//    }

//    /**
//     * Used by persistence layer. Purpose is to have a column for use in database
//     * queries. For application use see {@link Team#getLevel()} which
//     * has better performance.
//     */
//    public Integer getHierarchyLevel() {
//        Set<String> uids = Sets.newHashSet(uid);
//
//        Team current = this;
//
//        while ((current = current.getParent()) != null) {
//            boolean add = uids.add(current.getUid());
//
//            if (!add) {
//                break; // Protect against cyclic org team graphs
//            }
//        }
//
//        hierarchyLevel = uids.size();
//
//        return hierarchyLevel;
//    }
//
//    public Team hierarchyLevel(Integer hierarchyLevel) {
//        this.setHierarchyLevel(hierarchyLevel);
//        return this;
//    }

    //    /**
//     * Returns the list of ancestor teams for this team.
//     * Does not include itself. The list is ordered by root first.
//     *
//     * @throws IllegalStateException if circular parent relationships is detected.
//     */
////    @JsonProperty("ancestors")
//    @JsonSerialize(contentAs = Identifiable.class)
//    public List<Team> getAncestors() {
//        List<Team> teams = new ArrayList<>();
//        Set<Team> visitedTeams = new HashSet<>();
//
//        Team team = parent;
//
//        while (team != null) {
//            if (!visitedTeams.add(team)) {
//                throw new IllegalStateException(
//                    "Team '" + this.toString() + "' has circular parent relationships: '" + team + "'"
//                );
//            }
//
//            teams.add(team);
//            team = team.getParent();
//        }
//
//        Collections.reverse(teams);
//        return teams;
//    }
//
//    /**
//     * Returns the list of ancestor teams up to any of the given roots
//     * for this team. Does not include itself. The list is ordered by
//     * root first.
//     *
//     * @param roots the root teams, if null using real roots.
//     */
//    public List<Team> getAncestors(Collection<Team> roots) {
//        List<Team> teams = new ArrayList<>();
//        Team team = parent;
//
//        while (team != null) {
//            teams.add(team);
//
//            if (roots != null && roots.contains(team)) {
//                break;
//            }
//
//            team = team.getParent();
//        }
//
//        Collections.reverse(teams);
//        return teams;
//    }
//
//    //    public String getPath() {
////        return path;
////    }
//    public String getPath() {
//        List<String> pathList = new ArrayList<>();
//        Set<String> visitedSet = new HashSet<>();
//        Team team = parent;
//
//        pathList.add(uid);
//
//        while (team != null) {
//            if (!visitedSet.contains(team.getUid())) {
//                pathList.add(team.getUid());
//                visitedSet.add(team.getUid());
//                team = team.getParent();
//            } else {
//                team = null; // Protect against cyclic org unit graphs
//            }
//        }
//
//        Collections.reverse(pathList);
//
//        this.path = PATH_SEP + StringUtils.join(pathList, PATH_SEP);
//
//        return this.path;
//    }
//
//    public void setPath(String path) {
//        this.path = path;
//    }
//
//    public void setHierarchyLevel(Integer hierarchyLevel) {
//        this.hierarchyLevel = hierarchyLevel;
//    }
//
//    public Team getParent() {
//        return parent;
//    }
//
//    public void setParent(Team parent) {
//        this.parent = parent;
//    }
//    public Warehouse getWarehouse() {
//        return this.warehouse;
//    }
//
//    public void setWarehouse(Warehouse warehouse) {
//        this.warehouse = warehouse;
//    }
//
//    public Team warehouse(Warehouse warehouse) {
//        this.setWarehouse(warehouse);
//        return this;
//    }
//
//    //    @JsonSerialize(contentAs = Identifiable.class)
//    @JsonIgnore
//    public User getUserInfo() {
//        return this.userInfo;
//    }
//
//    public void setUserInfo(User user) {
//        this.userInfo = user;
//    }
//
//    public Team userInfo(User user) {
//        this.setUserInfo(user);
//        return this;
//    }
    //////////////////

    @Override
    public String toString() {
        return "Team{" + "id=" + getId() + ", uid='" + getUid() + "'" + ", code='" + getCode() + "'" + ", name='" + getName() + "'" + ", description='" + getDescription() + "'" + ", disabled='" + getDisabled() + "'" + ", deleteClientData='" + getDeleteClientData() + "'" + ", createdBy='" + getCreatedBy() + "'" + ", createdDate='" + getCreatedDate() + "'" + ", lastModifiedBy='" + getLastModifiedBy() + "'" + ", lastModifiedDate='" + getLastModifiedDate() + "'" + "}";
    }
}
