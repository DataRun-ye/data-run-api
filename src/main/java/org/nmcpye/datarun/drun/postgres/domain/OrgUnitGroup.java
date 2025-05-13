package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;

import java.util.HashSet;
import java.util.Set;

/**
 * A OrgUnitGroup.
 */
@Entity
@Table(name = "org_unit_group", indexes = {
    @Index(name = "idx_orgunitgroup_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_org_unit_group_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_org_unit_group_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnitGroup extends JpaBaseIdentifiableObject {
    @Column(name = "symbol")
    private String symbol;

    @Column(name = "color")
    private String color;

    @Column(name = "inactive")
    private Boolean inactive = false;

    @ManyToMany
    @JoinTable(
        name = "org_unit_group_members",
        joinColumns = @JoinColumn(name = "org_unit_group_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"parent", "children", "orgUnitGroups", "assignments", "hierarchyLevel", "ancestors", "translations", "path"}, allowSetters = true)
    private Set<OrgUnit> orgUnits = new HashSet<>();

    @ManyToMany(mappedBy = "orgUnitGroups")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"orgUnitGroups", "translations"}, allowSetters = true)
    private Set<OrgUnitGroupSet> orgUnitGroupSets = new HashSet<>();

    @JsonProperty
    public String getColor() {
        return this.color;
    }

    @JsonProperty
    public Boolean getInactive() {
        return this.inactive;
    }
}
