package org.nmcpye.datarun.jpa.entityType;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeInstance;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeType;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An Entity Definition, A Template to define an Entity shape and its attributes,
 * {@link EntityAttributeType}s are instantiated configured, removed, or updated
 * as an instance @{@link EntityAttributeInstance} of the Attribute Types needed to define the Entities
 * instantiated from
 * this Entity type.
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
//    @JsonIgnore
//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

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


    /**
     * JSON array of entity attribute types configuration:
     * [{ id:"householdId", type:"string", required:true }, …]
     */
    @JsonProperty
    @Type(JsonType.class)
    @Column(name = "entity_attributes", columnDefinition = "jsonb")
    protected Set<EntityAttributeInstance> entityAttributes = new LinkedHashSet<>();
}
