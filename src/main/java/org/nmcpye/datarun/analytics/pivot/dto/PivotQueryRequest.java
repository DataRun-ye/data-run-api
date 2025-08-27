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
    // Template context (template-mode first)
    @NonNull
    private String templateId;
    @NonNull
    private String templateVersionId;

    // list of dimension ids (e.g. "team_id", "element_id", "etc:123")
    private List<String> dimensions;

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
     *  behaviour flags
     */
    @Builder.Default
    private Boolean autoRenameAliases = false; // default: error on duplicate aliases
}
