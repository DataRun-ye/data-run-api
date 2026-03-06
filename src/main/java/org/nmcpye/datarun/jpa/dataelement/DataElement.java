package org.nmcpye.datarun.jpa.dataelement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.common.TranslatableInterface;
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
public class DataElement extends BaseDataElement implements TranslatableInterface {
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

    /**
     * Set of available object translation, normally filtered by locale.
     */
    @Type(JsonType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    protected Set<Translation> translations = new HashSet<>();

}
