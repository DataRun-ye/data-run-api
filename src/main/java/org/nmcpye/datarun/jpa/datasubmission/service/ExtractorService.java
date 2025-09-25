package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEvent;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.OutboxEventStatus;
import org.nmcpye.datarun.jpa.datasubmissionoutbox.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Service
@RequiredArgsConstructor
public class ExtractorService {
    private final DataSubmissionRepository dataSubmissionRepository;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void extractAndPublishManifest(String submissionId) {
        // 1) read submission
        DataSubmission submission = dataSubmissionRepository.findByUid(submissionId).orElse(null);

        // 2) build manifest json
        String manifestId = "buildManifest(submission)";

        // persist an outbox even for manifest, for etl, other poll and process services
        outboxEventRepository.persistAndFlush(createOutboxEvent(manifestId, submissionId));
    }

    private OutboxEvent createOutboxEvent(String manifestId, String submissionId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("submissionId", submissionId);
        payload.put("manifestId", manifestId);

        return OutboxEvent.builder()
            .aggregateType("ExtractionManifest")
            .aggregateId(manifestId)
            .eventType("ExtractionManifest.created")
            .payload(payload)
            .status(OutboxEventStatus.PENDING)
            .attempts(0)
            .createdAt(Instant.now())
            .availableAt(Instant.now())
            .build();
    }
}
