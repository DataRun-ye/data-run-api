package org.nmcpye.datarun.jpa.datasubmission.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmissionHistory;
import org.nmcpye.datarun.jpa.datasubmission.events.EventChangeType;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionHistoryRepository;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada
 * @since 28/08/2025
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionSnapshotAndPrunerService {

    private static final int MAX_HISTORY_VERSIONS = 5;
    private final DataSubmissionRepository submissionRepo;
    private final DataSubmissionHistoryRepository historyRepo;

    // Must be transactional so persistAndFlush() works

    public void createSnapshot(String submissionId, EventChangeType changeType) {
        DataSubmission committed = submissionRepo.findById(submissionId)
            .orElseThrow(() -> new IllegalStateException("Committed DataSubmission not found: " + submissionId));

        DataSubmissionHistory h = new DataSubmissionHistory();
        h.setSubmissionId(committed.getId());
        h.setVersionNo(committed.getLockVersion());
        h.setCreatedAt(committed.getLastModifiedDate() != null ? committed.getLastModifiedDate() : Instant.now());
        h.setCreatedBy(committed.getLastModifiedBy());
        h.setChangeType(changeType);
        h.setFormData(committed.getFormData());
        h.setSerialNumber(committed.getSerialNumber());

        // persistAndFlush requires a transaction
        historyRepo.persistAndFlush(h);
    }

    // Runs in its own transaction (suspended outer tx if any)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pruneOldVersions(String submissionId) {
        Pageable page = PageRequest.of(0, MAX_HISTORY_VERSIONS, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<DataSubmissionHistory> keep = historyRepo.findBySubmissionIdOrderByCreatedAtDesc(submissionId, page);
        List<Long> keepIds = keep.stream()
            .map(DataSubmissionHistory::getId)
            .collect(Collectors.toList());

        if (keepIds.isEmpty()) {
            historyRepo.deleteBySubmissionId(submissionId);
            return;
        }

        historyRepo.deleteOlderThanKeepIds(submissionId, keepIds);
    }
}
