package org.nmcpye.datarun.analytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Request payload for a pivot query (template-mode-first).
 * <pre>
 *
 * Fields:
 * - templateId (String, required): template UID (form) — used by metadata resolution & validation.
 * - templateVersionId (String, required): template version UID — used by metadata resolution.
 *
 * - dimensions (List<String>): backwards-compatible list of dimension ids (e.g. "team_uid", "etc_uid", "etc:<uid>").
 * - rowDimensions (List<String>): preferred for matrix mode (explicit row dimensions).
 * - columnDimensions (List<String>): preferred for matrix mode (explicit column dimensions).
 *
 * - measures (List<MeasureRequest>): requested measures (client-specified).
 * - filters (List<FilterDto>): global filters applied to the facts (see FilterDto for details).
 *
 * - from / to (LocalDateTime): time window for submission_completed_at (applied to WHERE).
 * - sorts (List<SortDto>): order specifiers that reference a dimension, fact column or a measure alias.
 * - limit / offset: pagination for table_rows.
 *
 * - allowedTeamUids (Set<String>): server-side ACL (applied to WHERE team_uid IN (...)).
 * - autoRenameAliases (boolean): whether duplicate alias auto-rename is permitted, default false.
 * - tolerantOrdering (boolean): hints the builder to be tolerant about ordering when alias/field conflicts happen.
 * </pre>
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
@Data
@Builder
public class QueryRequest {
    // Template context for template-mode-first
    @NotNull
    private String templateId;
    @NotNull
    private String templateVersionId;

    // Backwards-compatible generic list of dimensions (old behavior)
    // list of dimension ids (e.g. "team_id", "element_id", "etc:123")
    private List<String> dimensions;

    // New: explicit row and column dims (preferred for PIVOT_MATRIX)
    private List<String> rowDimensions; // required for matrix
    private List<String> columnDimensions; // usually 1, can be composite

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
    private Integer limit = 100; // rows limit (for TABLE_ROWS or for matrix rows)
    @Builder.Default
    private Integer offset = 0;


    /**
     * behaviour flags
     */
    @Builder.Default
    private Boolean autoRenameAliases = false; // default: error on duplicate aliases

    /**
     * PivotQueryService can pass tolerantOrdering = true to
     * PivotQueryBuilder to override strictOrderValidation.
     */
    @Builder.Default
    Boolean tolerantOrdering = false; // default false

    /**
     * optional ACL override (server should validate)
     */
    private Set<String> allowedTeamUids;
}
