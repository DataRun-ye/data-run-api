package org.nmcpye.datarun.jpa.analytics.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 12/09/2025
 */
@Getter
@Setter
public class FilterClause {
    @NotEmpty
    private String field;
    @NotEmpty
    private String operator; // e.g., "IN", "EQUALS", "GREATER_THAN"
    @NotEmpty
    private List<String> values;
}
