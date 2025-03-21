package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.util.Map;

/**
 * A OuLevel.
 */
@Entity
@Table(name = "data_element", indexes = {
    @Index(name = "idx_dataelement_uid_unq", columnList = "uid", unique = true)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElement extends JpaBaseIdentifiableObject {

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

    @Type(JsonType.class)
    @Column(name = "label", columnDefinition = "jsonb")
    private Map<String, String> label;

    /**
     * resourceType for ReferenceField type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", updatable = false)
    private ReferenceType resourceType;
}
