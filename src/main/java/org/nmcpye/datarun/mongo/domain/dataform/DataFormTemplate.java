package org.nmcpye.datarun.mongo.domain.dataform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A DataFormTemplate.
 */
//@JsonIgnoreProperties(value = {"elements"}, allowGetters = true)
@Document(collection = "data_form_template")
@Getter
@Setter
@CompoundIndex(name = "form_template_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormTemplate
    extends MongoBaseIdentifiableObject implements FormWithFields {
    @Id
    private String id;

    @Size(max = 11)
    @Field("uid")
    private String uid;

    @Field("code")
    private String code;

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

    @Field("fields")
    private List<FormDataElementConf> fields = new LinkedList<>();

    @Field("sections")
    private List<FormSectionConf> sections = new LinkedList<>();

    public DataFormTemplate() {
        setAutoFields();
    }

    @JsonIgnore
    public String getId() {
        return id;
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
