package org.nmcpye.datarun.mongo.domain.dataelement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.mongo.domain.AbstractAuditingEntityMongo;
import org.nmcpye.datarun.mongo.domain.datafield.ScannedCodeProperties;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;
import java.util.Objects;

@Document(collection = "data_element")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataElement extends AbstractAuditingEntityMongo<String> {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "data_element_uid")
    private String uid;

    @Field("code")
    @Indexed(unique = true, name = "data_element_code")
    private String code;

    @NotNull
    @Field("name")
    @Indexed(unique = true, name = "data_element_name")
    private String name;

    @NotNull
    @Field("type")
    private ValueType type;

    @Size(max = 2000)
    @Field("description")
    private String description;

    private Map<String, String> label;

    private Object defaultValue;

    private boolean mandatory;

    @Field("optionSet")
//    @DocumentReference(lookup = "{ 'uid' : ?#{#target} }")
    private String optionSet;


    /**
     * is gs1Enabled, for ScannedCodeField type
     */
    private Boolean gs1Enabled;

    /**
     * is properties, for ScannedCodeField type
     */
    private ScannedCodeProperties properties;

    /**
     * resourceType for ReferenceField type
     */
    @Field("resourceType")
    private ReferenceType resourceType;

    // TODO rename to referenceMetadataSchema
    /**
     * resourceMetadataSchema for ReferenceField type
     */
    @Field("resourceMetadataSchema")
    private String resourceMetadataSchema;

    public DataElement() {
        setAutoFields();
        if (getCode() == null || getCode().isEmpty()) {
            setCode(getUid());
        }
    }

    @JsonIgnore
    public String getId() {
        return id;
    }

    @JsonIgnore
    public void setId(String id) {
        this.id = id;
    }

    public DataElement id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public String getUid() {
        return this.uid;
    }

    @Override
    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getOptionSet() {
        return optionSet;
    }

    public void setOptionSet(String optionSet) {
        this.optionSet = optionSet;
    }

    public Boolean getGs1Enabled() {
        return gs1Enabled;
    }

    public void setGs1Enabled(Boolean gs1Enabled) {
        this.gs1Enabled = gs1Enabled;
    }

    public ScannedCodeProperties getProperties() {
        return properties;
    }

    public void setProperties(ScannedCodeProperties properties) {
        this.properties = properties;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataElement that)) return false;
        return Objects.equals(getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUid());
    }
}
