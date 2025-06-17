package org.nmcpye.datarun.jpa.entityattribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.dataelement.BaseDataElement;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.optionset.OptionSet;

/**
 * Definition of an attribute type (id, type, required); used by EntityInstance.
 * This is primarily metadata, but can be a persistent entity if attribute definitions are dynamic.
 *
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "entity_attribute")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EntityAttributeType extends BaseDataElement {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", updatable = false, nullable = false)
    private ValueType valueType;

    @Column(nullable = false)
    private Boolean mandatory = false;

    @ManyToOne
    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
    @JoinColumn(updatable = false)
    private OptionSet optionSet;

    @Column(name = "display_on_planned")
    private Boolean displayOnPlanned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_type_id", nullable = false)
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    private EntityType entityType;
}
