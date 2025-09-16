package org.nmcpye.datarun.jpa.analytics.domain;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.analytics.domain.enums.Cardinality;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "analytics_relationship", schema = "analytics",
    indexes = {
        @Index(name = "idx_rel_from", columnList = "from_entity_uid"),
        @Index(name = "idx_rel_to", columnList = "to_entity_uid")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_relationship_uid", columnNames = {"relationship_uid"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "relationship_uid", nullable = false, length = 128)
    private String relationshipUid;

    @Column(name = "from_entity_uid", nullable = false, length = 128)
    private String fromEntityUid;

    @Column(name = "to_entity_uid", nullable = false, length = 128)
    private String toEntityUid;

    @Column(name = "join_expr", columnDefinition = "text", nullable = false)
    private String joinExpr;

    @Enumerated(EnumType.STRING)
    @Column(name = "cardinality", length = 32)
    private Cardinality cardinality;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Type(JsonType.class)
    @Column(name = "provenance", columnDefinition = "jsonb")
    private Map<String, Object> provenance;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
