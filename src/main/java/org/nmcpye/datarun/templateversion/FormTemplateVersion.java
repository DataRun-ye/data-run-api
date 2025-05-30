package org.nmcpye.datarun.drun.postgres.domain.formtemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaAuditableObject;
import org.nmcpye.datarun.mongo.common.FormWithFields;
import org.nmcpye.datarun.mongo.domain.dataelement.FormDataElementConf;
import org.nmcpye.datarun.mongo.domain.dataelement.FormSectionConf;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "form_template_version", indexes = {
    @Index(name = "idx_form_template_uid_unq", columnList = "uid", unique = true)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplateVersion extends JpaAuditableObject
    implements FormWithFields {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @NotNull
    @Size(max = 11)
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

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long aLong) {

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
