package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class TeamServiceCustomImpl
    extends IdentifiableServiceImpl<Team>
    implements TeamServiceCustom {

    private final Logger log = LoggerFactory.getLogger(TeamServiceCustomImpl.class);

    TeamRepositoryCustom teamRepository;

    public TeamServiceCustomImpl(TeamRepositoryCustom teamRepositoryCustom) {
        super(teamRepositoryCustom);
        this.teamRepository = teamRepositoryCustom;
    }

    @Override
    public Optional<Team> partialUpdate(Team team) {
        log.debug("Request to partially update Team : {}", team);

        return teamRepository
            .findById(team.getId())
            .map(existingTeam -> {
                if (team.getUid() != null) {
                    existingTeam.setUid(team.getUid());
                }
                if (team.getCode() != null) {
                    existingTeam.setCode(team.getCode());
                }
                if (team.getName() != null) {
                    existingTeam.setName(team.getName());
                }
                if (team.getDescription() != null) {
                    existingTeam.setDescription(team.getDescription());
                }
                if (team.getMobile() != null) {
                    existingTeam.setMobile(team.getMobile());
                }
                if (team.getWorkers() != null) {
                    existingTeam.setWorkers(team.getWorkers());
                }
                if (team.getMobility() != null) {
                    existingTeam.setMobility(team.getMobility());
                }
                if (team.getTeamType() != null) {
                    existingTeam.setTeamType(team.getTeamType());
                }
                if (team.getDisabled() != null) {
                    existingTeam.setDisabled(team.getDisabled());
                }
                if (team.getDeleteClientData() != null) {
                    existingTeam.setDeleteClientData(team.getDeleteClientData());
                }
                if (team.getCreatedBy() != null) {
                    existingTeam.setCreatedBy(team.getCreatedBy());
                }
                if (team.getCreatedDate() != null) {
                    existingTeam.setCreatedDate(team.getCreatedDate());
                }
                if (team.getLastModifiedBy() != null) {
                    existingTeam.setLastModifiedBy(team.getLastModifiedBy());
                }
                if (team.getLastModifiedDate() != null) {
                    existingTeam.setLastModifiedDate(team.getLastModifiedDate());
                }

                return existingTeam;
            })
            .map(teamRepository::save);
    }

}
