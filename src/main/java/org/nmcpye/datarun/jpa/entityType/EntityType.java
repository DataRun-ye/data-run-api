package org.nmcpye.datarun.jpa.entityType;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeInstance;

import java.util.LinkedList;
import java.util.List;

/**
 * Defines the type of referencable domain object (e.g., Household, Patient).
 *
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "entity_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class EntityType extends JpaBaseIdentifiableObject {
    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "description")
    private String description;

    /**
     * JSON array of entity attribute types configuration:
     * [{ id:"householdId", type:"string", required:true }, …]
     */
    @JsonProperty
    @Type(JsonType.class)
    @Column(name = "entity_attributes", columnDefinition = "jsonb")
    protected List<EntityAttributeInstance> entityAttributes = new LinkedList<>();

    public EntityType(String id) {
        this.setId(id);
    }
}
