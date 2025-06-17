package org.nmcpye.datarun.jpa.flowtype;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nmcpye.datarun.jpa.common.JpaAuditable;
import org.nmcpye.datarun.jpa.common.JpaSoftDeleteObject;
import org.nmcpye.datarun.jpa.scopeinstance.DimensionalContext;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeElement;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @author Hamza Assada 27/05/2025 <7amza.it@gmail.com>
 */
@Entity
@Table(name = "flow_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FlowType extends JpaSoftDeleteObject {
    @Column(name = "deleted")
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;
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

    @Column(name = "force_stage_order")
    private Boolean forceStepOrder = false;

    /**
     * Ordered list of scope definitions, serialized as JSON
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_scope_definition", nullable = false)
    @JsonSerialize(contentAs = JpaAuditable.class)
    private DimensionalContext flowDimensionalContext;

    @OneToMany(mappedBy = "flowType", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties(value = {"flowType", "dataTemplate"}, allowSetters = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<StageDefinition> stages = new LinkedHashSet<>();

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    transient private Map<String, ScopeElement> scopeDefinitionCache = new HashMap<>();

    /**
     * a post load method from super
     */
    @Override
    protected void updateEntityState() {
        super.updateEntityState();
        initScopeMap();
    }

    private void initScopeMap() {
        scopeDefinitionCache = getFlowDimensionalContext().getElements().stream()
            .collect(Collectors.toMap(ScopeElement::getId, Function.identity()));
    }

    public ScopeElement getFlowScopeElement(String flowScopeElementId) {
        if (flowScopeElementId == null || getFlowDimensionalContext() == null || CollectionUtils.isEmpty(getFlowDimensionalContext().getElements())) {
            return null;
        }

        return scopeDefinitionCache.computeIfAbsent(flowScopeElementId,
            key -> getFlowScope(flowScopeElementId));
    }

    private ScopeElement getFlowScope(String flowScopeKey) {
        return flowDimensionalContext.getElements().stream()
            .filter(e -> Objects.equals(e.getId(), flowScopeKey))
            .findFirst().orElseThrow();
    }

    public void setFlowDimensionalContext(DimensionalContext dimensionalContext) {
        scopeDefinitionCache.clear();
        this.flowDimensionalContext = dimensionalContext;
    }

    @JsonIgnore
    @Override
    public String getUid() {
        return "";
    }
}
