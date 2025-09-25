package org.nmcpye.datarun.jpa.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.etl.model.NormalizedSubmission;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the ETL flow for a submission:
 * <ol>
 *   <li>Fetch DataSubmission from repository</li>
 *   <li>Normalize it using Normalizer (optionally generate missing repeat UIDs)</li>
 *   <li>Persist the resulting NormalizedSubmission using NormalizedSubmissionPersister</li>
 * </ol>
 *
 * <p>Design notes:
 * <ul>
 *   <li>Thin coordinator: delegates responsibilities to dedicated components (single responsibility).</li>
 *   <li>Does not itself manage transactions; persister handles transactional semantics.</li>
 * </ul>
 *
 * @author Hamza Assada
 * @since 13/08/2025
 */
@Service
@RequiredArgsConstructor
public class EtlCoordinatorService {
    private final DataSubmissionRepository submissionRepo;
    private final NormalizedSubmissionPersister persister;
    private final Normalizer normalizer;

    /**
     * Process a submission end-to-end.
     *
     * @param submissionId       id of the submission to process
     * @param generateMissingIds whether the Normalizer should synthesize missing repeat-instance UIDs
     */
    public void processSubmission(String submissionId, boolean generateMissingIds) {
        DataSubmission submission = submissionRepo.findById(submissionId)
            .orElseThrow(() -> new IllegalStateException("Submission not found: " + submissionId));
        NormalizedSubmission ns = normalizer.normalize(submission, generateMissingIds);
        persister.persist(ns);
    }
}
