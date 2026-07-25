package org.nmcpye.datarun.web.rest.v1.referenceentry;

import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.apiquery.QueryRequest;

public interface ReferenceEntryV1Service {
    PagedResponse<ReferenceEntryV1Dto> getForAssignment(
        String assignmentUid,
        QueryRequest queryRequest);
}
