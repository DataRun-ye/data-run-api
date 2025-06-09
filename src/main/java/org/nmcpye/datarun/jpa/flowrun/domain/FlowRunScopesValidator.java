package org.nmcpye.datarun.jpa.flowrun.domain;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.nmcpye.datarun.jpa.flowtype.FlowScopeType;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

/**
 * @author Hamza Assada 09/06/2025 <7amza.it@gmail.com>
 */
public class FlowRunScopesValidator
    implements ConstraintValidator<ValidFlowRunScopes, FlowRunRequest> {

    @Autowired
    private FlowTypeRepository flowTypeRepository;

    @Override
    public void initialize(ValidFlowRunScopes constraintAnnotation) {

    }

    @Override
    public boolean isValid(FlowRunRequest request, ConstraintValidatorContext context) {

        FlowType flowType = flowTypeRepository.findByUid(request.getFlowTypeUid()).orElse(null);

        if (flowType == null) {
            // This will be handled by another validator (existence of flowType)
            return true;
        }

        Set<FlowScopeType> flowScopes = flowType.getScopes();

        Map<String, String> requestScopes = request.getScopes();

        // Check required scopes are present
        for (FlowScopeType scopeDef : flowScopes) {

            if (scopeDef.isRequired() && !requestScopes.containsKey(scopeDef.getKey())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Scope '" + scopeDef.getKey() + "' is required")
                    .addPropertyNode("scopes")
                    .addConstraintViolation();

                return false;

            }

        }

        // Check that there are no extra scopes
        for (String key : requestScopes.keySet()) {
            boolean found = flowScopes.stream().anyMatch(s -> s.getKey().equals(key));
            if (!found) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Scope '" + key + "' is not defined in the flow type")
                    .addPropertyNode("scopes")
                    .addConstraintViolation();
                return false;
            }
        }
        // TODO: Type-specific validation? (e.g., check that the string for a team is a valid team UID, etc.)
        // This might require additional repository calls and could be heavy. We might do it in the service layer.
        return true;
    }
}
