package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class TeamRelationalServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<Team>
    implements TeamServiceCustom {

    TeamRelationalRepositoryCustom teamRepository;

    public TeamRelationalServiceCustomImpl(TeamRelationalRepositoryCustom teamRepositoryCustom) {
        super(teamRepositoryCustom);
        this.teamRepository = teamRepositoryCustom;
    }
}
