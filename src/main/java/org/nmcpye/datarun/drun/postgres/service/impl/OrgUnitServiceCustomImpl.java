package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class OrgUnitServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<OrgUnit>
    implements OrgUnitServiceCustom {

    final OrgUnitRelationalRepositoryCustom repository;
    final AssignmentRelationalRepositoryCustom assignmentRepository;
    final UserRepository userRepository;

    public OrgUnitServiceCustomImpl(OrgUnitRelationalRepositoryCustom repository, AssignmentRelationalRepositoryCustom assignmentRepository,
                                    UserRepository userRepository,
                                    CacheManager cacheManager) {
        super(repository, cacheManager);
        this.repository = repository;
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

        return repository.save(object);
    }

    private OrgUnit findParent(OrgUnit parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repository::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repository::findByUid))
            .or(() -> Optional.ofNullable(parent.getCode())
                .flatMap(repository::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

//    @Override
//    public Page<OrgUnit> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
//        if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
//            return Page.empty();
//        }
//
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            return repositoryCustom.findAll(pageable);
//        }
//
////        assignmentRepository
////        var allUserAssignments = assignmentRepository
////            .findAll(canReadWithChildren(
////                SecurityUtils.getCurrentUserLogin().get())
////                .and(isEnabled()));
//
//        final List<OrgUnit> userOrgUnits = repositoryCustom
//            .findAll(canRead());
//
//        var userDirectIndirectAssignments = assignmentRepository.findAll(AssignmentSpecifications.canRead());
////        final Set<String> uids = userOrgUnits
////            .stream()
////            .flatMap(orgUnit -> orgUnit.getAncestorUids(null).stream())
////            .collect(Collectors.toSet());
////        userOrgUnits.addAll(repositoryCustom.findAllByUidIn(uids));
//
//        final Set<OrgUnit> ancestors = userOrgUnits
//            .stream()
//            .flatMap(orgUnit -> orgUnit.getAncestors().stream())
//            .collect(Collectors.toSet());
//
//        userOrgUnits.addAll(ancestors);
//
//        if (pageable.isUnpaged()) {
//            return new PageImpl<>(userOrgUnits);
//        }
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min((start + pageable.getPageSize()), userOrgUnits.size());
//        if (start > end) {
//            return Page.empty(pageable);
//        }
//
//        List<OrgUnit> sublist = userOrgUnits.subList(start, end);
//        return new PageImpl<>(sublist, pageable, userOrgUnits.size());
//    }

    @Override
    public Page<OrgUnit> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return Page.empty();
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        String currentUserLogin = SecurityUtils.getCurrentUserLogin().get();

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

        List<OrgUnit> sortedOrgUnits = new ArrayList<>(userOrgUnits);
        sortedOrgUnits.sort(Comparator.comparing(OrgUnit::getName));

        if (pageable.isUnpaged()) {
            return new PageImpl<>(sortedOrgUnits);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedOrgUnits.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<OrgUnit> sublist = sortedOrgUnits.subList(start, end);
        return new PageImpl<>(sublist, pageable, sortedOrgUnits.size());
    }

    @Override
    public Page<OrgUnit> findAllByUser(Specification<OrgUnit> spec, Pageable pageable) {
        if (SecurityUtils.getCurrentUserLogin().isEmpty()) {
            return Page.empty();
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(spec, pageable);
        }

        String currentUserLogin = SecurityUtils.getCurrentUserLogin().get();

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

        List<OrgUnit> sortedOrgUnits = new ArrayList<>(userOrgUnits);
        sortedOrgUnits.sort(Comparator.comparing(OrgUnit::getName));

        if (pageable.isUnpaged()) {
            return new PageImpl<>(sortedOrgUnits);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedOrgUnits.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<OrgUnit> sublist = sortedOrgUnits.subList(start, end);
        return new PageImpl<>(sublist, pageable, sortedOrgUnits.size());
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
        repository.updatePaths();
    }

    @Override
    @Transactional
    public void forceUpdatePaths() {
        repository.forceUpdatePaths();
    }

}
