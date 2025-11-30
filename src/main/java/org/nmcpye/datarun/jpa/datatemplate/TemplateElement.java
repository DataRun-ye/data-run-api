package org.nmcpye.datarun.jpa.datatemplate;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.jpa.dataelement.DataElement;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Versioned Template Element, can be a repeat element, or a field element
/// Lightweight DTO for persisting and querying `template_element`.
///
/// This entity represents the configuration of a
/// [DataElement] within a [DataTemplate]. It is derived from a [FormDataElementConf],
/// and captures all template's related metadata
/// required to describe fields and repeatable sections of a DataTemplate.
///
/// Acts as the single source of truth for element configuration at runtime.
///
/// @author Hamza
/// @since 18/08/2025
@Entity
@Table(name = "template_element",
    uniqueConstraints = {
        @UniqueConstraint(name = "ux_template_element_tpl_ver_idpath",
            columnNames = {"template_uid", "template_version_uid", "canonical_path"})
    },
    indexes = {
        @Index(name = "idx_template_element_template_version", columnList = "template_uid, template_version_uid"),
        @Index(name = "idx_template_element_template_version_no", columnList = "template_uid, template_version_no"),
        @Index(name = "idx_template_element_canonical_uid", columnList = "canonical_element_id"),
        @Index(name = "idx_template_element_repeat_path", columnList = "template_uid, parent_repeat_json_data_path")
    }
)
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TemplateElement {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_element_seq")
    @SequenceGenerator(name = "template_element_seq", sequenceName = "template_element_seq", allocationSize = 1)
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 64, updatable = false, unique = true, nullable = false)
    protected String uid;

    /// uid of the [DataTemplate] containing this configuration.
    @NotNull
    @Column(name = "template_uid", length = 11, nullable = false)
    @ToString.Include
    private String templateUid;

    /// uid of the [TemplateVersion].
    @NotNull
    @Column(name = "template_version_uid", length = 11, nullable = false)
    @ToString.Include
    private String templateVersionUid;

    /// Version number of the [TemplateVersion].
    @NotNull
    @Column(name = "template_version_no", nullable = false)
    private Integer templateVersionNo;

    /// globally immutable unique uid of the [DataElement] being configured.
    @NotNull
    @Deprecated
    @Column(name = "data_element_uid", length = 100)
    private String dataElementUid;

    /// Used as a key in [#getFormData()].
    /// Immutable and copied from [DataElement] for fast access only, single source of truth is still [DataElement]
    @NotNull
    @Column(name = "name", columnDefinition = "text", nullable = false)
    private String name;

    /// Option set uid
    /// [#SelectMulti] field.
    /// Immutable and copied from [DataElement] for fast access, single source of truth is still [DataElement]
    @Column(name = "option_set_uid", length = 11)
    private String optionSetUid;
    /// Option set uid
    /// [#SelectMulti] field.
    /// Immutable and copied from [DataElement] for fast access, single source of truth is still [DataElement]
    @Column(name = "option_set_id", length = 26)
    private String optionSetId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /// Path built with element names (ends with name). Used during normalization.
    ///
    /// Used in normalization.
    /// For [FormSectionConf], the id is its name.
    /// For [FormDataElementConf], the id is linked to [DataElement] uid.
    @Column(name = "json_data_path", length = 3000, nullable = false)
    private String jsonDataPath;

    /// The Canonical path. that canonically represent a grain of data
    /// name of element if submission-level
    /// the full `repeat_path` (e.g., `root.householdinfo.children`) mixes two different concepts:
    /// 1.  **Structural Grouping:** The `householdinfo` part is just a visual grouping on the form. An admin could rename it to `household_details` tomorrow, and it would mean the exact same thing to the user.
    /// 2.  **Data Grain:** The `children` part, because it is `repeatable: true`, defines a fundamental change in the data's structure. It means "one or more children related to one parent submission." This is the true canonical grain.
    @Column(name = "canonical_path", length = 3000)
    private String canonicalPath;

    /**
     * repeat-only path to nearest repeatable ancestor
     */
    @Column(name = "parent_repeat_canonical_path", length = 3000)
    private String parentRepeatCanonicalPath;

    /// full path to nearest repeatable ancestor (or null), dot delimited. no array [*]
    @Column(name = "parent_repeat_json_data_path", length = 3000)
    private String parentRepeatJsonDataPath;

    /// Localized display labels, e.g. `{"en": "Child Name", "ar": "..."}`.
    @Type(JsonType.class)
    @Column(name = "display_label", columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, String> displayLabel;

    //-----------------------
    // Canonical and metadata
    //-----------------------
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 64)
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    @Column(name = "semantic_type", length = 64)
    private SemanticType semanticType;

    @Column(name = "canonical_element_id", nullable = false)
    private String canonicalElementId;

    /**
     * if this is a repeat, what's element compose a natural key for its instances
     */
    @Type(JsonType.class)
    @Singular
    @Column(name = "natural_key_candidates", columnDefinition = "jsonb default '[]'::jsonb")
    private Set<String> naturalKeyCandidates;


    /// Timestamp of creation.
    @NotNull
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    public boolean isRepeat() {
        return this.semanticType == SemanticType.Repeat;
    }

    public TemplateElement() {
        setAutoFields();
    }

    public void setAutoFields() {
        if (createdDate == null) createdDate = Instant.now();
        if (getUid() == null || getUid().isEmpty()) {
            setUid(CodeGenerator.generateUid());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TemplateElement that)) return false;
        return Objects.equals(getTemplateUid(), that.getTemplateUid()) &&
            Objects.equals(getTemplateVersionUid(),
                that.getTemplateVersionUid())
            && Objects.equals(getDataElementUid(),
            that.getDataElementUid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTemplateUid(), getTemplateVersionUid(), getDataElementUid());
    }
}
