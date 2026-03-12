package org.nmcpye.datarun.web.rest.v1.assignment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.common.ApiVersion;
import org.nmcpye.datarun.web.common.PagedResponse;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.nmcpye.datarun.web.rest.util.ResponseUtil;
import org.nmcpye.datarun.web.rest.v1.assignment.dto.AssignmentV1Dto;
import org.nmcpye.datarun.web.rest.v1.assignment.dto.AssignmentWithAccessV1Dto;
import org.nmcpye.datarun.web.rest.v1.assignment.service.AssignmentV1Service;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for managing Assignments.
 * Refactored for Phase 0 stabilization.
 */
@RestController
@RequestMapping(value = { AssignmentResource.CUSTOM, AssignmentResource.V1 })
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@RequiredArgsConstructor
@Slf4j
public class AssignmentResource {
    protected static final String NAME = "/assignments";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final AssignmentV1Service v1Service;

    @GetMapping("")
    public ResponseEntity<PagedResponse<AssignmentV1Dto>> getAll(QueryRequest queryRequest) {
        log.debug("REST request to get all Assignments");
        return ResponseEntity.ok(v1Service.getAll(queryRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentV1Dto> getById(@PathVariable("id") String id) {
        log.debug("REST request to get Assignment : {}", id);
        Optional<AssignmentV1Dto> dto = v1Service.getById(id);
        return ResponseUtil.wrapOrNotFound(dto);
    }

    @RequestMapping(value = "forms", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<PagedResponse<AssignmentWithAccessV1Dto>> getAllWithAccess(
            QueryRequest queryRequest,
            @RequestBody(required = false) String jsonQuery) {
        log.debug("REST request to get Assignments with forms/access");
        return ResponseEntity.ok(v1Service.getAllWithAccess(queryRequest, jsonQuery));
    }
}
