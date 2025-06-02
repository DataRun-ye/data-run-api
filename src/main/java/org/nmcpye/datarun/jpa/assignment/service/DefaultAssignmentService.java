package org.nmcpye.datarun.jpa.assignment.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.mapper.AssignmentWithAccessMapper;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamSpecifications;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Primary
@Transactional
public class DefaultAssignmentService
    extends DefaultJpaSoftDeleteService<Assignment>
    implements AssignmentService {

    private final AssignmentRepository repository;
    private final TeamRepository teamRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AssignmentMaintenanceService maintenanceService;
    private final AssignmentWithAccessMapper assignmentMapper;

    public DefaultAssignmentService(AssignmentRepository repository,
                                    TeamRepository teamRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    UserAccessService userAccessService,
                                    CacheManager cacheManager,
                                    AssignmentMaintenanceService maintenanceService,
                                    AssignmentWithAccessMapper assignmentMapper, AssignmentRepository assignmentRepository) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.maintenanceService = maintenanceService;
        this.assignmentMapper = assignmentMapper;
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

        return save(object);
    }

    private Assignment findParent(Assignment parent) {
        return Optional.ofNullable(parent.getId()).flatMap(repository::findById).or(() -> Optional.ofNullable(parent.getUid()).flatMap(repository::findByUid)).orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    private Team findTeam(Team team) {
        return Optional.ofNullable(team.getId()).flatMap(teamRepository::findById).or(() -> Optional.ofNullable(team.getUid()).flatMap(teamRepository::findByUid)).or(() -> Optional.ofNullable(team.getCode()).flatMap((code) -> teamRepository.findByCodeAndActivityUid(code, team.getActivity().getUid()))).orElseThrow(() -> new PropertyNotFoundException("Team not found: " + team));
    }

    private OrgUnit findOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getId()).flatMap(orgUnitRepository::findById).or(() -> Optional.ofNullable(orgUnit.getUid()).flatMap(orgUnitRepository::findByUid)).or(() -> Optional.ofNullable(orgUnit.getCode()).flatMap(orgUnitRepository::findByCode)).orElseThrow(() -> new PropertyNotFoundException("OrgUniy not found: " + orgUnit));
    }

//    private Page<Assignment> findWithStatus(Page<Assignment> assignments) {
//        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
//            assignments.forEach(assignment -> {
//                assignmentHistoryRepository.findLastEntryByUid(assignment.getUid())
//                    .flatMap(history -> history.getEntries()
//                        .stream().max(Comparator
//                            .comparing(AssignmentSubmissionHistory.HistoryEntry::getEntryDate)))
//                    .ifPresentOrElse(lastEntry -> {
//                        assignment.setStatus(lastEntry.getSubmissionStatus());
//                        assignment.setLastEntryDate(lastEntry.getEntryDate());
//                        assignment.setLastEntryBy(lastEntry.getSubmissionUser());
//                    }, () -> assignment.setStatus(AssignmentStatus.PLANNED));
//            });
//        } else {
//            String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow();
//            assignments.forEach(assignment -> {
//                assignmentHistoryRepository.findLastEntryByUidAndUser(assignment.getUid(), currentUserLogin)
//                    .flatMap(history -> history.getEntries()
//                        .stream().max(Comparator
//                            .comparing(AssignmentSubmissionHistory.HistoryEntry::getEntryDate)))
//                    .ifPresentOrElse(lastEntry -> {
//                        assignment.setStatus(lastEntry.getSubmissionStatus());
//                        assignment.setLastEntryDate(lastEntry.getEntryDate());
//                        assignment.setLastEntryBy(lastEntry.getSubmissionUser());
//                    }, () -> assignment.setStatus(AssignmentStatus.PLANNED));
//            });
//        }
//        return assignments;
//    }


    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentWithAccessDto> getAllUserAccessibleDto(QueryRequest queryRequest, String jsonQueryBody) {
        Page<Assignment> assignedPage = findAllByUser(queryRequest, jsonQueryBody);
        return assignedPage.map(assignmentMapper::toDto);
    }

    List<Assignment> getAssignmentsWithChildren(Collection<String> uids) {
        List<Assignment> assignments = new ArrayList<>();
        for (String uid : uids) {
            assignments.addAll(repository.findAllByPathContaining(uid));
        }
        return assignments;
    }

    List<Assignment> getManagedTeamsAssignmentsWithChildren() {
        Specification<Team> spec = TeamSpecifications
            .getManagedTeamsByUserTeams(SecurityUtils.getCurrentUserLoginOrThrow())
            .and(TeamSpecifications.isEnabled());

        List<String> managedAssignmentsUids = teamRepository
            .findAll(spec).stream()
            .flatMap(team -> team.getAssignments().stream())
            .map(Assignment::getUid).toList();
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
//        repository.updatePaths();
        maintenanceService.updateMissingPaths();
    }

    @Override
    @Transactional
    public void forceUpdatePaths() {
        maintenanceService.forceRecomputePaths();
//        repository.forceUpdatePaths();
    }
}
