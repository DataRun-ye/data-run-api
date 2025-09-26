package org.nmcpye.datarun.jpa.etl.repository;

import org.nmcpye.datarun.jpa.etl.dto.RepeatInstance;

import java.util.List;


/**
 * JDBC DAO for repeat_instance table.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Batch upsert repeat instances (with undelete semantics)</li>
 *   <li>Mark all repeat instances for a submission as deleted</li>
 * </ul>
 *
 * <p>Design notes:
 * <ul>
 *   <li>UPSERT sets deleted_at = NULL on conflict to "undelete" existing instances.</li>
 *   <li>Timestamps are ensured to exist before binding.</li>
 * </ul>
 *
 * @author Hamza Assada
 * @since 13/08/2025
 */
public interface IRepeatInstancesDao {
    void upsertRepeatInstancesBatch(List<RepeatInstance> batch);
    void markAllAsDeletedForSubmission(String submissionId);
}
