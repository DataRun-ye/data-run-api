package org.nmcpye.datarun.analytics.pivot.dto;

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
    String id;     // e.g., "element_id" or measure alias "SUM_VAL"
    String label;  // user friendly label
    String dataType; // optional, e.g., "value_num"
    Map<String,Object> extras;
}
