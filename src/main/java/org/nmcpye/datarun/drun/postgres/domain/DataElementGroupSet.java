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
 * A DataElementGroupSet.
 */
@Entity
@Table(name = "data_element_groupset", indexes = {
    @Index(name = "idx_data_element_groupset_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_data_element_groupset_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_data_element_groupset_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElementGroupSet extends JpaBaseIdentifiableObject {
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
