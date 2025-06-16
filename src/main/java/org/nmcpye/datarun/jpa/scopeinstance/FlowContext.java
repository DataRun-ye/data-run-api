package org.nmcpye.datarun.jpa.scopeinstance;

import jakarta.persistence.*;
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
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private FlowInstance flowInstance;

    public FlowContext(FlowInstance flowInstance) {
        this.flowInstance = flowInstance;
    }
}
