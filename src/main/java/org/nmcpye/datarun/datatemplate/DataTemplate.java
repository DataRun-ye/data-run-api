package org.nmcpye.datarun.datatemplate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.jpa.JpaSoftDeleteObject;
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
public class DataTemplate extends JpaSoftDeleteObject {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
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

    @NotNull
    @Column(name = "version_uid", nullable = false, unique = true)
    private String versionUid;

    @NotNull
    @Column(name = "version_number", nullable = false)
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

    public DataTemplate pumpVersion() {
        this.setVersionNumber(versionNumber + 1);
        return this;
    }

    public DataTemplate versionNumber(Integer currentVersion) {
        setVersionNumber(currentVersion);
        return this;
    }

    public DataTemplate id(Long id) {
        setId(id);
        return this;
    }

    public DataTemplate uid(String uid) {
        setUid(uid);
        return this;
    }

    public DataTemplate versionUid(String versionUid) {
        this.setVersionUid(versionUid);
        return this;
    }
}
