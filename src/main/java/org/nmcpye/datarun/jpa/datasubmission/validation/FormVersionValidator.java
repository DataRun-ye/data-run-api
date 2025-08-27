package org.nmcpye.datarun.jpa.datasubmission.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * @author Hamza Assada
 * @since 15/08/2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormVersionValidator implements SubmissionValidator {

    private final DataTemplateVersionRepository versionRepo;
    private final DataTemplateInstanceService dataTemplateInstanceService;

    @Override
    @Transactional(readOnly = true)
    public DataSubmission validateAndEnrich(DataSubmission submission) {
        String form = submission.getForm();
        String formVersionUid = submission.getFormVersion();
        Integer versionNum = submission.getVersion();

        if (Objects.isNull(submission.getForm()) || (Objects.isNull(submission.getFormVersion()) &&
            Objects.isNull(submission.getVersion()))) {
            log.error("Form or FormVersion of submission {} is not set in submission", submission.getUid());
            throw new DomainValidationException(ErrorCode.E4112, submission.getUid());
        }


        // try resolve by UID first, then by form & number (your existing helper)
        var resolved = dataTemplateInstanceService.findByTemplateAndVersionUid(form, formVersionUid)
            .orElseGet(() -> dataTemplateInstanceService.findByTemplateAndVersionNo(form, versionNum)
                .orElseThrow(() -> new DomainValidationException(
                    "Form/version not found: " + form + ":" + (formVersionUid != null ? formVersionUid : versionNum))));

        // optionally set canonical form template id if different
        submission.setForm(resolved.getUid());
        // ensure canonical formVersion is set to resolved UID (enrichment)
        submission.setFormVersion(resolved.getVersionUid());
        submission.setVersion(resolved.getVersionNumber());

        return submission;
    }
}
