package org.nmcpye.datarun.web.rest.v1.referenceentry;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ReferenceEntryCustomResource.CUSTOM)
@PreAuthorize(
    "hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \""
        + AuthoritiesConstants.USER + "\")")
@RequiredArgsConstructor
public class ReferenceEntryCustomResource {

    static final String CUSTOM =
        ApiVersion.API_CUSTOM + "/assignments/{assignmentUid}/referenceEntries";

    private final ReferenceEntryV1Service service;

    @GetMapping
    public ResponseEntity<PagedResponse<ReferenceEntryV1Dto>> getForAssignment(
        @PathVariable String assignmentUid,
        QueryRequest queryRequest
    ) {
        return ResponseEntity.ok(
            service.getBaselineForAssignment(assignmentUid, queryRequest)
        );
    }
}
