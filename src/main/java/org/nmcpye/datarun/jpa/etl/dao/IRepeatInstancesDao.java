package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;

import java.util.List;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public interface IRepeatInstancesDao {
    void upsertRepeatInstancesBatch(List<RepeatInstance> batch);
    void markAllAsDeletedForSubmission(String submissionId);
}
