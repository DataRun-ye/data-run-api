package org.nmcpye.datarun.web.rest.v1.stepinstance;

import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;
import org.nmcpye.datarun.jpa.stagesubmission.repository.StageSubmissionRepository;
import org.nmcpye.datarun.jpa.stagesubmission.service.StageSubmissionService;
import org.nmcpye.datarun.jpa.team.service.TeamService;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.utils.JsonFlattener;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequestValidator;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for managing {@link StageInstance}.
 */
@RestController
@RequestMapping(value = {StepInstanceResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class StepInstanceResource
    extends JpaBaseResource<StageInstance> {
    protected static final String NAME = "/stepInstance";
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    final private StageSubmissionService instanceService;
    final TeamService teamService;

    public StepInstanceResource(StageSubmissionService instanceService,
                                StageSubmissionRepository
                                          dataFormSubmissionRepository,
                                TeamService teamService) {
        super(instanceService, dataFormSubmissionRepository);
        this.instanceService = instanceService;
        this.teamService = teamService;
    }

    @Override
    public ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest) {
        QueryRequestValidator.validate(queryRequest);

//        Page<StepInstance> resultPage = queryService.query(queryRequest, getEntityClass());
        Page<StageInstance> resultPage = getList(queryRequest, null);

        Page<Map<String, Object>> processedPage = resultPage.map(submission -> {
            Map<String, Object> formData = submission.getInstanceData();
            if (queryRequest.isFlatten()) {
                formData = JsonFlattener.flatten(formData);
            }
            return formData;
        });

        String next = createNextPageLink(processedPage);

        PagedResponse<Map<String, Object>> response = new
            PagedResponse<>(processedPage, getName(), next);
        return ResponseEntity.ok(response);
    }


    // temporary returning the submission object instead of just the submission formData map
//    @RequestMapping(name = "objects", method = {RequestMethod.GET, RequestMethod.POST})
    @RequestMapping(value = "objects", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<PagedResponse<?>> getObjectSubmissions(QueryRequest queryRequest,
                                                                 @RequestBody(required = false) String jsonQuery) {
        QueryRequestValidator.validate(queryRequest);
//        Page<StepInstance> resultPage = queryService.query(queryRequest, getEntityClass());
        Page<StageInstance> resultPage = getList(queryRequest, jsonQuery);

        Page<StageInstance> processedPage = postProcess(resultPage, queryRequest);

        String next = createNextPageLink(processedPage);

        PagedResponse<StageInstance> response = new
            PagedResponse<>(processedPage, getName(), next);
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/existByCode")
//    public ResponseEntity<FindExistingSubmissionsDto> checkExisting(@RequestBody FindExistingVM findExisting) {
//
//        final var result = instanceService.findExistingAndMissingOrgUnitCodes(findExisting.getCodes(), findExisting.getForm());
//        return ResponseEntity.ok(result);
//    }

    private Page<StageInstance> postProcess(Page<StageInstance> resultPage, QueryRequest queryRequest) {
        return resultPage.map(submission -> {
            Map<String, Object> formData = submission.getInstanceData();
            if (queryRequest.isFlatten()) {
                formData = FormSubmissionDataUtil.flatten(formData, false, true);
            }
            submission.setInstanceData(formData);
            return submission;
        });
    }

    @Override
    protected void saveEntity(StageInstance entity, EntitySaveSummaryVM summary) {
        try {
            super.saveEntity(entity, summary);
        } catch (Exception e) {
            log.error("REST Error Saving entity {}:{}", getEntityClass().getSimpleName(), entity.getUid());
            summary.getFailed().put(entity.getUid(), e.getMessage());
        }
    }

    @Override
    protected String getName() {
        return "dataSubmission";
    }
}
