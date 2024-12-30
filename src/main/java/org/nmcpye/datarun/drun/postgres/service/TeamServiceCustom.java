package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link Team}.
 */
public interface TeamServiceCustom
    extends IdentifiableRelationalService<Team> {
    @Override
    default Specification<Team> canRead() {
        return TeamSpecifications.canRead();
    }

    Page<Team> findAllManagedByUser(Pageable pageable);

    List<Team> findAllManagedByUser();

    Optional<Team> partialUpdate(Team team);
}
