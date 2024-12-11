package org.nmcpye.datarun.web.rest.mongo;

import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.drun.mongo.repository.MetadataSubmissionGranularRepository;
import org.nmcpye.datarun.drun.mongo.repository.MetadataSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.MetadataSubmissionService;
import org.nmcpye.datarun.drun.mongo.service.submissionmigration.JsonFlattener;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
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
 * REST controller for managing {@link DataFormSubmission}.
 */
@RestController
@RequestMapping("/api/custom/metadataSubmissions")
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class MetadataSubmissionResourceCustom
    extends AbstractMongoResource<MetadataSubmission> {

    private final MetadataSubmissionGranularRepository metadataSubmissionGranularRepository;

    public MetadataSubmissionResourceCustom(MetadataSubmissionService metadataSubmissionService,
                                            MetadataSubmissionRepositoryCustom metadataSubmissionRepositoryCustom, MetadataSubmissionGranularRepository metadataSubmissionGranularRepository) {
        super(metadataSubmissionService, metadataSubmissionRepositoryCustom);
        this.metadataSubmissionGranularRepository = metadataSubmissionGranularRepository;
    }

    @Override
    protected Page<MetadataSubmission> getList(Pageable pageable, boolean eagerload) {
        return metadataSubmissionGranularRepository.getReferencedMetadataSubmissions(pageable);
    }

    @GetMapping("/form")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getByForm(
        @ParameterObject Pageable pageable,
        @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
        @RequestParam(name = "filter", required = false) String filter,
        @RequestParam(name = "sn", required = false) Long sn,
        @RequestParam(name = "resourceType", required = false) String resourceType,
        @RequestParam(name = "flatten", required = false, defaultValue = "true") boolean flatten) {

        // Handle pageable unpaged condition
        if (!paging) {
            pageable = Pageable.unpaged();
        }

        Page<MetadataSubmission> submissionsPage;
        if (filter != null) {
            if (sn != null) {
                submissionsPage = ((MetadataSubmissionService) identifiableService)
                    .findSubmissionsBySerialNumber(sn,
                        filter, pageable);

            } else if (resourceType != null) {
                submissionsPage = ((MetadataSubmissionService) identifiableService)
                    .findAllByResourceType(resourceType, pageable);
            } else {
                submissionsPage = ((MetadataSubmissionService) identifiableService)
                    .findAllByForm(List.of(filter), pageable);
            }

        } else {
            submissionsPage = metadataSubmissionGranularRepository.getReferencedMetadataSubmissions(pageable);
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

    @GetMapping("/entity")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getByEntity(
        @ParameterObject Pageable pageable,
        @RequestParam(name = "paging", required = false, defaultValue = "true") boolean paging,
        @RequestParam(name = "filter", required = false) String filter,
        @RequestParam(name = "sn", required = false) Long sn,
        @RequestParam(name = "flatten", required = false, defaultValue = "true") boolean flatten) {

        // Handle pageable unpaged condition
        if (!paging) {
            pageable = Pageable.unpaged();
        }

        Page<MetadataSubmission> submissionsPage;
        if (filter != null) {
            if (sn != null) {
                submissionsPage = ((MetadataSubmissionService) identifiableService)
                    .findSubmissionsBySerialNumber(sn,
                        filter, pageable);

            } else {
                submissionsPage = ((MetadataSubmissionService) identifiableService)
                    .findAllByEntity(List.of(filter), pageable);
            }

        } else {
            submissionsPage = metadataSubmissionGranularRepository.getReferencedMetadataSubmissions(pageable);
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
    private Map<String, Object> processSubmission(MetadataSubmission submission, boolean flatten) {
        // Initialize data map with basic fields
        Map<String, Object> data = new HashMap<>();
        data.put("id", submission.getId());
        data.put("uid", submission.getUid());
        data.put("sn", submission.getSerialNumber());
        data.put("form", submission.getMetadataSchema());
        data.put("version", submission.getVersion());
        data.put("entityType", submission.getResourceType());
        data.put("entityUid", submission.getResourceId());
        data.put("createdBy", submission.getCreatedBy());
        data.put("createdDate", submission.getCreatedDate());
        data.put("lastModifiedBy", submission.getLastModifiedBy());
        data.put("lastModifiedDate", submission.getLastModifiedDate());

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
    private PagedResponse<Map<String, Object>> buildPagedResponse(Page<MetadataSubmission> submissionsPage,
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
        return "metadataSubmissions";
    }
}
