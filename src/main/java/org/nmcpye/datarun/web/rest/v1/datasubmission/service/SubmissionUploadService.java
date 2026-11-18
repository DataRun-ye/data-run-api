package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.ErrorCodeException;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.intake.IntakeDecision;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Saves uploaded submissions one at a time, each in its own transaction, and
 * reports failures per submission: one rejected row no longer fails the rest of
 * the request (DataRun-ye/data-run-api#37). A submission and the Reference
 * entries it defines still commit or roll back together. Phones already apply
 * the per-row created/updated/failed result (mobile 5.3.0 and 6.0.x).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionUploadService {

    /**
     * What the phone shows for a submission that failed for an unexpected
     * reason; see {@link #failureMessage}.
     */
    public static final String UNEXPECTED_FAILURE =
        "Server error: this submission was not saved. Send it again later; if it keeps failing, tell your supervisor.";

    private final DataSubmissionService submissionService;
    private final DataSubmissionUploadV1Mapper mapper;
    private final ObjectMapper objectMapper;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentFormAccessService formAccessService;
    private final TemplateVersionResolver templateVersionResolver;
    private final ReferenceSubmissionResolver referenceResolver;
    private final PlatformTransactionManager transactionManager;
    private final SubmissionIntake intake;

    /** The versioned upload route (`referenceVersion=1`). */
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests) {
        return upsertAll(requests, false);
    }

    /**
     * The versioned upload route. Every submission is checked and saved as
     * usual; when {@link SubmissionIntake} holds it (a practice upload, or one
     * started before its campaign's go-live, #42) the save is rolled back and
     * the upload kept aside instead. Either way the phone gets the result of
     * the checked save, so held submissions show as sent.
     */
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests,
        boolean practice) {
        return upsertVersioned(requests, practice ? IntakeMode.PRACTICE : IntakeMode.LIVE);
    }

    /**
     * Saves a held upload an admin released, through the same path as a live
     * upload. The caller runs it as the upload's original submitter.
     */
    public EntitySaveSummaryVM release(DataSubmissionUploadV1Dto request) {
        return upsertVersioned(List.of(request), IntakeMode.RELEASE);
    }

    private EntitySaveSummaryVM upsertVersioned(
        List<DataSubmissionUploadV1Dto> requests,
        IntakeMode mode) {
        if (requests == null || requests.isEmpty()) {
            return new EntitySaveSummaryVM();
        }

        var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        Instant receivedAt = Instant.now();
        if (mode == IntakeMode.PRACTICE) {
            log.info("Practice upload by {}: {} submissions checked, kept apart",
                currentUser.getUsername(), requests.size());
        }
        return saveEach(
            requests,
            DataSubmissionUploadV1Dto::getUid,
            (request, summary) -> upsertOne(request, currentUser, summary, mode, receivedAt),
            objectMapper::valueToTree);
    }

    /**
     * The legacy upload route used by apps before 6.0.2: the same isolation,
     * with the route's own validation applied to each submission. These apps
     * cannot open Reference forms, and their uploads are never held (#42).
     */
    public EntitySaveSummaryVM upsertAllLegacy(
        List<DataSubmission> submissions,
        UnaryOperator<DataSubmission> validate) {
        if (submissions == null || submissions.isEmpty()) {
            return new EntitySaveSummaryVM();
        }

        return saveEach(
            submissions,
            DataSubmission::getUid,
            (submission, summary) -> {
                submissionService.upsertAll(List.of(validate.apply(submission)), summary);
                return IntakeDecision.save();
            },
            submission -> null);
    }

    private IntakeDecision upsertOne(
        DataSubmissionUploadV1Dto request,
        CurrentUserDetails currentUser,
        EntitySaveSummaryVM summary,
        IntakeMode mode,
        Instant receivedAt) {
        DataSubmission submission = mapper.toEntity(request);
        Assignment assignment = assignmentFor(submission);
        TemplateVersionContext template = templateFor(submission);
        canonicalizeContext(
            submission,
            assignment,
            template.getTemplate());
        authorize(submission, assignment, currentUser);
        IntakeDecision decision = intake.decide(
            mode,
            submission,
            SubmissionIntake.areaChainOf(assignment.getOrgUnit()),
            currentUser.getUsername(),
            receivedAt);
        generateMissingRepeatIds(submission, template);
        referenceResolver.resolve(
            submission,
            assignment,
            template.getTemplate(),
            request.getReferenceDefinitions());
        submissionService.upsertAll(List.of(submission), summary);
        return decision;
    }

    /** Saves one item and says whether to keep it (see {@link SubmissionIntake}). */
    @FunctionalInterface
    private interface SaveOne<T> {
        IntakeDecision save(T item, EntitySaveSummaryVM summary);
    }

    private <T> EntitySaveSummaryVM saveEach(
        List<T> items,
        Function<T, String> uidOf,
        SaveOne<T> save,
        Function<T, JsonNode> payloadOf) {
        TransactionTemplate perSubmission = new TransactionTemplate(transactionManager);
        perSubmission.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (int index = 0; index < items.size(); index++) {
            T item = items.get(index);
            EntitySaveSummaryVM itemSummary = new EntitySaveSummaryVM();
            try {
                IntakeDecision decision = perSubmission.execute(status -> {
                    IntakeDecision outcome = save.save(item, itemSummary);
                    if (outcome.held()) {
                        status.setRollbackOnly();
                    }
                    return outcome;
                });
                if (decision != null && decision.held()) {
                    intake.hold(uidOf.apply(item), payloadOf.apply(item), decision);
                }
                summary.getCreated().addAll(itemSummary.getCreated());
                summary.getUpdated().addAll(itemSummary.getUpdated());
                summary.getFailed().putAll(itemSummary.getFailed());
            } catch (RuntimeException failure) {
                String uid = Optional.ofNullable(uidOf.apply(item)).orElse("#" + index);
                summary.getFailed().put(uid, failureMessage(failure));
                String user = SecurityUtils.getCurrentUserLogin().orElse("unknown user");
                if (failure instanceof ErrorCodeException) {
                    log.warn("Submission {} from {} rejected: {}", uid, user, failure.getMessage());
                } else {
                    log.error("Submission {} from {} failed unexpectedly", uid, user, failure);
                }
            }
        }
        return summary;
    }

    /**
     * The text the phone shows for a submission that was not saved. A refusal
     * the server means (an {@link ErrorCodeException}: no access, an invalid
     * household, a missing assignment) keeps its message. Anything else gets
     * {@link #UNEXPECTED_FAILURE}, because its own message can carry SQL,
     * table names or submitted values; the log line in {@link #saveEach}
     * keeps the details under the submission uid and the user's login,
     * which is what a supervisor will report. Production 6.4.1 hid such
     * details in the same way when a whole upload failed.
     */
    private static String failureMessage(RuntimeException failure) {
        if (!(failure instanceof ErrorCodeException coded)) {
            return UNEXPECTED_FAILURE;
        }
        String message = Optional.ofNullable(coded.getMessage())
            .filter(text -> !text.isBlank())
            .orElse(coded.getClass().getSimpleName());
        return coded.getErrorCode() != null ? coded.getErrorCode().name() + ": " + message : message;
    }

    private Assignment assignmentFor(DataSubmission submission) {
        if (submission.getAssignment() == null) {
            throw new DomainValidationException("Assignment is required");
        }
        return assignmentRepository.findByUid(submission.getAssignment())
            .orElseThrow(() -> new DomainValidationException(
                "Assignment not found: " + submission.getAssignment()));
    }

    private TemplateVersionContext templateFor(DataSubmission submission) {
        if (submission.getFormVersion() != null) {
            return templateVersionResolver.resolveByUid(
                submission.getForm(),
                submission.getFormVersion());
        }
        if (submission.getVersion() != null) {
            return templateVersionResolver.resolveByNumber(
                submission.getForm(),
                submission.getVersion());
        }
        throw new IllegalQueryException(
            "Submission form version is required");
    }

    private void canonicalizeContext(
        DataSubmission submission,
        Assignment assignment,
        DataTemplateInstanceDto template) {
        submission.setForm(template.getUid());
        submission.setFormVersion(template.getVersionUid());
        submission.setVersion(template.getVersionNumber());
        submission.setAssignment(assignment.getUid());
        submission.setTeam(assignment.getTeam().getUid());
        submission.setTeamCode(assignment.getTeam().getCode());
        submission.setOrgUnit(assignment.getOrgUnit().getUid());
        submission.setOrgUnitCode(assignment.getOrgUnit().getCode());
        submission.setOrgUnitName(assignment.getOrgUnit().getName());
        submission.setActivity(assignment.getActivity().getUid());
    }

    private void authorize(
        DataSubmission submission,
        Assignment assignment,
        CurrentUserDetails user) {
        if (user.isSuper()) {
            return;
        }

        String teamUid = assignment.getTeam().getUid();
        if (!user.getUserTeamsUIDs().contains(teamUid)) {
            throw new IllegalQueryException(
                ErrorCode.E4114,
                teamUid,
                submission.getUid());
        }
        if (!formAccessService.canSubmitData(
            user,
            assignment,
            submission.getForm())) {
            throw new IllegalQueryException(ErrorCode.E1112, teamUid);
        }
    }

    private void generateMissingRepeatIds(
        DataSubmission submission,
        TemplateVersionContext template) {
        ObjectNode root = (ObjectNode) (submission.getFormData() == null
            ? objectMapper.createObjectNode()
            : submission.getFormData().deepCopy());
        int generated = new MigrationRepeatIdGenerator(template)
            .generateMissingIdsForMigration(root, submission.getUid());
        if (generated > 0) {
            submission.setFormData(root);
        }
    }
}
