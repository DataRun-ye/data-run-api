package org.nmcpye.datarun.jpa.dataelementgroup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.TranslatableIdentifiable;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.dataelementgroupset.DataElementGroupSet;

import java.util.HashSet;
import java.util.Set;

/**
 * A DataElementGroup.
 */
@Entity
@Table(name = "data_element_group")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElementGroup extends TranslatableIdentifiable {

    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true)
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
        name = "data_element_group_members",
        joinColumns = @JoinColumn(name = "group_id"),
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
