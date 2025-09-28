package org.nmcpye.datarun.jpa.datasubmissionoutbox;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * @author Hamza Assada
 * @since 15/08/2025
 */
@Getter
@Setter
@Entity
@Table(name = "outbox_event",
    indexes = {
        @Index(name = "idx_outbox_status_available", columnList = "status, available_at"),
        @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id"),

        @Index(name = "idx_outbox_eventtype_available", columnList = "event_type, status, available_at"),
        @Index(name = "idx_outbox_processing_started_at  ", columnList = "processing_started_at")
    }
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", length = 128, nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 64, nullable = false)
    private String aggregateId;

    /// # 2 — Event types / worker responsibilities
    ///
    /// Use `event_type` to route work to worker types.
    ///
    /// * `submission.created` — created by ingest; payload: `{ "submissionId": "..." }`
    ///   *Consumer:* Extractor workers
    ///
    /// * `manifest.created` — emitted by Extractor after manifest upsert; payload: `{ "manifestId":"...", "submissionId":"..." }`
    ///   *Consumer:* ETL workers
    ///
    /// * `index.update` / `analytics.update` — emitted by ETL if needed; *Consumer:* Indexer/analytics workers
    ///
    /// * `backfill.task` — used to schedule large reprocessing jobs; *Consumer:* Spring Batch worker
    @Column(name = "event_type", length = 128, nullable = false)
    private String eventType;

    @Column(name = "payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "available_at", nullable = false)
    @Builder.Default
    private Instant availableAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    //
    @Column(name = "processing_owner")
    private String processingOwner;
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;
}
