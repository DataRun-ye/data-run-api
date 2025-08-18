package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateElementService;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.nmcpye.datarun.jpa.option.service.OptionService;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Service
@SuppressWarnings("unused")
public class EtlCoordinatorService {
    private final TemplateElementService elementService;
    private final DataSubmissionRepository submissionRepo;
    private final OptionService optionService;
    private final NormalizedSubmissionPersister persister;

    public EtlCoordinatorService(TemplateElementService elementService, DataSubmissionRepository submissionRepo,
                                 OptionService optionService,
                                 NormalizedSubmissionPersister persister) {
        this.elementService = elementService;
        this.submissionRepo = submissionRepo;
        this.optionService = optionService;
        this.persister = persister;
    }

    public void processSubmission(DataSubmission submission) {
        final var normalizer = new SubmissionNormalizer(optionService,
            elementService.getTemplateElementMap(submission.getForm(), submission.getFormVersion()),
            true);
        NormalizedSubmission ns = normalizer.normalize(submission);
        persister.persist(ns);
    }

    public void processSubmission(String submissionId) {
        DataSubmission submission = submissionRepo.findById(submissionId)
            .orElseThrow(() -> new IllegalStateException("Submission not found: " + submissionId));

        final var normalizer = new SubmissionNormalizer(optionService,
            elementService.getTemplateElementMap(submission.getForm(), submission.getFormVersion()),
            true);
        NormalizedSubmission ns = normalizer.normalize(submission);
        persister.persist(ns);
    }
}
