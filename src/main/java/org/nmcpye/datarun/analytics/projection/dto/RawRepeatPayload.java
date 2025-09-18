package org.nmcpye.datarun.analytics.projection.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @author Hamza Assada
 * @since 17/09/2025
 */
@Entity
@Table(
    name = "raw_repeat_payload",
    indexes = {
        @Index(name = "uq_raw_repeat_by_submission_occ", columnList = "submission_uid, repeat_path, occurrence_index"),
        @Index(name = "uq_raw_repeat_by_submission_payload_id", columnList = "submission_uid, repeat_path, payload_id"),
        @Index(name = "idx_raw_repeat_by_repeat_uid", columnList = "repeat_uid"),
        @Index(name = "idx_raw_repeat_by_path", columnList = "repeat_path"),
        @Index(name = "idx_raw_repeat_by_submission", columnList = "submission_uid"),
        @Index(name = "idx_raw_repeat_by_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
public class RawRepeatPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false, length = 26)
    private String id;

    @Column(name = "template_version_uid", nullable = false, length = 11)
    private String templateVersionUid;

    @Column(name = "repeat_path", nullable = false)
    private String repeatPath;

    @Column(name = "submission_uid", nullable = false, length = 11)
    private String submissionUid;

    @Column(name = "repeat_uid", nullable = false, length = 32)
    private String repeatUid;

    @Column(name = "payload_id", nullable = false, length = 32)
    private String payloadId;

    @Column(name = "occurrence_index", nullable = false)
    private Integer occurrenceIndex = 0;

    @Type(JsonType.class)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;

    @Column(name = "payload_parent_id", length = 32)
    private String payloadParentId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public RawRepeatPayload() {
        if (this.id == null) {
            setId(CodeGenerator.nextUlid());
        }
    }
}
