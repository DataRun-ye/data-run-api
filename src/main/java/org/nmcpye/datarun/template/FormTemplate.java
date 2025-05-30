package org.nmcpye.datarun.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.drun.postgres.common.translation.Translation;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada, <7amza.it@gmail.com> <27-05-2025>
 */
@Entity
@Table(name = "data_template")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FormTemplate extends JpaBaseIdentifiableObject {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "form_version")
    private String formVersion;

    @Column(name = "version_number")
    private Integer versionNumber = 0;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "description", length = 2000)
    private String description;

    public void setLabel(Map<String, String> label) {
        final var translations = label.entrySet().stream()
            .map(entry -> Translation
                .builder()
                .locale(entry.getKey())
                .property("name")
                .value(entry.getValue()).build()).collect(Collectors.toSet());
        setTranslations(translations);
    }

    @JsonProperty
    public Map<String, String> getLabel() {
        return translations
            .stream()
            .collect(Collectors
                .toMap(Translation::getLocale, Translation::getValue));
    }

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
