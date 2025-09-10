package org.nmcpye.datarun.jpa.etl.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/// A generic descriptor for an analytics model
///
/// @author Hamza Assada
/// @since 07/09/2025
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEntity {
    /// e.g., "household_surveys" or "household_children"
    private String uid;

    /// "Household Surveys"
    private String displayName;

    /// "Each row represents one household survey submission."
    private String description;

    /// "analytics.mv_household_surveys"
    private String underlyingViewName;
    private List<AnalyticsAttribute> attributes;
    private List<AnalyticsRelationship> relationships;
}
