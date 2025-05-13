package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.domain.User;

import java.util.HashSet;
import java.util.Set;

/**
 * A UserGroup.
 */
@Entity
@Table(name = "user_group", indexes = {
    @Index(name = "idx_usergroup_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_user_group_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_user_group_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class UserGroup extends JpaBaseIdentifiableObject {

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
    @JsonIgnoreProperties(value = {"teams", "password", "authorities", "userGroups", "managedGroups", "managedByGroups"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users = new HashSet<>();

    /////
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_group_managed_groups",
        joinColumns = @JoinColumn(name = "user_group_id"),
        inverseJoinColumns = @JoinColumn(name = "managed_group_id")
    )
    @JsonIgnoreProperties(value = {"managedGroups", "managedByGroups", "users",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<UserGroup> managedGroups = new HashSet<>();

    @ManyToMany(mappedBy = "managedGroups")
    @JsonIgnoreProperties(value = {"managedGroups", "managedByGroups", "users",
        "createdBy", "createdDate", "lastModifiedDate", "lastModifiedBy"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<UserGroup> managedByGroups = new HashSet<>();

    public UserGroup addMember(User user) {
        this.users.add(user);
        user.getUserGroups().add(this);
        return this;
    }

    public UserGroup removeMember(User user) {
        this.users.remove(user);
        user.getUserGroups().remove(this);
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
