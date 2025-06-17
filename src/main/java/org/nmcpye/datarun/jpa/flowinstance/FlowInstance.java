package org.nmcpye.datarun.jpa.flowinstance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.scopeinstance.DimensionalContext;
import org.nmcpye.datarun.jpa.scopeinstance.FlowContext;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a runtime instance of a workflow.
 * Links to a specific FlowType and manages its overall status and submissions.
 *
 * @author Hamza Assada 20/03/2023
 */
@Entity
@Table(name = "flow_instance", indexes = {
    @Index(name = "idx_flow_status", columnList = "status"),
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FlowInstance extends JpaSoftDeleteObject {
    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Indicating which workflow template this instance belongs to.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "flow_type_id", nullable = false)
    @JsonSerialize(contentAs = JpaBaseIdentifiableObject.class)
    private FlowType flowType; // Assuming FlowType entity exists

    /**
     * managed by FlowInstance.
     * CascadeType.ALL ensures FlowScope is persisted, updated, deleted with FlowInstance.
     */
    @OneToOne(mappedBy = "flowInstance", cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JsonIgnoreProperties(value = {"flowInstance", "attributes"}, allowSetters = true)
    private FlowContext context;

    /**
     * representing all submissions for stages within this flow.
     * CascadeType.ALL ensures submissions are managed with the flow instance.
     */
    @OneToMany(mappedBy = "flowInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonSerialize(contentAs = JpaSoftDeleteObject.class)
    private List<StageInstance> stageInstances = new ArrayList<>();

    /**
     * Status of the flow instance (e.g., IN_PROGRESS, COMPLETED, CANCELLED).
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FlowStatus status;

    public FlowInstance(String id) {
        this.setId(id);
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getName() {
        return "";
    }

    public void setContext(FlowContext flowContext) {
        if (flowContext != null) {
            flowContext.setFlowInstance(this);
        }
        this.context = flowContext;
    }

    public void addSubmission(StageInstance submission) {
        this.stageInstances.add(submission);
        submission.setFlowInstance(this);
    }

    public void setFlowScopeDefinitionSnapshot(@NotNull DimensionalContext scopes) {
    }

    public void setStageDefinitionSnapshot(Set<StageDefinition> stages) {

    }
}
