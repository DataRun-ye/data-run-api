package org.nmcpye.datarun.jpa.datasubmission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

/**
 * @author Hamza Assada
 * @since 24/09/2025
 */
@Entity
@Table(name = "extraction_manifest")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionManifest {

    @Id
    @Column(name = "manifest_uid")
    private String manifestUid;

    @Column(name = "submission_uid", length = 64, nullable = false)
    private String submissionUid;

    @Column(name = "template_version_uid", length = 64)
    private String templateVersionUid;

    @Column(name = "extraction_run_ts")
    private Instant extractionRunTs;

    @Column(name = "manifest_json", columnDefinition = "jsonb", nullable = false)
    private String manifestJson; // raw JSONB payload

    @Column(name = "created_at")
    private Instant createdAt;
}
