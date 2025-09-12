package org.nmcpye.datarun.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Value
@Builder
public class ColumnDto {
    /**
     * Public logical identifier for the column (e.g. "core:org_unit_uid").
     * This is the stable id used by clients for display and referencing.
     */
    String id;

    /**
     * Machine-friendly key / SQL alias used in the jOOQ Record to fetch values.
     * This is safe for SQL (no ':' chars) and is used when mapping Record -> row.
     */
    @JsonIgnore
    String key;

    /**
     * Human-friendly label to show in UI (localized content may be embedded).
     */
    String name;

    /**
     * Human-friendly label to show in UI (localized content may be embedded).
     */
    Map<String, String> label;

    DataType dataType; // optional, e.g., "value_num"

    Map<String, Object> extras;
}
