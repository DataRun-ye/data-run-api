package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

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
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
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

    private final DataSubmissionService submissionService;
    private final DataSubmissionUploadV1Mapper mapper;
    private final ObjectMapper objectMapper;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentFormAccessService formAccessService;
    private final TemplateVersionResolver templateVersionResolver;
    private final ReferenceSubmissionResolver referenceResolver;
    private final PlatformTransactionManager transactionManager;

    /** The versioned upload route (`referenceVersion=1`). */
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests) {
        return upsertAll(requests, false);
    }

    /**
     * The versioned upload route. In a practice session (app 6.1.0+) every
     * submission is checked and saved exactly as usual, then rolled back: the
     * phone gets the real result, and nothing is stored, including household
     * names the submission defines.
     */
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests,
        boolean practice) {
        if (requests == null || requests.isEmpty()) {
            return new EntitySaveSummaryVM();
        }

        var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (practice) {
            log.info("Practice upload by {}: {} submissions checked, none stored",
                currentUser.getUsername(), requests.size());
        }
        return saveEach(
            requests,
            DataSubmissionUploadV1Dto::getUid,
            (request, summary) -> upsertOne(request, currentUser, summary),
            practice);
    }

    /**
     * The legacy upload route used by apps before 6.0.2: the same isolation,
     * with the route's own validation applied to each submission.
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
            (submission, summary) -> submissionService.upsertAll(
                List.of(validate.apply(submission)),
                summary),
            false);
    }

    private void upsertOne(
        DataSubmissionUploadV1Dto request,
        CurrentUserDetails currentUser,
        EntitySaveSummaryVM summary) {
        DataSubmission submission = mapper.toEntity(request);
        Assignment assignment = assignmentFor(submission);
        TemplateVersionContext template = templateFor(submission);
        canonicalizeContext(
            submission,
            assignment,
            template.getTemplate());
        authorize(submission, assignment, currentUser);
        generateMissingRepeatIds(submission, template);
        referenceResolver.resolve(
            submission,
            assignment,
            template.getTemplate(),
            request.getReferenceDefinitions());
        submissionService.upsertAll(List.of(submission), summary);
    }

    private <T> EntitySaveSummaryVM saveEach(
        List<T> items,
        Function<T, String> uidOf,
        BiConsumer<T, EntitySaveSummaryVM> save,
        boolean practice) {
        TransactionTemplate perSubmission = new TransactionTemplate(transactionManager);
        perSubmission.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        for (int index = 0; index < items.size(); index++) {
            T item = items.get(index);
            EntitySaveSummaryVM itemSummary = new EntitySaveSummaryVM();
            try {
                perSubmission.executeWithoutResult(status -> {
                    save.accept(item, itemSummary);
                    if (practice) {
                        status.setRollbackOnly();
                    }
                });
                summary.getCreated().addAll(itemSummary.getCreated());
                summary.getUpdated().addAll(itemSummary.getUpdated());
                summary.getFailed().putAll(itemSummary.getFailed());
            } catch (RuntimeException failure) {
                String uid = Optional.ofNullable(uidOf.apply(item)).orElse("#" + index);
                summary.getFailed().put(uid, failureMessage(failure));
                if (failure instanceof ErrorCodeException) {
                    log.warn("Submission {} rejected: {}", uid, failure.getMessage());
                } else {
                    log.error("Submission {} failed unexpectedly", uid, failure);
                }
            }
        }
        return summary;
    }

    private static String failureMessage(RuntimeException failure) {
        String message = Optional.ofNullable(failure.getMessage())
            .filter(text -> !text.isBlank())
            .orElse(failure.getClass().getSimpleName());
        if (failure instanceof ErrorCodeException coded && coded.getErrorCode() != null) {
            return coded.getErrorCode().name() + ": " + message;
        }
        return message;
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
