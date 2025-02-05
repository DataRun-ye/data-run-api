package org.nmcpye.datarun.mongo.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.Repeat;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    // TODO rename to referenceType
    @NotNull
    @Field("resourceType")
    private ReferenceType resourceType;

    private Integer version;

    @Field("default_local")
    private String defaultLocal;

    @Field("fields")
    private List<AbstractField> fields = new ArrayList<>();

    @Field("option_sets")
    private List<OptionSet> optionSets = new ArrayList<>();

    private Map<String, String> label;
    @Field("flattenedFields")
    private List<AbstractField> flattenedFields = new ArrayList<>();

    public List<AbstractField> getFlattenedFields() {
        if (flattenedFields.isEmpty()) {
            return flattenFields();
        }

        return flattenedFields;
    }

    public void setFlattenedFields(List<AbstractField> flattenedFields) {
        this.flattenedFields = flattenedFields;
    }

    @PrePersist
    @PreUpdate
    public void updateFlattenedFields() {
        this.flattenedFields = this.flattenFields();
    }

    public List<AbstractField> flattenFields() {
        List<AbstractField> flatList = new ArrayList<>();
        for (AbstractField field : this.fields) {
            flatList.add(field);
            if (field instanceof Repeat repeatSection) {
                flatList.addAll(flattenSectionFields(repeatSection.getFields()));
            } else if (field instanceof Section section) {
                flatList.addAll(flattenSectionFields(section.getFields()));
            }
        }
        return flatList;
    }

    private List<AbstractField> flattenSectionFields(List<AbstractField> fields) {
        List<AbstractField> flatList = new ArrayList<>();
        for (AbstractField field : fields) {
            flatList.add(field);
            if (field instanceof Repeat repeatSection) {
                flatList.addAll(flattenSectionFields(repeatSection.getFields()));
            } else if (field instanceof Section section) {
                flatList.addAll(flattenSectionFields(section.getFields()));
            }
        }
        return flatList;
    }

    public List<OptionSet> getOptionSets() {
        return optionSets;
    }

    public void setOptionSets(List<OptionSet> optionSets) {
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

    public List<AbstractField> getFields() {
        return this.fields;
    }

    public void setFields(List<AbstractField> dataFields) {
        this.fields = dataFields;
    }

    public MetadataSchema addField(AbstractField dataField) {
        this.fields.add(dataField);
        return this;
    }

    public MetadataSchema removeField(AbstractField dataField) {
        this.fields.remove(dataField);
        return this;
    }

    public ReferenceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ReferenceType resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataSchema dataForm = (MetadataSchema) o;
        return (id != null && id.equals(dataForm.id)) ||
            (uid != null && uid.equals(dataForm.uid));
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : (uid != null ? uid.hashCode() : 0);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataForm{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
//            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
