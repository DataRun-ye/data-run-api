package org.nmcpye.datarun.drun.mongo.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.util.*;

/**
 * A DataForm.
 */
@Document(collection = "data_form")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataForm
    extends AbstractAuditingEntityMongo<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Size(max = 11)
    @NotNull
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
    private Boolean disabled;

    @Field("activity")
    private String activity;

    private Integer version;

    @Field("default_local")
    private String defaultLocal;

    @Field("fields")
    private Set<DataField> fields = new HashSet<>();

    @Field("options")
    private Set<DataOption> options = new HashSet<>();

    @Field("option_sets")
    private Set<OptionSet> optionSets = new HashSet<>();

    @Field("orgUnits")
//    @JsonSerialize(using = OrgUnitUidsSetSerializer.class)
    private Set<String> orgUnits = new HashSet<>();

    private Map<String, String> label;

    @Field("flattenedFields")
    private List<DataField> flattenedFields = new ArrayList<>();


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

    public boolean hasReferenceFields() {
        return flattenedFields.stream().anyMatch(DataField::ofReferenceType);
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

    public List<DataField> getFlattenedFields() {
        if (flattenedFields.isEmpty()) {
            return flattenFields();
        }

        return flattenedFields;
    }

    public void setFlattenedFields(List<DataField> flattenedFields) {
        this.flattenedFields = flattenedFields;
    }

    public Set<String> getOrgUnits() {
        return orgUnits;
    }

    public void setOrgUnits(Set<String> orgUnits) {
        this.orgUnits = orgUnits;
    }

    public DataForm orgUnits(Set<String> orgUnits) {
        this.setOrgUnits(orgUnits);
        return this;
    }

    public DataForm addOrgUnit(String orgUnits) {
        this.orgUnits.add(orgUnits);
        return this;
    }

    public DataForm removeOrgUnit(String orgUnits) {
        this.orgUnits.remove(orgUnits);
        return this;
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

    public Set<DataOption> getOptions() {
        return options;
    }

    public void setOptions(Set<DataOption> options) {
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
        return this.disabled;
    }

    public DataForm disabled(Boolean disabled) {
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

    public DataForm fields(Set<DataField> dataFields) {
        this.setFields(dataFields);
        return this;
    }

    public DataForm addField(DataField dataField) {
        this.fields.add(dataField);
        return this;
    }

    public DataForm removeField(DataField dataField) {
        this.fields.remove(dataField);
        return this;
    }

    public String getActivity() {
        return this.activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public DataForm activity(String activityId) {
        this.setActivity(activityId);
        return this;
    }

//    /**
//     * Flattens all DataFields into a single list.
//     */
//    public void flattenFields() {
//        this.flattenedFields = flattenFieldsRecursively(fields, "");
//    }

//    private List<DataField> flattenFieldsRecursively(Set<DataField> fields, String parentPath) {
//        List<DataField> flatList = new ArrayList<>();
//        for (DataField field : fields) {
//            // Generate the full path for the field
//            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + DataField.PATH_SEP + field.getName();
//            field.setPath(currentPath);
//            flatList.add(field);
//
//            // Recursively process nested fields
//            if (field.getFields() != null && !field.getFields().isEmpty()) {
//                flatList.addAll(flattenFieldsRecursively(field.getFields(), currentPath));
//            }
//        }
//        return flatList;
//    }

//    private List<DataField> flattenFieldsRecursively(Set<DataField> fields, String parentPath) {
//        List<DataField> flatList = new ArrayList<>();
//
//        for (DataField field : fields) {
//            // Determine the current path: Start with the field name if parentPath is null or empty
//            String currentPath = (parentPath == null || parentPath.isEmpty())
//                ? field.getName()
//                : parentPath + DataField.PATH_SEP + field.getName();
//
//            field.setPath(currentPath);
//            flatList.add(field);
//
//            // Recursively process nested fields if present
//            if (field.getFields() != null && !field.getFields().isEmpty()) {
//                flatList.addAll(flattenFieldsRecursively(field.getFields(), currentPath));
//            }
//        }
//
//        return flatList;
//    }

//
//    public List<DataField> getFlattenFields() {
//        List<DataField> flatList = new ArrayList<>();
//        for (DataField field : fields) {
//            flattenField(field, flatList);
//        }
//        return flatList;
//    }


//    private void flattenField(DataField field, List<DataField> flatList) {
//        flatList.add(field);
//        for (DataField nestedField : field.getFields()) {
//            flattenField(nestedField, flatList);
//        }
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataForm dataForm = (DataForm) o;
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
