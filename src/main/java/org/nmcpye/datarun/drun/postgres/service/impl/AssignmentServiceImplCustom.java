package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.common.AssignmentSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.EntityScope;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.AssignmentServiceCustom;
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
import java.util.stream.Stream;

@Service
@Primary
@Transactional
public class AssignmentServiceImplCustom
    extends AssignmentSpecifications
    implements AssignmentServiceCustom {

    final AssignmentRelationalRepositoryCustom repositoryCustom;
    final TeamRelationalRepositoryCustom teamRepository;
    final OrgUnitRelationalRepositoryCustom orgUnitRepository;
    final UserRepository userRepository;

    public AssignmentServiceImplCustom(AssignmentRelationalRepositoryCustom repositoryCustom,
                                       TeamRelationalRepositoryCustom teamRepository,
                                       OrgUnitRelationalRepositoryCustom orgUnitRepository, UserRepository userRepository) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
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

        Assignment parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            object.setParent(parent);
        }

        object.setTeam(team);
        object.setOrgUnit(orgUnit);

        return repositoryCustom.save(object);
    }

    private Assignment findParent(Assignment parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repositoryCustom::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repositoryCustom::findByUid))
            .orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    private Team findTeam(Team team) {
        return Optional.ofNullable(team.getId())
            .flatMap(teamRepository::findById)
            .or(() -> Optional.ofNullable(team.getUid())
                .flatMap(teamRepository::findByUid))
            .or(() -> Optional.ofNullable(team.getCode())
                .flatMap((code) -> teamRepository.findByCodeAndActivityUid(code, team.getActivity().getUid())))
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
    public Page<Assignment> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
        return repositoryCustom.findAll(hasAccess(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getAllUserAccessible(User user) {
        return repositoryCustom.findAll(canReadWithChildren(user.getLogin()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Assignment> getAllUserAccessible(Pageable pageable) {
        Page<Assignment> assignedPage = repositoryCustom.findAll(hasAccess(), pageable);
        List<Assignment> assigned = assignedPage.getContent().stream()
            .peek(assignment -> assignment.setEntityScope(EntityScope.Assigned))
            .toList();

        List<String> assignedUids = assigned.stream().map(Assignment::getUid).collect(Collectors.toList());
        List<Assignment> managed = repositoryCustom.findAllByParentIn(assignedUids).stream()
            .peek(assignment -> assignment.setEntityScope(EntityScope.Managed))
            .toList();

        List<Assignment> combinedContent = Stream.concat(assigned.stream(), managed.stream())
            .collect(Collectors.toList());

        return new PageImpl<>(combinedContent, pageable, assignedPage.getTotalElements() + managed.size());
    }

    @Transactional(readOnly = true)
    public List<Assignment> getAllUserAccessibleHierarchy(User user) {
        List<Assignment> allAssignments = getAllUserAccessible(user);
        return buildHierarchy(allAssignments);
    }

    private List<Assignment> buildHierarchy(List<Assignment> allAssignments) {
        Map<Long, Assignment> assignmentMap = new HashMap<>();
        List<Assignment> rootAssignments = new ArrayList<>();

        // First pass: map all assignments by their ID
        for (Assignment assignment : allAssignments) {
            assignmentMap.put(assignment.getId(), assignment);
        }

        // Second pass: build the hierarchy
        for (Assignment assignment : allAssignments) {
            Assignment parent = assignment.getParent();
            if (parent == null || !assignmentMap.containsKey(parent.getId())) {
                rootAssignments.add(assignment);
            } else {
                Assignment parentAssignment = assignmentMap.get(parent.getId());
                parentAssignment.getChildren().add(assignment);
            }
        }

        return rootAssignments;
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
