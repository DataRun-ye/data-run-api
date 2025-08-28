package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.SubmissionDataProcessor;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequestValidator;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing {@link DataSubmission}.
 */
@RestController
@RequestMapping(value = {DataSubmissionResource.CUSTOM, DataSubmissionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class DataSubmissionResource extends JpaBaseResource<DataSubmission> {
    protected static final String NAME = "/dataSubmission";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;
    private final ObjectMapper objectMapper;
    final private DataSubmissionService submissionService;
    private final SubmissionDataProcessor submissionDataProcessor;

    public DataSubmissionResource(DataSubmissionService submissionService,
                                  DataSubmissionRepository submissionRepository, ObjectMapper objectMapper,
                                  SubmissionDataProcessor submissionDataProcessor) {
        super(submissionService, submissionRepository);
        this.submissionService = submissionService;
        this.objectMapper = objectMapper;
        this.submissionDataProcessor = submissionDataProcessor;
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

        String next = createNextPageLink(resultPage);

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
        var processedEntity = preProcess(List.of(payLoadEntity))
            .stream().findFirst().get();
        submissionService.upsert(processedEntity,
            SecurityUtils.getCurrentUserDetailsOrThrow(), summary);

    }

    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataSubmission> entities) {
        EntitySaveSummaryVM summaryVM = new EntitySaveSummaryVM();
        submissionService.upsertAll(entities, SecurityUtils.getCurrentUserDetailsOrThrow(), summaryVM);
        return ResponseEntity.ok(summaryVM);
    }

    @Override
    protected List<DataSubmission> preProcess(List<DataSubmission> payLoadEntities) {
        for (DataSubmission payLoadEntity : payLoadEntities) {
            submissionDataProcessor.processIncomingSubmission(payLoadEntity, SecurityUtils.getCurrentUserDetailsOrThrow());
        }

        return payLoadEntities;
    }

    @Override
    protected String getName() {
        return "dataSubmission";
    }
}
