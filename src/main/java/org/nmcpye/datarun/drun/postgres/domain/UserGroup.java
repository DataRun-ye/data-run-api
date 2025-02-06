package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A UserGroup.
 */
@Entity
@Table(name = "user_group")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserGroup extends BaseIdentifiableObject<Long> implements Serializable, Persistable<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true, nullable = false)
    private String uid;

    @NotNull
    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "disabled")
    private Boolean disabled;

    @Transient
    private boolean isPersisted;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_group_users",
        joinColumns = @JoinColumn(name = "user_group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties(value = {"teams", "password", "authorities", "groups", "managedGroups"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users = new HashSet<>();

    /////
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_group_managed_groups",
        joinColumns = @JoinColumn(name = "user_group_id"),
        inverseJoinColumns = @JoinColumn(name = "managed_group_id")
    )
    @JsonIgnoreProperties(value = {"managedGroups", "managedByGroups", "members",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<UserGroup> managedGroups = new HashSet<>();

    @ManyToMany(mappedBy = "managedGroups")
    @JsonIgnoreProperties(value = {"managedGroups", "managedByGroups", "members",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<UserGroup> managedByGroups = new HashSet<>();

    public UserGroup() {
        this.setAutoFields();
    }

    public UserGroup(String name) {
        this();
        this.name = name;
    }

    public UserGroup(String name, Set<User> users) {
        this(name);
        this.users = users;
    }

    public Long getId() {
        return this.id;
    }

    public UserGroup id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public UserGroup uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public UserGroup code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public UserGroup name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public UserGroup description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Boolean getDisabled() {
        return this.disabled;
    }

    public UserGroup disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
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

    public UserGroup setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public Set<User> getUsers() {
        return this.users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public UserGroup members(Set<User> users) {
        this.setUsers(users);
        return this;
    }

    public UserGroup addMember(User user) {
        this.users.add(user);
        user.getGroups().add(this);
        return this;
    }

    public UserGroup removeMember(User user) {
        this.users.remove(user);
        user.getGroups().remove(this);
        return this;
    }

    public Set<UserGroup> getManagedGroups() {
        return this.managedGroups;
    }

    public void setManagedGroups(Set<UserGroup> userGroups) {
        this.managedGroups = userGroups;
    }

    public UserGroup managedGroups(Set<UserGroup> userGroups) {
        this.setManagedGroups(userGroups);
        return this;
    }

    public UserGroup addManagedGroup(UserGroup userGroup) {
        this.managedGroups.add(userGroup);
        userGroup.getManagedByGroups().add(this);
        return this;
    }

    public UserGroup removeManagedGroup(UserGroup userGroup) {
        this.managedGroups.remove(userGroup);
        userGroup.getManagedByGroups().remove(this);
        return this;
    }

    public Set<UserGroup> getManagedByGroups() {
        return this.managedByGroups;
    }

    public void setManagedByGroups(Set<UserGroup> userGroups) {
        this.managedByGroups = userGroups;
    }

    public UserGroup managedByGroups(Set<UserGroup> userGroups) {
        this.setManagedByGroups(userGroups);
        return this;
    }

    public UserGroup addManagedByGroup(UserGroup userGroup) {
        this.managedByGroups.add(userGroup);
        userGroup.getManagedGroups().add(this);
        return this;
    }

    public UserGroup removeManagedByGroup(UserGroup userGroup) {
        this.managedByGroups.remove(userGroup);
        userGroup.getManagedGroups().remove(this);
        return this;
    }

}
