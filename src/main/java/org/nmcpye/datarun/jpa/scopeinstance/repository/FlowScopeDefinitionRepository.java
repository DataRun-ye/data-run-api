package org.nmcpye.datarun.jpa.scopeinstance.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.scopeinstance.ScopeDefinition;
import org.springframework.stereotype.Repository;

/**
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface FlowScopeDefinitionRepository
    extends BaseJpaIdentifiableRepository<ScopeDefinition, String> {
    // No specific custom methods needed here beyond basic CRUD, as its main content is JSONB.
}
