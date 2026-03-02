package org.nmcpye.datarun.jpa.team.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.web.query.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamService
    extends JpaIdentifiableObjectService<Team> {

    Page<Team> findAllManagedByUser(Pageable pageable, QueryRequest queryRequest);

    Optional<Team> partialUpdate(Team team);
}
