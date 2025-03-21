package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.common.jpa.impl.DefaultJpaAuditableService;
import org.nmcpye.datarun.common.repository.UserRepository;
import org.nmcpye.datarun.drun.postgres.common.TeamSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.EntityScope;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.AssignmentStatus;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepository;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRepository;
import org.nmcpye.datarun.drun.postgres.service.AssignmentService;
import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.nmcpye.datarun.mongo.repository.AssignmentSubmissionHistoryRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.useraccess.UserAccessService;
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
import java.util.stream.Stream;

@Service
@Primary
@Transactional
public class DefaultAssignmentService
    extends DefaultJpaAuditableService<Assignment>
    implements AssignmentService {

    final AssignmentRepository repository;
    final TeamRepository teamRepository;
    final OrgUnitRepositoryCustom orgUnitRepository;
    final UserRepository userRepository;
    private final AssignmentSubmissionHistoryRepository assignmentHistoryRepository;

    public DefaultAssignmentService(AssignmentRepository repository,
                                    TeamRepository teamRepository,
                                    OrgUnitRepositoryCustom orgUnitRepository,
                                    UserRepository userRepository,
                                    AssignmentSubmissionHistoryRepository assignmentHistoryRepository, UserAccessService userAccessService,
                                    CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.assignmentHistoryRepository = assignmentHistoryRepository;
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

        return repository.save(object);
    }

    private Assignment findParent(Assignment parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repository::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repository::findByUid))
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

    private Page<Assignment> findWithStatus(Page<Assignment> assignments) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            assignments.forEach(assignment -> {
                assignmentHistoryRepository.findLastEntryByUidAndAssignedTeam(assignment.getUid(),
                        assignment.getTeam().getUid())
                    .flatMap(history -> history.getEntries().stream()
                        .max(Comparator.comparing(AssignmentSubmissionHistory.HistoryEntry::getEntryDate)))
                    .ifPresentOrElse(lastEntry -> {
                        assignment.setStatus(lastEntry.getSubmissionStatus());
                        assignment.setLastEntryDate(lastEntry.getEntryDate());
                        assignment.setLastEntryBy(lastEntry.getSubmissionUser());
                    }, () -> assignment.setStatus(AssignmentStatus.NOT_STARTED));
            });
        } else {
            String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow();
            assignments.forEach(assignment -> {
                assignmentHistoryRepository.findLastEntryByUidAndUser(assignment.getUid(), currentUserLogin)
                    .flatMap(history -> history.getEntries().stream()
                        .max(Comparator.comparing(AssignmentSubmissionHistory.HistoryEntry::getEntryDate)))
                    .ifPresentOrElse(lastEntry -> {
                        assignment.setStatus(lastEntry.getSubmissionStatus());
                        assignment.setLastEntryDate(lastEntry.getEntryDate());
                        assignment.setLastEntryBy(lastEntry.getSubmissionUser());
                    }, () -> assignment.setStatus(AssignmentStatus.NOT_STARTED));
            });
        }
        return assignments;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<Assignment> getAllUserAccessible(Pageable pageable, QueryRequest queryRequest) {
        Page<Assignment> assignedPage = findAllByUser(pageable, queryRequest);
        List<Assignment> assigned = assignedPage.getContent().stream()
            .filter(assignment -> !assignment.getActivity().getDisabled())
            .peek(assignment -> assignment.setEntityScope(EntityScope.Assigned))
            .toList();

        List<String> assignedUids = assigned.stream().map(Assignment::getUid).toList();

        List<Assignment> managed = getManagedTeamsAssignmentsWithChildren().stream()
            .peek(assignment -> assignment.setEntityScope(EntityScope.Managed))
            .toList();

        List<Assignment> combinedContent = Stream.concat(assigned.stream(), managed.stream())
            .collect(Collectors.toList());

        // TODO Create AssignmentHistory in app and fetch all and sort the latest out on the app
        Page<Assignment> assignments = findWithStatus(new PageImpl<>(combinedContent, pageable, assignedPage.getTotalElements() + managed.size()));

        return assignments;
    }

    List<Assignment> getAssignmentsWithChildren(Collection<String> uids) {
        List<Assignment> assignments = new ArrayList<>();
        for (String uid : uids) {
            assignments.addAll(repository.findAllByPathContaining(uid));
        }
        return assignments;
    }

    List<Assignment> getManagedTeamsAssignmentsWithChildren() {
        Specification<Team> spec = TeamSpecifications.getManagedTeamsByUserTeams(
                SecurityUtils.getCurrentUserLoginOrThrow())
            .and(TeamSpecifications.isEnabled());

        List<String> managedAssignmentsUids = teamRepository
            .findAll(spec)
            .stream()
            .flatMap(team -> team.getAssignments().stream()).map(Assignment::getUid).toList();
        return getAssignmentsWithChildren(managedAssignmentsUids);
    }

//    @Transactional(readOnly = true)
//    public List<Assignment> getAllUserAccessibleHierarchy(User user) {
//        List<Assignment> allAssignments = getAllUserAccessible(user);
//        return buildHierarchy(allAssignments);
//    }

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
        repository.updatePaths();
    }

    @Override
    @Transactional
    public void forceUpdatePaths() {
        repository.forceUpdatePaths();
    }
}
