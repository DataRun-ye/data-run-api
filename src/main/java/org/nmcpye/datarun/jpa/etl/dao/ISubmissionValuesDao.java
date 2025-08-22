package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.ElementDataValue;

import java.util.List;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public interface ISubmissionValuesDao {
    void upsertSubmissionValuesBatch(List<ElementDataValue> rows);

    void markAllAsDeletedForSubmission(String submissionId);
}
