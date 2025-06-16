package org.nmcpye.datarun.jpa.scopeinstance.repository;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;

/**
 * @author Hamza Assada 13/06/2025 <7amza.it@gmail.com>
 */
public interface ScopeDimensionsProjection {
    String getOrgUnitId();
    String getTeamId();
    LocalDate getScopeDate();
    JsonNode getScopeData();
}
