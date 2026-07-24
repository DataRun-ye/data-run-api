package org.nmcpye.datarun.web.rest.v1.referenceentry;

import org.nmcpye.datarun.web.common.PagedResponse;
import org.nmcpye.datarun.web.query.QueryRequest;

public interface ReferenceEntryV1Service {

    PagedResponse<ReferenceEntryV1Dto> getForAssignment(String assignmentUid, QueryRequest queryRequest);
}
