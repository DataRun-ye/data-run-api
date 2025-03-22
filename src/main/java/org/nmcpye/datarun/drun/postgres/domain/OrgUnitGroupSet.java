package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;

import java.util.HashSet;
import java.util.Set;

/**
 * A OrgUnitGroupSet.
 */
@Entity
@Table(name = "org_unit_groupset", indexes = {
    @Index(name = "idx_orgunitgroupset_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_org_unit_groupset_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_org_unit_groupset_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnitGroupSet extends JpaBaseIdentifiableObject {
    @ManyToMany
    @JoinTable(
        name = "org_unit_groupset_org_unit_group",
        joinColumns = @JoinColumn(name = "org_unit_groupset_id"),
        inverseJoinColumns = @JoinColumn(name = "org_unit_group_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"groupSets", "members"}, allowSetters = true)
//    @JsonSerialize(contentAs = IdentifiableObject.class)
    private Set<OrgUnitGroup> orgUnitGroups = new HashSet<>();

    public boolean hasOrgUnitGroups() {
        return orgUnitGroups != null && orgUnitGroups.size() > 0;
    }

    public boolean isMemberOfOrgUnitGroups(OrgUnit organisationUnit) {
        for (OrgUnitGroup group : orgUnitGroups) {
            if (group.getMembers().contains(organisationUnit)) {
                return true;
            }
        }

        return false;
    }

    public OrgUnitGroupSet addOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        this.orgUnitGroups.add(orgUnitGroup);
        orgUnitGroup.getGroupSets().add(this);
        return this;
    }

    public OrgUnitGroupSet removeOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        this.orgUnitGroups.remove(orgUnitGroup);
        orgUnitGroup.getGroupSets().remove(this);
        return this;
    }
}
