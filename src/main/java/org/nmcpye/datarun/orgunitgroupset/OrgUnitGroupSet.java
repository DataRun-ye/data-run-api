package org.nmcpye.datarun.orgunitgroupset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.orgunit.OrgUnit;
import org.nmcpye.datarun.orgunitgroup.OrgUnitGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * A OrgUnitGroupSet.
 */
@Entity
@Table(name = "org_unit_groupset", uniqueConstraints = {
    @UniqueConstraint(name = "uc_org_unit_groupset_uid", columnNames = "uid"),
    @UniqueConstraint(name = "uc_org_unit_groupset_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_org_unit_groupset_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class OrgUnitGroupSet extends JpaBaseIdentifiableObject {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code")
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false)
    protected String name;

    @ManyToMany
    @JoinTable(
        name = "org_unit_groupset_org_unit_group",
        joinColumns = @JoinColumn(name = "org_unit_groupset_id"),
        inverseJoinColumns = @JoinColumn(name = "org_unit_group_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"orgUnitGroupSets", "orgUnits"}, allowSetters = true)
    private Set<OrgUnitGroup> orgUnitGroups = new HashSet<>();

    public boolean hasOrgUnitGroups() {
        return orgUnitGroups != null && !orgUnitGroups.isEmpty();
    }

    public boolean isMemberOfOrgUnitGroups(OrgUnit organisationUnit) {
        for (OrgUnitGroup group : orgUnitGroups) {
            if (group.getOrgUnits().contains(organisationUnit)) {
                return true;
            }
        }

        return false;
    }

    public OrgUnitGroupSet addOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        this.orgUnitGroups.add(orgUnitGroup);
        orgUnitGroup.getOrgUnitGroupSets().add(this);
        return this;
    }

    public OrgUnitGroupSet removeOrgUnitGroup(OrgUnitGroup orgUnitGroup) {
        this.orgUnitGroups.remove(orgUnitGroup);
        orgUnitGroup.getOrgUnitGroupSets().remove(this);
        return this;
    }
}
