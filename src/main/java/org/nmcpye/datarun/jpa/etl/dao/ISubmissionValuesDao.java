package org.nmcpye.datarun.jpa.etl.dao;

import org.nmcpye.datarun.jpa.etl.dto.SubmissionValueRow;

import java.util.List;

/**
 * @author Hamza Assada 13/08/2025 (7amza.it@gmail.com)
 */
public interface ISubmissionValuesDao {
    void upsertSubmissionValue(SubmissionValueRow r); // existing

    void upsertSubmissionValuesBatch(List<SubmissionValueRow> rows); // new

    //

    /**
     * Find currently stored option_ids for a given submission/repeat/element.
     * Returns list of option_id strings (non-null only). If no rows found returns empty list.
     * Return selection identities for the given submission/repeat/element.
     * Identity = COALESCE(option_id, value_text)
     * Only returns active rows (deleted_at IS NULL).
     * <p>
     * repeatInstanceId may be null to match top-level rows (repeat_instance_id IS NULL).
     */
    List<String> findSelectionIdentitiesForElementRepeat(String submissionId, String repeatInstanceId, String elementId);

    /**
     * Mark selection rows deleted by their identity (COALESCE(option_id, value_text)).
     * identities must be non-empty.
     */
    public void markSelectionValuesDeletedByIdentity(String submissionId, String repeatInstanceId,
                                                     String elementId, List<String> identities);

    // existing method kept
    void markValuesDeletedForRepeatUids(String submissionId, List<String> repeatUids);

    void markValuesDeletedForSubmission(String submissionId);
}
