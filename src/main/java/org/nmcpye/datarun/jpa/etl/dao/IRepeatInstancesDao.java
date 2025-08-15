package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;

import java.util.List;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public interface IRepeatInstancesDao {
    void upsertRepeatInstance(RepeatInstance repeatInstance); // existing
    void upsertRepeatInstancesBatch(List<RepeatInstance> batch); // new
    List<String> findActiveRepeatUids(String submissionId, String repeatPath);
    void markRepeatInstancesDeleted(String submissionId, String repeatPath, List<String> repeatUids);
    void markRepeatInstancesDeletedBySubmission(String submissionId);
}
