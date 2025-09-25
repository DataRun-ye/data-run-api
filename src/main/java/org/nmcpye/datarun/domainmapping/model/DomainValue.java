//package org.nmcpye.datarun.domainmapping.model;
//
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.Instant;
//import java.util.UUID;
//
///**
// * @author Hamza Assada
// * @since 23/09/2025
// */
//@Entity
//@Table(name = "domain_value", indexes = {
//    @Index(name = "ix_domainvalue_domain_concept", columnList = "domain_concept_id"),
//    @Index(name = "ix_domainvalue_submission", columnList = "submission_uid"),
//    @Index(name = "ux_domainvalue_idempotency", columnList = "idempotency_key", unique = true)
//})
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class DomainValue {
//    @Id
//    @Column(name = "id", nullable = false)
//    private String id;
//
//    @Column(name = "domain_concept_id", nullable = false)
//    private UUID domainConceptId;
//
//    @Column(name = "submission_uid")
//    private String submissionUid;
//
//    @Column(name = "repeat_instance_id")
//    private String repeatInstanceId;
//
//    @Column(name = "value_text", columnDefinition = "text")
//    private String valueText;
//
//    @Column(name = "value_num")
//    private Double valueNum;
//
//    @Column(name = "value_bool")
//    private Boolean valueBool;
//
//    @Column(name = "value_ts")
//    private Instant valueTs;
//
//    @Column(name = "value_array", columnDefinition = "jsonb")
//    private String valueArray; // store JSON string
//
//    @Column(name = "source_element_data_value_id")
//    private Long sourceElementDataValueId;
//
//    @Column(name = "etl_run_id")
//    private java.util.UUID etlRunId;
//
//    @Column(name = "mapping_uid")
//    private String mappingUid;
//
//    @Column(name = "idempotency_key", length = 128, nullable = false, unique = true)
//    private String idempotencyKey;
//
//    @Column(name = "created_at")
//    private Instant createdAt;
//}
