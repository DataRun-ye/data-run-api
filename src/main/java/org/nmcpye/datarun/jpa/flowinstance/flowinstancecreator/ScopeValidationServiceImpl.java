package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.jpa.flowinstance.exception.FlowCreationException;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Hamza Assada 13/06/2025 <7amza.it@gmail.com>
 */
@Service
public class ScopeValidationServiceImpl implements ScopeValidationService {
    private final EntityInstanceRepository eiRepo;

    public ScopeValidationServiceImpl(EntityInstanceRepository eiRepo) {
        this.eiRepo = eiRepo;
    }


    @Override
    public void validate(FlowType flowType, Map<String, Object> scopes) {
        // Validate scopes keys
        for (var def : flowType.getScopes().getScopeElements()) {
            if (def.isRequired() && !scopes.containsKey(def.getKey()))
                throw new FlowCreationException("Missing scope " + def.getKey());
        }

        final var current = SecurityUtils.getCurrentUserDetailsOrNull();
        // Validate team
        String teamId = scopes.get("team").toString();
        if (!current.getUserTeamsUIDs().contains(teamId))
            throw new FlowCreationException("Team not in user context");
        // Validate entityInstance if needed
        if (scopes.containsKey("entityInstance")) {
            String eiId = scopes.get("entityInstance").toString();
            if (!eiRepo.existsById(eiId))
                throw new FlowCreationException("Invalid entityInstance");
        }
    }
}
