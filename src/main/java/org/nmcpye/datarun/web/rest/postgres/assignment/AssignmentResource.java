package org.nmcpye.datarun.web.rest.postgres.assignment;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowinstance.repository.FlowInstanceRepository;
import org.nmcpye.datarun.jpa.flowinstance.service.FlowInstanceService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.errors.BadRequestAlertException;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

import static org.nmcpye.datarun.web.rest.postgres.assignment.AssignmentResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.assignment.AssignmentResource.V1;

/**
 * REST Extended controller for managing {@link FlowInstance}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class AssignmentResource
    extends JpaBaseResource<FlowInstance> {
    protected static final String NAME = "/assignments";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    private final Logger log = LoggerFactory.getLogger(AssignmentResource.class);

    private final FlowInstanceService flowInstanceService;

    public AssignmentResource(FlowInstanceService flowInstanceService,
                              FlowInstanceRepository flowInstanceRepository) {
        super(flowInstanceService, flowInstanceRepository);
        this.flowInstanceService = flowInstanceService;
    }

//    @RequestMapping(value = "forms", method = {RequestMethod.GET, RequestMethod.POST})
//    protected ResponseEntity<PagedResponse<?>> getAllDto(QueryRequest queryRequest,
//                                                         @RequestBody(required = false) String jsonQuery) throws Exception {
//        final var userLogin = SecurityUtils.getCurrentUserLoginOrThrow();
//        log.debug("REST request to getAll {}:{}", userLogin, getName());
//
//        Page<AssignmentWithAccessDto> processedPage = flowInstanceService.getAllUserAccessibleDto(queryRequest, jsonQuery);
//
//        String next = createNextPageLink(processedPage);
//
//        PagedResponse<AssignmentWithAccessDto> response = initPageResponse(processedPage, next);
//        return ResponseEntity.ok(response);
//    }

    @Override
    protected String getName() {
        return "assignments";
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveOne(FlowInstance entity) {
        log.debug("REST request to saveOne {}", getName());
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
        }
        return super.saveOne(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<?> saveReturnSaved(FlowInstance entity) {
        log.debug("REST request to saveOne, return saved {}", getName());
        if (entity.getId() != null) {
            throw new BadRequestAlertException("A new entity cannot already have an ID", getName() + ":" + entity.getId().toString(), "idexists");
        }
        return super.saveReturnSaved(entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FlowInstance> updateEntity(String uid, FlowInstance entity) throws URISyntaxException {
        return super.updateEntity(uid, entity);
    }

    @Override
    @PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<FlowInstance> entities) {
        return super.saveAll(entities);
    }

    @GetMapping("/updatePaths")
    public ResponseEntity<String> updatePaths(
        @RequestParam(name = "forceUpdate", required = false, defaultValue = "false") boolean forceUpdate) {
        log.debug("REST request to update orgUnit Paths");

        try {
            if (forceUpdate) {
                flowInstanceService.forceUpdatePaths();
            } else {
                flowInstanceService.updatePaths();
            }
            return ResponseEntity.ok("Paths updated successfully");
        } catch (Exception e) {
            log.error("Error occurred while updating paths", e);
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1003, e.getMessage()));
        }
    }
}
