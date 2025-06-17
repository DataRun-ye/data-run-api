package org.nmcpye.datarun.jpa.entityattribute;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;

import java.util.Objects;


/**
 * Stores a single attribute value for an EntityInstance, linked to its EntityAttributeType.
 * This forms the "value" part of the EAV model for domain objects.
 *
 * @author Hamza Assada 29/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "entity_attribute_value",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_entity_attribute_instance",
            columnNames = {"entity_instance_id", "entity_attribute_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class EntityAttributeValue extends JpaAuditable<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Assuming auto-increment for this table's ID
    private Long id;

    /**
     * the EntityInstance it belongs to.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "entity_instance_id", nullable = false)
    @JsonSerialize(contentAs = JpaIdentifiableObject.class)
    private EntityInstance entityInstance;

    //    /**
//     * the definition of this attribute.
//     */
//    @ManyToOne(optional = false)
//    @JoinColumn(name = "entity_attribute_type_id", nullable = false)
//    @JsonSerialize(contentAs = JpaIdentifiableObject.class)
//    private EntityAttributeType attributeType;
    @Column(name = "entity_attribute_id", nullable = false)
    private String entityAttributeId;


    /**
     * The actual value of the attribute, stored as String to be flexible.
     */
    @Size(max = 50000)
    @Column(name = "attribute_value", nullable = false, length = 50000)
    private String value;


    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        EntityAttributeValue that = (EntityAttributeValue) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
