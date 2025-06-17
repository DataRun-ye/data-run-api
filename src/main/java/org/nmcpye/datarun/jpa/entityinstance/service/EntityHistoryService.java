package org.nmcpye.datarun.jpa.entityinstance.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.entityinstance.EntityHistory;

import java.util.Map;

public interface EntityHistoryService
    extends JpaIdentifiableObjectService<EntityHistory> {
    void recordHistory(String entityId, String flowInstanceId, String stageSubmissionId, String stage, Map<String, Object> data);
}
