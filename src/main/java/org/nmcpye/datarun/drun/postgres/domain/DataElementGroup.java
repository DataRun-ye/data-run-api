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
 * A DataElementGroup.
 */
@Entity
@Table(name = "data_element_group", indexes = {
    @Index(name = "idx_data_element_group_uid_unq", columnList = "uid", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_data_element_group_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_data_element_group_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElementGroup extends JpaBaseIdentifiableObject {
    @ManyToMany
    @JoinTable(
        name = "data_element_group_members",
        joinColumns = @JoinColumn(name = "data_element_group_id"),
        inverseJoinColumns = @JoinColumn(name = "data_element_id")
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"parent", "children", "dataElementGroups", "assignments", "hierarchyLevel", "ancestors", "translations", "path"}, allowSetters = true)
    private Set<DataElement> dataElements = new HashSet<>();

    @ManyToMany(mappedBy = "dataElementGroups")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"dataElementGroups", "translations"}, allowSetters = true)
    private Set<DataElementGroupSet> dataElementGroupSets = new HashSet<>();
}
