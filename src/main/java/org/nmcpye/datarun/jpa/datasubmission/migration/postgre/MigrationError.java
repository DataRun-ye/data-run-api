package org.nmcpye.datarun.jpa.datasubmission.migration.postgre;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

/**
 * @author Hamza Assada 16/08/2025 (7amza.it@gmail.com)
 */
@Entity
@Table(name = "migration_errors")
@Getter
@Setter
public class MigrationError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", length = 100)
    private String sourceId;

    @Column(name = "submission_uid", length = 26)
    private String submissionUid;

    @Column(name = "stage", length = 16, nullable = false)
    private String stage;

    @Column(name = "error_class", length = 255)
    private String errorClass;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    // jsonb column - keep as text or JsonNode; using String avoids type mapping issues,
    // but we keep JsonNode convenience (your project uses Jackson + JdbcTypeCode elsewhere).
    @Column(name = "payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload; // store JSON string; you can change to JsonNode if you prefer

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public MigrationError() {
    }

    public MigrationError(String sourceId, String submissionUid, String stage,
                          String errorClass, String errorMessage, JsonNode payload) {
        this.sourceId = sourceId;
        this.submissionUid = submissionUid;
        this.stage = stage;
        this.errorClass = errorClass;
        this.errorMessage = errorMessage;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MigrationError)) return false;
        MigrationError that = (MigrationError) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
