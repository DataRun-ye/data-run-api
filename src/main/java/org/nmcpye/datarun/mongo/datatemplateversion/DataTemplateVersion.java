package org.nmcpye.datarun.mongo.datatemplateversion;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.mongo.common.MongoBaseIdentifiableObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A DataTemplateTemplateVersion.
 */
@Document(collection = "data_template_version")
@CompoundIndex(name = "data_template_version_uid_number_idx",
    def = "{'templateUid': 1, 'versionNumber': -1}", unique = true)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class DataTemplateVersion
    extends MongoBaseIdentifiableObject
    implements DataTemplateVersionInterface {
    @JsonIgnore
    @Id
    private String id;

    @NotNull
    @Size(max = 11)
    @Indexed(unique = true, name = "data_template_version_uid_idx")
    private String uid;

    @NotNull
    @Size(max = 11)
    @Field("templateUid")
    @Indexed(name = "data_template_version_master_uid_idx")
    private String templateUid;

    @Field("versionNumber")
    @NotNull
    @Indexed(name = "data_template_version_number_idx", direction = IndexDirection.DESCENDING)
    private Integer versionNumber;

    private List<FormDataElementConf> fields = new LinkedList<>();

    private List<FormSectionConf> sections = new LinkedList<>();

    public DataTemplateVersion() {
        setAutoFields();
    }

    public DataTemplateVersion uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public DataTemplateVersion version(Integer version) {
        this.setVersionNumber(version);
        return this;
    }

    public DataTemplateVersion templateUid(String templateUid) {
        this.setTemplateUid(templateUid);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataTemplateVersion that)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public DataTemplateVersion sections(List<FormSectionConf> sections) {
        this.setSections(sections);
        return this;
    }

    @Override
    public DataTemplateVersion fields(List<FormDataElementConf> fields) {
        this.setFields(fields);
        return this;
    }

    @JsonIgnore
    @Override
    public String getCode() {
        return null;
    }

    @JsonIgnore
    @Override
    public String getName() {
        return null;
    }
}
