package org.nmcpye.datarun.analytics.pivot.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@Builder
public class PivotMetadataResponse {
    private List<PivotFieldDto> coreDimensions;
    private List<PivotFieldDto> formDimensions;
    private List<PivotFieldDto> measures;
    private Map<String, Object> hints;
}
