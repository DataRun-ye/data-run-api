package org.nmcpye.datarun.analytics.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Simple matrix representation:
 * - rowDimensionNames: e.g. ["region", "district"]
 * - columnDimensionNames: e.g. ["gender"]
 * - measureAliases: e.g. ["total_nets_distributed"]
 * - rowHeaders: each element is a list of row-dimension values for the row
 * - columnHeaders: each element is a list of column-dimension values for the column
 * - cells: rectangular matrix of size rowHeaders.size() x columnHeaders.size(); each cell is a map alias -> value
 *
 * @author Hamza Assada
 * @since 30/08/2025
 */
@Value
@Builder
public class PivotMatrixDto {
    List<String> rowDimensionNames;
    List<String> columnDimensionNames;
    List<String> measureAliases;

    List<List<String>> rowHeaders;   // rowHeaders[i] is the list of values for row i across rowDimensionNames
    List<List<String>> columnHeaders; // columnHeaders[j] is the list of values for column j across columnDimensionNames

    // cells[r][c] => Map(alias -> aggregatedValue)
    List<List<Map<String, Object>>> cells;
}
