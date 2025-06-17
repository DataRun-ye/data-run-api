package org.nmcpye.datarun.jpa.scopeinstance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 16/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "work_flow_context")
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

    @OneToMany(mappedBy = "context", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<DimensionalValue> dimensionalValues = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (contextDate == null) {
            contextDate = LocalDate.now();
        }
    }

    public void addContextDimension(DimensionalValue element) {
        element.setContext(this);
        this.dimensionalValues.add(element);
    }
}


