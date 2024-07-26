package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.ProgressStatus;
import org.nmcpye.datarun.service.ProgressStatusService;

/**
 * Service Interface for managing {@link ProgressStatus}.
 */
public interface ProgressStatusServiceCustom
    extends IdentifiableService<ProgressStatus>, ProgressStatusService {
}
