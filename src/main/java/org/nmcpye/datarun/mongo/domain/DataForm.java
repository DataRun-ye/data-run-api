package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.FieldDeserializer;
import org.nmcpye.datarun.mongo.domain.datafield.Repeat;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

/**
 * A DataForm.
 */
@Document(collection = "data_form")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataForm
    extends AbstractAuditingEntityMongo<String> {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "form_uid")
    private String uid;

    @Field("code")
    @Indexed(unique = true, name = "form_code")
    private String code;

    @NotNull
    @Field("name")
    @Indexed(unique = true, name = "form_name")
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    @Field("disabled")
    private boolean disabled;

    @Field("deleted")
    private boolean deleted;

    private Integer version;

    @Field("defaultLocal")
    private String defaultLocal;

    @Field("fields")
    @JsonDeserialize(contentUsing = FieldDeserializer.class)
    private List<AbstractField> fields = new ArrayList<>();

    @Field("options")
    private List<DataOption> options = new ArrayList<>();

    @DocumentReference
    @Field("optionSets")
    private List<OptionSet> optionSets = new ArrayList<>();

    private Map<String, String> label;

    @Field("flattenedFields")
    private List<AbstractField> flattenedFields = new ArrayList<>();

    // field.uid, fieldConfigs
    @Field("fieldsConf")
    private List<FormDataElementConf> fieldsConf = new ArrayList<>();

    @Field("sections")
    private List<FormSectionConf> sections = new ArrayList<>();

//    public DataForm() {
//        setAutoFields();
//    }

    @PrePersist
    @PreUpdate
    public void updateFlattenedFields() {
        this.flattenedFields = this.flattenFields();
    }

    /**
     * Flattens the hierarchical structure of fields in the DataForm.
     * This method traverses through all fields, including nested fields within Repeat and Section instances,
     * and creates a flat list of all fields.
     *
     * @return A List of AbstractField objects representing all fields in the form, including nested fields,
     * in a flattened structure.
     */
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

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public List<OptionSet> getOptionSets() {
        return optionSets;
    }

    public void setOptionSets(List<OptionSet> optionSets) {
        this.optionSets = optionSets;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of(getDefaultLocal(), this.name));
    }

    public Map<String, String> getLabel() {
        return Optional.ofNullable(this.label).orElse(Map.of(getDefaultLocal(), this.name));
    }

    public List<AbstractField> getFlattenedFields() {
        if (flattenedFields.isEmpty()) {
            return flattenFields();
        }

        return flattenedFields;
    }

    public void setFlattenedFields(List<AbstractField> flattenedFields) {
        this.flattenedFields = flattenedFields;
    }

    public List<FormDataElementConf> getFieldsConf() {
        return fieldsConf;
    }

    public void setFieldsConf(List<FormDataElementConf> fieldsConf) {
        this.fieldsConf = fieldsConf;
    }

    public List<FormSectionConf> getSections() {
        return sections;
    }

    public void setSections(List<FormSectionConf> sections) {
        this.sections = sections;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDefaultLocal() {
        return Optional.ofNullable(defaultLocal).orElse("en");
    }

    public void setDefaultLocal(String defaultLocal) {
        this.defaultLocal = Objects.requireNonNullElse(defaultLocal, "en");
    }

    public List<DataOption> getOptions() {
        return options;
    }

    public void setOptions(List<DataOption> options) {
        this.options = options;
    }

    public DataForm addOption(DataOption option) {
        this.options.add(option);
        return this;
    }

    public DataForm removeOption(DataOption option) {
        this.options.remove(option);
        return this;
    }

    public String getId() {
        return this.id;
    }

    public DataForm id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public DataForm uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getCode() {
        return this.code;
    }

    public DataForm code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public DataForm name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public DataForm description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDisabled() {
        return Optional.ofNullable(this.disabled).orElse(false);
    }

    public DataForm disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = Optional.ofNullable(disabled).orElse(false);
    }

    public List<AbstractField> getFields() {
        return this.fields;
    }

    public void setFields(List<AbstractField> fields) {
        this.fields = fields;
    }

    public DataForm fields(List<AbstractField> dataFields) {
        this.setFields(dataFields);
        return this;
    }

    public DataForm addField(AbstractField dataField) {
        this.fields.add(dataField);
        return this;
    }

    public DataForm removeField(AbstractField dataField) {
        this.fields.remove(dataField);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataForm dataForm = (DataForm) o;
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
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
