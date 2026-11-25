package org.nmcpye.datarun.web.rest.v1.datasubmission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
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
import org.nmcpye.datarun.web.rest.common.BaseReadResource;
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
 * REST controller for managing {@link DataSubmission}: the inherited reads, and the writes it declares itself, each
 * through {@link SubmissionCommandService}.
 * <p>
 * It does not inherit the generic writes of {@code BaseReadWriteResource}. Their {@code POST /}, {@code POST /return}
 * and {@code PUT /{uid}} could never save a submission (its id cannot be bound, so bean validation refused every
 * body with 400, A2 W4-01 to W5-02), and production's access logs of 2026-11-10 to 24 show no call to them. Since
 * 6.5.5 they are not routes here, so Spring answers 405 for every body and every user (B2, owner decision 4).
 */
@RestController
@RequestMapping(value = {DataSubmissionResource.CUSTOM, DataSubmissionResource.V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
@Slf4j
public class DataSubmissionResource extends BaseReadResource<DataSubmission, String> {
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

    /**
     * The generic list reads ({@code GET /}, {@code /byLastModified}, {@code POST /query}, {@code /objects}), bounded
     * for an admin before any query runs: a page holds at most {@value #MAX_PAGE_SIZE} submissions, starts within the
     * first {@value #MAX_OFFSET}, and is never unpaged (E2060, 400; {@code POST /query} answers its own empty 400
     * instead, as it does for any failure). Deep pages cost an OFFSET to the end plus a count, two whole-table scans;
     * the admin tool that read pages 61 to 66 of 1,000 no longer depends on them (2026-11-25). A non-admin's read is
     * unchanged: it finds nothing without an EDIT grant (the G0 pull, A2 R1).
     */
    @Override
    protected Page<DataSubmission> getList(QueryRequest queryRequest, String jsonQueryBody) {
        if (SecurityUtils.getCurrentUserDetailsOrThrow().isSuper()) {
            refuseDeepPaging(queryRequest);
        }
        return super.getList(queryRequest, jsonQueryBody);
    }

    static final int MAX_PAGE_SIZE = 1_000;
    static final int MAX_OFFSET = 10_000;

    private static void refuseDeepPaging(QueryRequest queryRequest) {
        if (!queryRequest.isPaged()
            || queryRequest.getSize() > MAX_PAGE_SIZE
            || (long) queryRequest.getPage() * queryRequest.getSize() > MAX_OFFSET) {
            throw new IllegalQueryException(ErrorCode.E2060);
        }
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
     * The legacy upload route ({@code POST /bulk} without {@code referenceVersion=1}), used by apps before 6.0.2. Its
     * body is bound as it was when the route was inherited: as entities, unvalidated.
     */
    @PostMapping("/bulk")
    public ResponseEntity<EntitySaveSummaryVM> saveAll(@RequestBody List<DataSubmission> entities) {
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
