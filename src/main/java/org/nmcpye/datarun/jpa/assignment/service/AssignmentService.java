package org.nmcpye.datarun.jpa.assignment.service;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.web.rest.queryrequest.QueryRequest;
import org.springframework.data.domain.Page;

public interface AssignmentService
    extends JpaIdentifiableObjectService<Assignment> {

    void updateStatusForSubmission(String submissionId);

    /**
     * Updates the paths of organization units in the system.
     * This method is scheduled to run automatically at 3:00 AM every day.
     * It ensures that the hierarchical paths of organization units are kept up-to-date.
     * The method is transactional to ensure data consistency during the update process.
     */
    void updatePaths();

    void forceUpdatePaths();

    Page<AssignmentWithAccessDto> getAllUserAccessibleDto(QueryRequest queryRequest, String jsonQueryBody);
}
