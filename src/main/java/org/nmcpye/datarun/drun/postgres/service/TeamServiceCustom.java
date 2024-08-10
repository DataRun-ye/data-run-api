package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamServiceCustom
    extends IdentifiableRelationalService<Team> {
    Page<Team> findAllByUser(Pageable pageable);
}
