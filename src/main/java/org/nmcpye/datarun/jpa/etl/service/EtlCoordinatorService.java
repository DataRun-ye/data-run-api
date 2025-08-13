package org.nmcpye.datarun.jpa.etl.service;

import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
@Service
@SuppressWarnings("unused")
public class EtlCoordinatorService {
    private final TemplateElementService elementService;
    private final NormalizedSubmissionPersister persister;

    public EtlCoordinatorService(TemplateElementService elementService,
                                 NormalizedSubmissionPersister persister) {
        this.elementService = elementService;
        this.persister = persister;
    }

    public void processSubmission(DataFormSubmission submission) {
        final var normalizer = new SubmissionNormalizer(
            elementService.getTemplateElementMap(submission.getForm(), submission.getFormVersion()),
            true);
        NormalizedSubmission ns = normalizer.normalize(submission);
        persister.persist(ns);
    }
}
