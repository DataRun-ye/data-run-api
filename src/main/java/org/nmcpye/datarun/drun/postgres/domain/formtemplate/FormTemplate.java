package org.nmcpye.datarun.drun.postgres.domain.formtemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;

import java.util.Map;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "form_template", indexes = {
    @Index(name = "idx_form_template_uid_unq", columnList = "uid", unique = true)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonIgnoreProperties(value = {"new"})
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplate extends JpaBaseIdentifiableObject {

    private String formVersion;


    @Column(name = "version_number")
    private Integer versionNumber = 0;

    @Column(name = "disabled")
    private Boolean disabled = false;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @NotNull
    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "default_locale")
    String defaultLocale = "ar";

    @Type(JsonType.class)
    @Column(name = "label", columnDefinition = "jsonb")
    private Map<String, String> label;


    public FormTemplate versionNumber(Integer currentVersion) {
        setVersionNumber(currentVersion);
        return this;
    }

    public FormTemplate id(Long id) {
        setId(id);
        return this;
    }

    public FormTemplate uid(String uid) {
        setUid(uid);
        return this;
    }

    public FormTemplate formVersion(String formVersion) {
        this.setFormVersion(formVersion);
        return this;
    }
}
