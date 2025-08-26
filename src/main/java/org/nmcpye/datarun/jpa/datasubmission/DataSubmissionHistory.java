package org.nmcpye.datarun.jpa.datasubmission;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.nmcpye.datarun.jpa.datasubmission.events.SubmissionChangeType;

import java.time.Instant;
import java.util.Objects;

/**
 * @author Hamza Assada
 * @since 14/08/2025
 */
@Entity
@Table(name = "data_submission_history",
    indexes = {
        @Index(name = "idx_sh_submission_id", columnList = "submission_id"),
        @Index(name = "idx_sh_submission_version", columnList = "submission_id, version_no")
    })
@Getter
@Setter
public class DataSubmissionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    private Long id;

    /**
     * Original DataSubmission.id (ULID)
     */
    @Column(name = "submission_id", length = 26, nullable = false)
    private String submissionId;

    @Column(name = "version_no")
    private Long versionNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100, nullable = false, updatable = false)
    private String createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", length = 32)
    private SubmissionChangeType changeType; // CREATE/UPDATE/DELETE/RESTORE

    @Column(name = "form_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode formData;

    @Column(name = "serial_number")
    private Long serialNumber;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataSubmissionHistory that)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getSubmissionId(), that.getSubmissionId()) && Objects.equals(getVersionNo(), that.getVersionNo()) && getChangeType() == that.getChangeType() && Objects.equals(getSerialNumber(), that.getSerialNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSubmissionId(), getVersionNo(), getChangeType(), getSerialNumber());
    }
}
