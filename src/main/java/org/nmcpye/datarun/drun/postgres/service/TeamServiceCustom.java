package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.service.TeamService;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamServiceCustom
    extends IdentifiableService<Team>, TeamService {
}
