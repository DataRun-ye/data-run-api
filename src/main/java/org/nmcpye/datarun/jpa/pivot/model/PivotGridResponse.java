package org.nmcpye.datarun.jpa.pivot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada 23/08/2025 (7amza.it@gmail.com)
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PivotGridResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The aggregated data, where each map represents a row.
     * Keys in the map correspond to dimension names and measure aliases.
     * Example: {"team_name": "Alpha", "element_name": "Weight", "SUM_value_num": 1500.0}
     */
    private List<Map<String, Object>> data;

    /**
     * The total count of records that would have been returned without pagination.
     */
    private Long totalRecords;
}
