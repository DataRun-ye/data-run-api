package org.nmcpye.datarun.jpa.pivot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import static org.nmcpye.datarun.jpa.pivot.dto.PivotParameters.*;

/**
 * @author Hamza Assada 17/08/2025 (7amza.it@gmail.com)
 */
public record PivotQueryRequest(
    String templateId,
    @NotNull
    @NotEmpty
    List<DimensionRef> rows,
    List<DimensionRef> columns,
    @NotNull
    @NotEmpty
    List<MeasureRef> measures,
    List<FilterRef> filters,
    DateRange dateRange,
    Pagination pagination,
    Sorting sorting,
    ResultFormat format
) {
    public PivotQueryRequest {
        if (format == null) {
            format = ResultFormat.MATRIX;
        }
    }
}
