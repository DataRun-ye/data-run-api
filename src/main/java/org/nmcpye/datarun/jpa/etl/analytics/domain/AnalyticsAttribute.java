package org.nmcpye.datarun.jpa.etl.analytics.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nmcpye.datarun.analytics.pivot.dto.DataType;

/// @author Hamza Assada
/// @since 07/09/2025
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsAttribute {
    /// e.g., "is_urban"
    private String uid;

    /// "Is Urban"
    private String displayName;

    /// ENUM: NUMERIC, BOOLEAN, TEXT, DATE
    private DataType dataType;

    /// "is_urban"
    private String underlyingColumnName;

    /// Can you group/filter by this? (e.g., team_name)
    private boolean isDimension;

    /// Can you aggregate this? (e.g., child_age)
    private boolean isMeasure;
}

