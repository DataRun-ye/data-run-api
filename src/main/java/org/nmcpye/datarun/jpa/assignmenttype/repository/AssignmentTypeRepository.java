package org.nmcpye.datarun.jpa.assignmenttype.repository;

import org.nmcpye.datarun.jpa.assignmenttype.AssignmentType;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentTypeRepository
        extends JpaIdentifiableRepository<AssignmentType> {
}
