package org.nmcpye.datarun.jpa.datasubmission.service;

import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Service Interface for managing {@link DataSubmission}.
 */
public interface DataSubmissionService
    extends JpaIdentifiableObjectService<DataSubmission> {

    /**
     * Creates or updates the submissions, in the caller's transaction. Each one whose version the write moves gets a
     * revision recording {@code revision}, and its outbox row.
     */
    @Transactional
    List<DataSubmission> upsertAll(
        Collection<DataSubmission> entities,
        EntitySaveSummaryVM summary,
        RevisionContext revision);

    /**
     * Marks a stored submission deleted, records the revision, and writes its DELETE event, carrying its last data,
     * to the outbox, in the caller's transaction.
     */
    @Transactional
    void softDelete(DataSubmission submission, RevisionContext revision);
}
