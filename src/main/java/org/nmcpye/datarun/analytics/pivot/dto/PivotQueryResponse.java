package org.nmcpye.datarun.analytics.pivot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PivotQueryResponse {
    private List<ColumnDto> columns;
    private List<Map<String, Object>> rows;
    /**
     * optional: count of result rows (if available)
     */
    private long total;
    private Map<String, Object> meta;
}
