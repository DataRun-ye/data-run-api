package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;

import java.util.List;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public interface ISubmissionValuesDao {
    void upsertSubmissionValue(SubmissionValueRow r); // existing

    void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows); // new

    void markValuesDeletedForRepeatUids(String submissionId, List<String> repeatUids);
}
