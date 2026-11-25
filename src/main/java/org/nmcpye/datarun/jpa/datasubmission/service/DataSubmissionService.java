package org.nmcpye.datarun.jpa.datasubmission.service;

import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Interface for managing {@link DataSubmission}.
 */
public interface DataSubmissionService
    extends JpaIdentifiableObjectService<DataSubmission> {

    /**
     * Creates the incoming submission, or updates the stored one with it, in the caller's transaction. A write that
     * moves the version gets a revision recording {@code revision}, and its outbox row.
     *
     * @param stored the stored submission with the incoming uid, as the caller took it under the submission lock
     *               ({@link DataSubmissionRepository#findByUidForWrite}), or null when none was stored: the incoming
     *               one is then created
     */
    @Transactional
    void upsert(
        DataSubmission incoming,
        DataSubmission stored,
        EntitySaveSummaryVM summary,
        RevisionContext revision);

    /**
     * Marks a stored submission deleted, records the revision, and writes its DELETE event, carrying its last data,
     * to the outbox, in the caller's transaction.
     */
    @Transactional
    void softDelete(DataSubmission submission, RevisionContext revision);
}
