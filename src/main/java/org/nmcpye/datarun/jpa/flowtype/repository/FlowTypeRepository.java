package org.nmcpye.datarun.jpa.flowtype.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlowTypeRepository
        extends JpaIdentifiableRepository<FlowType> {
    Optional<FlowType> findFirstByUidOrCode(String uid, String code);
}
