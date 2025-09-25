package org.nmcpye.datarun.jpa.datatemplate;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
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
            columnNames = {"template_uid", "template_version_uid", "id_path"}),
        @UniqueConstraint(name = "ux_template_element_tpl_ver_idpath",
            columnNames = {"template_uid", "template_version_uid", "semantic_path", "data_element_uid"})
    },
    indexes = {
        @Index(name = "idx_template_element_template_version",
            columnList = "template_uid, template_version_uid"),
        @Index(name = "idx_template_element_template_version_no",
            columnList = "template_uid, template_version_no"),
        @Index(name = "idx_template_element_dataelement", columnList = "data_element_uid"),
        @Index(name = "idx_template_element_repeat_uid", columnList = "repeat_uid"),
        @Index(name = "idx_template_element_repeat_path", columnList = "template_uid, ancestor_repeat_path")
    }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TemplateElement {
    public enum ElementKind {FIELD, REPEAT}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_element_seq")
    @SequenceGenerator(name = "template_element_seq", sequenceName = "template_element_seq", allocationSize = 1)
    private Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true, nullable = false)
    protected String uid;

    @Enumerated(EnumType.STRING)
    @Column(name = "element_kind", length = 16, nullable = false)
    @Builder.Default
    private ElementKind elementKind = ElementKind.FIELD;

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
    private Integer versionNo;

    /// globally immutable unique uid of the [DataElement] being configured.
    @NotNull
    @Column(name = "data_element_uid", length = 100, nullable = false)
    private String dataElementUid;

    /// Used as a key in [#getFormData()].
    /// Immutable and copied from [DataElement] for fast access only, single source of truth is still [DataElement]
    @NotNull
    @Column(name = "name", columnDefinition = "text", nullable = false)
    private String name;

    /// Value type of this element.
    /// For sections = null.
    /// Immutable and copied from [DataElement] for fast access only, single source of truth is still [DataElement]
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", updatable = false)
    protected ValueType valueType;

    /// Aggregation strategy when used as a measure in analytics (pivot tables, charts, etc).
    /// Examples: `SUM`, `AVG`, `COUNT`, `FIRST`, `LAST`, etc.
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", length = 32)
    private AggregationType aggregationType;

    /// True if this element valueType is a reference type (e.g. Team, OrgUnit, Activity, Option).
    @Column(name = "is_reference")
    @Builder.Default
    private Boolean isReference = Boolean.FALSE;

    /**
     * true if this element itself, is configured as a category for a repeat,
     * if {@link FormSectionConf#getCategoryId()} is set.
     */
    @Column(name = "is_category")
    @Builder.Default
    private Boolean isCategory = Boolean.FALSE;

    /// Option set uid
    /// [#SelectMulti] field.
    /// Immutable and copied from [DataElement] for fast access, single source of truth is still [DataElement]
    @Column(name = "option_set_uid", length = 11)
    private String optionSetUid;
    /// True if this element is a multi-select.
    @Column(name = "is_multi")
    @Builder.Default
    private Boolean isMulti = Boolean.FALSE;

    /// True if this element is considered a measure for analytics.
    @Column(name = "is_measure")
    @Builder.Default
    private Boolean isMeasure = Boolean.FALSE;

    /// True if this element is considered a measure for analytics.
    @Column(name = "is_dimension")
    @Builder.Default
    private Boolean isDimension = Boolean.FALSE;

    @Column(name = "sort_order")
    private Integer sortOrder;


    /// Path built with element IDs (e.g. "household.children.<elementUid>").
    @NotNull
    @Column(name = "id_path", length = 3000, nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private String idPath;

    /// Path built with element names (ends with name). Used during normalization.
    ///
    /// Used in normalization.
    /// For [FormSectionConf], the id is its name.
    /// For [FormDataElementConf], the id is linked to [DataElement] uid.
    @Column(name = "name_path", length = 3000, nullable = false)
    private String namePath;

    /// The Canonical path. that semantically represent a grain of data
    /// null if submission-level
    /// the full `repeat_path` (e.g., `root.householdinfo.children`) mixes two different concepts:
    /// 1.  **Structural Grouping:** The `householdinfo` part is just a visual grouping on the form. An admin could rename it to `household_details` tomorrow, and it would mean the exact same thing to the user.
    /// 2.  **Data Grain:** The `children` part, because it is `repeatable: true`, defines a fundamental change in the data's structure. It means "one or more children related to one parent submission." This is the true semantic grain.
    @Column(name = "semantic_path", length = 3000)
    private String canonicalPath;

    /**
     * repeat-only path to nearest repeatable ancestor
     */
    @Column(name = "ancestor_repeat_semantic_path", length = 3000)
    private String parentRepeatCanonicalPath;

    /**
     * te.uid of the repeat this te belongs to
     */
    @Column(name = "repeat_uid", length = 64)
    private String repeatUid;

    //------------------------------
    // Repeat related attributes
    //------------------------------
    /// Is this field part of a repeatable section?
    @Column(name = "has_repeat_ancestor", nullable = false)
    @Builder.Default
    private Boolean hasRepeatAncestor = Boolean.FALSE;


    /// full idPath to nearest repeatable ancestor (or null)
    @Column(name = "ancestor_repeat_path", length = 3000)
    private String ancestorRepeatPath;

    /// Localized display labels, e.g. `{"en": "Child Name", "ar": "..."}`.
    @Type(JsonType.class)
    @Column(name = "display_label", columnDefinition = "jsonb default '{}'::jsonb")
    private Map<String, String> displayLabel;

    /// Snapshot of the element definition, stored as JSON and deserialized to
    @Type(JsonType.class)
    @Column(name = "definition_json", columnDefinition = "jsonb")
    private JsonNode definitionJson;

    @Column(name = "is_natural_candidate", length = 64)
    private Boolean isNaturalKeyCandidate;

    //-----------------------
    // Canonical and metadata
    //-----------------------
    @Column(name = "control_type", length = 64)
    private String controlType;

    @Column(name = "semantic_type", length = 64)
    private String semanticType;

    @Column(name = "cardinality", length = 20)
    private String cardinality; // 1 | N

    @Column(name = "schema_fingerprint", length = 64)
    private String schemaFingerprint;

    @Column(name = "canonical_element_uid")
    private String canonicalElementUid; // FK to canonical_element

    // ---------------------
    // Properties set if elementKind = REPEAT
    // ---------------------
    /**
     * uid of the category element that in the same repeat this element is part of
     * if {@link FormSectionConf#getCategoryId()} is set.
     */
    @Column(name = "category_id", length = 26)
    private String categoryId;

    /**
     * if this is a repeat, what's element compose a natural key for its instances
     */
    @Type(JsonType.class)
    @Column(name = "natural_key_candidates", columnDefinition = "jsonb default '[]'::jsonb")
    private Set<String> naturalKeyCandidates;


    /// Timestamp of creation.
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isRepeat() {
        return this.elementKind == ElementKind.REPEAT;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (getUid() == null || getUid().isEmpty()) setUid(CodeGenerator.generateUid());
        if (aggregationType == null) aggregationType = AggregationType.DEFAULT;
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
