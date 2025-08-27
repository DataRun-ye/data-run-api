package org.nmcpye.datarun.analytics.pivot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnDto {
    private String name;   // label/alias
    private String type;   // textual representation (e.g. "value_num","value_text")
    private String source; // e.g. "dimension"|"measure"
}
