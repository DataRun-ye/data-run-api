package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;

import java.util.Optional;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamServiceCustom
    extends IdentifiableRelationalService<Team> {

    Optional<Team> partialUpdate(Team team);
}
