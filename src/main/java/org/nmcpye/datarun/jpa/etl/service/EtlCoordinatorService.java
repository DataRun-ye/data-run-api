package org.nmcpye.datarun.jpa.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.analytics.ElementMetadataService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada
 * @since 13/08/2025
 */
@Service
@RequiredArgsConstructor
public class EtlCoordinatorService {
    private final DataSubmissionRepository submissionRepo;
    private final NormalizedSubmissionPersister persister;
    private final Normalizer normalizer;
    private final ElementMetadataService elementMetadataService;

    public void processSubmission(String submissionId, boolean generateMissingIds) {
        DataSubmission submission = submissionRepo.findById(submissionId)
            .orElseThrow(() -> new IllegalStateException("Submission not found: " + submissionId));
        NormalizedSubmission ns = normalizer.normalize(submission, generateMissingIds);
        persister.persist(ns);
    }
}
