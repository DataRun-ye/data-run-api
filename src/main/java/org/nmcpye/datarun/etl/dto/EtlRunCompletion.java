package org.nmcpye.datarun.etl.dto;

import lombok.Builder;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link org.nmcpye.datarun.etl.entity.EtlRun}
 */
@Builder
@Accessors(chain=true)
public record EtlRunCompletion(UUID etlRunId, String status, Long minSubmissionSerial,
                               Long maxSubmissionSerial, Long submittedCount, Long successCount, Long failureCount,
                               String watermark, String metadata) implements Serializable {
}
