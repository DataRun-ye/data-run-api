package org.nmcpye.datarun.mongo.domain;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.mongo.common.MongoAuditableBaseObject;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.Repeat;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.jpa.optionset.OptionSet;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A DataForm.
 */
@Document(collection = "metadata_schema")
@Getter
@Setter
@CompoundIndex(name = "schema_uid", def = "{'uid': 1}", unique = true)
@CompoundIndex(name = "schema_name", def = "{'name': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MetadataSchema
    extends MongoAuditableBaseObject {
    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    private String uid;

    @NotNull
    @Field("name")
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

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of("en", this.name));
    }

    public Map<String, String> getLabel() {
        if (this.label == null) {
            return Map.of("en", this.name);
        }
        return label;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataForm{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
