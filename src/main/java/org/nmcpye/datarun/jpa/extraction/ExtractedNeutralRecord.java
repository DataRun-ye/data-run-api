package org.nmcpye.datarun.jpa.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * @author Hamza Assada
 * @since 21/09/2025
 */
@Entity
@Table(name = "extracted_neutral_record",
    indexes = {
        @Index(name = "idx_enr_plan_id", columnList = "plan_id"),
        @Index(name = "idx_enr_repeat_uid", columnList = "repeat_uid"),
        @Index(name = "idx_enr_submission_uid", columnList = "submission_uid")
    })
@Getter
@Setter
public class ExtractedNeutralRecord {

    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", foreignKey = @ForeignKey(name = "fk_enr_plan"))
    private RepeatExtractionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_snapshot_id", foreignKey = @ForeignKey(name = "fk_enr_snapshot"))
    private TemplateExtractionSnapshot extractionSnapshot;

    @Column(name = "template_version_uid", length = 128)
    private String templateVersionUid;

    @Column(name = "repeat_uid", length = 64)
    private String repeatUid;

    @Column(name = "raw_payload_id", length = 128)
    private String rawPayloadId;

    @Column(name = "submission_uid", length = 26)
    private String submissionUid;

    @Column(name = "occurrence_index")
    private Integer occurrenceIndex;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted", columnDefinition = "jsonb", nullable = false)
    private JsonNode extracted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provenance", columnDefinition = "jsonb")
    private JsonNode provenance;

    @Column(name = "created_at", columnDefinition = "timestamp default now()", insertable = false, updatable = false)
    private Instant createdAt;

    public ExtractedNeutralRecord() {
    }
}
