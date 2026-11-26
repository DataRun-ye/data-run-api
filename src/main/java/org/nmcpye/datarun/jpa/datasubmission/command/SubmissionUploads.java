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
import org.nmcpye.datarun.jpa.datasubmission.conflict.AccessRefusal;
import org.nmcpye.datarun.jpa.datasubmission.conflict.ComparedState;
import org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictCheck;
import org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictDetection;
import org.nmcpye.datarun.jpa.datasubmission.conflict.PhoneCopy;
import org.nmcpye.datarun.jpa.datasubmission.conflict.StoredState;
import org.nmcpye.datarun.jpa.datasubmission.conflict.UploadRoute;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datasubmission.validation.StoredSubmissionGuard;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.TemplateVersionContext;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.intake.HoldReason;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Saves uploaded submissions for {@link SubmissionCommandService}, one at a time, each in its own transaction, and
 * reports failures per submission: one rejected row does not fail the rest of the request (DataRun-ye/data-run-api#37).
 * A submission, the Reference entries it defines and its outbox row commit or roll back together. Phones apply the
 * per-row created/updated/failed result (mobile 5.3.0 and 6.0.x).
 *
 * <p>Each transaction takes the submission lock first ({@link SubmissionLock}): the stored-record guard and the save
 * use the submission it locked, and another change of the submission waits until the transaction ends instead of
 * committing between its reads. Waits are bounded: an item whose submission another change holds past the bound fails
 * as any unexpected failure does. A new submission has nothing to lock; a second save of its uid fails on the uid's
 * unique constraint.</p>
 *
 * <p>Conflict detection ({@link ConflictDetection}, behind its switch) reads each live upload item of a stored
 * submission right after the lock, before the upload checks and before any row id is minted: a copy that changes
 * nothing, or that a server-side change keeps aside, is answered {@code updated} and nothing is written. After a
 * server-side move, so is a changed copy the checks refuse for access. While the uploader has an open conflict, their
 * copy counts there or replaces its copy, and never writes the submission. Practice uploads and held releases are not
 * detected.</p>
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
    private final SubmissionLock submissionLock;
    private final ConflictDetection conflictDetection;

    /**
     * The versioned upload route. Every submission is checked and saved as
     * usual; when {@link SubmissionIntake} holds it (a practice upload, or one
     * started before its campaign's go-live, #42) the save is rolled back and
     * the upload kept aside instead. Either way the phone gets the result of
     * the checked save, so held submissions show as sent. A released upload
     * goes through {@link #saveReleased} instead.
     */
    public EntitySaveSummaryVM save(
        List<DataSubmissionUploadV1Dto> requests,
        SubmissionActor actor,
        IntakeMode mode) {
        if (mode == IntakeMode.RELEASE) {
            throw new IllegalArgumentException("A released upload is saved with its decision: use saveReleased");
        }
        if (requests == null || requests.isEmpty()) {
            return new EntitySaveSummaryVM();
        }

        Instant receivedAt = Instant.now();
        if (mode == IntakeMode.PRACTICE) {
            log.info("Practice upload by user {}: {} submissions checked, kept apart",
                actor.userId(), requests.size());
        }
        RevisionContext revision = RevisionContext.upload(actor.userId());
        return saveEach(
            requests,
            actor,
            DataSubmissionUploadV1Dto::getUid,
            DataSubmissionUploadV1Dto::getForm,
            (request, summary, transaction) -> upsertOne(request, actor, summary, mode, receivedAt, revision),
            objectMapper::valueToTree,
            failure -> { });
    }

    /** What became of a released upload; see {@link #saveReleased}. */
    public enum ReleaseOutcome {
        /** Saved, together with the decision that released it. */
        RELEASED,
        /** The held upload changed after the release read it; nothing was saved. */
        CHANGED,
        /** Its uid was already real data when the replay saved it; nothing was saved. */
        SUPERSEDED,
        /** The checks refused it, or the save failed; see the failure. */
        FAILED
    }

    /**
     * A released upload's outcome.
     *
     * @param failure when the outcome is {@link ReleaseOutcome#FAILED}, the refusal's error code or the failure's
     *                exception type (never its message, which can quote submitted values); else null
     */
    public record Released(ReleaseOutcome outcome, String failure) {
    }

    /**
     * Saves a held upload an admin released, through the same checks and save as a live upload (never held), and
     * records the release in the same transaction: {@code decide} runs after the save and returns false when the
     * held upload changed since the release read it (a re-send refreshed it, or another decision came first). Then
     * the save is rolled back, so the data a release saves is always the copy its decision released.
     *
     * <p>A released copy only ever creates its submission. When the save finds the uid already in the real data (a
     * live upload landed after the release last looked), it is rolled back and the outcome is
     * {@link ReleaseOutcome#SUPERSEDED}: the older held copy never overwrites real data.</p>
     *
     * @param submitter the uploader, whose access the checks read and whom the records name
     * @param release   the release's revision: the admin acting for the uploader, with the admin's note
     */
    public Released saveReleased(
        DataSubmissionUploadV1Dto upload,
        SubmissionActor submitter,
        RevisionContext release,
        BooleanSupplier decide) {
        AtomicReference<ReleaseOutcome> outcome = new AtomicReference<>(ReleaseOutcome.FAILED);
        AtomicReference<RuntimeException> failed = new AtomicReference<>();
        Instant receivedAt = Instant.now();
        saveEach(
            List.of(upload),
            submitter,
            DataSubmissionUploadV1Dto::getUid,
            DataSubmissionUploadV1Dto::getForm,
            (request, itemSummary, transaction) -> {
                IntakeDecision decision = upsertOne(
                    request, submitter, itemSummary, IntakeMode.RELEASE, receivedAt, release);
                if (!itemSummary.getCreated().contains(request.getUid())) {
                    transaction.setRollbackOnly();
                    outcome.set(ReleaseOutcome.SUPERSEDED);
                } else if (decide.getAsBoolean()) {
                    outcome.set(ReleaseOutcome.RELEASED);
                } else {
                    transaction.setRollbackOnly();
                    outcome.set(ReleaseOutcome.CHANGED);
                }
                return decision;
            },
            objectMapper::valueToTree,
            failed::set);
        return failed.get() != null
            ? new Released(ReleaseOutcome.FAILED, SubmissionLog.code(failed.get()))
            : new Released(outcome.get(), null);
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

        RevisionContext revision = RevisionContext.legacyUpload(actor.userId());
        return saveEach(
            submissions,
            actor,
            DataSubmission::getUid,
            DataSubmission::getForm,
            (submission, summary, transaction) -> {
                DataSubmission stored = submissionLock.take(submission.getUid());
                TemplateVersionContext template = templateVersionResolver.resolveByUid(
                    submission.getForm(),
                    submission.getFormVersion());
                ConflictCheck conflict = detectConflict(UploadRoute.LEGACY, () -> objectMapper.valueToTree(submission),
                    submission, stored, template, actor);
                if (conflict.answeredBeforeChecks()) {
                    return answeredUpdated(submission, summary);
                }
                try {
                    checkLegacy(submission, stored, template, actor);
                } catch (ErrorCodeException refusal) {
                    if (!conflict.keepsAsideOnRefusal(accessRefusal(refusal))) {
                        throw refusal;
                    }
                    conflict.keepAside();
                    return answeredUpdated(submission, summary);
                }
                ConflictCheck.Outcome outcome = conflict.afterChecks();
                if (outcome != ConflictCheck.Outcome.APPLY) {
                    // The legacy route checks no Reference value: nothing else to refuse.
                    if (outcome == ConflictCheck.Outcome.KEEP_ASIDE) {
                        conflict.keepAside();
                    }
                    return answeredUpdated(submission, summary);
                }
                submissionService.upsert(submission, stored, summary, revision);
                return IntakeDecision.save();
            },
            submission -> null,
            failure -> { });
    }

    private IntakeDecision upsertOne(
        DataSubmissionUploadV1Dto request,
        SubmissionActor actor,
        EntitySaveSummaryVM summary,
        IntakeMode mode,
        Instant receivedAt,
        RevisionContext revision) {
        DataSubmission submission = mapper.toEntity(request);
        DataSubmission stored = submissionLock.take(submission.getUid());
        Assignment assignment = assignmentFor(submission);
        TemplateVersionContext template = templateFor(submission);
        canonicalizeContext(
            submission,
            assignment,
            template.getTemplate());
        ConflictCheck conflict = mode != IntakeMode.LIVE ? ConflictCheck.OFF
            : detectConflict(UploadRoute.VERSIONED, () -> objectMapper.valueToTree(request), submission, stored,
            template, actor);
        if (conflict.answeredBeforeChecks()) {
            return answeredUpdated(submission, summary);
        }
        try {
            authorize(submission, stored, assignment, actor.access(), assignment.getTeam().getUid());
        } catch (ErrorCodeException refusal) {
            if (!conflict.keepsAsideOnRefusal(accessRefusal(refusal))) {
                throw refusal;
            }
            try {
                checkReferences(submission, assignment, template, request);
            } catch (ErrorCodeException referenceRefusal) {
                // Refused for access today, before its Reference values were read: it stays refused so.
                throw refusal;
            }
            conflict.keepAside();
            return answeredUpdated(submission, summary);
        }
        ConflictCheck.Outcome outcome = conflict.afterChecks();
        if (outcome != ConflictCheck.Outcome.APPLY) {
            checkReferences(submission, assignment, template, request);
            if (outcome == ConflictCheck.Outcome.KEEP_ASIDE) {
                conflict.keepAside();
            }
            return answeredUpdated(submission, summary);
        }
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
        submissionService.upsert(submission, stored, summary, revision);
        return decision;
    }

    /**
     * Conflict detection's check of a live upload item, or {@link ConflictCheck#OFF} for a new submission or with
     * detection off. Detection reads only plain values, built here and only when it runs, from what the upload holds
     * under the submission lock: the copy as received, tagged before any row id is minted; the stored submission's
     * state, its document read exactly as the database prints it ({@link SubmissionLock#documentText}), with whether
     * the uploader made it; and the uploader's user id.
     */
    private ConflictCheck detectConflict(UploadRoute route, Supplier<JsonNode> uploadItem, DataSubmission submission,
                                         DataSubmission stored, TemplateVersionContext template,
                                         SubmissionActor actor) {
        if (stored == null || !conflictDetection.isOn()) {
            return ConflictCheck.OFF;
        }
        PhoneCopy copy = new PhoneCopy(route, uploadItem.get(), new ComparedState(
            submission.getFormData(),
            Boolean.TRUE.equals(submission.getDeleted()),
            submission.getAssignment(),
            submission.getStatus() == null ? null : submission.getStatus().name()),
            new MigrationRepeatIdGenerator(template).hasRowsWithoutIds(submission.getFormData()));
        StoredState state = new StoredState(
            stored.getId(),
            stored.getLockVersion() == null ? 0 : stored.getLockVersion(),
            submissionLock.documentText(stored.getId()),
            Boolean.TRUE.equals(stored.getDeleted()),
            stored.getAssignment(),
            stored.getStatus() == null ? null : stored.getStatus().name(),
            StoredSubmissionGuard.madeBy(stored, actor.access()));
        return conflictDetection.check(copy, state, actor.userId());
    }

    /**
     * The access refusals of the upload checks that a server-side move can cause, which the move rule keeps aside;
     * null for any other refusal, which stands.
     */
    private static AccessRefusal accessRefusal(ErrorCodeException refusal) {
        ErrorCode code = refusal.getErrorCode();
        if (code == null) {
            return null;
        }
        return switch (code) {
            case E4124 -> AccessRefusal.OUT_OF_SCOPE;
            case E4114 -> AccessRefusal.ASSIGNMENT_REFUSED;
            case E4122 -> AccessRefusal.FORM_NOT_PERMITTED;
            default -> null;
        };
    }

    /**
     * The Reference checks of a copy the upload will not save, against its own assignment, in the save's order (row ids
     * first): a refusal stays a refusal (E4117 to E4121), as today, and no entry is created, so a copy kept aside
     * creates nothing until it is accepted.
     */
    private void checkReferences(DataSubmission submission, Assignment assignment, TemplateVersionContext template,
                                 DataSubmissionUploadV1Dto request) {
        generateMissingRepeatIds(submission, template);
        referenceResolver.check(submission, assignment, template.getTemplate(), request.getReferenceDefinitions());
    }

    /** The upload item is answered {@code updated}, as an unchanged re-send always was, and nothing is written. */
    private static IntakeDecision answeredUpdated(DataSubmission submission, EntitySaveSummaryVM summary) {
        summary.getUpdated().add(submission.getUid());
        return IntakeDecision.save();
    }

    /**
     * The legacy route's checks and context, in that route's order, after its form version (see {@link #saveLegacy}).
     *
     * @param stored   the stored submission, as the submission lock returned it ({@link SubmissionLock#take})
     * @param template the form version the item names, resolved first
     */
    private void checkLegacy(DataSubmission submission, DataSubmission stored, TemplateVersionContext template,
                             SubmissionActor actor) {
        generateMissingRepeatIds(submission, template);
        Assignment assignment = submission.getAssignment() == null
            ? null
            : assignmentRepository.findByUid(submission.getAssignment()).orElse(null);
        if (!actor.access().isSuper()) {
            if (assignment == null) {
                throw new DomainValidationException("Assignment is required");
            }
            authorize(submission, stored, assignment, actor.access(), submission.getTeam());
        }
        if (submission.getAssignment() == null) {
            throw new DomainValidationException("Assignment is required");
        }
        if (assignment == null) {
            throw new DomainValidationException("Assignment not found: " + submission.getAssignment());
        }
        canonicalizeContext(submission, assignment, template.getTemplate());
    }

    /**
     * Saves one item inside its transaction and says whether to keep it (see {@link SubmissionIntake}); it may also
     * mark the transaction rollback-only.
     */
    @FunctionalInterface
    private interface SaveOne<T> {
        IntakeDecision save(T item, EntitySaveSummaryVM summary, TransactionStatus transaction);
    }

    private <T> EntitySaveSummaryVM saveEach(
        List<T> items,
        SubmissionActor actor,
        Function<T, String> uidOf,
        Function<T, String> formOf,
        SaveOne<T> save,
        Function<T, JsonNode> payloadOf,
        Consumer<RuntimeException> onFailure) {
        TransactionTemplate perSubmission = new TransactionTemplate(transactionManager);
        perSubmission.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (int index = 0; index < items.size(); index++) {
            T item = items.get(index);
            try {
                EntitySaveSummaryVM itemSummary = saveItem(perSubmission, item, actor, uidOf, formOf, save, payloadOf);
                summary.getCreated().addAll(itemSummary.getCreated());
                summary.getUpdated().addAll(itemSummary.getUpdated());
                summary.getFailed().putAll(itemSummary.getFailed());
            } catch (RuntimeException failure) {
                String uid = Optional.ofNullable(uidOf.apply(item)).orElse("#" + index);
                summary.getFailed().put(uid, failureMessage(failure));
                onFailure.accept(failure);
                // Identifiers and shapes only (SubmissionLog): never the login, a value or an exception's message.
                if (failure instanceof ErrorCodeException refusal) {
                    log.warn("Submission {} (form {}) from user {} refused: {}",
                        uid, formOf.apply(item), actor.userId(), SubmissionLog.refusal(refusal));
                } else if (SubmissionLog.isUidRace(failure)) {
                    log.warn("Submission {} (form {}) from user {} not saved: another save of the same uid came "
                        + "first: {}", uid, formOf.apply(item), actor.userId(), SubmissionLog.failure(failure));
                } else if (SubmissionLog.isLockTimeout(failure)) {
                    log.warn("Submission {} (form {}) from user {} not saved: another change held it past the "
                        + "submission lock's bound: {}", uid, formOf.apply(item), actor.userId(),
                        SubmissionLog.failure(failure));
                } else {
                    log.error("Submission {} (form {}) from user {} failed unexpectedly: {}",
                        uid, formOf.apply(item), actor.userId(), SubmissionLog.failure(failure));
                }
            }
        }
        return summary;
    }

    /**
     * Saves one item in a transaction of its own, or keeps it held. A hold applies to a held or discarded copy only:
     * when the held copy was released or superseded after this upload was checked, its uid is real data by now. A
     * live upload is then saved again, in a second transaction, as the update of real data it has become (never held);
     * the phone is told the outcome of that save. A practice upload is never saved: it keeps the outcome it was checked
     * with, and its copy is not kept.
     */
    private <T> EntitySaveSummaryVM saveItem(
        TransactionTemplate perSubmission,
        T item,
        SubmissionActor actor,
        Function<T, String> uidOf,
        Function<T, String> formOf,
        SaveOne<T> save,
        Function<T, JsonNode> payloadOf) {
        EntitySaveSummaryVM checked = new EntitySaveSummaryVM();
        IntakeDecision decision = saveInTransaction(perSubmission, item, checked, save);
        if (decision == null || !decision.held() || intake.hold(uidOf.apply(item), payloadOf.apply(item), decision)) {
            return checked;
        }
        if (decision.holdReason() == HoldReason.PRACTICE) {
            log.warn("Practice upload {} (form {}) from user {} not kept: its held copy was already decided",
                uidOf.apply(item), formOf.apply(item), actor.userId());
            return checked;
        }
        EntitySaveSummaryVM saved = new EntitySaveSummaryVM();
        IntakeDecision again = saveInTransaction(perSubmission, item, saved, save);
        if (again != null && again.held()) {
            throw new IllegalStateException("Held again after its held copy was decided");
        }
        return saved;
    }

    private static <T> IntakeDecision saveInTransaction(
        TransactionTemplate perSubmission, T item, EntitySaveSummaryVM itemSummary, SaveOne<T> save) {
        return perSubmission.execute(status -> {
            IntakeDecision outcome = save.save(item, itemSummary, status);
            if (outcome.held()) {
                status.setRollbackOnly();
            }
            return outcome;
        });
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
     * @param stored        the stored submission, as the submission lock returned it ({@link SubmissionLock#take})
     * @param teamInRefusal the team E4114 names: the assignment's on the versioned route, the claimed one on the
     *                      legacy route
     */
    private void authorize(
        DataSubmission submission,
        DataSubmission stored,
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
        storedSubmissionGuard.check(submission, stored, user);
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
