package org.nmcpye.datarun.jpa.datasubmission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author Hamza Assada
 * @since 25/09/2025
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionManifestDto {
    private String manifestUid;
    private String submissionUid;
    private String templateVersionUid;
    private Instant extractionRunTs;
    private List<ManifestElement> elements;
    private List<ManifestRepeatSummary> repeatSummaries;
    private ManifestSummary summary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManifestElement {
        private String templateElementUid;
        private String canonicalElementUid;
        private String namePath;
        private String valueSnippet;
        private Object valueNormalized; // String or Number or Boolean
        private boolean isNull;
        private String extractionConfidence; // low|medium|high
        private String repeatInstanceId;
        private List<String> warnings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManifestRepeatSummary {
        private String repeatInstanceId;
        private List<String> ancestorChain;
        private Integer ordinalIndex;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManifestSummary {
        private Integer elementCount;
        private Double nullRateEstimate;
        private Map<String, Object> extra; // extensible
    }
}

