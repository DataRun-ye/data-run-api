package org.nmcpye.datarun.jpa.dataelement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.jpa.option.OptionSet;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Hamza Assada
 * @since 08/02/2024
 */
@Entity
@Table(name = "data_element")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElement extends BaseDataElement {
    /**
     * Type of Value (e.g, Text, Number, Integer, OrgUnit, Entity, Team, Date, Coordinates)
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", updatable = false, nullable = false)
    @JsonProperty(value = "type")
    protected ValueType valueType;

    /**
     * an option set groups a predefined JSONP List of options, used when
     * the dataType is of type `Select` or =null otherwise.
     */
    @ManyToOne
    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
    @JoinColumn(updatable = false)
    protected OptionSet optionSet;

    /**
     * resourceType for ReferenceField type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", updatable = false)
    private ReferenceType resourceType;


    @ManyToMany(mappedBy = "dataElements")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"dataElementGroupSets", "dataElements", "translations"}, allowSetters = true)
    protected Set<DataElementGroup> dataElementGroups = new HashSet<>();

    @JsonIgnore
    @Column(name = "is_measure")
    private Boolean isMeasure = false;

    @JsonIgnore
    @Column(name = "is_dimension")
    private Boolean isDimension = false;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type")
    private AggregationType aggregationType = AggregationType.DEFAULT;

}
