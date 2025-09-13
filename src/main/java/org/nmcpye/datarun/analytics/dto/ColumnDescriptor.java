package org.nmcpye.datarun.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDescriptor {
    /**
     * Public logical stable identifier for the column (e.g. "core:org_unit_uid"),
     * used by clients and for row keys.
     */
    String id;

    /**
     * Machine-friendly key / SQL alias used in the jOOQ Record to fetch values.
     * This is safe for SQL (no ':' chars) and is used when mapping Record -> row.
     * <p>
     * Internal use only (removed from response)
     */
    @JsonIgnore
    String key;

    /**
     * default human friendly labels map to show in UI.
     */
    String displayLabel;

    /**
     * human friendly label localizations map to show in UI.
     */
    Map<String, String> label;

    /**
     * type of value in this column.
     */
    DataType dataType;

    Map<String, Object> extras = new HashMap<>();
}
