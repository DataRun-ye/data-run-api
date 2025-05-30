package org.nmcpye.datarun.orgunitgroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.orgunit.OrgUnit;
import org.nmcpye.datarun.orgunitgroupset.OrgUnitGroupSet;

import java.util.HashSet;
import java.util.Set;

/**
 * A OrgUnitGroup.
 */
@Entity
@Table(name = "org_unit_group", uniqueConstraints = {
    @UniqueConstraint(name = "uc_org_unit_group_uid", columnNames = "uid"),
    @UniqueConstraint(name = "uc_org_unit_group_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_org_unit_group_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class OrgUnitGroup extends JpaBaseIdentifiableObject {
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
