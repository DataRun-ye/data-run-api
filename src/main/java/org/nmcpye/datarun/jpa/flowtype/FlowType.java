package org.nmcpye.datarun.jpa.flowtype;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.nmcpye.datarun.jpa.common.JpaBaseIdentifiableObject;
import org.nmcpye.datarun.jpa.steptype.StepType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * equivalent to some degree to dhis2 program
 * <pre>
 *     {@code
 *  FlowType {
 *       id: "malariaCampaign",
 *      name: "Malaria Case Management",
 *      planningMode: "PLANNED",
 *      submissionMode: "MULTI_STAGE",
 *      scopes: [
 *          { "key":"team",    "type":"TEAM",     "required":true,  "multiple":false },
 *          { "key":"orgUnit", "type":"ORG_UNIT", "required":true,  "multiple":false },
 *          { "key":"date",    "type":"DATE",     "required":true,  "multiple":false },
 *          { "key":"supplier","type":"ENTITY",   "required":true,  "multiple":false, "entityTypeId":"Supplier" }
 *        ],
 *      steps: [
 *          { id: "registration",   name: "Registration",   formTemplateId: "householdForm", repeatable: false },
 *          { id: "caseVisit",      name: "Case Visit",     formTemplateId: "caseForm",      repeatable: true  },
 *          { id: "treatmentFollow",name: "Follow-Up",      formTemplateId: "treatmentForm", repeatable: false }
 *        ]
 *     }
 *  }
 * </pre>
 *
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "flow_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FlowType extends JpaBaseIdentifiableObject {
    public enum PlanningMode {
        PLANNED, LOG_AS_YOU_GO, PERIODIC
    }

    public enum RunningMode {
        SINGLE, MULTI_STEP
    }

//    @Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
//    @SequenceGenerator(name = "sequenceGenerator")
//    @Column(name = "id")
//    protected Long id;

    @Size(max = 11)
    @Column(name = "uid", length = 11, nullable = false, unique = true)
    private String uid;

    /**
     * The unique code for this object.
     */
    @Column(name = "code", unique = true)
    protected String code;

    /**
     * The name of this object. Required and unique.
     */
    @Column(name = "name", nullable = false, unique = true)
    protected String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "planning_mode", nullable = false)
    private PlanningMode planningMode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "running_mode")
    private RunningMode runningMode;

    @Column(name = "force_step_order")
    private Boolean forceStepOrder = false;

    /**
     * Ordered list of scope definitions, serialized as JSON
     */
    @JsonProperty
    @Type(JsonType.class)
    @Column(name = "scopes", columnDefinition = "jsonb", nullable = false)
    private Set<FlowScopeType> scopes;

//    @OneToMany(fetch = FetchType.LAZY, mappedBy = "flowType")
//    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
//    @JsonIgnoreProperties(value = {"flowType", "dataTemplate"}, allowSetters = true)
//    private Set<StepType> steps = new LinkedHashSet<>();

    @OneToMany(mappedBy = "flowType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties(value = {"flowType", "dataTemplate"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<StepType> steps = new LinkedHashSet<>();

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    transient private Map<String, FlowScopeType> scopeDefinitionCache = new HashMap<>();

    /**
     * a post load method from super
     */
    @Override
    protected void updateEntityState() {
        this.setIsPersisted();
        initScopeMap();
    }

    private void initScopeMap() {
        scopeDefinitionCache = getScopes().stream()
            .collect(Collectors.toMap(FlowScopeType::getKey, Function.identity()));
    }

    public FlowScopeType getFlowScope(String flowScopeKey) {
        return scopeDefinitionCache.get(flowScopeKey);
    }
}
