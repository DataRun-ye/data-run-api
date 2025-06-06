package org.nmcpye.datarun.jpa.entityattribute;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxy;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;

import java.io.Serializable;
import java.util.Objects;


/**
 * A EntityAttributeValue.
 */
@Entity
@Table(name = "entity_attribute_value",
    indexes = {@Index(name = "idx_entity_attribute_instance_uid", columnList = "entity_attribute_uid")},
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_entity_attribute_instance",
            columnNames = {"entity_instance_id", "entity_attribute_uid"})
    })
@Getter
@Setter
@NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class EntityAttributeValue
    extends JpaAuditable<Long> implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entity_instance_id", nullable = false)
//    @JsonIgnoreProperties(value = {"entityAttributeValues", "entityType", "entityInstanceOwners"}, allowSetters = true)
    @JsonSerialize(contentAs = JpaIdentifiableObject.class)
    protected EntityInstance entityInstance;

    @Column(name = "entity_attribute_uid", nullable = false)
    private String entityAttributeUid;

    @Column(name = "value"/*, columnDefinition = "TEXT"*/)
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
