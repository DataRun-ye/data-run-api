package org.nmcpye.datarun.jpa.entityattribute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.optionset.OptionSet;

/**
 * A OuLevel.
 */
@Entity
@Table(name = "entity_attribute")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EntityAttributeType extends JpaBaseIdentifiableObject {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "short_name", length = 50)
    protected String shortName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", updatable = false, nullable = false)
    private ValueType valueType;

    @Size(max = 2000)
    @Column(name = "description")
    private String description;

    @ManyToOne
    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
    @JoinColumn(updatable = false)
    private OptionSet optionSet;

    @Column(name = "display_when_planned")
    private Boolean displayOnNonSource = false;

//    @Column(name = "generated")
//    private Boolean generated = false;
//
//    @Column(name = "unique_value")
//    private Boolean uniqueValue = false;
//
//    @Column(name = "orgunitscope")
//    private Boolean orgUnitScope = false;
//
//    public Boolean isSystemWideUnique() {
//        return getUniqueValue() && !getOrgUnitScope();
//    }
}
