package org.nmcpye.datarun.etl.admin;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.repository.jdbc.OutboxJdbcRepository;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackfillServiceImpl implements BackfillService {
    private final DataSubmissionRepository submissionRepo;
    private final OutboxJdbcRepository outboxRepo;

    @Override
    public int enqueueBySubmissionIds(List<String> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) return 0;
        List<DataSubmission> submissions = submissionRepo.findAllById(submissionIds);
        return enqueueSubmissions(submissions);
    }

    @Override
    public int enqueueBySerialRange(Long fromSerial, Long toSerial) {
        if (fromSerial > toSerial) return 0;
        List<DataSubmission> submissions = submissionRepo.findBySerialNumberBetween(fromSerial, toSerial);
        return enqueueSubmissions(submissions);
    }

    private int enqueueSubmissions(List<DataSubmission> submissions) {
        if (submissions == null || submissions.isEmpty()) return 0;

        List<OutboxJdbcRepository.OutboxInsert> inserts = new ArrayList<>(submissions.size());
        for (DataSubmission s : submissions) {
            if (s == null) continue;
            String submissionId = s.getId();
            if (submissionId == null) continue;

            String payload = s.getForm();
            String submissionUid = s.getUid();
            String templateVersionUid = s.getFormVersion();

            OutboxJdbcRepository.OutboxInsert insert = new OutboxJdbcRepository.OutboxInsert(
                submissionId,
                submissionUid,
                templateVersionUid,
                payload,
                Instant.now(),
                s.getSerialNumber()
            );
            inserts.add(insert);
        }

        if (inserts.isEmpty()) return 0;
        return outboxRepo.insertBackfillIfNotExists(inserts);
    }
}

