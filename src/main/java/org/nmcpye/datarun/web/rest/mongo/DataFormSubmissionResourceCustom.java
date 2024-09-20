package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/dataSubmissions")
public class DataFormSubmissionResourceCustom
    extends AbstractMongoResource<DataFormSubmission> {

    public DataFormSubmissionResourceCustom(DataFormSubmissionService dataFormSubmissionService,
                                            DataFormSubmissionRepositoryCustom dataFormSubmissionRepositoryCustom) {
        super(dataFormSubmissionService, dataFormSubmissionRepositoryCustom);
    }

    @GetMapping("/form")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getByForm(
        @RequestParam(name = "filter", required = false) String filter,
        @ParameterObject Pageable pageable,
        @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
        @RequestParam(name = "flatten", required = false, defaultValue = "true") boolean flatten) {

        // Handle pageable unpaged condition
        if (!paging) {
            pageable = Pageable.unpaged();
        }

        // Fetch submissions based on filter or all submissions
        Page<DataFormSubmission> submissionsPage;
        if (filter != null) {
            submissionsPage = ((DataFormSubmissionService) identifiableService)
                .findAllByForm(List.of(filter), pageable);
        } else {
            submissionsPage = identifiableService.findAllByUser(pageable);
        }

        // Process each submission based on the flattening requirement
        List<Map<String, Object>> processedSubmissions = submissionsPage.getContent()
            .stream()
            .map(submission -> processSubmission(submission, flatten))
            .collect(Collectors.toList());

        // Build paged response
        PagedResponse<Map<String, Object>> response = buildPagedResponse(
            submissionsPage, processedSubmissions, "results");

        return ResponseEntity.ok(response);
    }

    /**
     * Processes a DataFormSubmission object into a map and optionally flattens form data.
     *
     * @param submission the DataFormSubmission to process
     * @param flatten    whether to flatten the formData or not
     * @return the processed map representing the submission
     */
    private Map<String, Object> processSubmission(DataFormSubmission submission, boolean flatten) {
        // Initialize data map with basic fields
        Map<String, Object> data = new HashMap<>();
        data.put("id", submission.getId());
        data.put("uid", submission.getUid());
        data.put("deleted", submission.getDeleted());
        data.put("form", submission.getForm());
        data.put("version", submission.getVersion());
        data.put("orgUnit", submission.getOrgUnit());
        data.put("activity", submission.getActivity());
        data.put("team", submission.getTeam());
        data.put("createdBy", submission.getCreatedBy());
        data.put("createdDate", submission.getCreatedDate());
        data.put("lastModifiedBy", submission.getLastModifiedBy());
        data.put("lastModifiedDate", submission.getLastModifiedDate());
        data.put("finishedEntryTime", submission.getFinishedEntryTime());
        data.put("startEntryTime", submission.getStartEntryTime());

        // Retrieve and optionally flatten the formData
        Map<String, Object> formData = submission.getFormData();
        if (flatten) {
            formData = JsonFlattener.flatten(formData);
        }

        // Merge formData into data
        data.putAll(formData);

        return data;
    }

    /**
     * Builds a paged response object for the given submissions and metadata.
     *
     * @param submissionsPage      the paginated submissions
     * @param processedSubmissions the processed submissions
     * @param entityName           the entity name for the response
     * @return the paged response
     */
    private PagedResponse<Map<String, Object>> buildPagedResponse(Page<DataFormSubmission> submissionsPage,
                                                                  List<Map<String, Object>> processedSubmissions,
                                                                  String entityName) {
        PagedResponse<Map<String, Object>> response = new PagedResponse<>();
        response.setPaging(submissionsPage.getPageable().isPaged());
        response.setPage(submissionsPage.getNumber());
        response.setPageCount(submissionsPage.getTotalPages());
        response.setTotal(submissionsPage.getTotalElements());
        response.setPageSize(submissionsPage.getSize());
        response.setItems(processedSubmissions);
        response.setEntityName(entityName);

        return response;
    }

    @Override
    protected String getName() {
        return "dataSubmissions";
    }
}
