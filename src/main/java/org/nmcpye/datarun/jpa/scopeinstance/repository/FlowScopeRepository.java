package org.nmcpye.datarun.jpa.scopeinstance.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.scopeinstance.FlowContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for managing FlowScope entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface FlowScopeRepository
    extends BaseJpaIdentifiableRepository<FlowContext, String> {
    // Find FlowScope by its ID (which is also the flowInstanceId)
    Optional<FlowContext> findByFlowInstanceId(String flowInstanceId);

    // Find FlowScope by org unit and date
    Optional<FlowContext> findByOrgUnitIdAndScopeDate(String orgUnitId, LocalDate scopeDate);
}
