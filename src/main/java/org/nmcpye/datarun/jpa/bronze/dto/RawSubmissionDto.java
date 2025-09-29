package org.nmcpye.datarun.jpa.bronze.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;


/**
 * @author Hamza Assada
 * @since 28/09/2025
 */
@Data
@Builder
public class RawSubmissionDto {
    private UUID ingestionId;
    private OffsetDateTime receivedAt;
    private String sourceSystem;
    private String templateId;
    private String templateVersion;
    private String submissionId;
    private String userId;
    private String orgUnit;
    private JsonNode submissionJson; // Jackson JsonNode -> stored as JSONB
}

