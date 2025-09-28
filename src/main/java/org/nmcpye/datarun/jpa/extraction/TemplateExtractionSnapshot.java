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
///**
// * @author Hamza Assada
// * @since 21/09/2025
// */
//@Entity
//@Table(name = "template_extraction_snapshot",
//    indexes = {
//        @Index(name = "idx_tes_template_version", columnList = "template_version_uid")
//    })
//@Getter
//@Setter
//public class TemplateExtractionSnapshot {
//
//    @Id
//    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
//    private String id;
//
//    @Column(name = "template_version_uid", length = 128, nullable = false)
//    private String templateVersionUid;
//
//    @Column(name = "generator_version", length = 64)
//    private String generatorVersion;
//
//    @Column(name = "checksum", columnDefinition = "text")
//    private String checksum;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
//    private JsonNode payload;
//
//    @Column(name = "created_at", columnDefinition = "timestamp default now()", insertable = false, updatable = false)
//    private Instant createdAt;
//
//    public TemplateExtractionSnapshot() {
//    }
//
//    // convenience constructor
//    public TemplateExtractionSnapshot(String templateVersionUid, JsonNode payload) {
//        this.templateVersionUid = templateVersionUid;
//        this.payload = payload;
//    }
//}
