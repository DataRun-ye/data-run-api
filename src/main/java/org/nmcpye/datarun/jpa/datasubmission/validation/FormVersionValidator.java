package org.nmcpye.datarun.jpa.datasubmission.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datatemplate.dto.DataTemplateInstanceDto;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada
 * @since 15/08/2025
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FormVersionValidator implements SubmissionValidator {
    private final DataTemplateInstanceService dataTemplateInstanceService;

    @Override
    @Transactional(readOnly = true)
    public DataSubmission validateAndEnrich(DataSubmission submission) {
        // DataSubmissionResource.preProcess has already resolved this form and version uid through
        // TemplateVersionResolver, which refuses a missing or unknown one; this sets the version number,
        // which phones do not send.
        String form = submission.getForm();
        String formVersionUid = submission.getFormVersion();
        DataTemplateInstanceDto resolved = dataTemplateInstanceService
            .findByTemplateAndVersionUid(form, formVersionUid)
            .orElseThrow(() -> new DomainValidationException(
                "Form/version uid not found: " + form + ":" + formVersionUid));

        // optionally set canonical form template uid if different
        submission.setForm(resolved.getUid());
        // ensure canonical formVersion is set to resolved UID (enrichment)
        submission.setFormVersion(resolved.getVersionUid());
        submission.setVersion(resolved.getVersionNumber());

        return submission;
    }
}
