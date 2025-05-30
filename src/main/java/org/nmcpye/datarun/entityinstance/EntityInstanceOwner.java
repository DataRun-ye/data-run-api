//package org.nmcpye.datarun.entityinstance;
//
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotNull;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.nmcpye.datarun.assignmenttype.AssignmentType;
//import org.nmcpye.datarun.common.AuditableObject;
//import org.nmcpye.datarun.common.jpa.JpaAuditable;
//import org.nmcpye.datarun.orgunit.OrgUnit;
//
///**
// * A EntityOwner.
// */
//@Entity
//@Table(name = "entity_instance_owner", uniqueConstraints = {
//        @UniqueConstraint(name = "uc_eio_assignment_type",
//                columnNames = {"entity_instance_id", "assignment_type_id"})})
//@Getter
//@Setter
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@NoArgsConstructor
//@JsonIgnoreProperties(value = {"createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate"}, allowGetters = true)
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class EntityInstanceOwner extends JpaAuditable<Long> {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @JsonIgnore
//    protected Long id;
//
//    @NotNull
//    @ManyToOne(optional = false, fetch = FetchType.LAZY)
//    @JoinColumn(name = "entity_instance_id")
//    @JsonSerialize(as = JpaAuditable.class)
//    private EntityInstance entityInstance;
//
//    @NotNull
//    @ManyToOne(optional = false)
//    @JoinColumn(name = "assignment_type_id")
//    @JsonSerialize(as = AuditableObject.class)
//    private AssignmentType assignmentType;
//
//    @ManyToOne
//    @JoinColumn(name = "org_unit_id")
//    @JsonSerialize(as = AuditableObject.class)
//    private OrgUnit orgUnit;
//
//    public EntityInstanceOwner(EntityInstance trackedEntityInstance, AssignmentType assignmentType,
//                               OrgUnit orgUnit) {
//        this.entityInstance = trackedEntityInstance;
//        this.assignmentType = assignmentType;
//        this.orgUnit = orgUnit;
//        this.createdBy = "internal";
//    }
//
//    @Override
//    public boolean equals(Object object) {
//        if (this == object) {
//            return true;
//        }
//
//        if (object == null) {
//            return false;
//        }
//
//        if (!getClass().isAssignableFrom(object.getClass())) {
//            return false;
//        }
//
//        final EntityInstanceOwner other = (EntityInstanceOwner) object;
//
//        if (entityInstance == null) {
//            if (other.entityInstance != null) {
//                return false;
//            }
//        } else if (!entityInstance.equals(other.entityInstance)) {
//            return false;
//        }
//
//        if (assignmentType == null) {
//            return other.assignmentType == null;
//        } else return assignmentType.equals(other.assignmentType);
//    }
//
//    @Override
//    public int hashCode() {
//        final int prime = 31;
//        int result = super.hashCode();
//
//        result = prime * result + ((entityInstance == null) ? 0 : entityInstance.hashCode());
//        result = prime * result + ((assignmentType == null) ? 0 : assignmentType.hashCode());
//
//        return result;
//    }
//}
