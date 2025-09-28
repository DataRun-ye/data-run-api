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
//
//@Entity
//@Table(name = "repeat_extraction_plan",
//    indexes = {
//        @Index(name = "idx_repplan_template_version", columnList = "template_version_uid"),
//        @Index(name = "idx_repplan_repeat_uid", columnList = "repeat_uid")
//    })
//@Getter
//@Setter
//public class RepeatExtractionPlan {
//
//    @Id
//    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
//    private String id;
//
//    /**
//     * Snapshot reference (not nullable).
//     */
//    @ManyToOne(optional = false, fetch = FetchType.LAZY)
//    @JoinColumn(name = "snapshot_id", nullable = false, foreignKey = @ForeignKey(name = "fk_repplan_snapshot"))
//    private TemplateExtractionSnapshot snapshot;
//
//    @Column(name = "template_version_uid", length = 128, nullable = false)
//    private String templateVersionUid;
//
//    @Column(name = "repeat_uid", length = 64, nullable = false)
//    private String repeatUid;
//
//    @Column(name = "semantic_repeat_path", columnDefinition = "text")
//    private String semanticRepeatPath;
//
//    @Column(name = "extraction_path", columnDefinition = "text")
//    private String extractionPath;
//
//    @Column(name = "natural_key_strategy", length = 64)
//    private String naturalKeyStrategy;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "payload_hint", columnDefinition = "jsonb")
//    private JsonNode payloadHint;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "flags", columnDefinition = "jsonb")
//    private JsonNode flags;
//
//    @Column(name = "descriptor_count")
//    private Integer descriptorCount;
//
//    @Column(name = "created_at", columnDefinition = "timestamp default now()", insertable = false, updatable = false)
//    private Instant createdAt;
//
//    public RepeatExtractionPlan() {
//    }
//}
