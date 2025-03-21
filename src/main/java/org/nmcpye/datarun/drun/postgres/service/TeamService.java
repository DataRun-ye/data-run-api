package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.IdentifiableService;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamService
    extends IdentifiableService<Team, Long> {

    Page<Team> findAllManagedByUser(Pageable pageable);

    Optional<Team> partialUpdate(Team team);
}
