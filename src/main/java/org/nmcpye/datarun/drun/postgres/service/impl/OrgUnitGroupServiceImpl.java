package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnitGroup;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitGroupRelationalRepository;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitGroupService;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class OrgUnitGroupServiceImpl
    extends IdentifiableRelationalServiceImpl<OrgUnitGroup>
    implements OrgUnitGroupService {

    private final OrgUnitGroupRelationalRepository repository;
    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;
    private final UserRepository userRepository;

    public OrgUnitGroupServiceImpl(OrgUnitGroupRelationalRepository repository,
                                   AssignmentRelationalRepositoryCustom assignmentRepository,
                                   OrgUnitRelationalRepositoryCustom orgUnitRepository,
                                   UserRepository userRepository,
                                   CacheManager cacheManager) {
        super(repository, cacheManager);
        this.repository = repository;
        this.orgUnitRepository = orgUnitRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }


    @Override
    public OrgUnitGroup saveWithRelations(OrgUnitGroup object) {
        if (!object.getMembers().isEmpty()) {
            Set<OrgUnit> orgUnits = new HashSet<>();
            for (OrgUnit orgUnit : object.getMembers()) {
                orgUnits.add(findOrgUnit(orgUnit));
            }

            object.setMembers(orgUnits);
            return repository.save(object);
        }

        return repository.save(object);
    }

    private OrgUnit findOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getUid())
            .flatMap(orgUnitRepository::findByUid)
            .or(() -> Optional.ofNullable(orgUnit.getId())
                .flatMap(orgUnitRepository::findById))
            .or(() -> Optional.ofNullable(orgUnit.getCode())
                .flatMap(orgUnitRepository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("OrgUnit not found: " + orgUnit));
    }

    @Override
    public Page<OrgUnitGroup> findAllByUser(Pageable pageable, QueryRequest queryRequest) {

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        String currentUserLogin =  SecurityUtils.getCurrentUserLoginOrThrow(
            new ErrorMessage(ErrorCode.E3004, getClass().getName()));

        // Get user's direct teams
        List<Team> userDirectTeams = userRepository.findOneByLogin(currentUserLogin)
            .map(user -> user.getTeams().stream()
                .filter(team -> !team.getDisabled())
                .filter(team -> !team.getActivity().getDisabled())
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());

        // Get user's indirect teams (managed teams of direct teams)
        List<Team> userIndirectTeams = userDirectTeams.stream()
            .flatMap(team -> team.getManagedTeams().stream())
            .filter(team -> !team.getDisabled())
            .filter(team -> !team.getActivity().getDisabled())
            .toList();

        // Combine direct and indirect teams
        List<Team> allUserTeams = new ArrayList<>(userDirectTeams);
        allUserTeams.addAll(userIndirectTeams);

        // Get assignments for all teams
        List<Assignment> userAssignments = assignmentRepository.findAllByTeamIn(allUserTeams);

        // Extract OrgUnits from assignments
        Set<OrgUnit> userOrgUnits = userAssignments.stream()
            .map(Assignment::getOrgUnit)
            .collect(Collectors.toSet());

        // Add ancestors of the user's OrgUnits
        Set<OrgUnit> ancestors = userOrgUnits.stream()
            .flatMap(orgUnit -> orgUnit.getAncestors().stream())
            .collect(Collectors.toSet());

        userOrgUnits.addAll(ancestors);

        List<OrgUnitGroup> orgUnitGroups = userOrgUnits.stream().flatMap((o) -> o.getGroups().stream()).toList();

        if (pageable.isUnpaged()) {
            return new PageImpl<>(orgUnitGroups);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), orgUnitGroups.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<OrgUnitGroup> sublist = orgUnitGroups.subList(start, end);
        return new PageImpl<>(sublist, pageable, orgUnitGroups.size());
    }

    @Override
    public Page<OrgUnitGroup> findAllByUser(Specification<OrgUnitGroup> spec, Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(spec, pageable);
        }

        String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
            new ErrorMessage(ErrorCode.E3004, getClass().getName()));

        // Get user's direct teams
        List<Team> userDirectTeams = userRepository.findOneByLogin(currentUserLogin)
            .map(user -> user.getTeams().stream()
                .filter(team -> !team.getDisabled())
                .filter(team -> !team.getActivity().getDisabled())
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());

        // Get user's indirect teams (managed teams of direct teams)
        List<Team> userIndirectTeams = userDirectTeams.stream()
            .flatMap(team -> team.getManagedTeams().stream())
            .filter(team -> !team.getDisabled())
            .filter(team -> !team.getActivity().getDisabled())
            .toList();

        // Combine direct and indirect teams
        List<Team> allUserTeams = new ArrayList<>(userDirectTeams);
        allUserTeams.addAll(userIndirectTeams);

        // Get assignments for all teams
        List<Assignment> userAssignments = assignmentRepository.findAllByTeamIn(allUserTeams);

        // Extract OrgUnits from assignments
        Set<OrgUnit> userOrgUnits = userAssignments.stream()
            .map(Assignment::getOrgUnit)
            .collect(Collectors.toSet());

        // Add ancestors of the user's OrgUnits
        Set<OrgUnit> ancestors = userOrgUnits.stream()
            .flatMap(orgUnit -> orgUnit.getAncestors().stream())
            .collect(Collectors.toSet());

        userOrgUnits.addAll(ancestors);

        List<OrgUnitGroup> orgUnitGroups = userOrgUnits.stream().flatMap((o) -> o.getGroups().stream()).toList();

        if (pageable.isUnpaged()) {
            return new PageImpl<>(orgUnitGroups);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), orgUnitGroups.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<OrgUnitGroup> sublist = orgUnitGroups.subList(start, end);
        return new PageImpl<>(sublist, pageable, orgUnitGroups.size());
    }
}
