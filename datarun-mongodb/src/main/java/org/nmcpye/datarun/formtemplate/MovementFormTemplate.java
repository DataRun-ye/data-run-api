package org.nmcpye.datarun.formtemplate;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A DataForm.
 */
@Document(collection = "data_form_template")
@Getter
@Setter
@CompoundIndex(name = "form_template_uid", def = "{'uid': 1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MovementFormTemplate
    extends MongoBaseIdentifiableObject {

    @Field("code")
    private String code;

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

    public MovementFormTemplate id(String id) {
        this.setId(id);
        return this;
    }

    public MovementFormTemplate uid(String uid) {
        this.setUid(uid);
        return this;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataForm{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", disabled='" + isDisabled() + "'" +
            "}";
    }
}
