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
@Table(name = "dimensional_value")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class DimensionalValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private WorkflowContext context;

    @ManyToOne
    @JoinColumn(name = "dimensional_element_id")
    private DimensionalElement dimensionalElement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private OrgUnit orgUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_instance_id")
    private EntityInstance entityInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team teamRef;

    @Column(name = "string_value")
    private String stringValue;

    @Column(name = "date_value")
    private LocalDate dateValue;

    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    // Helper methods
    public Object getValue() {
        return switch (dimensionalElement.getType()) {
            case ACTIVITY -> orgUnit;
            case TEAM -> teamRef;
            case ORG_UNIT -> activity;
            case ENTITY -> entityInstance;
            case STRING -> stringValue;
            case DATE -> dateValue;
            case NUMBER -> numericValue;
        };
    }
}
