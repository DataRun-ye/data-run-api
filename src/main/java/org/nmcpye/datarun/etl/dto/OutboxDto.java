package org.nmcpye.datarun.etl.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight DTO used by the ETL consumer to process outbox events.
 * This is created by OutboxJdbcRepository using NamedParameterJdbcTemplate row mapping.
 * <p>
 * NOTE: payload is modeled as String (JSON). You may switch to JsonNode later.
 */
@Data
@Builder
public class OutboxDto {
    private Long outboxId;
    // Reference to DataSubmission.serial_number (DB sequence value)
    private Long submissionSerialNumber;

    private String submissionId;
    private String submissionUid;
    private String topic;
    private String eventType;
    private String payload;        // JSON payload as String (maps to JSONB)
    private String status;
    private Integer attempt;
    private Instant createdAt;
    private String claimedBy;
    private Instant claimedAt;
    private UUID ingestId;         // assigned at claim time
}
