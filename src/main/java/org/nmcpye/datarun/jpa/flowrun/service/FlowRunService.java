package org.nmcpye.datarun.jpa.flowrun.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.nmcpye.datarun.jpa.flowrun.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

public interface FlowRunService
    extends JpaIdentifiableObjectService<FlowRun> {

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
