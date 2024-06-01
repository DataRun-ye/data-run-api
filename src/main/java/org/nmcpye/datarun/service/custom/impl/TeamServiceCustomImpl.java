package org.nmcpye.datarun.service.custom.impl;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.repository.TeamRepositoryCustom;
import org.nmcpye.datarun.service.custom.TeamServiceCustom;
import org.nmcpye.datarun.service.impl.TeamServiceImpl;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class TeamServiceCustomImpl
    extends TeamServiceImpl
    implements TeamServiceCustom {

    private final Logger log = LoggerFactory.getLogger(TeamServiceCustomImpl.class);

    TeamRepositoryCustom teamRepository;

    public TeamServiceCustomImpl(TeamRepositoryCustom teamRepositoryCustom) {
        super(teamRepositoryCustom);
        this.teamRepository = teamRepositoryCustom;
    }

    @Override
    public Team save(Team team) {
        if (team.getUid() == null || team.getUid().isEmpty()) {
            team.setUid(CodeGenerator.generateUid());
        }
        return super.save(team);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Team> findAll(Pageable pageable) {
        log.debug("Request to get all Teams");
        return teamRepository.findAllByUser(pageable);
    }

    public Page<Team> findAllWithEagerRelationships(Pageable pageable) {
        return teamRepository.findAllWithEagerRelationshipsByUser(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findOne(Long id) {
        log.debug("Request to get Team : {}", id);
        return teamRepository.findOneWithEagerRelationships(id);
    }
}
