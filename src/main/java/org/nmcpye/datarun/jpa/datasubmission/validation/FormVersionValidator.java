package org.nmcpye.datarun.jpa.datasubmission.validation;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.mongo.datatemplateversion.DataTemplateVersion;
import org.nmcpye.datarun.mongo.datatemplateversion.repository.DataTemplateVersionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada 15/08/2025 (7amza.it@gmail.com)
 */
@Component
public class FormVersionValidator implements SubmissionValidator {

    private final DataTemplateVersionRepository versionRepo;

    public FormVersionValidator(DataTemplateVersionRepository versionRepo) {
        this.versionRepo = versionRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public DataSubmission validateAndEnrich(DataSubmission submission) {
        String form = submission.getForm();
        String formVersionUid = submission.getFormVersion();
        Integer versionNum = submission.getVersion();

        // try resolve by UID first, then by form & number (your existing helper)
        DataTemplateVersion resolved = versionRepo.findByUid(formVersionUid)
            .orElseGet(() -> versionRepo.findByTemplateUidAndVersionNumber(form, versionNum)
                .orElseThrow(() -> new DomainValidationException(
                    "Form/version not found: " + form + ":" + (formVersionUid != null ? formVersionUid : versionNum))));

        // ensure canonical formVersion is set to resolved UID (enrichment)
        submission.setFormVersion(resolved.getUid());
        // optionally set canonical form template uid if different
        submission.setForm(resolved.getTemplateUid());
        submission.setVersion(resolved.getVersionNumber());

        return submission;
    }
}
