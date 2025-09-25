//package org.nmcpye.datarun.domainmapping.model;
//
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.*;
//
//import java.time.Instant;
//
///**
// * @author Hamza Assada
// * @since 23/09/2025
// */
//@Entity
//@Table(name = "dataelement_domain_mapping")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class DataElementDomainMapping {
//    @Id
//    private String id;
//
//    @Column(name = "mapping_uid", nullable = false)
//    private String mappingUid;
//
//    @Column(name = "template_version_uid")
//    private String templateVersionUid;
//
//    @Column(name = "etc_uid")
//    private String etcUid;
//
//    @Column(name = "data_element_uid")
//    private String dataElementUid;
//
//    @Column(name = "domain_concept_id")
//    private String domainConceptId;
//
//    @Column(name = "mapping_expr", columnDefinition = "jsonb")
//    private String mappingExprJson;
//
//    @Column(name = "status")
//    private String status; // DRAFT | VALIDATED | PUBLISHED
//
//    @Column(name = "created_at")
//    private Instant createdAt;
//}
