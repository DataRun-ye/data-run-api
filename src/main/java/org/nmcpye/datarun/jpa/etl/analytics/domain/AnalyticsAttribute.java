package org.nmcpye.datarun.jpa.etl.analytics.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.nmcpye.datarun.analytics.dto.Aggregation;
import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.AttributeScope;
import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.AttributeType;
import org.nmcpye.datarun.jpa.etl.analytics.domain.enums.DataType;

import java.util.Map;

@Entity
@Table(name = "analytics_attribute",
    schema = "analytics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_analytics_attr_version_uid", columnNames = {"templateVersionUid", "uid"})
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String uid;

    @Column(nullable = false, length = 11)
    private String templateUid;

    @Column(nullable = false, length = 11)
    private String templateVersionUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private AnalyticsSource source;

    /// -- physical column alias planned in MV
    @Column(nullable = false, length = 128)
    private String attributeName;

    @Column(nullable = false)
    private String sourceColumnMapping;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeType attributeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType dataType;

    @Enumerated(EnumType.STRING)
    private Aggregation aggregationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttributeScope attributeScope;

    @Column(length = 50)
    private String repeatGroupUid;

    // for ui to call end point to resolve
    @Column(length = 50)
    private String entityRefType;

    @Column(length = 11)
    private String sourceElementUid;

    @Column(length = 3000)
    private String sourceSemanticPath;

    @Column(name = "ordinal")
    private Integer ordinal;

    /// -- if SelectOne/SelectMulti
    @Column(length = 11)
    private String optionSetUid;
}

//public class AnalyticsAttribute {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    /// -- e.g., "aa_{element_template_config_uid}" or "aa_{entity_uid}_{de_uid}"
//    @Column(nullable = false, length = 50)
//    private String uid;
//
//    /// dimension/measure
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private AttributeType attributeType;
//
//    @Column(columnDefinition = "display_name", nullable = false)
//    private String displayName;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private DataType dataType;
//
//    /// **`db_mapping_info` is the critical link**: It decouples the public `uid` from the physical database column. This is
//    ///   what allows the backend schema to evolve independently.
//    /// Store as a JSON string
//    /// * For a simple dimension: `{"source": "PIVOT_GRID_FACTS", "column": "value_text", "element_uid": "de_abc"}`
//    /// * For an aggregated measure:
//    ///   `{"source": "PIVOT_GRID_FACTS", "column": "value_num", "element_uid": "de_xyz", "aggregate_fn": "SUM"}`
//    /// * For a system-level dimension: `{"source": "PIVOT_GRID_FACTS", "column": "team_name"}`
//    /// * For a system-level measure:
//    ///   `{"source": "PIVOT_GRID_FACTS", "column": "submission_uid", "aggregate_fn": "COUNT_DISTINCT"}`
//    @Type(JsonType.class)
//    @Column(columnDefinition = "jsonb", nullable = false)
//    private String dbMappingInfo;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private AttributeScope attributeScope;
//
//    @Column(length = 50)
//    private String repeatGroupUid;
//
//    @Column(length = 20)
//    private String aggregationType;
//
//    @Column(length = 50)
//    private String entityRefType;
//
//    @Column(length = 11)
//    private String sourceElementUid;
//
//    @Column(length = 3000)
//    private String sourceSemanticPath;
//
//    // NMC ----------------
//    @Column(name = "ordinal")
//    private Integer ordinal;
//
//    /// -- physical column alias planned in MV
//    @Column(nullable = false, length = 128)
//    private String attributeName;
//
//    @Type(JsonType.class)
//    @Column(name = "label", columnDefinition = "jsonb")
//    private Map<String, String> label = new HashMap<>();
//
//    @Column(name = "element_template_config_uid", length = 64)
//    private String elementTemplateConfigUid;
//
//    @Column(name = "data_element_uid", length = 64)
//    private String dataElementUid;
//
//    /// -- if SelectOne/SelectMulti
//    @Column(length = 11)
//    private String optionSetUid;
//}
