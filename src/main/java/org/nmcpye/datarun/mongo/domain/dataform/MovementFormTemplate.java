package org.nmcpye.datarun.mongo.domain.dataform;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.mongo.domain.AbstractAuditingEntityMongo;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.*;

/**
 * A DataForm.
 */
@Document(collection = "data_form_template")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MovementFormTemplate
    extends AbstractAuditingEntityMongo<String> {

    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    @Indexed(unique = true, name = "form_template_uid")
    private String uid;

    @Field("name")
    @Indexed(unique = true, name = "form_template_name")
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    @Field("disabled")
    private boolean disabled;

    @Field("deleted")
    private boolean deleted;

    private Integer version;

    @Field("defaultLocale")
    private String defaultLocale;

    private Map<String, String> label;

    @Field("fields")
    private List<FormDataElementConf> fields = new ArrayList<>();

    @Field("sections")
    private List<FormSectionConf> sections = new ArrayList<>();

    public MovementFormTemplate() {
        setAutoFields();
    }

    public String getId() {
        return this.id;
    }

    public MovementFormTemplate id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public MovementFormTemplate uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MovementFormTemplate name(String name) {
        this.setName(name);
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public MovementFormTemplate description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDisabled() {
        return Optional.ofNullable(this.disabled).orElse(false);
    }

    public MovementFormTemplate disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = Optional.ofNullable(disabled).orElse(false);
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setLabel(Map<String, String> label) {
        this.label = Objects.requireNonNullElseGet(label, () -> Map.of(getDefaultLocale(), this.name));
    }

    public Map<String, String> getLabel() {
        return Optional.ofNullable(this.label).orElse(Map.of(getDefaultLocale(), this.name));
    }

    public String getDefaultLocale() {
        return Optional.ofNullable(defaultLocale).orElse("en");
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = Objects.requireNonNullElse(defaultLocale, "en");
    }

    public List<FormDataElementConf> getFields() {
        return fields;
    }

    public void setFields(List<FormDataElementConf> fields) {
        this.fields = fields;
    }

    public MovementFormTemplate fieldsConf(List<FormDataElementConf> fieldsConf) {
        this.setFields(fieldsConf);
        return this;
    }

    public List<FormSectionConf> getSections() {
        return sections;
    }

    public void setSections(List<FormSectionConf> sections) {
        this.sections = sections;
    }

    public MovementFormTemplate sectionsConf(List<FormSectionConf> sections) {
        this.setSections(sections);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovementFormTemplate dataForm = (MovementFormTemplate) o;
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
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
