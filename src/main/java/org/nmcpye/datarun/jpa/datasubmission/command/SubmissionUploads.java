package org.nmcpye.datarun.jpa.datasubmission.command;

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
import org.nmcpye.datarun.jpa.datasubmission.validation.StoredSubmissionGuard;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.intake.IntakeDecision;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Saves uploaded submissions for {@link SubmissionCommandService}, one at a time, each in its own transaction, and
 * reports failures per submission: one rejected row does not fail the rest of the request (DataRun-ye/data-run-api#37).
 * A submission, the Reference entries it defines and its outbox row commit or roll back together. Phones apply the
 * per-row created/updated/failed result (mobile 5.3.0 and 6.0.x).
 *
 * <p>Only the command service calls this class; it is a separate component so that a held release can replay an
 * upload through it.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmissionUploads {

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
    private final StoredSubmissionGuard storedSubmissionGuard;

    /**
     * The versioned upload route. Every submission is checked and saved as
     * usual; when {@link SubmissionIntake} holds it (a practice upload, or one
     * started before its campaign's go-live, #42) the save is rolled back and
     * the upload kept aside instead. Either way the phone gets the result of
     * the checked save, so held submissions show as sent. A released upload
     * ({@link IntakeMode#RELEASE}) is always saved.
     */
    public EntitySaveSummaryVM save(
        List<DataSubmissionUploadV1Dto> requests,
        SubmissionActor actor,
        IntakeMode mode) {
        if (requests == null || requests.isEmpty()) {
            return new EntitySaveSummaryVM();
        }

        Instant receivedAt = Instant.now();
        if (mode == IntakeMode.PRACTICE) {
            log.info("Practice upload by user {}: {} submissions checked, kept apart",
                actor.userId(), requests.size());
        }
        return saveEach(
            requests,
            actor,
            DataSubmissionUploadV1Dto::getUid,
            DataSubmissionUploadV1Dto::getForm,
            (request, summary) -> upsertOne(request, actor, summary, mode, receivedAt),
            objectMapper::valueToTree);
    }

    /**
     * The legacy upload route ({@code POST /bulk} without {@code referenceVersion=1}), used by apps before 6.0.2 and
     * by API callers: the same per-submission transactions, with the route's own order of checks.
     * <ol>
     *   <li>The form version, by uid only, before anything else: a missing or unknown version is refused even for a
     *       user who may not submit there.</li>
     *   <li>Missing repeat-row ids.</li>
     *   <li>For a non-admin: the assignment (a missing and an unknown one are both "Assignment is required"), the team
     *       scope (E4114 names the team the upload claims, which phones never send), the form grant (E4122) and the
     *       stored record ({@link StoredSubmissionGuard}).</li>
     *   <li>For everyone: the assignment ("Assignment is required", "Assignment not found"), then the context from the
     *       assignment and the version.</li>
     * </ol>
     * Reference values are not resolved and nothing is held (#42): these apps cannot open Reference forms. The A2
     * goldens pin each difference from the versioned route (W3-*); they end when the route retires (B2).
     */
    public EntitySaveSummaryVM saveLegacy(
        List<DataSubmission> submissions,
        SubmissionActor actor) {
        if (submissions == null || submissions.isEmpty()) {
            return new EntitySaveSummaryVM();
        }

        return saveEach(
            submissions,
            actor,
            DataSubmission::getUid,
            DataSubmission::getForm,
            (submission, summary) -> {
                checkLegacy(submission, actor);
                submissionService.upsertAll(List.of(submission), summary);
                return IntakeDecision.save();
            },
            submission -> null);
    }

    private IntakeDecision upsertOne(
        DataSubmissionUploadV1Dto request,
        SubmissionActor actor,
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
        authorize(submission, assignment, actor.access(), assignment.getTeam().getUid());
        IntakeDecision decision = intake.decide(
            mode,
            submission,
            SubmissionIntake.areaChainOf(assignment.getOrgUnit()),
            actor.login(),
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

    /** The legacy route's checks and context, in that route's order; see {@link #saveLegacy}. */
    private void checkLegacy(DataSubmission submission, SubmissionActor actor) {
        TemplateVersionContext template = templateVersionResolver.resolveByUid(
            submission.getForm(),
            submission.getFormVersion());
        generateMissingRepeatIds(submission, template);
        Assignment assignment = submission.getAssignment() == null
            ? null
            : assignmentRepository.findByUid(submission.getAssignment()).orElse(null);
        if (!actor.access().isSuper()) {
            if (assignment == null) {
                throw new DomainValidationException("Assignment is required");
            }
            authorize(submission, assignment, actor.access(), submission.getTeam());
        }
        if (submission.getAssignment() == null) {
            throw new DomainValidationException("Assignment is required");
        }
        if (assignment == null) {
            throw new DomainValidationException("Assignment not found: " + submission.getAssignment());
        }
        canonicalizeContext(submission, assignment, template.getTemplate());
    }

    /** Saves one item and says whether to keep it (see {@link SubmissionIntake}). */
    @FunctionalInterface
    private interface SaveOne<T> {
        IntakeDecision save(T item, EntitySaveSummaryVM summary);
    }

    private <T> EntitySaveSummaryVM saveEach(
        List<T> items,
        SubmissionActor actor,
        Function<T, String> uidOf,
        Function<T, String> formOf,
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
                // Identifiers and shapes only (SubmissionLog): never the login, a value or an exception's message.
                if (failure instanceof ErrorCodeException refusal) {
                    log.warn("Submission {} (form {}) from user {} refused: {}",
                        uid, formOf.apply(item), actor.userId(), SubmissionLog.refusal(refusal));
                } else {
                    log.error("Submission {} (form {}) from user {} failed unexpectedly: {}",
                        uid, formOf.apply(item), actor.userId(), SubmissionLog.failure(failure));
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
     * table names or submitted values. The log line in {@link #saveEach}
     * names the submission, its form and the user's id, and describes the
     * failure by its exception types and where it was thrown, never by its
     * message (see {@link SubmissionLog}). Production 6.4.1 hid such
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

    /**
     * The upload checks for a non-admin: the assignment's team is in the user's scope (E4114), the user may submit
     * the form there (E4122), and an existing record may be changed by them ({@link StoredSubmissionGuard}).
     *
     * @param teamInRefusal the team E4114 names: the assignment's on the versioned route, the claimed one on the
     *                      legacy route
     */
    private void authorize(
        DataSubmission submission,
        Assignment assignment,
        CurrentUserDetails user,
        String teamInRefusal) {
        if (user.isSuper()) {
            return;
        }

        if (!AssignmentFormAccessService.isInTeamScope(user, assignment)) {
            throw new IllegalQueryException(
                ErrorCode.E4114,
                teamInRefusal,
                submission.getUid());
        }
        if (!formAccessService.canSubmitData(
            user,
            assignment,
            submission.getForm())) {
            throw new IllegalQueryException(
                ErrorCode.E4122,
                submission.getForm(),
                assignment.getUid());
        }
        storedSubmissionGuard.check(submission, user);
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
