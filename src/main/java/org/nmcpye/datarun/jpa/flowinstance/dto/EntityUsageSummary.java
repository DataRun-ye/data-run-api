package org.nmcpye.datarun.jpa.flowinstance.dto;

import java.time.Instant;

/**
 * DTO: Entity usage across flows
 * @author Hamza Assada 10/06/2025 <7amza.it@gmail.com>
 */
public interface EntityUsageSummary {
    String getEntityInstanceId();
    String getEntityType();
    int getTotalFlows();

    Instant getFirstUsed();
    Instant getLastUsed();
}
