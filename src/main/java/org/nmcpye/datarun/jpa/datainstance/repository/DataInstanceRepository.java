package org.nmcpye.datarun.jpa.datainstance.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.datainstance.DataInstance;
import org.springframework.stereotype.Repository;

@Repository
public interface DataInstanceRepository
    extends JpaAuditableRepository<DataInstance> {
}
