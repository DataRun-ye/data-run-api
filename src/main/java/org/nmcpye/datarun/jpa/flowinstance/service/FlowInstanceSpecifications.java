package org.nmcpye.datarun.jpa.flowinstance.service;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.scopeinstance.FlowContext;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * Utility class for creating JPA Specifications for FlowInstance entities which
 * can be combined to build complex dynamic queries.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
public class FlowInstanceSpecifications {

    /**
     * Creates a Specification to filter FlowInstances by their FlowScope's organization unit ID.
     *
     * @param orgUnitId The organization unit ID to filter by.
     * @return A Specification for FlowInstance.
     */
    public static Specification<FlowInstance> withOrgUnitId(String orgUnitId) {
        return (root, query, criteriaBuilder) -> {
            // Join to FlowScope
            Join<FlowInstance, FlowContext> flowScopeJoin = root.join("flowScope");
            return criteriaBuilder.equal(flowScopeJoin.get("orgUnitId"), orgUnitId);
        };
    }

    /**
     * Creates a Specification to filter FlowInstances by their FlowScope's scope date.
     *
     * @param scopeDate The scope date to filter by.
     * @return A Specification for FlowInstance.
     */
    public static Specification<FlowInstance> withScopeDate(LocalDate scopeDate) {
        return (root, query, criteriaBuilder) -> {
            Join<FlowInstance, FlowContext> flowScopeJoin = root.join("flowScope");
            return criteriaBuilder.equal(flowScopeJoin.get("scopeDate"), scopeDate);
        };
    }

    /**
     * Creates a Specification to filter FlowInstances that have a specific dynamic attribute
     * (key-value pair) within their FlowScope's ScopeAttributes.
     * This involves joining from FlowInstance -> FlowScope -> ScopeAttribute.
     *
     * @param attributeKey   The key of the dynamic attribute.
     * @param attributeValue The value of the dynamic attribute.
     * @return A Specification for FlowInstance.
     */
    public static Specification<FlowInstance> withFlowScopeAttribute(String attributeKey, String attributeValue) {
        return (root, query, criteriaBuilder) -> {
            // Join from FlowInstance to FlowScope
            Join<FlowInstance, FlowContext> flowScopeJoin = root.join("flowScope");
            // Join from FlowScope to ScopeAttribute
            Join<FlowContext, ScopeAttribute> scopeAttributeJoin = flowScopeJoin.join("attributes");

            // Build predicates for attribute key and value
            Predicate keyPredicate = criteriaBuilder.equal(scopeAttributeJoin.get("key"), attributeKey);
            Predicate valuePredicate = criteriaBuilder.equal(scopeAttributeJoin.get("value"), attributeValue);

            // Combine predicates with AND
            return criteriaBuilder.and(keyPredicate, valuePredicate);
        };
    }

    /**
     * Example of combining multiple specifications.
     * Finds FlowInstances by orgUnitId AND a specific FlowScope dynamic attribute.
     *
     * @param orgUnitId      The organization unit ID.
     * @param attributeKey   The key of the dynamic attribute.
     * @param attributeValue The value of the dynamic attribute.
     * @return A combined Specification.
     */
    public static Specification<FlowInstance> byOrgUnitIdAndFlowScopeAttribute(String orgUnitId, String attributeKey, String attributeValue) {
        return Specification.where(withOrgUnitId(orgUnitId))
            .and(withFlowScopeAttribute(attributeKey, attributeValue));
    }

    // How to use in a service:
    /*
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    public List<FlowInstance> findComplexFlows(String orgUnitId, String invoiceNumber) {
        Specification<FlowInstance> spec = Specification
            .where(FlowInstanceSpecifications.withOrgUnitId(orgUnitId))
            .and(FlowInstanceSpecifications.withFlowScopeAttribute("invoiceNumber", invoiceNumber));
        // You can add more .and() or .or() conditions here

        return flowInstanceRepository.findAll(spec);
    }
    */
}
