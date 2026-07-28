package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.repository.UpdateAccessDeniedException;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AssignmentV1Resource.V1)
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentV1Resource extends JpaBaseResource<Assignment> {

    static final String V1 = ApiVersion.API_V1 + "/assignments";

    private final Logger log = LoggerFactory.getLogger(AssignmentV1Resource.class);
    private final AssignmentService assignmentService;

    public AssignmentV1Resource(
        AssignmentService assignmentService,
        AssignmentRepository assignmentRepository
    ) {
        super(assignmentService, assignmentRepository);
        this.assignmentService = assignmentService;
    }

    @Override
    @GetMapping("")
    protected ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest) {
        return page(assignmentService.findAllReleasedWork(queryRequest, null));
    }

    @GetMapping("forms")
    protected ResponseEntity<PagedResponse<?>> getAllReleasedForms(
        QueryRequest queryRequest,
        @RequestBody(required = false) String jsonQuery,
        @RequestParam(name = "referenceVersion", required = false, defaultValue = "0")
        int referenceVersion
    ) {
        return page(assignmentService.getAllReleasedWorkDto(
            queryRequest,
            jsonQuery,
            referenceVersion
        ));
    }

    @PostMapping("forms")
    protected ResponseEntity<PagedResponse<?>> queryAllForms(
        QueryRequest queryRequest,
        @RequestBody(required = false) String jsonQuery,
        @RequestParam(name = "referenceVersion", required = false, defaultValue = "0")
        int referenceVersion,
        @AuthenticationPrincipal CurrentUserDetails user
    ) {
        requireResourceApiAccess(user);
        return page(assignmentService.getAllUserAccessibleDto(
            queryRequest,
            jsonQuery,
            referenceVersion
        ));
    }

    @GetMapping("/updatePaths")
    public ResponseEntity<String> updatePaths(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false")
        boolean forceUpdate,
        @AuthenticationPrincipal CurrentUserDetails user
    ) {
        requireResourceApiAccess(user);
        if (!resourceApiAuthorization.canManage(user)) {
            throw new UpdateAccessDeniedException("AccessDenied");
        }
        log.debug("REST request to update assignment paths");
        try {
            if (forceUpdate) {
                assignmentService.forceUpdatePaths();
            } else {
                assignmentService.updatePaths();
            }
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception exception) {
            log.error("Error occurred while updating paths", exception);
            throw new IllegalQueryException(
                new ErrorMessage(ErrorCode.E1003, exception.getMessage())
            );
        }
    }

    private <T> ResponseEntity<PagedResponse<?>> page(Page<T> processedPage) {
        String next = PagingConfigurator.createNextPageLink(processedPage);
        PagedResponse<T> response = PagingConfigurator.initPageResponse(
            processedPage,
            next,
            getName()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    protected String getName() {
        return "assignments";
    }
}
