package org.nmcpye.datarun.jpa.pivotdata.dto;

import java.util.List;

/**
 * @author Hamza Assada 18/08/2025 (7amza.it@gmail.com)
 */
public class PivotMetadataResponse {
    public final List<DimensionDto> dimensions;
    public final List<MeasureDto> measures;
    public final ConfigDto config;

    public PivotMetadataResponse(List<DimensionDto> dimensions, List<MeasureDto> measures, ConfigDto config) {
        this.dimensions = dimensions;
        this.measures = measures;
        this.config = config;
    }

    public static class ConfigDto {
        public final long maxCells;
        public final int maxCols;
        public final int maxQueryTimeMs;

        public ConfigDto(long maxCells, int maxCols, int maxQueryTimeMs) {
            this.maxCells = maxCells;
            this.maxCols = maxCols;
            this.maxQueryTimeMs = maxQueryTimeMs;
        }

        public static ConfigDto from(long maxCells, int maxCols, int maxQueryTimeMs) {
            return new ConfigDto(maxCells, maxCols, maxQueryTimeMs);
        }
    }
}
