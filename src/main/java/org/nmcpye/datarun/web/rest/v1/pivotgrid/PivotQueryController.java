package org.nmcpye.datarun.web.rest.v1.pivotgrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.analytics.pivot.PivotMetadataService;
import org.nmcpye.datarun.analytics.pivot.PivotQueryService;
import org.nmcpye.datarun.analytics.pivot.dto.PivotOutputFormat;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryRequest;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * REST controller exposing pivot metadata and query endpoints.
 * <p>
 * Endpoints:
 * - GET /api/v1/analytics/pivot/metadata?templateId=...&templateVersionId=...
 * Returns the PivotMetadataResponse used by the frontend to build drag/drop pivot UI.
 * <p>
 * - POST /api/v1/analytics/pivot/query?format={TABLE_ROWS|PIVOT_MATRIX}
 * Accepts PivotQueryRequest and returns PivotQueryResponse formatted according to the requested shape.
 * <p>
 * - GET /api/v1/analytics/pivot/field?id=...&templateId=...&templateVersionId=...
 * Resolve a single field (useful for interactive UIs).
 * <p>
 * Controller responsibilities:
 * - Basic request validation, translation of query params, and calling PivotQueryService.
 * - Convert service exceptions into appropriate HTTP codes (400 for validation, 403 for ACL, 500 for server errors).
 * - The controller should not perform measure validation or heavy logic — that belongs to service/validation components.
 *
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
     * Return metadata required to render the pivot UI for a template version.
     */
    @GetMapping("/metadata")
    public ResponseEntity<?> metadata(@RequestParam String templateId, @RequestParam String templateVersionId) {
        return ResponseEntity.ok(pivotMetadataService
            .getMetadataForTemplate(templateId, templateVersionId));
    }

    /**
     * Execute pivot query. The server-side ACL (allowedTeamUids) must be applied regardless of client-supplied filters.
     *
     * @param request pivot request (template context, dims, measures, filters, pagination)
     * @param format  desired output format
     * @return PivotQueryResponse
     */
    @PostMapping(value = "/query", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PivotQueryResponse> query(
        @RequestBody PivotQueryRequest request,
        @RequestParam(name = "format", defaultValue = "TABLE_ROWS") PivotOutputFormat format
    ) {
        // Resolve allowed teams from auth normally; for now pass null.
        Set<String> allowedTeams = null;
        PivotQueryResponse resp = pivotQueryService.query(request, format, allowedTeams);
        return ResponseEntity.ok(resp);
    }

    /**
     * Resolve a single field by id (template-scoped lookup).
     */
    @GetMapping("/field")
    public ResponseEntity<?> resolveField(@RequestParam String id, @RequestParam String templateId, @RequestParam String templateVersionId) {
        return ResponseEntity.of(pivotMetadataService.resolveFieldByUidOrId(id, templateId, templateVersionId));
    }

//    @PostMapping("/count")
//    public ResponseEntity<Long> count(@RequestBody PivotQueryRequest request) {
//        long total = pivotQueryService.count(request);
//        return ResponseEntity.ok(total);
//    }
}
