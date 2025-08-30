package org.nmcpye.datarun.web.rest.v1.pivotgrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.analytics.pivot.PivotMetadataService;
import org.nmcpye.datarun.analytics.pivot.PivotQueryService;
import org.nmcpye.datarun.analytics.pivot.dto.PivotOutputFormat;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryRequest;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * @author Hamza Assada
 * @since 27/08/2025
 */
@RestController
@RequestMapping("/api/v1/analytics/pivot")
@RequiredArgsConstructor
@Slf4j
public class PivotQueryController {

    private final PivotMetadataService pivotMetadataService;
    private final PivotQueryService pivotQueryService;

    /**
     * Metadata: get the metadata for a particular template/version (template-mode)
     */
    @GetMapping("/metadata")
    public ResponseEntity<?> metadata(@RequestParam String templateId, @RequestParam String templateVersionId) {
        return ResponseEntity.ok(pivotMetadataService.getMetadataForTemplate(templateId, templateVersionId));
    }

    /**
     * Execute a pivot query
     */
    @PostMapping("/query")
    public ResponseEntity<PivotQueryResponse> query(@RequestBody PivotQueryRequest request,
                                                    @RequestParam(name = "format", defaultValue = "TABLE_ROWS") PivotOutputFormat format) {
        // You may resolve allowedTeamIds from security context. For now pass null (service will accept null).
        Set<String> allowedTeams = null;

//        PivotQueryResponse resp = pivotQueryService.query(request, format, allowedTeams);
        PivotQueryResponse resp = pivotQueryService.execute(request, allowedTeams);
        return ResponseEntity.ok(resp);
    }

    /**
     * Resolve a single field (helpful for UI). Accepts either "etc:123" or dataElement uid.
     */
    @GetMapping("/field")
    public ResponseEntity<?> resolveField(@RequestParam String id, @RequestParam String templateId, @RequestParam String templateVersionId) {
        return ResponseEntity.of(pivotMetadataService.resolveFieldByUidOrId(id, templateId, templateVersionId));
    }

    /**
     * Optional: count endpoint to get total rows for pagination without heavy result payload.
     */
    @PostMapping("/count")
    public ResponseEntity<Long> count(@RequestBody PivotQueryRequest request) {
        long total = pivotQueryService.count(request);
        return ResponseEntity.ok(total);
    }
}
