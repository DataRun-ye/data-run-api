package org.nmcpye.datarun.jpa.scopeinstance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.scopeinstance.DimensionalValue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 17/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "flow_context")
@Getter
@Setter
@NoArgsConstructor
public class FlowContext {
    @Id
    @Column(name = "flow_instance_id")
    protected String flowInstanceId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "flow_instance_id")
    private FlowInstance flowInstance;

    @OneToMany(mappedBy = "flowScope", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DimensionalValue> dimensionalValues = new ArrayList<>();
}
