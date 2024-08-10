package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.OrgUnit;
import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service Interface for managing {@link Team}.
 */
public interface OrgUnitServiceCustom
    extends IdentifiableRelationalService<OrgUnit> {
    Page<OrgUnit> findAllByUser(Pageable pageable);

    Optional<OrgUnit> findAssignedByUid(String uid);
}
