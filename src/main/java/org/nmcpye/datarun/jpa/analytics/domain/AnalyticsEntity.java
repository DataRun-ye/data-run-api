package org.nmcpye.datarun.jpa.analytics.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.analytics.domain.enums.EntityKind;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "analytics_entity", schema = "analytics",
    indexes = {
        @Index(name = "idx_analytics_entity_by_template", columnList = "template_uid, template_version_uid"),
        @Index(name = "idx_analytics_entity_by_semantic", columnList = "semantic_repeat_path")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_entity_uid_metadata_version", columnNames = {"entity_uid", "metadata_version"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /// deterministic, e.g. "an_{canonical_name}" or hashed
    @Column(name = "entity_uid", nullable = false, length = 128)
    private String entityUid;

    /// human-friendly canonical name
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String entityName;

    /// Localized display Labels, {"en":"...", "fr": "..."}
    @Type(JsonType.class)
    @Column(name = "label", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> label = new HashMap<>();

    /// -- 'root' | 'repeat' | 'derived'
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_kind", nullable = false, length = 32)
    private EntityKind entityKind;

    /// -- nullable if canonical (global) entity
    @Column(name = "template_uid", length = 64)
    private String templateUid;

    @Column(name = "template_version_uid", length = 64)
    private String templateVersionUid;

    /// -- canonical repeat path if template-scoped
    @Column(name = "semantic_repeat_path", columnDefinition = "text")
    private String semanticRepeatPath;

    /// -- sanitized short form for naming
    @Column(name = "semantic_path_normalized", length = 512)
    private String semanticPathNormalized;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /// -- FK -> grain_config.id (nullable)
    @Column(name = "grain_config_id")
    private Long grainConfigId;

    /// -- suggested MV name (sanitized)
    @Column(name = "mv_name_suggestion", length = 128)
    private String mvNameSuggestion;

    /// -- actual registered name in MVRegistry if deployed
    @Column(name = "mv_name_registered", length = 128)
    private String mvNameRegistered;

    /// -- every change increments version for this entity_uid
    @Column(name = "metadata_version", nullable = false)
    @Builder.Default
    private Integer metadataVersion = 1;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isCurrent = true;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * freeform tags jsonb
     */
    @Type(JsonType.class)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, Object> tags;

    /**
     * provenance jsonb,  -- input/template_version, derivation trace, etc.
     */
    @Type(JsonType.class)
    @Column(name = "provenance", columnDefinition = "jsonb")
    private Map<String, Object> provenance;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
