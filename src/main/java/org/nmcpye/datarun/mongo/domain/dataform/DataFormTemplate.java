package org.nmcpye.datarun.mongo.domain.dataform;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
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

/**
 * A DataFormTemplate.
 */
@Document(collection = "data_form_template")
@Getter
@Setter
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

    @Getter
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

    public DataFormTemplate version(Integer version) {
        this.setVersion(version);
        return this;
    }

    public DataFormTemplate fields(List<FormDataElementConf> fieldsConf) {
        this.setFields(fieldsConf);
        return this;
    }

    public DataFormTemplate sections(List<FormSectionConf> sections) {
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
