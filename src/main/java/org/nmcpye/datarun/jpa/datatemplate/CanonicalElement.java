package org.nmcpye.datarun.jpa.datatemplate;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;

/**
 * @author Hamza Assada
 * @since 25/09/2025
 */
@Entity
@Table(name = "canonical_element", indexes = {
    @Index(name = "idx_canonical_element_fp", columnList = "schema_fingerprint")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalElement {

    @Id
    @Column(name = "canonical_element_uid", updatable = false, nullable = false)
    private String canonicalElementUid;

    @Column(name = "schema_fingerprint", length = 64, nullable = false, unique = true, updatable = false)
    private String schemaFingerprint;

    @Column(name = "preferred_name")
    private String preferredName;

    @Column(name = "semantic_type", length = 64)
    private String semanticType;

    @Type(JsonType.class)
    @Column(name = "canonical_candidates", columnDefinition = "jsonb default '[]'::jsonb")
    private List<String> canonicalCandidates;

    @Column(name = "value_constraints", columnDefinition = "jsonb")
    private String valueConstraintsJson;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    // convenience method to get canonicalCandidates as List<String> via ObjectMapper
}
