package org.nmcpye.datarun.jpa.dataelementgroupset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * A DataElementGroupSet.
 */
@Entity
@Table(name = "data_element_groupset", uniqueConstraints = {
    @UniqueConstraint(name = "uc_data_element_groupset_uid", columnNames = "uid"),
    @UniqueConstraint(name = "uc_data_element_groupset_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_data_element_groupset_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class DataElementGroupSet extends JpaBaseIdentifiableObject {
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
        name = "data_element_groupset_groups",
        joinColumns = @JoinColumn(name = "data_element_groupset_id"),
        inverseJoinColumns = @JoinColumn(name = "data_element_group_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"dataElementGroupSets", "dataElements"}, allowSetters = true)
    private Set<DataElementGroup> dataElementGroups = new HashSet<>();

    public boolean hasOrgUnitGroups() {
        return dataElementGroups != null && !dataElementGroups.isEmpty();
    }

    public boolean isMemberOfOrgUnitGroups(DataElement dataElement) {
        for (DataElementGroup group : dataElementGroups) {
            if (group.getDataElements().contains(dataElement)) {
                return true;
            }
        }

        return false;
    }

    public DataElementGroupSet addOrgUnitGroup(DataElementGroup dataElementGroup) {
        this.dataElementGroups.add(dataElementGroup);
        dataElementGroup.getDataElementGroupSets().add(this);
        return this;
    }

    public DataElementGroupSet removeOrgUnitGroup(DataElementGroup dataElementGroup) {
        this.dataElementGroups.remove(dataElementGroup);
        dataElementGroup.getDataElementGroupSets().remove(this);
        return this;
    }
}
