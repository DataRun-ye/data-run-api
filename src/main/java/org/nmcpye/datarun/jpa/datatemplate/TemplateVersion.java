package org.nmcpye.datarun.jpa.datatemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;

import java.util.List;

/**
 * A DataTemplateTemplateVersion.
 */
@Entity
@Table(name = "data_template_version", uniqueConstraints = {
//        @UniqueConstraint(name = "ux_template_uid_version_number", columnNames = {"template_uid", "version_number"}),
        @UniqueConstraint(name = "ux_tv_id_data_template_id", columnNames = {"id", "data_template_id"}),
        @UniqueConstraint(name = "ux_tv_no_data_template_id", columnNames = {"version_number", "data_template_id"})
}, indexes = {
        @Index(name = "idx_tv_template_uid", columnList = "template_uid"),
        @Index(name = "idx_tv_version_no", columnList = "version_number"),
        @Index(name = "idx_tv_template_uid_version_desc", columnList = "template_uid, version_number"),
//        @Index(name = "idx_tv_data_template_id_version_desc", columnList = "version_number, data_template_id"),
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@AllArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TemplateVersion extends JpaIdentifiableObject {

    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true, nullable = false)
    protected String uid;

    @Deprecated
    @Size(max = 11)
    @Column(name = "template_uid", length = 11, updatable = false)
    private String templateUid;

    @NotNull
    @Column(name = "version_number", nullable = false, updatable = false)
    private Integer versionNumber;

    @Column(name = "release_notes", length = 4000)
    private String releaseNotes;

    @JdbcTypeCode(SqlTypes.JSON) // Specifies JSON mapping
    @Column(name = "fields", columnDefinition = "jsonb", nullable = false)
    private List<FormDataElementConf> fields;

    @JdbcTypeCode(SqlTypes.JSON) // Specifies JSON mapping
    @Column(name = "sections", columnDefinition = "jsonb", nullable = false)
    private List<FormSectionConf> sections;

    @ManyToOne
    @JoinColumn(name = "data_template_id", nullable = false)
    private DataTemplate dataTemplate;

    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    public TemplateVersion() {
        setAutoFields();
    }

    public TemplateVersion uid(String uid) {
        this.setUid(uid);
        return this;
    }

    public TemplateVersion version(Integer version) {
        this.setVersionNumber(version);
        return this;
    }

    public TemplateVersion templateUid(String templateUid) {
        this.setTemplateUid(templateUid);
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
