package org.nmcpye.datarun.jpa.scopeinstance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObject;

/**
 * @author Hamza Assada 08/06/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "scope_element")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class ScopeElementDefinition extends JpaIdentifiableObject {
    private String code;
    private CoreElementType type;
    private boolean required;

    @Column(name = "entity_type_id")
    private String entityTypeId;

    // UI properties
    private String name;

    @Column(name = "display_order")
    private int displayOrder;
}
