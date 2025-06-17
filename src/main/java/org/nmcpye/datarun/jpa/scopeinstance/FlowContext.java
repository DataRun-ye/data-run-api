package org.nmcpye.datarun.jpa.scopeinstance;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;

/**
 * Represents the flow-level contextual scope for a {@link FlowInstance}.
 *
 * @author Hamza Assada 14/06/2025 <7amza.it@gmail.com>
 */
@Entity
@DiscriminatorValue("FLOW")
@Getter
@Setter
@NoArgsConstructor
public class FlowContext extends WorkflowContext {
    /**
     * Only a Bean Validation (JSR 380) Annotations
     * Although this won't create a NOT NULL DB constraint, it ensures Hibernate/VN validates before persisting
     */
    @NotNull
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private FlowInstance flowInstance;

    public FlowContext(FlowInstance flowInstance) {
        this.flowInstance = flowInstance;
    }
}
