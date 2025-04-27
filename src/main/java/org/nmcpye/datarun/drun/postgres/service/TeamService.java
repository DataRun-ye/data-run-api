package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamService
    extends JpaAuditableObjectService<Team> {

    Page<Team> findAllManagedByUser(Pageable pageable, QueryRequest queryRequest);

    Optional<Team> partialUpdate(Team team);
}
