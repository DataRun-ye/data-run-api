package org.nmcpye.datarun.jpa.datatemplate;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.datatemplateelement.AggregationType;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.FormSectionConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ValueType;
import org.nmcpye.datarun.jpa.dataelement.DataElement;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight DTO for persisting and querying {@code element_template_config}.
 *
 * <p>This entity represents the configuration of a
 * {@link DataElement} within a
 * {@link DataTemplate}. It is derived from a {@link FormDataElementConf} or a
 * {@code repeatable=true} {@link FormSectionConf}, and captures all metadata
 * required to describe fields and repeatable sections of a DataTemplate.</p>
 *
 * <p>Acts as the single source of truth for element configuration at runtime.</p>
 *
 * @author Hamza
 * @since 18/08/2025
 */
@Entity
@Table(name = "element_template_config",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_element_template_config_tpl_ver_idpath",
                        columnNames = {"template_id", "template_version_id", "id_path"})
        },
        indexes = {
                @Index(name = "idx_element_template_config_template_version", columnList = "template_id, template_version_id"),
                @Index(name = "idx_element_template_config_template_version_no", columnList = "template_id, version_no"),
                @Index(name = "idx_element_template_config_dataelement", columnList = "data_element_id"),
                @Index(name = "idx_element_template_config_repeat_path", columnList = "template_id, repeat_path")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString(onlyExplicitlyIncluded = true)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ElementTemplateConfig {
    public enum ElementKind {FIELD, SECTION}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_template_config_seq")
    @SequenceGenerator(name = "element_template_config_seq", sequenceName = "element_template_config_seq", allocationSize = 1)
    private Long id;

    /**
     * ID of the {@link DataTemplate} containing this configuration.
     */
    @NotNull
    @Column(name = "template_id", length = 26, nullable = false)
    @ToString.Include
    private String templateId;

    /**
     * ID of the {@link org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion}.
     */
    @NotNull
    @Column(name = "template_version_id", length = 26, nullable = false)
    @ToString.Include
    private String templateVersionId;

    /**
     * Version number of the {@link org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion}.
     */
    @NotNull
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /**
     * ID of the {@link DataElement} being configured.
     */
    @NotNull
    @Column(name = "data_element_id", length = 26, nullable = false)
    private String dataElementId;

    /**
     * Path built with element IDs (e.g. "household.children.<elementId>").
     */
    @NotNull
    @Column(name = "id_path", columnDefinition = "text", nullable = false)
    @ToString.Include
    @EqualsAndHashCode.Include
    private String idPath;

    @Column(name = "template_order")
    private Integer templateOrder;

    /**
     * Path built with element names (ends with name). Used during normalization.
     * <p>Used in normalization.
     * For {@link org.nmcpye.datarun.datatemplateelement.FormSectionConf}, the ID is its name.
     * For {@link FormDataElementConf}, the ID is the linked {@link DataElement} ID.</p>
     */
    @Column(name = "name_path", columnDefinition = "text")
    private String namePath;

    /**
     * Immutable element name, copied from {@link DataElement}.
     * Used as a key in {@link DataSubmission#getFormData()}.
     */
    @NotNull
    @Column(name = "name", columnDefinition = "text", nullable = false)
    private String name;

    /**
     * Value type of this element.
     * For sections, {@code valueType = null}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", updatable = false, nullable = false)
    protected ValueType valueType;

    /**
     * Aggregation strategy when used as a measure in analytics (pivot tables, charts, etc).
     * Examples: {@code SUM}, {@code AVG}, {@code COUNT}, {@code FIRST}, {@code LAST}, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", length = 32, nullable = false)
    private AggregationType aggregationType;

    /**
     * True if this element is a reference type (e.g. Team, OrgUnit, Activity, Option).
     */
    @Column(name = "is_reference", nullable = false)
    private Boolean isReference;

    /**
     * Name of the database table containing the referenced values (e.g. {@code team}, {@code org_unit}).
     */
    @Column(name = "reference_table", length = 100)
    private String referenceTable;

    /**
     * Option set ID if this element configures a {@link ValueType#SelectOne} or
     * {@link ValueType#SelectMulti} field.
     */
    @Column(name = "option_set_id", length = 100)
    private String optionSetId;

    /**
     * True if this element is inside a repeatable section.
     */
    @Column(name = "is_repeatable", nullable = false)
    private Boolean isRepeatable = Boolean.FALSE;


    /**
     * Path of the ancestor repeatable section, if applicable.
     */
    @Column(name = "repeat_path", columnDefinition = "text")
    private String repeatPath;

    /**
     * True if this element is a multi-select (values reference multiple Option codes).
     */
    @Column(name = "is_multi", nullable = false)
    private Boolean isMulti = Boolean.FALSE;

    /**
     * True if this element is considered a measure for analytics.
     */
    @Column(name = "is_measure", nullable = false)
    private Boolean isMeasure = Boolean.FALSE;

    /**
     * ID of the category element that in the same repeat,
     * if {@link FormSectionConf#getCategoryDataElementId()} is set.
     */
    @Column(name = "category_for_repeat", length = 26)
    private String categoryForRepeat;

    /**
     * true if this element is a category of a repeat,
     * if {@link FormSectionConf#getCategoryDataElementId()} is set.
     */
    @Column(name = "is_category")
    private Boolean isCategory = Boolean.FALSE;

    /**
     * Localized display labels, e.g. {@code {"en": "Child Name", "ar": "..."}}.
     */
    @Type(JsonType.class)
    @Column(name = "display_label", columnDefinition = "jsonb")
    private Map<String, String> displayLabel;

    /**
     * Snapshot of the element definition, stored as JSON and deserialized to
     * AbstractElement (actual runtime type decided by the elementType property).
     */
    @Type(JsonType.class)
    @Column(name = "definition_json", columnDefinition = "jsonb")
    private Object definitionJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "element_kind", length = 16, nullable = false)
    private ElementKind elementKind = ElementKind.FIELD;

    /**
     * Timestamp of creation.
     */
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (aggregationType == null) aggregationType = org.nmcpye.datarun.datatemplateelement.AggregationType.DEFAULT;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ElementTemplateConfig that)) return false;
        return Objects.equals(getTemplateId(), that.getTemplateId()) &&
                Objects.equals(getTemplateVersionId(),
                        that.getTemplateVersionId())
                && Objects.equals(getDataElementId(),
                that.getDataElementId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTemplateId(), getTemplateVersionId(), getDataElementId());
    }
}
