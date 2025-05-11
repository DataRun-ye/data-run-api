package org.nmcpye.datarun.formtemplate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Objects;

/**
 * A DataForm.
 */
@Document(collection = "data_form_template_version")
@Getter
@Setter
@NoArgsConstructor
@CompoundIndex(name = "template_version_idx", def = "{'templateUid': 1, 'version': -1}", unique = true)
@CompoundIndex(name = "template_version_uid_idx", def = "{'uid': 1}", unique = true)
@CompoundIndex(name = "template_uid_idx", def = "{'templateUid': 1}")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataFormTemplateVersion
    extends MongoBaseIdentifiableObject implements FormWithFields {

    @Field("code")
    private String code;

    @Field("name")
    private String name;

    @NotNull
    @Size(max = 11)
    @Field("templateUid")
    private String templateUid;

    @NotNull
    private Integer version;

    private List<FormDataElementConf> fields;

    private List<FormSectionConf> sections;


    public DataFormTemplateVersion createVersion(DataFormTemplate template) {
        final var formVersion = new DataFormTemplateVersion();
        formVersion.setId(template.getId() + "_" + template.getVersion());
        formVersion.setTemplateUid(template.getUid());
        formVersion.setCode(template.getCode());
        formVersion.setName(template.getName());
        formVersion.setVersion(template.getVersion());
        formVersion.setFields(template.getFields());
        formVersion.setSections(template.getSections());
        return formVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataFormTemplateVersion that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "DataForm{" +
            "id=" + getId() +
            ", uid='" + getUid() + "'" +
            "}";
    }

    @Override
    public FormWithFields version(Integer version) {
        this.setVersion(version);
        return this;
    }
}
