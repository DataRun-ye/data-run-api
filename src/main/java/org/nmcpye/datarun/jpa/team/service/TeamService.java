package org.nmcpye.datarun.jpa.team.service;

import org.nmcpye.datarun.jpa.common.JpaAuditableObjectService;
import org.nmcpye.datarun.jpa.team.Team;
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

    void runFormPermissionsMigration();
}
