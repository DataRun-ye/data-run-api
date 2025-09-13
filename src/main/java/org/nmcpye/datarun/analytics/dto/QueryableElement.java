package org.nmcpye.datarun.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

/// A self-describing representation of any field available for analytics.
///
/// This DTO is the core of metadata contract returned to the user for available elements to query.
///
/// @param id               The standardized, unique identifier for this field. This is the ID the
///                         client will send back in query requests. <p> **Format:** `namespace:value`
///                         (e.g., `core:team_uid`, `etc:abc123xyz`). or system ids like `team_uid`, `org_unit_uid`.
/// @param label            Localized Human-readable labels map for the UI. E.g., "Organization Unit", "MUAC Score"
/// @param dataType         The data type of the field's *value* (e.g., `value_num`, `value_ts`, `option_uid`).
///                         This informs the UI how to build filter controls and informs the backend which value column in the materialized view to aggregate.
/// @param sourceColumn     The column in the materialized view/table used for *identifying* rows belonging to this field.
///                         This is used in `WHERE/GROUP` BY clauses. (e.g., `etc_uid`, `team_uid`).
/// @param deUid
/// @param aggregationModes List of aggregations allowed for this field. E.g., `["SUM", "AVG"]`
/// @param extras           Extras for UI: `referenceTable`, `repeatPath`, `isMulti`, etc.
/// @param displayGroup     For UI grouping.
/// @param isDimension
/// @param isSortable
/// @author Hamza Assada
/// @since 26/08/2025
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record QueryableElement(
        String id,
        String name,
        Map<String, String> label,
        DataType dataType,
        String sourceColumn,
        String deUid, // The canonical DataElement UID, if applicable.
        Set<Aggregation> aggregationModes,
        String displayGroup,
        Map<String, Object> extras,
        boolean isDimension,
        boolean isSortable) {
}
