package org.nmcpye.datarun.jpa.entityinstance;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.common.IdentifiableProperty;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaAuditableObject;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * an {@link EntityType} instance created/updated by a submission
 *
 * @author Hamza Assada (27-05-2025), <7amza.it@gmail.com>
 */
@Entity
@Table(name = "entity_instance")
@Getter
@Setter
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings({"common-java:DuplicatedBlocks", "unused"})
public class EntityInstance extends JpaAuditableObject {

    public enum EntityStatus {ACTIVE, INACTIVE, ARCHIVED}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    protected String uid;

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid;

    @Column(name = "code")
    protected String code;

    @Column(name = "name")
    protected String name;

//    /**
//     * The assignment that created or most recently updated this entity.
//     */
//    @Column(name = "assignment_uid", nullable = false)
//    private String assignmentUid;

//    /**
//     * Optionally link back to the exact stage submission that drove this update.
//     */
//    @Column(name = "stage_submission_uid")
//    private String stageSubmissionUid;

    @Enumerated(EnumType.STRING)
    private EntityStatus status = EntityStatus.ACTIVE;
//
//    private Boolean deleted = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entity_type_id")
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    protected EntityType entityType;

    @Column(name = "created_at_client")
    private Instant createdAtClient;

    @Column(name = "updated_at_client")
    private Instant updatedAtClient;

    @OneToMany(mappedBy = "entityInstance", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonIgnoreProperties(value = {"entityAttribute", "entityInstance"}, allowSetters = true)
    @JsonSerialize(contentAs = JpaAuditable.class)
    protected Set<EntityAttributeValue> entityAttributeValues = new LinkedHashSet<>();

//    @OneToMany(mappedBy = "entityInstance", orphanRemoval = true)
//    @JsonIgnoreProperties(value = {"entityInstance", "assignmentType", "orgUnit"}, allowSetters = true)
//    private Set<EntityInstanceOwner> entityInstanceOwners = new LinkedHashSet<>();

    public void setAutoFields() {
        super.setAutoFields();
        final var now = Instant.now();
        if (createdAtClient == null) {
            createdAtClient = now;
        }

        if (updatedAtClient == null) {
            updatedAtClient = now;
        }

        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
    }

    public EntityInstance() {
        setAutoFields();
    }

    @Override
    public String getPropertyValue(IdScheme idScheme) {
        final var propertyValue = super.getPropertyValue(idScheme);
        if (propertyValue == null) {
            if (idScheme.is(IdentifiableProperty.UUID)) {
                return uuid.toString();
            }
        }

        return null;
    }
}
