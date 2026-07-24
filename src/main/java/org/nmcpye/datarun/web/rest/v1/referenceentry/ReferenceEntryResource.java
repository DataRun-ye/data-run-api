package org.nmcpye.datarun.web.rest.v1.referenceentry;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.common.ApiVersion;
import org.nmcpye.datarun.web.common.PagedResponse;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = { ReferenceEntryResource.CUSTOM, ReferenceEntryResource.V1 })
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@RequiredArgsConstructor
public class ReferenceEntryResource {

    static final String NAME = "/assignments/{assignmentUid}/referenceEntries";
    static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    static final String V1 = ApiVersion.API_V1 + NAME;

    private final ReferenceEntryV1Service service;

    @GetMapping
    public ResponseEntity<PagedResponse<ReferenceEntryV1Dto>> getForAssignment(
            @PathVariable String assignmentUid,
            QueryRequest queryRequest) {
        return ResponseEntity.ok(service.getForAssignment(assignmentUid, queryRequest));
    }
}
