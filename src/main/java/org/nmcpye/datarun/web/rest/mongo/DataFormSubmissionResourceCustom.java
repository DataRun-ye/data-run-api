package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.drun.mongo.service.submissionmigration.JsonFlattener;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.submission.GenericQueryService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequestValidator;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataFormSubmissionResourceCustom
    extends AbstractMongoResource<DataFormSubmission> {

    final private GenericQueryService queryService;

    public DataFormSubmissionResourceCustom(DataFormSubmissionService dataFormSubmissionService,
                                            DataFormSubmissionRepositoryCustom dataFormSubmissionRepositoryCustom, GenericQueryService queryService) {
        super(dataFormSubmissionService, dataFormSubmissionRepositoryCustom);
        this.queryService = queryService;
    }

    @GetMapping("/query")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getSubmissions(QueryRequest queryRequest) {
        QueryRequestValidator.validate(queryRequest);

        Page<DataFormSubmission> resultPage = queryService.query(queryRequest, DataFormSubmission.class);

        // Flatten the data if requested
        List<Map<String, Object>> content = resultPage.getContent().stream()
            .map(submission -> {
                Map<String, Object> formData = submission.getFormData();
                if (queryRequest.isFlatten()) {
                    formData.put("serialNumber", submission.getSerialNumber());
                    formData.put("deleted", submission.getDeleted());
                    formData.put("createdBy", submission.getCreatedBy());
                    formData.put("createdDate", submission.getCreatedDate());
                    formData.put("lastModifiedBy", submission.getLastModifiedBy());
                    formData.put("lastModifiedDate", submission.getLastModifiedDate());
                    formData.put("finishedEntryTime", submission.getFinishedEntryTime());
                    formData.put("startEntryTime", submission.getStartEntryTime());
                    formData.put("id", submission.getId());
                    formData = JsonFlattener.flatten(formData, submission.getId());
                }
                return formData;
            }).collect(Collectors.toList());

        PagedResponse<Map<String, Object>> response = new PagedResponse<>();
        response.setItems(content);
        response.setPage(resultPage.getNumber());
        response.setPageSize(resultPage.getSize());
        response.setTotal(resultPage.getTotalElements());
        response.setPageCount(resultPage.getTotalPages());
        response.setPaging(queryRequest.isPaged());
        response.setFlatten(queryRequest.isFlatten());
        response.setEntityName("result");

        return ResponseEntity.ok(response);
    }


    @Deprecated
    @GetMapping("/form")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getByForm(
        @ParameterObject Pageable pageable,
        @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
        @RequestParam(name = "filter", required = false) String filter,
        @RequestParam(name = "sn", required = false) Long sn,
        @RequestParam(name = "flatten", required = false, defaultValue = "true") boolean flatten) {

        // Handle pageable unpaged condition
        if (!paging) {
            pageable = Pageable.unpaged();
        }

        Page<DataFormSubmission> submissionsPage;
        if (filter != null) {
            if (sn != null) {
                submissionsPage = ((DataFormSubmissionService) identifiableService)
                    .findSubmissionsBySerialNumber(sn,
                        filter, pageable);

            } else {
                submissionsPage = ((DataFormSubmissionService) identifiableService)
                    .findAllByForm(List.of(filter), pageable);
            }

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
        data.put("sn", submission.getSerialNumber());
        data.put("deleted", submission.getDeleted());
        data.put("form", submission.getForm());
        data.put("version", submission.getVersion());
//        data.put("orgUnit", submission.getOrgUnit());
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
            formData = JsonFlattener.flatten(formData, submission.getId());
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

//    @GetMapping("/query")
//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
//    public ResponseEntity<PagedResponse<Map<String, Object>>> getSubmissions(
//        @ParameterObject Pageable pageable,
//        @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paged,
//        @RequestParam(name = "filter", required = false) String filter,
////        @RequestParam(name = "sn", required = false) Long sn,
//        @RequestParam(name = "flatten", required = false, defaultValue = "true") boolean flatten) throws IOException {
//
//        // Parse filters if present
//        Map<String, Object> filters = (filter != null) ? QueryRequest.parseFilter(filter) : null;
//
//
//        // Create a QueryRequest object
//        QueryRequest queryRequest = new QueryRequest();
//        queryRequest.setFilters(filters);
//        queryRequest.setPage(pageable.getPageNumber());
//        queryRequest.setSize(pageable.getPageSize());
//        queryRequest.setSort(pageable.getSort().toString());
//        queryRequest.setFlatten(flatten);
//
//        // Validate the query request
//        QueryRequestValidator.validate(queryRequest);
//
//        // Query the data
//        Page<DataFormSubmission> resultPage = queryService.query(queryRequest);
//
//        // Flatten the data if required
//        List<Map<String, Object>> content = resultPage.getContent().stream()
//            .map(submission -> {
//                Map<String, Object> formData = submission.getFormData();
//                if (flatten) {
//                    formData = JsonFlattener.flatten(formData);
//                }
//                return formData;
//            }).collect(Collectors.toList());
//
//        PagedResponse<Map<String, Object>> response = new PagedResponse<>();
//        response.setItems(content);
//        response.setPage(resultPage.getNumber());
//        response.setPageSize(resultPage.getSize());
//        response.setTotal(resultPage.getTotalElements());
//        response.setPageCount(resultPage.getTotalPages());
//
//        return ResponseEntity.ok(response);
//    }

    @Override
    protected String getName() {
        return "dataSubmissions";
    }
}
