package org.nmcpye.datarun.jpa.scopeinstance;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nmcpye.datarun.jpa.stagesubmission.StageInstance;

/**
 * Represents the stage-level contextual scope for a {@link StageInstance}.
 *
 * @author Hamza Assada 14/06/2025 <7amza.it@gmail.com>
 */
@Entity
@DiscriminatorValue("STAGE")
@Getter
@Setter
@NoArgsConstructor
public class StageContext extends WorkflowContext {
    /**
     * Only a Bean Validation (JSR 380) Annotations
     * Although this won't create a NOT NULL DB constraint, it ensures Hibernate/VN validates before persisting
     */
    @NotNull
    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private StageInstance stageInstance;

    public StageContext(StageInstance stageInstance) {
        this.stageInstance = stageInstance;
    }
}
