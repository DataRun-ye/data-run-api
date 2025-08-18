package org.nmcpye.datarun.jpa.datatemplate;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight DTO used to insert/select from template_field table.
 * displayLabel and definition are JSON strings (jsonb in DB).
 *
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
@Getter
@Setter
public class TemplateField {
    private Long id; // DB PK (nullable for inserts)
    private String templateId;
    private String versionId;
    private Integer versionNo;

    private String elementId;
    private String path;
    private String name;

    private String valueType;
    /**
     * if ValueType any of: Team, OrgUnit, Activity,
     */
    private Boolean isReference;
    private String referenceTable;
    private String optionSetId;

    private Boolean isRepeatable;
    private String repeatPath;
    private Boolean isMulti;
    private Boolean isMeasure;
    private String defaultAggregation;
    /**
     * (element_id or null) or a section→category mapping, if this is a section
     * This allows TemplateFieldGenerator to validate: only allow category if
     * is_reference = true OR if reference_table = 'option'
     * (if we allow option-sets as categories).
     */
    private String categoryForRepeat;

    /**
     * JSON as strings; repository writes them to jsonb
     */
    private String displayLabelJson;
    /**
     * JSON as strings; repository writes them to jsonb
     */
    private String definitionJson;

    private Instant createdAt;

    public TemplateField() {
    }

    // equals/hashCode for tests
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateField that)) return false;
        return Objects.equals(templateId, that.templateId) &&
            Objects.equals(versionId, that.versionId) &&
            Objects.equals(elementId, that.elementId) &&
            Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, versionId, elementId, path);
    }
}
