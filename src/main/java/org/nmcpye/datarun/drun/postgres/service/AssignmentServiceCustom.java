package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.common.AssignmentSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface AssignmentServiceCustom
    extends IdentifiableRelationalService<Assignment> {
    @Override
    default Specification<Assignment> canRead() {
        return AssignmentSpecifications.canRead();
    }

    /**
     * Updates the paths of organization units in the system.
     * This method is scheduled to run automatically at 3:00 AM every day.
     * It ensures that the hierarchical paths of organization units are kept up-to-date.
     * The method is transactional to ensure data consistency during the update process.
     */
    void updatePaths();

    void forceUpdatePaths();

    Page<Assignment> getAllUserAccessible(Pageable pageable);
}
