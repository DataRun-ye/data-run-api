package org.nmcpye.datarun.assignmentshadow;

import java.util.List;
import java.util.Map;

public record AssignmentCaptureComparisonReport(
    AssignmentCaptureSurface surface,
    Map<AssignmentCaptureComparisonCategory, Long> categories,
    List<AssignmentCaptureShadowResult> shadowResults
) {
    public AssignmentCaptureComparisonReport {
        categories = Map.copyOf(categories);
        shadowResults = List.copyOf(shadowResults);
    }

    public static AssignmentCaptureComparisonReport skipped(
        AssignmentCaptureSurface surface
    ) {
        return new AssignmentCaptureComparisonReport(
            surface,
            Map.of(),
            List.of()
        );
    }

    public long count(AssignmentCaptureComparisonCategory category) {
        return categories.getOrDefault(category, 0L);
    }
}
