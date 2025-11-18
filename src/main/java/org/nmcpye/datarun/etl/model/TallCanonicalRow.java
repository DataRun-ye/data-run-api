package org.nmcpye.datarun.etl.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing a single canonical tall row to be upserted into tall_canonical.
 * Used as the batch unit for the JDBC repository.
 */
@Data
@Builder
public class TallCanonicalRow {
    private Long outboxId;
    private UUID ingestId;
    private Long submissionSerialNumber;
    private String submissionId;
    private String submissionUid;
    private String templateVersionUid;
    private String canonicalElementUid; // UUID string
    private String elementPath;
    private String repeatInstanceId; // nullable
    private String parentInstanceId; // nullable
    private Integer repeatIndex; // nullable// fallback if repeatInstanceId absent
    private String valueText;
    private BigDecimal valueNumber;
    private String valueJson; // JSON string
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isDeleted;
}
