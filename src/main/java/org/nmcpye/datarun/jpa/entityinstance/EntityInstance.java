package org.nmcpye.datarun.jpa.entityinstance;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an instance of an EntityType; holds values per EntityAttributeValue.
 *
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "entity_instance")
@Getter
@Setter
@NoArgsConstructor
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class EntityInstance extends JpaBaseIdentifiableObject {
    public enum EntityStatus {ACTIVE, INACTIVE, ARCHIVED}

    @Enumerated(EnumType.STRING)
    private EntityStatus status = EntityStatus.ACTIVE;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entity_type_id", nullable = false)
    @JsonSerialize(contentAs = JpaIdentifiableObject.class)
    private EntityType entityType;

    @Column(name = "created_at_client")
    private Instant createdAtClient;

    @Column(name = "updated_at_client")
    private Instant updatedAtClient;

    /**
     * Holding actual attribute values for this instance.
     */
    @OneToMany(mappedBy = "entityInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonSerialize(contentAs = JpaAuditable.class)
    private List<EntityAttributeValue> attributeValues = new ArrayList<>();


    public EntityInstance(String id) {
        this.setId(id);
    }

    @Override
    public void prePersist() {
        super.prePersist();
        final var now = Instant.now();
        if (createdAtClient == null) {
            createdAtClient = now;
        }

        if (updatedAtClient == null) {
            updatedAtClient = now;
        }
    }

    public void addAttributeValue(EntityAttributeValue attributeValue) {
        this.attributeValues.add(attributeValue);
        attributeValue.setEntityInstance(this);
    }

    @Override
    public String getUid() {
        return "";
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }
}
