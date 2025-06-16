package org.nmcpye.datarun.jpa.scopeinstance.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.scopeinstance.DimensionalValue;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hamza Assada 17/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "stage_context")
@Getter
@Setter
@NoArgsConstructor
public class StageContext {
    @Id
    @Column(name = "stage_instance_id")
    protected String flowInstanceId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "stage_instance_id")
    private StageInstance stageSubmission;

    @OneToMany(mappedBy = "stageContext", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DimensionalValue> dimensionalValues = new ArrayList<>();
}
