package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.DomainValidationException;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.security.CurrentUserDetails;
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
    private final AssignmentFormAccessService formAccessService;
    private final TemplateElementService templateElementService;
    private final ReferenceSubmissionResolver referenceResolver;

    @Transactional
    public EntitySaveSummaryVM upsertAll(
        List<DataSubmissionUploadV1Dto> requests) {
        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        if (requests == null || requests.isEmpty()) {
            return summary;
        }

        var currentUser = SecurityUtils.getCurrentUserDetailsOrThrow();
        List<DataSubmission> submissions = new ArrayList<>(requests.size());
        for (DataSubmissionUploadV1Dto request : requests) {
            DataSubmission submission = mapper.toEntity(request);
            Assignment assignment = assignmentFor(submission);
            TemplateElementMap template = templateFor(submission);
            canonicalizeContext(
                submission,
                assignment,
                template.getTemplateInstanceDto());
            authorize(submission, assignment, currentUser);
            generateMissingRepeatIds(submission, template);
            referenceResolver.resolve(
                submission,
                assignment,
                template.getTemplateInstanceDto(),
                request.getReferenceDefinitions());
            submissions.add(submission);
        }

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

    private TemplateElementMap templateFor(DataSubmission submission) {
        if (submission.getFormVersion() != null) {
            return templateElementService.getTemplateElementMap(
                submission.getForm(),
                submission.getFormVersion());
        }
        if (submission.getVersion() != null) {
            return templateElementService.getTemplateElementMap(
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
        TemplateElementMap template) {
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
