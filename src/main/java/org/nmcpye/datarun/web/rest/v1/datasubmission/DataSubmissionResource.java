package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommand;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.FormSubmissionDataUtil;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.apiquery.QueryRequestValidator;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.jhipster.web.util.HeaderUtil;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    private final SubmissionCommandService commands;

    public DataSubmissionResource(DataSubmissionService submissionService,
                                  DataSubmissionRepository submissionRepository,
                                  ObjectMapper objectMapper,
                                  SubmissionCommandService commands) {
        super(submissionService, submissionRepository);
        this.objectMapper = objectMapper;
        this.commands = commands;
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

    /**
     * The body of the generic {@code POST /} and {@code POST /return}. Unreachable today: bean validation refuses
     * every body first, because a submission's id cannot be bound (A2 W4-01, W4-02), and B2 refuses both routes as
     * methods. A body that ever got here would be saved as one legacy upload.
     */
    @Override
    protected void saveEntity(DataSubmission payLoadEntity, EntitySaveSummaryVM summary) {
        CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
        requireResourceApiAccess(user);
        EntitySaveSummaryVM saved = commands.uploadLegacy(
            new SubmissionCommand.LegacyUpload(SubmissionActor.of(user), List.of(payLoadEntity)));
        summary.getCreated().addAll(saved.getCreated());
        summary.getUpdated().addAll(saved.getUpdated());
        summary.getFailed().putAll(saved.getFailed());
    }

    @Override
    public ResponseEntity<EntitySaveSummaryVM> saveAll(List<DataSubmission> entities) {
        CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
        requireResourceApiAccess(user);
        return ResponseEntity.ok(commands.uploadLegacy(
            new SubmissionCommand.LegacyUpload(SubmissionActor.of(user), entities)));
    }

    @PostMapping(value = "/bulk", params = "referenceVersion=1")
    public ResponseEntity<EntitySaveSummaryVM> saveVersionedUpload(
        @RequestBody List<DataSubmissionUploadV1Dto> requests) {
        CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
        requireResourceApiAccess(user);
        return ResponseEntity.ok(commands.upload(
            new SubmissionCommand.Upload(SubmissionActor.of(user), requests)));
    }

    /**
     * Uploads from a practice session (app 6.1.0+): every submission is checked
     * and saved exactly as by {@link #saveVersionedUpload}, then rolled back, so
     * nothing is stored.
     * <p>
     * A route of its own rather than a flag on the ordinary route: a server
     * without practice support answers 404 here, where it would ignore a flag
     * and store the practice data.
     */
    @PostMapping(value = "/bulk/practice", params = "referenceVersion=1")
    public ResponseEntity<EntitySaveSummaryVM> savePracticeUpload(
        @RequestBody List<DataSubmissionUploadV1Dto> requests) {
        CurrentUserDetails user = SecurityUtils.getCurrentUserDetailsOrThrow();
        requireResourceApiAccess(user);
        return ResponseEntity.ok(commands.uploadPractice(
            new SubmissionCommand.PracticeUpload(SubmissionActor.of(user), requests)));
    }

    @Override
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteByIdUid(@PathVariable("id") String id,
                                              @AuthenticationPrincipal CurrentUserDetails user) {
        requireResourceApiAccess(user);
        log.debug("REST request to delete from {}: {}", getName(), id);
        if (commands.delete(new SubmissionCommand.Delete(SubmissionActor.of(user), id))
            == SubmissionCommandService.DeleteOutcome.NOT_FOUND) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND); // as ResponseUtil.wrapOrNotFound on the reads
        }

        return ResponseEntity
            .noContent()
            .headers(HeaderUtil
                .createEntityDeletionAlert(applicationName, true, getName(), id)).build();
    }

    @Override
    protected String getName() {
        return "dataSubmission";
    }
}
