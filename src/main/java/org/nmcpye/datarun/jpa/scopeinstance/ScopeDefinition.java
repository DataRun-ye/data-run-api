package org.nmcpye.datarun.jpa.scopeinstance;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.common.uidgenerate.CodeGenerator;
import org.nmcpye.datarun.jpa.common.JpaAuditable;

import java.util.List;

/**
 * CREATE INDEX idx_scope_instance_scope_data_gin ON scope_instance USING GIN (scope_data);
 * CREATE INDEX idx_stage_submission_submission_data_gin ON stage_submission USING GIN (submission_data);
 *
 * @author Hamza Assada 10/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "scope_definition")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class ScopeDefinition extends JpaAuditable<String> {
    @Id
    @Column(name = "id", length = 26, updatable = false, nullable = false)
    private String id;

    @Type(JsonType.class)
    @Column(name = "scope_elements", columnDefinition = "jsonb")
    List<ScopeElementDefinition> scopeElements;

    /**
     * Lifecycle hook to generate a ULID ID before persisting.
     */
    @PrePersist
    public void prePersist() {
        if (getId() == null) {
            this.id = CodeGenerator.ULIDGenerator.nextString();
        }
    }
}
