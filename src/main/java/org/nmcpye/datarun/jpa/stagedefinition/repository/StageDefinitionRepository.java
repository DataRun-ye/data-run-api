package org.nmcpye.datarun.jpa.stagedefinition.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing StageDefinition entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
@JaversSpringDataAuditable
public interface StageDefinitionRepository
    extends JpaIdentifiableRepository<StageDefinition> {
    Optional<StageDefinition> findByName(String name);

    List<StageDefinition> findByFlowType(FlowType flowType);
}
