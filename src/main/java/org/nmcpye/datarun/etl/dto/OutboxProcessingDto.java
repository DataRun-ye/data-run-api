package org.nmcpye.datarun.etl.dto;

import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link org.nmcpye.datarun.etl.entity.OutboxProcessing}
 */
@Builder
public record OutboxProcessingDto(UUID etlRunId, Long outboxId, Long submissionSerialNumber, String submissionId,
                                  String submissionUid, String status, Integer attempt, String error,
                                  Instant processedAt) implements Serializable {
}
