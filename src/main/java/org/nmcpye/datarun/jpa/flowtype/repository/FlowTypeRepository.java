package org.nmcpye.datarun.jpa.flowtype.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing FlowType entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface FlowTypeRepository
    extends JpaIdentifiableRepository<FlowType> {
    Optional<FlowType> findByName(String name);
    Optional<FlowType> findFirstByUidOrCode(String uid, String code);
}
