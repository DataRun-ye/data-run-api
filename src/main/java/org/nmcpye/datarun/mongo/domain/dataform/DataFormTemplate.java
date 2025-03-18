package org.nmcpye.datarun.mongo.domain.dataform;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.mongo.domain.AbstractAuditingEntityMongo;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A DataFormTemplate.
 */
@Document(collection = "data_form_template")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormTemplate
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
    private Boolean disabled = false;

    @Field("deleted")
    private boolean deleted;

    private Integer version;

    @Field("defaultLocale")
    private String defaultLocale;

    private Map<String, String> label;

    @Field("elements")
    private List<FormElementConf> elements;

    @Field("fields")
    private List<FormDataElementConf> fields = new LinkedList<>();

    @Field("sections")
    private List<FormSectionConf> sections = new LinkedList<>();

    public DataFormTemplate() {
        setAutoFields();
    }

    public String getId() {
        return this.id;
    }

    public DataFormTemplate id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return this.uid;
    }

    public DataFormTemplate uid(String uid) {
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

    public DataFormTemplate name(String name) {
        this.setName(name);
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public DataFormTemplate description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDisabled() {
        return Optional.ofNullable(this.disabled).orElse(false);
    }

    public DataFormTemplate disabled(Boolean disabled) {
        this.setDisabled(disabled);
        return this;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public DataFormTemplate version(Integer version) {
        this.setVersion(version);
        return this;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getLabel() {
        return Optional.ofNullable(this.label).orElse(Map.of(getDefaultLocale(), this.name));
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public List<FormElementConf> getElements() {
        return elements;
    }

    public void setElements(List<FormElementConf> elements) {
        this.elements = elements;
    }

    public List<FormDataElementConf> getFields() {
        return fields;
    }

    public void setFields(List<FormDataElementConf> fields) {
        this.fields = fields;
    }

    public DataFormTemplate fieldsConf(List<FormDataElementConf> fieldsConf) {
        this.setFields(fieldsConf);
        return this;
    }

    public List<FormSectionConf> getSections() {
        return sections;
    }

    public void setSections(List<FormSectionConf> sections) {
        this.sections = sections;
    }

    public DataFormTemplate sectionsConf(List<FormSectionConf> sections) {
        this.setSections(sections);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataFormTemplate dataForm = (DataFormTemplate) o;
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
        return "DataFormTemplate{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + getDisabled() + "'" +
            "}";
    }
}
