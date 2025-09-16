package org.nmcpye.datarun.jpa.analytics.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 12/09/2025
 */
// DTO for the incoming query request from the client.
public class QueryRequest {
    @NotEmpty
    private String templateVersionUid;

    private List<String> dimensions;
    private List<String> measures;
    private List<FilterClause> filters;
    // We can add orderBy and paging later, keeping it simple for now.

    // Getters and Setters...
}
