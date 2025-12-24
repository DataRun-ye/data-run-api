package org.nmcpye.datarun.etl.service;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.etl.dto.SubmissionKeyDto;
import org.nmcpye.datarun.etl.repository.SubmissionKeysJdbcRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionKeysService {
    private final SubmissionKeysJdbcRepository submissionKeysJdbcRepository;

    public void upsert(String submissionUid, Long submissionSerial, String status, String submissionId, String assignmentUid,
                       String activityUid, String orgUnitUid, String teamUid, String templateUid, Instant lastSeen) {
        submissionKeysJdbcRepository.upsertSubmissionKey(submissionUid, submissionSerial, status, submissionId,
            assignmentUid, activityUid, orgUnitUid, teamUid, templateUid, lastSeen);
    }

    public Optional<SubmissionKeyDto> find(String submissionUid) {
        return submissionKeysJdbcRepository.findBySubmissionUid(submissionUid);
    }
}
