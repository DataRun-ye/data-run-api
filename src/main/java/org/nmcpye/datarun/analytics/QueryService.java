package org.nmcpye.datarun.analytics;

import org.nmcpye.datarun.analytics.dto.GridResponseFormat;
import org.nmcpye.datarun.analytics.dto.QueryRequest;
import org.nmcpye.datarun.analytics.dto.QueryResponse;

import java.util.Set;

/**
 * PivotQueryService is the application-facing service used by controllers to perform pivot queries.
 * <p>
 * Responsibilities:
 * - Validate and orchestrate incoming PivotQueryRequest (translate measures via MeasureValidationService).
 * - Use PivotMetadataService to resolve fields and to validate requested dimensions/measures.
 * - Call PivotQueryBuilder to build and execute queries.
 * - Format results in requested output format: TABLE_ROWS (list of rows) or PIVOT_MATRIX (row/col matrix).
 * - Apply ACL (allowedTeamUids) and other server-side constraints regardless of client input.
 * <p>
 * The service returns a PivotQueryResponse object which includes metadata (columns/aliases),
 * result rows (or pivot matrix), and optional pagination metadata (totalCount / totalPages).
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */
public interface QueryService {
    /**
     * Execute a pivot query and return the requested format.
     *
     * @param request         pivot request describing template, dims, measures, filters and pagination
     * @param format          desired output format (TABLE_ROWS | PIVOT_MATRIX)
     * @param allowedTeamUids server-side ACL allowing only these teams (maybe null to allow all)
     * @return PivotQueryResponse containing result payload and metadata
     */
    QueryResponse query(QueryRequest request,
                        GridResponseFormat format, Set<String> allowedTeamUids);
}
