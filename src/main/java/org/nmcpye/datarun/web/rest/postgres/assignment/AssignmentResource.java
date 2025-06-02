package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.List;

import static org.nmcpye.datarun.web.rest.postgres.assignment.AssignmentResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.assignment.AssignmentResource.V1;

/**
 * REST Extended controller for managing {@link Assignment}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
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
                                                         @RequestBody(required = false) String jsonQuery) throws Exception {
        final var userLogin = SecurityUtils.getCurrentUserLoginOrThrow();
        log.debug("REST request to getAll {}:{}", userLogin, getName());

        Page<AssignmentWithAccessDto> processedPage = assignmentService.getAllUserAccessibleDto(queryRequest, jsonQuery);

        String next = createNextPageLink(processedPage);

        PagedResponse<AssignmentWithAccessDto> response = initPageResponse(processedPage, next);
        return ResponseEntity.ok(response);
    }

    @Override
    protected String getName() {
        return "assignments";
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveOne(Assignment entity) {
        log.debug("REST request to saveOne {}", getName());
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
        }
        return super.saveOne(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> saveReturnSaved(Assignment entity) {
        log.debug("REST request to saveOne, return saved {}", getName());
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
        }
        return super.saveReturnSaved(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Assignment> updateEntity(String uid, Assignment entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<Assignment> entities) {
        return super.saveAll(entities);
    }

    @GetMapping("/updatePaths")
    public ResponseEntity<String> updatePaths(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false") boolean forceUpdate) {
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
