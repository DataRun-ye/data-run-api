package org.nmcpye.datarun.drun.postgres.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.drun.postgres.common.BaseIdentifiableObject;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;

import java.io.Serializable;
import java.util.Map;

/**
 * A OuLevel.
 */
@Entity
@Table(name = "data_element")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElement
    extends BaseIdentifiableObject<Long> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, unique = true, nullable = false)
    private String uid;

    @Column(name = "code", unique = true)
    private String code;

    @NotNull
    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", updatable = false, nullable = false)
    private ValueType type;

    @Size(max = 2000)
    @Column(name = "description")
    private String description;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "mandatory")
    private boolean mandatory;

    @ManyToOne
    @JsonIgnoreProperties(value = {"options"}, allowSetters = true)
    private OptionSet optionSet;

    /**
     * resourceType for ReferenceField type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private ReferenceType resourceType;

    /**
     * resourceMetadataSchema for ReferenceField type
     */
    @Column(name = "reference_metadata_schema")
    private String resourceMetadataSchema;

    /**
     * is gs1Enabled, for ScannedCodeField type
     */
    @Column(name = "gs1_enabled")
    private Boolean gs1Enabled;

    @Type(JsonType.class)
    @Column(name = "label", columnDefinition = "jsonb")
    private Map<String, String> label;

    /**
     * is properties, for ScannedCodeField type
     */
    @Type(JsonType.class)
    @Column(name = "properties", columnDefinition = "jsonb")
    private ScannedCodeProperties properties;

    public DataElement() {
        setAutoFields();
    }

    public Long getId() {
        return this.id;
    }

    public DataElement id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public DataElement uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public OptionSet getOptionSet() {
        return optionSet;
    }

    public void setOptionSet(OptionSet optionSet) {
        this.optionSet = optionSet;
    }

    public ReferenceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ReferenceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceMetadataSchema() {
        return resourceMetadataSchema;
    }

    public void setResourceMetadataSchema(String resourceMetadataSchema) {
        this.resourceMetadataSchema = resourceMetadataSchema;
    }

    public Boolean getGs1Enabled() {
        return gs1Enabled;
    }

    public void setGs1Enabled(Boolean gs1Enabled) {
        this.gs1Enabled = gs1Enabled;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public ScannedCodeProperties getProperties() {
        return properties;
    }

    public void setProperties(ScannedCodeProperties properties) {
        this.properties = properties;
    }
}
