//package org.nmcpye.datarun.jpa.extraction;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//
//import java.time.Instant;
//
///**
// * @author Hamza Assada
// * @since 21/09/2025
// */
//@Entity
//@Table(name = "field_extraction_descriptor",
//    indexes = {
//        @Index(name = "idx_descriptor_plan_id", columnList = "plan_id")
//    })
//@Getter
//@Setter
//public class FieldExtractionDescriptor {
//
//    @Id
//    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
//    private String id;
//
//    @ManyToOne(optional = false, fetch = FetchType.LAZY)
//    @JoinColumn(name = "plan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_descriptor_plan"))
//    private RepeatExtractionPlan plan;
//
//    @Column(name = "element_uid", length = 64)
//    private String elementUid;
//
//    @Column(name = "field_uid", length = 64)
//    private String fieldUid;
//
//    @Column(name = "json_pointer", columnDefinition = "text")
//    private String jsonPointer;
//
//    @Column(name = "value_type", length = 32)
//    private String dataType;
//
//    @Column(name = "option_set_uid", length = 64)
//    private String optionSetUid;
//
//    @Column(name = "is_multi")
//    private Boolean multi = Boolean.FALSE;
//
//    @Column(name = "explode")
//    private Boolean explode = Boolean.FALSE;
//
//    @Column(name = "is_natural_key_candidate")
//    private Boolean naturalKeyCandidate = Boolean.FALSE;
//
//    @Column(name = "is_measure")
//    private Boolean measure = Boolean.FALSE;
//
//    @Column(name = "is_dimension")
//    private Boolean dimension = Boolean.FALSE;
//
//    @Column(name = "output_column", length = 128)
//    private String outputColumn;
//
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "post_transform", columnDefinition = "jsonb")
//    private JsonNode postTransform;
//
//    @Column(name = "sort_order")
//    private Integer sortOrder = 0;
//
//    @Column(name = "created_at", columnDefinition = "timestamp default now()", insertable = false, updatable = false)
//    private Instant createdAt;
//
//    public FieldExtractionDescriptor() {
//    }
//}
