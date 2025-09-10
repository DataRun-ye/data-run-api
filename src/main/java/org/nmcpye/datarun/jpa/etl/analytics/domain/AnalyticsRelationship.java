package org.nmcpye.datarun.jpa.etl.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// @author Hamza Assada
/// @since 07/09/2025
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsRelationship {
    /// "household_children"
    private String toEntityUid;

    /// "submission_uid"
    private String joinFromAttribute;

    /// "submission_uid"
    private String joinToAttribute;
}
