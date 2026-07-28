package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowComparator;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowComparator.VersionedUploadComparison;
import org.nmcpye.datarun.assignmentshadow.BaselineAssignmentCaptureAdapter;
import org.nmcpye.datarun.assignmentshadow.BaselineAssignmentCaptureAdapter.VersionedUploadDecision;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
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
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionUploadService {

    private final DataSubmissionService submissionService;
    private final DataSubmissionUploadV1Mapper mapper;
    private final ObjectMapper objectMapper;
    private final AssignmentRepository assignmentRepository;
    private final TemplateVersionResolver templateVersionResolver;
    private final ReferenceSubmissionResolver referenceResolver;
    private final BaselineAssignmentCaptureAdapter baselineCapture;
    private final AssignmentCaptureShadowComparator captureShadow;

    @Transactional
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        if (requests == null || requests.isEmpty()) {
            return summary;
        }

        var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        List<DataSubmission> submissions = new ArrayList<>(requests.size());
        List<VersionedUploadComparison> comparisons =
            new ArrayList<>(requests.size());
        for (DataSubmissionUploadV1Dto request : requests) {
            DataSubmission submission = mapper.toEntity(request);
            Assignment assignment = assignmentFor(submission);
            TemplateVersionContext template = templateFor(submission);
            canonicalizeContext(
                submission,
                assignment,
                template.getTemplate());
            VersionedUploadDecision authorization =
                baselineCapture.decideVersionedUpload(
                    currentUser,
                    assignment,
                    submission.getForm()
                );
            VersionedUploadComparison comparison =
                new VersionedUploadComparison(
                    assignment,
                    submission.getForm(),
                    authorization.accepted()
                );
            if (!authorization.accepted()) {
                captureShadow.compareVersionedUploads(
                    currentUser,
                    List.of(comparison)
                );
            }
            enforceAuthorization(
                authorization,
                submission,
                assignment
            );
            generateMissingRepeatIds(
                submission,
                template
            );
            referenceResolver.resolve(
                submission,
                assignment,
                template.getTemplate(),
                request.getReferenceDefinitions());
            submissions.add(submission);
            comparisons.add(comparison);
        }

        captureShadow.compareVersionedUploads(currentUser, comparisons);
        submissionService.upsertAll(submissions, summary);
        return summary;
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

    private void enforceAuthorization(
        VersionedUploadDecision authorization,
        DataSubmission submission,
        Assignment assignment
    ) {
        String teamUid = assignment.getTeam().getUid();
        if (authorization == VersionedUploadDecision.NOT_DIRECT_TEAM) {
            throw new IllegalQueryException(
                ErrorCode.E4114,
                teamUid,
                submission.getUid());
        }
        if (authorization == VersionedUploadDecision.NO_CAPTURE_PERMISSION) {
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
