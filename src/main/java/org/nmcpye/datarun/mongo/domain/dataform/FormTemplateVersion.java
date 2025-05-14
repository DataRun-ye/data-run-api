package org.nmcpye.datarun.mongo.domain.dataform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoBaseIdentifiableObject;
import org.nmcpye.datarun.mapper.dto.FormTemplateDto;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A DataForm.
 */
@Document(collection = "form_template_version")
@Getter
@Setter
@NoArgsConstructor
@CompoundIndex(name = "template_version_idx", def = "{'templateUid': 1, 'version': -1}", unique = true)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplateVersion
    extends MongoBaseIdentifiableObject implements FormWithFields {
    @MongoId
    @Field("versionId")
    private String id;

    @Field("uid")
    @Indexed(unique = true, name = "template_version_uid_idx")
    private String uid;

    @NotNull
    @Size(max = 11)
    @Field("templateUid")
    @Indexed(name = "parent_template_uid_idx")
    private String templateUid;

    @Field("code")
    private String code;

    @Field("name")
    @NotNull
    private String name;

    @Size(max = 2000)
    @Field("description")
    private String description;

    String defaultLocale = "ar";

    private Map<String, String> label;

    @NotNull
    private Integer version;

    private List<FormDataElementConf> fields;

    private List<FormSectionConf> sections;

    public FormTemplateVersion createVersion(FormTemplateDto template) {
        final var formVersion = new FormTemplateVersion();
        formVersion.setTemplateUid(template.getUid());
        formVersion.setId(template.getUid() + "_" + template.getLatestVersion());
        formVersion.setUid(template.getUid() + "_" + template.getLatestVersion());
        formVersion.setCode(template.getCode());
        formVersion.setName(template.getName());
        formVersion.setDescription(template.getDescription());
        formVersion.setVersion(template.getLatestVersion());
        return formVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormTemplateVersion that)) return false;
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

    @JsonIgnore
    @Override
    public List<FormDataElementConf> getFieldsConf() {
        return getFields();
    }

    @Override
    public void setFieldsConf(List<FormDataElementConf> sections) {

    }
}
