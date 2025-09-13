package org.nmcpye.datarun.analytics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/// Query Request payload
///
/// @author Hamza Assada
/// @since 27/08/2025
@Data
@Builder
public class QueryRequest {
    /// `templateId` (String, required): template UID (form) — used by metadata resolution & validation.
    @NotNull
    private String templateId;

    /// `templateVersionId` (String, required): template version UID — used by metadata resolution.
    @NotNull
    private String templateVersionId;

    /// list of dimension ids (e.g. "team_id", "element_id", "etc:123")
    /// treated as row dims
    private List<String> dimensions;

    /// list of measure requests (client-specified).
    private List<MeasureRequest> measures;

    /// `rowDimensions`: preferred for matrix mode (explicit row dimensions).
    private List<String> rowDimensions;

    /// `columnDimensions`: explicit column dimensions preferred for matrix mode.
    /// usually 1, and can be composite
    private List<String> columnDimensions;

    /// global filters applied
    ///
    /// @see QueryFilter for details).
    private List<QueryFilter> filters;

    /// `from` date: an optional explicit time window (`submission_completed_at`)
    private LocalDateTime from;

    /// `to` date: an optional explicit time window (`submission_completed_at`)
    private LocalDateTime to;

    /// order specifiers that reference a dimension, fact column or a measure alias.
    private List<QuerySort> sorts;

    /// pagination rows limit.
    @Builder.Default
    private Integer limit = 100;

    /// pagination offset.
    @Builder.Default
    private Integer offset = 0;

    /// behaviour flags: whether duplicate alias auto-rename is permitted.
    ///
    /// default: error on duplicate aliases
    @Builder.Default
    private Boolean autoRenameAliases = false;

    /// optional server-side ACL (applied to WHERE team_uid IN (...))
    private Set<String> allowedTeamUids;
}
