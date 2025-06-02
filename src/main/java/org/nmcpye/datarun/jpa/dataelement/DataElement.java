package org.nmcpye.datarun.jpa.dataelement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.dataelementgroup.DataElementGroup;
import org.nmcpye.datarun.jpa.optionset.OptionSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A OuLevel.
 */
@Entity
@Table(name = "data_element", uniqueConstraints = {
    @UniqueConstraint(name = "uc_data_element_uid", columnNames = "name"),
    @UniqueConstraint(name = "uc_data_element_name", columnNames = "name"),
    @UniqueConstraint(name = "uc_data_element_code", columnNames = "code")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElement extends JpaBaseIdentifiableObject {
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

    @Column(name = "short_name", length = 50)
    protected String shortName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", updatable = false, nullable = false)
    private ValueType type;

    @Size(max = 2000)
    @Column(name = "description")
    private String description;

    @ManyToOne
    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
    @JoinColumn(updatable = false)
    private OptionSet optionSet;

    /**
     * resourceType for ReferenceField type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", updatable = false)
    private ReferenceType resourceType;

    @ManyToMany(mappedBy = "dataElements")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"dataElementGroupSets", "dataElements", "translations"}, allowSetters = true)
    private Set<DataElementGroup> dataElementGroups = new HashSet<>();

    public void setLabel(Map<String, String> label) {
        final var translations = label.entrySet().stream()
            .map(entry -> Translation
                .builder()
                .locale(entry.getKey())
                .property("name")
                .value(entry.getValue()).build()).collect(Collectors.toSet());
        setTranslations(translations);
    }

    @JsonProperty
    public Map<String, String> getLabel() {
        return translations
            .stream()
            .collect(Collectors
                .toMap(Translation::getLocale, Translation::getValue));
    }
}
