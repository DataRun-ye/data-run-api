package org.nmcpye.datarun.web.rest.v1.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.CompositeSubmissionValidator;
import org.nmcpye.datarun.jpa.datasubmission.validation.SubmissionAccessValidator;
import org.nmcpye.datarun.jpa.datasubmissionbatching.job.MigrationRepeatIdGenerator;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.jpa.etl.model.TemplateElementMap;
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
public class ReferenceSubmissionUploadService {

    private final DataSubmissionService submissionService;
    private final DataSubmissionUploadV1Mapper mapper;
    private final ObjectMapper objectMapper;
    private final CompositeSubmissionValidator compositeValidator;
    private final SubmissionAccessValidator submissionAccessValidator;
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
            TemplateElementMap template = templateFor(submission);
            generateMissingRepeatIds(submission, template);
            compositeValidator.validateAndEnrich(
                submissionAccessValidator.validateAccess(
                    submission,
                    currentUser));
            referenceResolver.resolve(
                submission,
                template.getTemplateInstanceDto(),
                request.getReferenceDefinitions());
            submissions.add(submission);
        }

        submissionService.upsertAll(submissions, currentUser, summary);
        return summary;
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
