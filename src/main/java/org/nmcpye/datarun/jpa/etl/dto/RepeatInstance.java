package org.nmcpye.datarun.jpa.etl.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * @author Hamza Assada 02/06/2025 (7amza.it@gmail.com)
 */
//@Entity
//@Table(name = "repeat_instance",
//    indexes = {
//        @Index(name = "idx_repeat_instance_submission_id", columnList = "id,submission_id", unique = true),
//        @Index(name = "idx_repeat_instance_deleted_at", columnList = "deleted_at"),
//    })
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NoArgsConstructor
@Getter
//@Setter
public class RepeatInstance implements Serializable/*, AuditableObject<String> */ {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ULID id (Universally Unique Lexicographically Sortable Identifier).
     * Length: 26 characters, Base32 encoded.
     */
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    protected String id;

    @Column(name = "submission_id", nullable = false)
    private String submission;

    @Column(name = "repeat_path", length = 3000, nullable = false)
    private String repeatPath;

    @Column(name = "repeat_index")
    private Long repeatIndex;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "client_updated_at")
    private Instant clientUpdatedAt;

    //    @CreatedDate
    @Column(name = "created_date", updatable = false)
    @Setter
    protected Instant createdDate = Instant.now();

    //    @LastModifiedDate
    @Column(name = "last_modified_date")
    @Setter
    protected Instant lastModifiedDate = Instant.now();

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    protected String lastModifiedBy;


    @Builder(toBuilder = true)
    public static RepeatInstance from(String id, String submission, String repeatPath,
                                      Long repeatIndex, String createdBy, String lastModifiedBy,
                                      Instant clientUpdatedAt, Instant deletedAt,
                                      Instant createdDate, Instant lastModifiedDate) {
        final RepeatInstance r = new RepeatInstance();
        r.id = id;
        r.submission = submission;
        r.repeatPath = repeatPath;
        r.repeatIndex = repeatIndex;
        r.createdBy = createdBy;
        r.lastModifiedBy = lastModifiedBy;
        r.clientUpdatedAt = clientUpdatedAt;
        r.deletedAt = deletedAt;
        r.createdDate = createdDate;
        r.lastModifiedDate = lastModifiedDate;
        return r;
    }
}
