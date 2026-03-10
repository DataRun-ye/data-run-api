package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.acl.AclService;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.repository.DeleteAccessDeniedException;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.web.common.ApiVersion;
import org.nmcpye.datarun.web.common.PagedResponse;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.nmcpye.datarun.web.rest.util.HeaderUtil;
import org.nmcpye.datarun.web.rest.util.ResponseUtil;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionV1Mapper;
import org.nmcpye.datarun.web.rest.v1.datasubmission.service.DataSubmissionV1Service;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing {@link DataSubmission}.
 * Refactored to use DTOs for API Surface Stabilization (Strangler Fig Phase 0).
 */
@RestController
@RequestMapping(value = { DataSubmissionResource.CUSTOM, DataSubmissionResource.V1 })
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@RequiredArgsConstructor
@Slf4j
public class DataSubmissionResource {
    protected static final String NAME = "/dataSubmission";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;

    @Value("${jhipster.clientApp.name}")
    protected String applicationName;

    private final DataSubmissionService submissionService;
    private final DataSubmissionV1Service v1Service;
    private final DataSubmissionV1Mapper v1Mapper;
    private final ObjectMapper objectMapper;
    private final AclService aclService;

    @GetMapping("")
    public ResponseEntity<PagedResponse<DataSubmissionV1Dto>> getAll(QueryRequest queryRequest) {
        log.debug("REST request to get all DataSubmissions");
        Page<DataSubmission> page = submissionService.findAllByUser(queryRequest, null);

        // Apply flattening if requested
        Page<DataSubmission> processedPage = page.map(s -> postProcess(s, queryRequest));

        Page<DataSubmissionV1Dto> dtoPage = processedPage.map(v1Mapper::toDto);
        String next = PagingConfigurator.createNextPageLink(dtoPage);
        PagedResponse<DataSubmissionV1Dto> response = PagingConfigurator.initPageResponse(dtoPage, next,
                "dataSubmission");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataSubmissionV1Dto> getById(@PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUserDetails user) {
        log.debug("REST request to get DataSubmission : {}", id);
        Optional<DataSubmission> entity = submissionService.findByIdOrUid(id);
        return ResponseUtil.wrapOrNotFound(entity.map(v1Mapper::toDto));
    }

    @PostMapping("")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@RequestBody List<DataSubmissionV1Dto> dtoList) {
        log.debug("REST request to save {} DataSubmissions", dtoList.size());
        EntitySaveSummaryVM summary = v1Service.upsertAll(dtoList);
        return ResponseEntity.ok(summary);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUserDetails user) {
        log.debug("REST request to delete DataSubmission : {}", id);
        final var entity = submissionService.findByUid(id).orElseThrow();
        if (aclService.canDelete(entity, user)) {
            submissionService.delete(entity);
        } else {
            throw new DeleteAccessDeniedException("Access denied to delete submission: " + id);
        }

        return ResponseEntity
                .noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, "dataSubmission", id))
                .build();
    }

    private DataSubmission postProcess(DataSubmission submission, QueryRequest queryRequest) {
        if (queryRequest.isFlatten()) {
            Map<String, Object> formData = objectMapper.convertValue(submission.getFormData(), new TypeReference<>() {
            });
            formData = FormSubmissionDataUtil.flatten(formData, false, true);
            submission.setFormData(objectMapper.convertValue(formData, JsonNode.class));
        }
        return submission;
    }
}
