package org.nmcpye.datarun.jpa.scopeinstance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 16/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "work_flow_context")
@Check(constraints =
    "(flow_instance_id IS NOT NULL AND stage_submission_id IS NULL) OR " +
        "(flow_instance_id IS NULL AND stage_submission_id IS NOT NULL)")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "context_type")
@Getter
@Setter
public abstract class WorkflowContext {
    /**
     * ULID PK derived from the associated entity (flow or stage Context)
     */
    @Id
    @Column(name = "id", length = 26)
    protected String id;

    @Column(name = "context_date", nullable = false)
    private LocalDate contextDate;

    @OneToMany(mappedBy = "scope", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<DimensionalValue> contextDimensions = new ArrayList<>();

    @OneToMany(mappedBy = "scope", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties(value = {"scope"}, allowSetters = true)
    protected List<ScopeAttribute> attributes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (contextDate == null) {
            contextDate = LocalDate.now();
        }
    }

    public void addContextDimension(DimensionalValue element) {
        element.setScope(this);
        this.contextDimensions.add(element);
    }
}


