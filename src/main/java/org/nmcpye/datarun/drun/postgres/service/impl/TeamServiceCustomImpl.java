package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class TeamServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<Team>
    implements TeamServiceCustom {

    TeamRelationalRepositoryCustom repositoryCustom;

    public TeamServiceCustomImpl(TeamRelationalRepositoryCustom teamRepositoryCustom) {
        super(teamRepositoryCustom);
        this.repositoryCustom = teamRepositoryCustom;
    }

    @Override
    public Page<Team> findAllByUser(Pageable pageable) {
        return repositoryCustom.findAllByUser(pageable);
    }

    @Override
    public void updatePaths() {
        repositoryCustom.updatePaths();
    }

    /**
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Override
    @Transactional
    public void forceUpdatePaths() {
        repositoryCustom.forceUpdatePaths();
    }


}
