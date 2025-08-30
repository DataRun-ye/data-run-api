package org.nmcpye.datarun.analytics.pivot.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;


/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PivotQueryRequest {
    // Template context for template-mode-first
    @NonNull
    private String templateId;
    @NonNull
    private String templateVersionId;

    // Backwards-compatible generic list of dimensions (old behavior)
    // list of dimension ids (e.g. "team_id", "element_id", "etc:123")
    private List<String> dimensions;

    // New: explicit row and column dims (preferred for PIVOT_MATRIX)
    private List<String> rowDimensions;
    private List<String> columnDimensions;

    // list of measure requests
    private List<MeasureRequest> measures;

    // simple filters
    private List<FilterDto> filters;

    // optional explicit time window (submission_completed_at)
    private LocalDateTime from;
    private LocalDateTime to;

    // sort
    private List<SortDto> sorts;

    // pagination
    @Builder.Default
    private Integer limit = 100;
    @Builder.Default
    private Integer offset = 0;

    /**
     * optional ACL override (server should validate)
     */
    private Set<String> allowedTeamUids;

    /**
     * behaviour flags
     */
    @Builder.Default
    private Boolean autoRenameAliases = false; // default: error on duplicate aliases

    /**
     * PivotQueryService can pass tolerantOrdering = true to
     * PivotQueryBuilder to override strictOrderValidation.
     */
    Boolean tolerantOrdering = false; // default false
}
