package org.nmcpye.datarun.assignmenttype.repository;

import org.nmcpye.datarun.assignmenttype.AssignmentType;
import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentTypeRepository
        extends JpaAuditableRepository<AssignmentType> {
}
