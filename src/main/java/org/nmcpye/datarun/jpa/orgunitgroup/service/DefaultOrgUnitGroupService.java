package org.nmcpye.datarun.jpa.orgunitgroup.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaAuditableService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.orgunitgroup.OrgUnitGroup;
import org.nmcpye.datarun.jpa.orgunitgroup.repository.OrgUnitGroupRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Primary
@Transactional
public class DefaultOrgUnitGroupService extends DefaultJpaAuditableService<OrgUnitGroup> implements OrgUnitGroupService {

    private final OrgUnitRepository orgUnitRepository;

    public DefaultOrgUnitGroupService(OrgUnitGroupRepository repository, OrgUnitRepository orgUnitRepository, UserAccessService userAccessService, CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.orgUnitRepository = orgUnitRepository;
    }


    @Override
    public OrgUnitGroup saveWithRelations(OrgUnitGroup object) {
        if (!object.getOrgUnits().isEmpty()) {
            Set<OrgUnit> orgUnits = new HashSet<>();
            for (OrgUnit orgUnit : object.getOrgUnits()) {
                orgUnits.add(findOrgUnit(orgUnit));
            }

            object.setOrgUnits(orgUnits);
            return save(object);
        }

        return save(object);
    }

    private OrgUnit findOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getUid()).flatMap(orgUnitRepository::findByUid).or(() -> Optional.ofNullable(orgUnit.getId()).flatMap(orgUnitRepository::findById)).or(() -> Optional.ofNullable(orgUnit.getCode()).flatMap(orgUnitRepository::findByCode)).orElseThrow(() -> new PropertyNotFoundException("OrgUnit not found: " + orgUnit));
    }

//    @Override
//    public Page<OrgUnitGroup> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repository.findAll(pageable);
//        }
//
//        String currentUserLogin =  SecurityUtils.getCurrentUserLoginOrThrow(
//            new ErrorMessage(ErrorCode.E3004, getClass().getName()));
//
//        // Get user's direct teams
//        List<Team> userDirectTeams = userRepository.findOneByLogin(currentUserLogin)
//            .map(user -> user.getTeams().stream()
//                .filter(team -> !team.getDisabled())
//                .filter(team -> !team.getActivity().getDisabled())
//                .collect(Collectors.toList()))
//            .orElse(Collections.emptyList());
//
//        // Get user's indirect teams (managed teams of direct teams)
//        List<Team> userIndirectTeams = userDirectTeams.stream()
//            .flatMap(team -> team.getManagedTeams().stream())
//            .filter(team -> !team.getDisabled())
//            .filter(team -> !team.getActivity().getDisabled())
//            .toList();
//
//        // Combine direct and indirect teams
//        List<Team> allUserTeams = new ArrayList<>(userDirectTeams);
//        allUserTeams.addAll(userIndirectTeams);
//
//        // Get assignments for all teams
//        List<Assignment> userAssignments = assignmentRepository.findAllByTeamIn(allUserTeams);
//
//        // Extract OrgUnits from assignments
//        Set<OrgUnit> userOrgUnits = userAssignments.stream()
//            .map(Assignment::getOrgUnit)
//            .collect(Collectors.toSet());
//
//        // Add ancestors of the user's OrgUnits
//        Set<OrgUnit> ancestors = userOrgUnits.stream()
//            .flatMap(orgUnit -> orgUnit.getAncestors().stream())
//            .collect(Collectors.toSet());
//
//        userOrgUnits.addAll(ancestors);
//
//        List<OrgUnitGroup> orgUnitGroups = userOrgUnits.stream().flatMap((o) -> o.getGroups().stream()).toList();
//
//        if (pageable.isUnpaged()) {
//            return new PageImpl<>(orgUnitGroups);
//        }
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min((start + pageable.getPageSize()), orgUnitGroups.size());
//        if (start > end) {
//            return Page.empty(pageable);
//        }
//
//        List<OrgUnitGroup> sublist = orgUnitGroups.subList(start, end);
//        return new PageImpl<>(sublist, pageable, orgUnitGroups.size());
//    }

//    @Override
//    public Page<OrgUnitGroup> findAllByUser(Specification<OrgUnitGroup> spec, Pageable pageable) {
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repository.findAll(spec, pageable);
//        }
//
//        String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow(
//            new ErrorMessage(ErrorCode.E3004, getClass().getName()));
//
//        // Get user's direct teams
//        List<Team> userDirectTeams = userRepository.findOneByLogin(currentUserLogin)
//            .map(user -> user.getTeams().stream()
//                .filter(team -> !team.getDisabled())
//                .filter(team -> !team.getActivity().getDisabled())
//                .collect(Collectors.toList()))
//            .orElse(Collections.emptyList());
//
//        // Get user's indirect teams (managed teams of direct teams)
//        List<Team> userIndirectTeams = userDirectTeams.stream()
//            .flatMap(team -> team.getManagedTeams().stream())
//            .filter(team -> !team.getDisabled())
//            .filter(team -> !team.getActivity().getDisabled())
//            .toList();
//
//        // Combine direct and indirect teams
//        List<Team> allUserTeams = new ArrayList<>(userDirectTeams);
//        allUserTeams.addAll(userIndirectTeams);
//
//        // Get assignments for all teams
//        List<Assignment> userAssignments = assignmentRepository.findAllByTeamIn(allUserTeams);
//
//        // Extract OrgUnits from assignments
//        Set<OrgUnit> userOrgUnits = userAssignments.stream()
//            .map(Assignment::getOrgUnit)
//            .collect(Collectors.toSet());
//
//        // Add ancestors of the user's OrgUnits
//        Set<OrgUnit> ancestors = userOrgUnits.stream()
//            .flatMap(orgUnit -> orgUnit.getAncestors().stream())
//            .collect(Collectors.toSet());
//
//        userOrgUnits.addAll(ancestors);
//
//        List<OrgUnitGroup> orgUnitGroups = userOrgUnits.stream().flatMap((o) -> o.getGroups().stream()).toList();
//
//        if (pageable.isUnpaged()) {
//            return new PageImpl<>(orgUnitGroups);
//        }
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min((start + pageable.getPageSize()), orgUnitGroups.size());
//        if (start > end) {
//            return Page.empty(pageable);
//        }
//
//        List<OrgUnitGroup> sublist = orgUnitGroups.subList(start, end);
//        return new PageImpl<>(sublist, pageable, orgUnitGroups.size());
//    }
}
