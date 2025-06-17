package org.nmcpye.datarun.jpa.scopeinstance;

/**
 * @author Hamza Assada 16/06/2025 <7amza.it@gmail.com>
 */
public class ScopeValidator {
    public void validate(WorkflowContext scope, DimensionalContext definition) {
        // System-enforced orgUnit at flow level
        if (scope instanceof FlowContext) {
            validateOrgUnitPresent(scope);
        }

        // Configurable requirements
        for (DimensionalElement elementDef : definition.getElements()) {
            DimensionalValue element = findElement(scope, elementDef.getId());

            if (elementDef.isRequired() && element == null) {
                throw new ValidationException(elementDef.getLabel() + " is required");
            }

            if (element != null) {
                validateTypeMatch(element, elementDef);
            }
        }
    }
}
