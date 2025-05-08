package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

public interface AssignmentService
    extends JpaAuditableObjectService<Assignment> {

    /**
     * Updates the paths of organization units in the system.
     * This method is scheduled to run automatically at 3:00 AM every day.
     * It ensures that the hierarchical paths of organization units are kept up-to-date.
     * The method is transactional to ensure data consistency during the update process.
     */
    void updatePaths();

    void forceUpdatePaths();

    Page<Assignment> getAllUserAccessible(QueryRequest queryRequest);
}
