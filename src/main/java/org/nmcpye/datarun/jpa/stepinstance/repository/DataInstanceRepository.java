package org.nmcpye.datarun.jpa.stepinstance.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.stepinstance.StepInstance;
import org.springframework.stereotype.Repository;

@Repository
public interface DataInstanceRepository
    extends JpaIdentifiableRepository<StepInstance> {
}
