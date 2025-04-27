package org.nmcpye.datarun.web.rest.mongo.submission;

import org.nmcpye.datarun.drun.postgres.service.TeamService;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.mapping.importsummary.EntitySaveSummaryVM;
import org.nmcpye.datarun.mongo.repository.DataFormSubmissionRepository;
import org.nmcpye.datarun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.utils.JsonFlattener;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.MongoBaseResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/dataSubmission")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormSubmissionResource
    extends MongoBaseResource<DataFormSubmission> {

    final private GenericQueryService queryService;
    final TeamService teamService;

    public DataFormSubmissionResource(DataFormSubmissionService dataFormSubmissionService,
                                      DataFormSubmissionRepository
                                          dataFormSubmissionRepository,
                                      GenericQueryService queryService,
                                      TeamService teamService) {
        super(dataFormSubmissionService, dataFormSubmissionRepository);
        this.queryService = queryService;
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
    @GetMapping("objects")
    public ResponseEntity<PagedResponse<?>> getObjectSubmissions(QueryRequest queryRequest) {
        QueryRequestValidator.validate(queryRequest);

        Page<DataFormSubmission> resultPage = queryService.query(queryRequest, getEntityClass());

        Page<DataFormSubmission> processedPage = resultPage.map(submission -> {
            Map<String, Object> formData = submission.getFormData();
            if (queryRequest.isFlatten()) {
                formData = FormSubmissionDataUtil.flatten(formData, false, true);
            }
            submission.setFormData(formData);
            return submission;
        });

        String next = createNextPageLink(processedPage);

        PagedResponse<DataFormSubmission> response = new
            PagedResponse<>(processedPage, getName(), next);
        return ResponseEntity.ok(response);
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
}
