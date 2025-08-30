package org.nmcpye.datarun.analytics.pivot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

/**
 * Represents a single field (dimension or measure) available for analytics.
 *
 * @param id               The unique, external-facing identifier for this field. E.g., "org_unit_uid", "de:muac_score"
 *                         (prefer short id if available, otherwise ULID id). Examples: "de:01ARYZ..." or "etc:1234" or "team:01T...".
 * @param label            Human-readable label for the UI. E.g., "Organization Unit", "MUAC Score"
 * @param category         The category of the field, for UI grouping.
 * @param dataType         The underlying data type. Informs the front-end on how to format values.
 * @param factColumn       The specific column name in the `pivot_grid_facts` view this field maps to.
 *                         Crucial for the query builder in Step 2. E.g., "org_unit_id", "value_num"
 * @param aggregationModes List of aggregations allowed for this field. E.g., ["SUM", "AVG"]
 * @param templateModeOnly True if this field is only relevant in the context of a specific form template.
 * @param source           Source of metadata: "element_template_config" or "data_element"
 * @param extras           Extras for UI: optionSetId, referenceTable, repeatPath, isMulti, isCategory, categoryForRepeat
 * @author Hamza Assada
 * @since 26/08/2025
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record PivotFieldDto(
    String id,
    String label,
    String category,
    String dataType,
    String factColumn,
    Set<String> aggregationModes,
    boolean templateModeOnly,
    String source,
    Map<String, Object> extras
) {
}
