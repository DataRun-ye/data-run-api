package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.MetadataResourceType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.*;

/**
 * A DataForm.
 */
@Document(collection = "metadata_schema")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MetadataSchema
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "schema_uid")
    private String uid;

    @NotNull
    @Field("name")
    @Indexed(unique = true, name = "schema_name")
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    @Field("disabled")
    private Boolean disabled;

    @NotNull
    @Field("resourceType")
    private MetadataResourceType resourceType;

    private Integer version;

    @Field("default_local")
    private String defaultLocal;

    @Field("fields")
    private Set<DataField> fields = new HashSet<>();

    @Field("option_sets")
    private Set<OptionSet> optionSets = new HashSet<>();

    private Map<String, String> label;
    @Field("flattenedFields")
    private List<DataField> flattenedFields = new ArrayList<>();

    public List<DataField> getFlattenedFields() {
        if(flattenedFields.isEmpty()) {
            return flattenFields();
        }

        return flattenedFields;
    }

    public void setFlattenedFields(List<DataField> flattenedFields) {
        this.flattenedFields = flattenedFields;
    }

    @PrePersist
    @PreUpdate
    public void updateFlattenedFields() {
        this.flattenedFields = this.flattenFields();
    }

    public List<DataField> flattenFields() {
        List<DataField> flatList = new ArrayList<>();
        for (DataField field : this.fields) {
            flatList.add(field);
            if (field.getFields() != null && !field.getFields().isEmpty()) {
                flatList.addAll(field.flattenFields());
            }
        }
        return flatList;
    }

    public Set<OptionSet> getOptionSets() {
        return optionSets;
    }

    public void setOptionSets(Set<OptionSet> optionSets) {
        this.optionSets = optionSets;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDefaultLocal() {
        return defaultLocal;
    }

    public void setDefaultLocal(String defaultLocal) {
        this.defaultLocal = Objects.requireNonNullElse(defaultLocal, "en");
    }

    public String getId() {
        return this.id;
    }

    public MetadataSchema id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public MetadataSchema uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return this.name;
    }

    public MetadataSchema name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public MetadataSchema description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDisabled() {
        return this.disabled;
    }

    public MetadataSchema disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Set<DataField> getFields() {
        return this.fields;
    }

    public void setFields(Set<DataField> dataFields) {
        this.fields = dataFields;
    }

    public MetadataSchema fields(Set<DataField> dataFields) {
        this.setFields(dataFields);
        return this;
    }

    public MetadataSchema addField(DataField dataField) {
        this.fields.add(dataField);
        return this;
    }

    public MetadataSchema removeField(DataField dataField) {
        this.fields.remove(dataField);
        return this;
    }

    public MetadataResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(MetadataResourceType resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataSchema dataForm = (MetadataSchema) o;
        return getUid().equals(dataForm.getUid());
    }

    @Override
    public int hashCode() {
        return getUid().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataForm{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
