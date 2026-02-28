package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.queryrequest.QueryRequest;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Extended controller for managing {@link Assignment}.
 */
@RestController
@RequestMapping(value = {AssignmentResource.CUSTOM, AssignmentResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentResource
    extends JpaBaseResource<Assignment> {
    protected static final String NAME = "/assignments";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final Logger log = LoggerFactory.getLogger(AssignmentResource.class);

    private final AssignmentService assignmentService;

    public AssignmentResource(AssignmentService assignmentService,
                              AssignmentRepository assignmentRepository) {
        super(assignmentService, assignmentRepository);
        this.assignmentService = assignmentService;
    }

    @RequestMapping(value = "forms", method = {RequestMethod.GET, RequestMethod.POST})
    protected ResponseEntity<PagedResponse<?>> getAllDto(QueryRequest queryRequest,
                                                         @RequestBody(required = false) String jsonQuery,
                                                         @AuthenticationPrincipal CurrentUserDetails user) throws Exception {
        hasMinimalRightsOrThrow(user);
        log.debug("REST request to getAll {}:{}", user.getUsername(), getName());

        Page<AssignmentWithAccessDto> processedPage = assignmentService.getAllUserAccessibleDto(queryRequest, jsonQuery);

        String next = PagingConfigurator.createNextPageLink(processedPage);

        PagedResponse<AssignmentWithAccessDto> response = PagingConfigurator.initPageResponse(processedPage, next, getName());
        return ResponseEntity.ok(response);
    }

    @Override
    protected String getName() {
        return "assignments";
    }

    @GetMapping("/updatePaths")
    public ResponseEntity<String> updatePaths(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false") boolean forceUpdate,
        @AuthenticationPrincipal CurrentUserDetails user) {
        hasMinimalRightsOrThrow(user);
        log.debug("REST request to update orgUnit Paths");

        try {
            if (forceUpdate) {
                assignmentService.forceUpdatePaths();
            } else {
                assignmentService.updatePaths();
            }
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception e) {
            log.error("Error occurred while updating paths", e);
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1003, e.getMessage()));
        }
    }
}
