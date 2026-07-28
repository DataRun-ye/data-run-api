package org.nmcpye.datarun.jpa.assignment.service;

import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface AssignmentService
    extends JpaIdentifiableObjectService<Assignment> {

    /**
     * Updates the paths of organization units in the system.
     * This method is scheduled to run automatically at 3:00 AM every day.
     * It ensures that the hierarchical paths of organization units are kept up-to-date.
     * The method is transactional to ensure data consistency during the update process.
     */
    void updatePaths();

    void forceUpdatePaths();

    Page<AssignmentWithAccessDto> getAllUserAccessibleDto(
        QueryRequest queryRequest,
        String jsonQueryBody,
        int referenceVersion);

    Page<AssignmentWithAccessDto> getAllReleasedWorkDto(
        QueryRequest queryRequest,
        String jsonQueryBody,
        int referenceVersion
    );

    Page<Assignment> findAllReleasedWork(
        QueryRequest queryRequest,
        String jsonQueryBody
    );

    Optional<Assignment> findAccessibleByIdOrUid(String idOrUid);
}
