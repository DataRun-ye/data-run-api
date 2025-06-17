//package org.nmcpye.datarun.jpa.entityattribute;
//
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
//import org.nmcpye.datarun.jpa.common.JpaAuditable;
//import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
//import org.nmcpye.datarun.jpa.entityType.EntityType;
//
///**
// * an {@link EntityAttributeInstance} links an {@link EntityType} to an {@link EntityAttributeType}
// * and configure it with additional configuration properties
// *
// * @author Hamza Assada 29/05/2025 <7amza.it@gmail.com>
// */
//@Entity
//@Table(name = "entity_attribute")
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@Getter
//@Setter
//public class EntityAttributeInstance extends JpaAuditable<String> {
//    @Id
//    @Column(name = "id", length = 26, updatable = false, nullable = false)
//    private String id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "entity_type_id", nullable = false)
//    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
//    private EntityAttributeType attributeType;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "entity_type_id", nullable = false)
//    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
//    private EntityType entityType;
//
//    private Boolean displayInList = false;
//
//    private Boolean mandatory = false;
//
//    private Boolean searchable = false;
//
//    /**
//     * Lifecycle hook to generate a ULID ID before persisting.
//     */
//    @PrePersist
//    public void prePersist() {
//        if (getId() == null) {
//            this.id = CodeGenerator.ULIDGenerator.nextString();
//        }
//    }
//}
