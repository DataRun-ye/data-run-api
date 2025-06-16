package org.nmcpye.datarun.jpa.scopeinstance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.team.Team;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Hamza Assada 16/06/2025 <7amza.it@gmail.com>
 */

@Entity
@Table(name = "scope_element_value")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class DimensionalValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private WorkflowContext scope;

    @ManyToOne
    @JoinColumn(name = "dimensional_element_id", nullable = true)
    private ScopeElementDefinition dimensionalElement;

    // Entity reference or primitive value
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activityRef;

    // Entity reference or primitive value
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private OrgUnit orgUnitRef;

    // Entity reference or primitive value
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_instance_id")
    private EntityInstance entityRef;

    // Entity reference or primitive value
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team teamRef;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "number_value")
    private BigDecimal numberValue;

    // Helper methods
    public Object getValue() {
        return switch (dimensionalElement.getType()) {
            case ACTIVITY -> orgUnitRef;
            case TEAM -> teamRef;
            case ORG_UNIT -> activityRef;
            case ENTITY -> entityRef;
            case STRING -> stringValue;
            case DATE -> dateValue;
            case NUMBER -> numberValue;
        };
    }
}
