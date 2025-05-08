package org.nmcpye.datarun.web.rest.mongo.submission;

import org.nmcpye.datarun.common.FindExistingSubmissionsDto;
import org.nmcpye.datarun.drun.postgres.service.TeamService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.utils.JsonFlattener;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.nmcpye.datarun.web.rest.mongo.submission.DataFormSubmissionResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.mongo.submission.DataFormSubmissionResource.V1;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormSubmissionResource
    extends MongoBaseResource<DataFormSubmission> {
    protected static final String NAME = "/dataSubmission";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    final private GenericQueryService queryService;
    final private DataFormSubmissionService dataFormSubmissionService;
    final TeamService teamService;

    public DataFormSubmissionResource(DataFormSubmissionService dataFormSubmissionService,
                                      DataFormSubmissionRepository
                                          dataFormSubmissionRepository,
                                      GenericQueryService queryService,
                                      TeamService teamService) {
        super(dataFormSubmissionService, dataFormSubmissionRepository);
        this.queryService = queryService;
        this.dataFormSubmissionService = dataFormSubmissionService;
        this.teamService = teamService;
    }

    @Override
    public ResponseEntity<PagedResponse<?>> getAll(QueryRequest queryRequest) {
        QueryRequestValidator.validate(queryRequest);

        Page<DataFormSubmission> resultPage = queryService.query(queryRequest, getEntityClass());

        Page<Map<String, Object>> processedPage = resultPage.map(submission -> {
            Map<String, Object> formData = submission.getFormData();
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
//        Page<DataFormSubmission> resultPage = queryService.query(queryRequest, getEntityClass());
        Page<DataFormSubmission> resultPage = getList(queryRequest, jsonQuery);

        Page<DataFormSubmission> processedPage = postProcess(resultPage, queryRequest);

        String next = createNextPageLink(processedPage);

        PagedResponse<DataFormSubmission> response = new
            PagedResponse<>(processedPage, getName(), next);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/existByCode")
    public ResponseEntity<FindExistingSubmissionsDto> checkExisting(@RequestBody FindExistingVM findExisting) {

        final var result = dataFormSubmissionService.findExistingAndMissingOrgUnitCodes(findExisting.getCodes(), findExisting.getForm());
        return ResponseEntity.ok(result);
    }

    private Page<DataFormSubmission> postProcess(Page<DataFormSubmission> resultPage, QueryRequest queryRequest) {
        return resultPage.map(submission -> {
            Map<String, Object> formData = submission.getFormData();
            if (queryRequest.isFlatten()) {
                formData = FormSubmissionDataUtil.flatten(formData, false, true);
            }
            submission.setFormData(formData);
            return submission;
        });
    }

    @Override
    protected void saveEntity(DataFormSubmission entity, EntitySaveSummaryVM summary) {
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

    @Override
    protected void applySecurityConstraints(Query query) {
        final CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();

        query.addCriteria(Criteria.where("form").in(user.getUserFormsUIDs()).and("team")
            .in(user.getUserTeamsUIDs()));

    }
}
