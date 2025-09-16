package org.nmcpye.datarun.web.rest.v1.analytics;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.analytics.dto.AnalyticsAttributeDto;
import org.nmcpye.datarun.jpa.analytics.service.AnalyticsMetadataServiceNew;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Hamza Assada
 * @since 12/09/2025
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsMetadataServiceNew metadataService;

    /**
     * GET /api/analytics/meta/{templateVersionUid}
     * <p>
     * Retrieves the list of all queryable attributes (dimensions and measures)
     * for a specific data template version. This metadata is used by the frontend
     * to dynamically build UI controls for creating analytics queries.
     *
     * @param templateVersionUid The unique identifier of the template version.
     * @return A list of analytics attributes.
     */
    @GetMapping("/meta/{templateVersionUid}")
    public ResponseEntity<List<AnalyticsAttributeDto>> getMetadata(@PathVariable String templateVersionUid) {
        List<AnalyticsAttributeDto> attributes = metadataService.getAttributes(templateVersionUid);
        return ResponseEntity.ok(attributes);
    }

    // The POST /query endpoint will be added in the next step.
}
