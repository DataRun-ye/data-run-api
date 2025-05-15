package org.nmcpye.datarun.mongo.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.FieldDeserializer;
import org.nmcpye.datarun.mongo.domain.datafield.Repeat;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

/**
 * A DataForm.
 */
@Document(collection = "data_form")
@Getter
@Setter
@CompoundIndex(name = "form_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataForm
    extends MongoBaseIdentifiableObject {
    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
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

    @Field("deleted")
    private boolean deleted;

    private Integer version;

    @Field("defaultLocal")
    private String defaultLocal;

    @Field("fields")
    @JsonDeserialize(contentUsing = FieldDeserializer.class)
    private List<AbstractField> fields = new LinkedList<>();

    @Field("options")
    private List<DataOption> options = new LinkedList<>();

    @DocumentReference
    @Field("optionSets")
    private List<OptionSet> optionSets = new LinkedList<>();

    private Map<String, String> label;

    @Field("flattenedFields")
    private List<AbstractField> flattenedFields = new LinkedList<>();

    // field.uid, fieldConfigs
    @Field("fieldsConf")
    private List<FormDataElementConf> fieldsConf = new LinkedList<>();

    @Field("sections")
    private List<FormSectionConf> sections = new LinkedList<>();

    public boolean getDeleted() {
        return deleted;
    }

    public void setDisabled(boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getDisabled() {
        return Optional.ofNullable(this.disabled).orElse(false);
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = Optional.ofNullable(disabled).orElse(false);
    }

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

    public List<AbstractField> getFlattenedFields() {
        if (flattenedFields.isEmpty()) {
            return flattenFields();
        }

        return flattenedFields;
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

    public DataForm version(Integer version) {
        this.setVersion(version);
        return this;
    }
}
