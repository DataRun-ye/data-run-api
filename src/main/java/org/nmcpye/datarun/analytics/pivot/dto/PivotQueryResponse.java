package org.nmcpye.datarun.analytics.pivot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Unified response for pivot queries.
 * <p>
 * Fields:
 * - format (PivotOutputFormat): echo the format used.
 * - columns (List<ColumnDescriptor>): metadata for returned columns (name, alias, type).
 * - rows (List<Map<String, Object>>): used for TABLE_ROWS (list of column->value maps).
 * - matrix (PivotMatrixDto): used for PIVOT_MATRIX (rows x columns mapping with cell values) - optional.
 * - total (long): total groups count (for pagination).
 * - limit / offset: echo of pagination.
 * <p>
 * - ColumnDescriptor should describe alias, original factColumn (e.g., "value_num"/"team_uid") and dataType.
 * <pre>
 * SQL mapping:
 * - columns — derived from SELECT list: group dimensions (fact columns), aggregated aliases.
 * - rows — map each returned SQL row directly to JSON objects using the SELECT aliases.
 * - matrix — server-side transform of rows into matrix[rowKey][columnKey] = cellValue.
 * </pre>
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PivotQueryResponse {
    Long total; // optional total rows/groups count when available
    Map<String, Object> meta; // e.g. { format: "PIVOT_MATRIX", templateId: "..."}
    List<ColumnDto> columns;  // used for TABLE_ROWS
    List<Map<String, Object>> rows; // used for TABLE_ROWS (each row keyed by column id / alias)

    // matrix variant (only one of rows/columns vs matrix is filled)
    PivotMatrixDto matrix;
}
