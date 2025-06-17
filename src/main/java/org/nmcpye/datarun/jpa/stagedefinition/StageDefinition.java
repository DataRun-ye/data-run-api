package org.nmcpye.datarun.jpa.stagedefinition;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.scopeinstance.DimensionalContext;

import java.time.Instant;

/**
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "stage_definition", indexes = {
    @Index(name = "idx_stage_flow_order", columnList = "flow_type_id, stage_order")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uc_stage_definition_template_id",
        columnNames = {"flow_type_id", "data_template_id"})
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class StageDefinition extends JpaSoftDeleteObject {
    @Size(max = 11)
    @Column(name = "uid", length = 11, updatable = false, unique = true)
    private String uid;

    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @NotNull
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @Column(name = "description")
    private String description;

    @Column(name = "repeatable")
    private Boolean repeatable = false;

    @NotNull
    @Column(name = "stage_order", nullable = false)
    private Integer stepOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_type_id", nullable = false)
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    private FlowType flowType;

    /**
     * Ordered list of scope definitions, serialized as JSON
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_scope_definition")
    @JsonSerialize(contentAs = JpaAuditable.class)
    private DimensionalContext stageDimensionalContext;

    @NotNull
    @Column(name = "data_template_id", length = 26)
    private String dataTemplateId;

    /**
     * If this step is an entity-bound step specify EntityType or = null (i.e, client need to specify
     * an EntityInstance (e.g, Household, Patient) of the specifies EntityType to
     * submit the step data instances against.
     * If entity instance is already available, it can be selected/searched.
     * if not then `add new, save, and select it` (maybe adding new is configurable?),
     * Consider optional uniqueness (across a FlowType, a FlowRun}
     * an OrgUnit, or mix, or across the whole Activity  running at the time
     *
     * @see EntityType
     * @see EntityInstance
     * @see FlowType
     * @see FlowInstance
     */
    @Column(name = "entity_bound_type_id", length = 26)
    private String entityBoundTypeId;

    public StageDefinition(String id) {
        this.setId(id);
    }
}
