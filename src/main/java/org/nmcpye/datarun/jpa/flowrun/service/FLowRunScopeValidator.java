package org.nmcpye.datarun.jpa.flowrun.service;

import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.nmcpye.datarun.jpa.flowrun.domain.ScopeValidationResult;
import org.nmcpye.datarun.jpa.flowtype.FlowScopeType;
import org.nmcpye.datarun.jpa.flowtype.ScopePropertyType;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hamza Assada 08/06/2025 <7amza.it@gmail.com>
 */
@Component
@Transactional(readOnly = true)
public class FLowRunScopeValidator {
    private final List<ErrorMessage> errors = new ArrayList<>();
    private final EntityInstanceRepository entityInstanceRepository;
    private final TeamRepository teamRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ActivityRepository activityRepository;

    public FLowRunScopeValidator(EntityInstanceRepository entityInstanceRepository, TeamRepository teamRepository, OrgUnitRepository orgUnitRepository, ActivityRepository activityRepository) {
        this.entityInstanceRepository = entityInstanceRepository;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.activityRepository = activityRepository;
    }

    public ScopeValidationResult validateScopes(FlowRun flowRun) {
        errors.clear();
        if (flowRun == null || flowRun.getFlowType() == null) {
            errors.add(new ErrorMessage(ErrorCode.E1109));
            return ScopeValidationResult.invalid(errors);
        }

        final var flowType = flowRun.getFlowType();

        Set<FlowScopeType> requiredScopes = flowType.getScopes().stream()
            .filter(FlowScopeType::isRequired)
            .collect(Collectors.toSet());

        // Validate presence
        requiredScopes.forEach(scope -> {
            if (flowType.getFlowScope(scope.getKey()) == null) {
                errors.add(new ErrorMessage(ErrorCode.E1115, scope.getKey()));
            }
        });

        // Validate types and cardinality
        flowRun.getScopes().forEach((key, value) -> {
            flowType.getScopes().stream()
                .filter(s -> s.getKey().equals(key))
                .findFirst()
                .ifPresent(scopeDef -> {
                    validateScopeValue(scopeDef, value, key);
                });
        });
        return errors.isEmpty() ? ScopeValidationResult.valid() : ScopeValidationResult.invalid(errors);
    }

    private void validateScopeValue(FlowScopeType definition, Object value, String key) {
        // Cardinality check
        if (definition.isMultiple() && !(value instanceof List)) {
            errors.add(new ErrorMessage(ErrorCode.E1116, key));
        } else if (!definition.isMultiple() && value instanceof List) {
            errors.add(new ErrorMessage(ErrorCode.E1117, key));
        }

        // Type-specific validation
        if (definition.getType() == ScopePropertyType.ENTITY) {
            validateEntityReference(key, value, definition.getEntityTypeId());
        }

        if (definition.getType() == ScopePropertyType.TEAM) {
            validateTeamReference(key, value, definition.getEntityTypeId());
        }

        if (definition.getType() == ScopePropertyType.ORG_UNIT) {
            validateOrgUnitReference(key, value, definition.getEntityTypeId());
        }

        if (definition.getType() == ScopePropertyType.ORG_UNIT) {
            validateActivityReference(key, value, definition.getEntityTypeId());
        }

        // Add other type validations (DATE format, etc.)
    }

    private void validateEntityReference(String key, Object value, String entityType) {
        // Implementation depends on your service layer
        // Example pseudocode:
        if (value instanceof List) {
            ((List<?>) value).forEach(item -> {
                if (!entityInstanceRepository.existsByEntityTypeUidAndUid(entityType, item.toString())) {
                    new ErrorMessage(ErrorCode.E1116, key);
                    errors.add(new ErrorMessage(ErrorCode.E1118, key, entityType));
                }
            });
        } else {
            if (!entityInstanceRepository.existsByEntityTypeUidAndUid(entityType, value.toString())) {
                errors.add(new ErrorMessage(ErrorCode.E1118, value, entityType));
            }
        }
    }

    private void validateTeamReference(String key, Object value, String entityType) {
        // Implementation depends on your service layer
        // Example pseudocode:
        if (value instanceof List) {
            ((List<?>) value).forEach(item -> {
                if (!teamRepository.existsByUid(item.toString())) {
                    new ErrorMessage(ErrorCode.E1116, key);
                    errors.add(new ErrorMessage(ErrorCode.E1118, key, entityType));
                }
            });
        } else {
            if (!teamRepository.existsByUid(value.toString())) {
                errors.add(new ErrorMessage(ErrorCode.E1118, value, entityType));
            }
        }
    }

    private void validateOrgUnitReference(String key, Object value, String entityType) {
        if (value instanceof List) {
            ((List<?>) value).forEach(item -> {
                if (!orgUnitRepository.existsByUid(item.toString())) {
                    new ErrorMessage(ErrorCode.E1116, key);
                    errors.add(new ErrorMessage(ErrorCode.E1118, key, entityType));
                }
            });
        } else {
            if (!orgUnitRepository.existsByUid(value.toString())) {
                errors.add(new ErrorMessage(ErrorCode.E1118, value, entityType));
            }
        }
    }

    private void validateActivityReference(String key, Object value, String entityType) {
        if (value instanceof List) {
            ((List<?>) value).forEach(item -> {
                if (!activityRepository.existsByUid(item.toString())) {
                    new ErrorMessage(ErrorCode.E1116, key);
                    errors.add(new ErrorMessage(ErrorCode.E1118, key, entityType));
                }
            });
        } else {
            if (!activityRepository.existsByUid(value.toString())) {
                errors.add(new ErrorMessage(ErrorCode.E1118, value, entityType));
            }
        }
    }
}

