package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.repository.DeleteAccessDeniedException;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.SubmissionDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequestValidator;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link DataSubmission}.
 */
@RestController
@RequestMapping(value = {DataSubmissionResource.CUSTOM, DataSubmissionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class DataSubmissionResource extends JpaBaseResource<DataSubmission> {
    protected static final String NAME = "/dataSubmission";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;
    private final ObjectMapper objectMapper;
    final private DataSubmissionService submissionService;
    private final SubmissionDataProcessor submissionDataProcessor;
    private final CompositeSubmissionValidator compositeValidator;
    private final SubmissionAccessValidator submissionAccessValidator;
    private final TemplateElementService templateElementService;

    public DataSubmissionResource(DataSubmissionService submissionService,
                                  DataSubmissionRepository submissionRepository, ObjectMapper objectMapper,
                                  SubmissionDataProcessor submissionDataProcessor, CompositeSubmissionValidator compositeValidator, SubmissionAccessValidator submissionAccessValidator, TemplateElementService templateElementService) {
        super(submissionService, submissionRepository);
        this.submissionService = submissionService;
        this.objectMapper = objectMapper;
        this.submissionDataProcessor = submissionDataProcessor;
        this.compositeValidator = compositeValidator;
        this.submissionAccessValidator = submissionAccessValidator;
        this.templateElementService = templateElementService;
    }

    @Deprecated(since = "V7, main method do the same now")
    @RequestMapping(value = "objects", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<PagedResponse<?>> getObjectSubmissions(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
        QueryRequest queryRequest,
        @RequestBody(required = false) String jsonQuery) {
        QueryRequestValidator.validate(queryRequest);
        Instant effectiveSince = since != null ? since : Instant.EPOCH;

        Page<DataSubmission> resultPage = getList(queryRequest.setSince(effectiveSince), jsonQuery);

        String next = PagingConfigurator.createNextPageLink(resultPage);

        PagedResponse<DataSubmission> response = new
            PagedResponse<>(resultPage, getName(), next);
        return ResponseEntity.ok(response);
    }

    @Override
    protected DataSubmission postProcess(DataSubmission submission,
                                         QueryRequest queryRequest,
                                         String jsonQuery) {
        if (queryRequest.isFlatten()) {
            Map<String, Object> formData =
                objectMapper.convertValue(submission.getFormData(), new TypeReference<>() {
                });
            formData = FormSubmissionDataUtil.flatten(formData, false, true);
            submission
                .setFormData(objectMapper.convertValue(formData,
                    objectMapper.getTypeFactory().constructType(JsonNode.class)));
        }

        return submission;
    }

    @Override
    protected void saveEntity(DataSubmission payLoadEntity, EntitySaveSummaryVM summary) {
        hasMinimalRightsOrThrow(SecurityUtils.getCurrentUserDetailsOrThrow());
        var processedEntity = preProcess(List.of(payLoadEntity))
            .stream().findFirst().orElseThrow(() -> new IllegalQueryException("processing: " + payLoadEntity.getUid() + " swallowed submission"));
        submissionService.upsert(processedEntity,
            SecurityUtils.getCurrentUserDetailsOrThrow(), summary);

    }

    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataSubmission> entities) {
        hasMinimalRightsOrThrow(SecurityUtils.getCurrentUserDetailsOrThrow());
        EntitySaveSummaryVM summaryVM = new EntitySaveSummaryVM();
        submissionService.upsertAll(preProcess(entities), SecurityUtils.getCurrentUserDetailsOrThrow(), summaryVM);
        return ResponseEntity.ok(summaryVM);
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("id") String id,
                                              @AuthenticationPrincipal CurrentUserDetails user) {
        hasMinimalRightsOrThrow(user);
        log.debug("REST request to delete from {}: {}", getName(), id);
        final var entity = identifiableObjectService.findByUid(id).orElseThrow();
        if (aclService.canDelete(entity, user)) {
            identifiableObjectService.delete(entity);
        } else {
            throw new DeleteAccessDeniedException("");
        }

        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id)).build();
    }

    @Override
    protected List<DataSubmission> preProcess(List<DataSubmission> payLoadEntities) {
        return payLoadEntities.stream()
            .peek(payLoadEntity -> {
                ObjectNode root = (ObjectNode) (payLoadEntity.getFormData() == null ? objectMapper.createObjectNode() : payLoadEntity.getFormData().deepCopy());
                final var migrationRepeatIdGenerator = new MigrationRepeatIdGenerator(templateElementService.getTemplateElementMap(payLoadEntity.getForm(), payLoadEntity.getFormVersion()));
                int generated = migrationRepeatIdGenerator
                    .generateMissingIdsForMigration(root, payLoadEntity.getUid());
                if (generated > 0) {
                    payLoadEntity.setFormData(root);
                }

                compositeValidator.validateAndEnrich(submissionAccessValidator.validateAccess(payLoadEntity,
                    SecurityUtils.getCurrentUserDetailsOrThrow()));
            }).collect(Collectors.toList());
    }

    @Override
    protected String getName() {
        return "dataSubmission";
    }
}
