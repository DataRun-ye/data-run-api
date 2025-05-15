package org.nmcpye.datarun.mongo.domain.dataform;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.common.mongo.MongoAuditableBaseObject;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A DataForm.
 */
@Document(collection = "form_template_version")
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplateVersion extends MongoAuditableBaseObject implements FormWithFields {
    @Id
    private String id;

    @NotNull
    @Size(max = 11)
    @Indexed(unique = true, name = "template_version_uid_idx")
    private String uid;

    @NotNull
    @Size(max = 11)
    @Field("templateUid")
    @Indexed(name = "template_version_master_uid_idx")
    private String templateUid;

    @NotNull
    private Integer versionNumber;

    private List<FormDataElementConf> fields = new LinkedList<>();

    private List<FormSectionConf> sections = new LinkedList<>();

    public FormTemplateVersion() {
        setAutoFields();
    }

    public FormTemplateVersion version(Integer version) {
        this.setVersionNumber(version);
        return this;
    }

    public FormTemplateVersion templateUid(String templateUid) {
        this.setTemplateUid(templateUid);
        return this;
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
}
