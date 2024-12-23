package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.postgres.common.OrgUnitSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class OrgUnitServiceCustomImpl
    extends OrgUnitSpecifications
    implements OrgUnitServiceCustom {

    final OrgUnitRelationalRepositoryCustom repositoryCustom;
    final AssignmentRelationalRepositoryCustom assignmentRepository;
    final UserRepository userRepository;

    public OrgUnitServiceCustomImpl(OrgUnitRelationalRepositoryCustom orgUnitRepositoryCustom, AssignmentRelationalRepositoryCustom assignmentRepository, UserRepository userRepository) {
        super(orgUnitRepositoryCustom);
        this.repositoryCustom = orgUnitRepositoryCustom;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }


    @Override
    public OrgUnit saveWithRelations(OrgUnit object) {
        OrgUnit parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            object.setParent(parent);
        }

        return repositoryCustom.save(object);
    }

    private OrgUnit findParent(OrgUnit parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repositoryCustom::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repositoryCustom::findByUid))
            .or(() -> Optional.ofNullable(parent.getCode())
                .flatMap(repositoryCustom::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    @Override
    public Page<OrgUnit> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
        final List<OrgUnit> userOrgUnits = repositoryCustom
            .findAllWithRelation();

        final Set<String> uids = userOrgUnits
            .stream()
            .flatMap(orgUnit -> orgUnit.getAncestorUids(null).stream())
            .collect(Collectors.toSet());
        userOrgUnits.addAll(repositoryCustom.findAllByUidIn(uids));

        if (pageable.isUnpaged()) {
            return new PageImpl<>(userOrgUnits);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userOrgUnits.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<OrgUnit> sublist = userOrgUnits.subList(start, end);
        return new PageImpl<>(sublist, pageable, userOrgUnits.size());
    }

    @Override
    public Set<OrgUnit> getUserTeamsOrganisationUnits() {
        var userLogin = SecurityUtils.getCurrentUserLogin();
        if (userLogin.isPresent()) {
            var user = userRepository.findOneByLogin(userLogin.get());
            return user.stream().flatMap(u -> u.getTeams().stream())
                .filter(team -> !team.getDisabled())
                .filter(team -> !team.getActivity().getDisabled())
                .map(Team::getAssignments)
                .flatMap(Collection::stream)
                .map(Assignment::getOrgUnit)
                .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }

    @Override
    public Set<OrgUnit> getUserManagedTeamsOrganisationUnits() {
        return Set.of();
    }

    @Override
    public Set<OrgUnit> getAllUserAccessibleOrganisationUnits() {
        return Set.of();
    }

    /**
     * Updates the paths of organization units in the system.
     * This method is scheduled to run automatically at 3:00 AM every day.
     * It ensures that the hierarchical paths of organization units are kept up-to-date.
     * The method is transactional to ensure data consistency during the update process.
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void updatePaths() {
        repositoryCustom.updatePaths();
    }

    @Override
    @Transactional
    public void forceUpdatePaths() {
        repositoryCustom.forceUpdatePaths();
    }

}
