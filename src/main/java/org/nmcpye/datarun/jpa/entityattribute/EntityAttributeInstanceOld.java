//package org.nmcpye.datarun.entityattribute;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.databind.annotation.JsonSerialize;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.NotNull;
//import jakarta.validation.constraints.Size;
//import lombok.Getter;
//import lombok.Setter;
//import org.hibernate.annotations.Cache;
//import org.hibernate.annotations.CacheConcurrencyStrategy;
//import org.nmcpye.datarun.common.AuditableObject;
//import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
//import org.nmcpye.datarun.entityType.EntityType;
//import org.nmcpye.datarun.mongo.domain.enumeration.ValueType;
//
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * an {@link EntityAttributeInstanceOld} links an {@link EntityType} to an {@link EntityAttributeType}
// * and configure it with additional configuration properties
// *
// * @author Hamza Assada, <7amza.it@gmail.com> <29-05-2025>
// */
//@Entity
//@Table(name = "entity_attribute_type_instance", uniqueConstraints = {
//        @UniqueConstraint(name = "uc_assignment_form_uid",
//                columnNames = {"entity_type_id", "entity_attribute_type_id"})})
//@Getter
//@Setter
//@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//@SuppressWarnings("common-java:DuplicatedBlocks")
//public class EntityAttributeInstanceOld
//        extends JpaBaseIdentifiableObject {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    protected Long id;
//
//    @Size(max = 11)
//    @Column(name = "uid", length = 11, nullable = false, unique = true)
//    protected String uid;
//
//    /**
//     * The unique code for this object.
//     */
//    @Column(name = "code", unique = true)
//    protected String code;
//
//    /**
//     * The name of this object. Required and unique.
//     */
//    @Column(name = "name", nullable = false, unique = true)
//    protected String name;
//
//    @Column(name = "displayinlist")
//    private Boolean displayInList = false;
//
//    @Column(name = "mandatory")
//    private Boolean mandatory = false;
//
//    @Column(name = "searchable")
//    private Boolean searchable = false;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "entity_type_id")
//    private EntityType entityType;
//
//    @NotNull
//    @JsonSerialize(as = AuditableObject.class)
//    @ManyToOne(optional = false, fetch = FetchType.LAZY)
//    @JoinColumn(name = "entity_attribute_type_id")
//    private EntityAttributeType entityAttributeType;
//
//
//    @OneToMany(mappedBy = "entityAttribute", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonIgnoreProperties(value = {"entityAttribute", "entityInstance"}, allowSetters = true)
//    private Set<EntityAttributeValue> entityAttributeValues = new HashSet<>();
//
//    @Override
//    public String getName() {
//        return (entityType != null ? entityType.getDisplayName() + " " : "")
//                + (entityAttributeType != null ? entityAttributeType.getDisplayName() : "");
//    }
//
//    @JsonProperty
//    public ValueType getValueType() {
//        return entityAttributeType != null ? entityAttributeType.getValueType() : null;
//    }
//}
