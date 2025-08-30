package org.nmcpye.datarun.analytics.pivot;

import org.nmcpye.datarun.analytics.pivot.dto.PivotOutputFormat;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryRequest;
import org.nmcpye.datarun.analytics.pivot.dto.PivotQueryResponse;

import java.util.Set;

/**
 * Service that validates measure requests, builds the query via PivotQueryBuilder,
 * executes it, and returns rows as List<Map<String,Object>>. Template-mode-first.
 *
 * @author Hamza Assada
 * @since 27/08/2025
 */

public interface PivotQueryService {
    PivotQueryResponse query(PivotQueryRequest request, PivotOutputFormat format,
                             Set<String> allowedTeamIdsFromAuth);

    PivotQueryResponse execute(PivotQueryRequest request, Set<String> allowedTeamIdsFromAuth);

    long count(PivotQueryRequest request);
}
