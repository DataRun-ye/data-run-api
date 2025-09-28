//package org.nmcpye.datarun.jpa.extraction;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//
//import java.time.Instant;
//import java.time.OffsetDateTime;
//
///**
// * @author Hamza Assada
// * @since 21/09/2025
// */
//@Entity
//@Table(name = "raw_repeat_instance",
//    indexes = {
//        @Index(name = "idx_rpi_repeat_uid", columnList = "repeat_uid"),
//        @Index(name = "idx_rpi_submission_uid", columnList = "submission_uid"),
//        @Index(name = "idx_rpi_template_version", columnList = "template_version_uid"),
//        @Index(name = "idx_rpi_processed", columnList = "processed")
//    })
//@Getter
//@Setter
//public class RawRepeatInstance {
//
//    @Id
//    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
//    private String id;
//
//    @Column(name = "submission_uid", length = 26, nullable = false)
//    private String submissionUid;
//
//    @Column(name = "template_uid", length = 128)
//    private String templateUid;
//
//    @Column(name = "template_version_uid", length = 128, nullable = false)
//    private String templateVersionUid;
//
//    @Column(name = "repeat_uid", length = 64, nullable = false)
//    private String repeatUid;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
//    private JsonNode payload;
//
//    @Column(name = "payload_id", length = 128)
//    private String payloadId;
//
//    @Column(name = "occurrence_index")
//    private Integer occurrenceIndex = 0;
//
//    @Column(name = "payload_checksum", columnDefinition = "text")
//    private String payloadChecksum;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "extraction_snapshot_id", foreignKey = @ForeignKey(name = "fk_rpi_snapshot"))
//    private TemplateExtractionSnapshot extractionSnapshot;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "plan_id", foreignKey = @ForeignKey(name = "fk_rpi_plan"))
//    private RepeatExtractionPlan plan;
//
//    @Column(name = "processed")
//    private Boolean processed = Boolean.FALSE;
//
//    @Column(name = "processed_at")
//    private OffsetDateTime processedAt;
//
//    @Column(name = "created_at", columnDefinition = "timestamp default now()", insertable = false, updatable = false)
//    private Instant createdAt;
//
//    public RawRepeatInstance() {
//    }
//}
