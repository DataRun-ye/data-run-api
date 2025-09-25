package org.nmcpye.datarun.jpa.featureflag;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;


/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String uid;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String environment = "production";

    @Column(nullable = false)
    private boolean enabled = false;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> variations; // example: {"on":true,"off":false} or more complex

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rules; // targeting DSL

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rolloutStrategy; // e.g. { "type":"percentage", "percentage":30, "by":"userId" }

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    private Integer version = 1;

    private String createdBy;
    private Instant createdAt = Instant.now();
    private String updatedBy;
    private Instant updatedAt = Instant.now();
}
