package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class AssignmentServiceImplCustom
    extends IdentifiableRelationalServiceImpl<Assignment>
    implements AssignmentServiceCustom {

    final AssignmentRelationalRepositoryCustom repositoryCustom;
    final TeamRelationalRepositoryCustom teamRepository;
    final OrgUnitRelationalRepositoryCustom orgUnitRepository;

    public AssignmentServiceImplCustom(AssignmentRelationalRepositoryCustom repositoryCustom,
                                       TeamRelationalRepositoryCustom teamRepository,
                                       OrgUnitRelationalRepositoryCustom orgUnitRepository) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Override
    public Assignment saveWithRelations(Assignment object) {

        Team team = null;
        OrgUnit orgUnit = null;

        if (object.getTeam() != null) {
            team = findTeam(object.getTeam());
        }
        if (object.getOrgUnit() != null) {
            orgUnit = findOrgUnit(object.getOrgUnit());
        }
        object.setTeam(team);
        object.setOrgUnit(orgUnit);

        return repositoryCustom.save(object);
    }

    private Team findTeam(Team team) {
        return Optional.ofNullable(team.getId())
            .flatMap(teamRepository::findById)
            .or(() -> Optional.ofNullable(team.getUid())
                .flatMap(teamRepository::findByUid))
            .or(() -> Optional.ofNullable(team.getCode())
                .flatMap((code) -> teamRepository.findTeamByCodeAndActivityCode(code, team.getActivity().getCode())))
            .orElseThrow(() -> new PropertyNotFoundException("Team not found: " + team));
    }

    private OrgUnit findOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getId())
            .flatMap(orgUnitRepository::findById)
            .or(() -> Optional.ofNullable(orgUnit.getUid())
                .flatMap(orgUnitRepository::findByUid))
            .or(() -> Optional.ofNullable(orgUnit.getCode())
                .flatMap(orgUnitRepository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("OrgUniy not found: " + orgUnit));
    }

    @Override
    public Page<Assignment> findAllByUser(Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
        return repositoryCustom.findAllByUser(pageable);
    }

//    @Override
//    public Assignment update(Assignment object) {
//        return super.update(object);
//    }
}
