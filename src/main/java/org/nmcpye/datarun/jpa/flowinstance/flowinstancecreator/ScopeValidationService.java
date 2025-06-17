package org.nmcpye.datarun.jpa.flowinstance.flowinstancecreator;

import org.nmcpye.datarun.jpa.flowtype.FlowType;

import java.util.Map;

/**
 * @author Hamza Assada 13/06/2025 <7amza.it@gmail.com>
 */
public interface ScopeValidationService {
    void validate(FlowType definition, Map<String, Object> scopeValues);
}
