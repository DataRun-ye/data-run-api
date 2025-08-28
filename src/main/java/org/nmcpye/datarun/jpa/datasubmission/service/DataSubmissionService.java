package org.nmcpye.datarun.jpa.datasubmission.service;

import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * Service Interface for managing {@link DataSubmission}.
 */
public interface DataSubmissionService
    extends JpaIdentifiableObjectService<DataSubmission> {
    @Transactional
    DataSubmission upsert(DataSubmission entity, CurrentUserDetails user, EntitySaveSummaryVM summary);

    @Transactional
    List<DataSubmission> upsertAll(Collection<DataSubmission> entities,
                                   CurrentUserDetails user, EntitySaveSummaryVM summary);
}
