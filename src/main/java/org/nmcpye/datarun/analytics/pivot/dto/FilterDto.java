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
public class FilterDto {
    private String field; // dimension or alias or fact column
    private String op;    // =, !=, IN, >, <, >=, <=, LIKE, ILIKE
    private Object value; // scalar or list for IN
}
