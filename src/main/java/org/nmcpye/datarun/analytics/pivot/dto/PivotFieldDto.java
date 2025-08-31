package org.nmcpye.datarun.analytics.pivot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

/**
 * Represents a single field (dimension or measure) available for analytics.
 * Metadata describing a field that can be used in pivot rows/cols/values/filters.
 *
 * @param id                "etc:<uid>" for template-scoped element configs, "de:<uid>" for canonical data elements,
 *                          or system ids like "team_uid", "org_unit_uid".
 * @param label             Human-readable label for the UI. E.g., "Organization Unit", "MUAC Score"
 * @param category          The category of the field, for UI grouping.
 * @param dataType          The underlying data type. Informs the front-end on how to format values.
 * @param factColumn        column in pivot_grid_facts to use for template-mode predicates (e.g. "etc_uid")
 * @param factColumnGlobal  column in pivot_grid_facts to use for global-mode predicates (e.g. "de_uid")
 * @param deUid
 * @param deValueType
 * @param deAggregationType
 * @param deIsMeasure
 * @param deIsDimension
 * @param aggregationModes  List of aggregations allowed for this field. E.g., ["SUM", "AVG"]
 * @param templateModeOnly  True if this field is only relevant in the context of a specific form template.
 * @param source            Source of metadata: "element_template_config" or "data_element"
 * @param extras            Extras for UI: optionSetId, referenceTable, repeatPath, isMulti, isCategory, categoryForRepeat
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
    String factColumnGlobal,
    String deUid,
    String deValueType,
    String deAggregationType,
    Boolean deIsMeasure,
    Boolean deIsDimension,
    Set<String> aggregationModes,
    boolean templateModeOnly,
    String source,
    Map<String, Object> extras
) {
}
