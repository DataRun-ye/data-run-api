package org.nmcpye.datarun.analytics.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Simple matrix representation:
 *
 * @author Hamza Assada
 * @since 30/08/2025
 */
@Value
@Builder
public class MatrixResponse {
    /// e.g. ["region", "district"]
    List<String> rowDimensionNames;

    /// e.g. ["gender"]
    List<String> columnDimensionNames;

    /// e.g. ["total_nets_distributed"]
    List<String> measureAliases;

    /// each element is a list of row-dimension values for the row
    /// `rowHeaders[i]` is the list of values for row `i` across `rowDimensionNames`
    List<List<String>> rowHeaders;

    /// each element is a list of column-dimension values for the column
    /// `columnHeaders[j]` is the list of values for column `j` across `columnDimensionNames`
    List<List<String>> columnHeaders;

    /// rectangular matrix of size `rowHeaders.size()` x `columnHeaders.size()`;
    /// each cell is a map alias -> value, e.g. `cells[r][c] => Map(alias -> aggregatedValue)`.
    List<List<Map<String, Object>>> cells;
}
