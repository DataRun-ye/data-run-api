package org.nmcpye.datarun.jpa.flowrun.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.nmcpye.datarun.jpa.flowrun.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.flowrun.mapper.AssignmentWithAccessMapper;
import org.nmcpye.datarun.jpa.flowrun.repository.FlowRunRepository;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Primary
@Transactional
public class DefaultFlowRunService
    extends DefaultJpaSoftDeleteService<FlowRun>
    implements FlowRunService {

    private final FlowRunRepository repository;
    private final FlowTypeRepository flowTypeRepository;
    private final TeamRepository teamRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final FlowRunMaintenanceService maintenanceService;
    private final AssignmentWithAccessMapper assignmentMapper;

    public DefaultFlowRunService(FlowRunRepository repository,
                                 TeamRepository teamRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 UserAccessService userAccessService,
                                 CacheManager cacheManager,
                                 FlowRunMaintenanceService maintenanceService,
                                 AssignmentWithAccessMapper assignmentMapper,
                                 FlowTypeRepository flowTypeRepository) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.maintenanceService = maintenanceService;
        this.assignmentMapper = assignmentMapper;
        this.flowTypeRepository = flowTypeRepository;
    }

    @Override
    public FlowRun saveWithRelations(FlowRun object) {

        Team team = null;
        OrgUnit orgUnit = null;

        if (object.getTeam() != null) {
            team = findTeam(object.getTeam());
        }

        if (object.getOrgUnit() != null) {
            orgUnit = findOrgUnit(object.getOrgUnit());
        }

        FlowRun parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            object.setParent(parent);
        }

        object.setTeam(team);
        object.setOrgUnit(orgUnit);

        return save(object);
    }

    private FlowRun findParent(FlowRun parent) {
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
//            assignments.forEach(flowRun -> {
//                assignmentHistoryRepository.findLastEntryByUid(flowRun.getUid())
//                    .flatMap(history -> history.getEntries()
//                        .stream().max(Comparator
//                            .comparing(AssignmentSubmissionHistory.HistoryEntry::getEntryDate)))
//                    .ifPresentOrElse(lastEntry -> {
//                        flowRun.setStatus(lastEntry.getSubmissionStatus());
//                        flowRun.setLastEntryDate(lastEntry.getEntryDate());
//                        flowRun.setLastEntryBy(lastEntry.getSubmissionUser());
//                    }, () -> flowRun.setStatus(AssignmentStatus.PLANNED));
//            });
//        } else {
//            String currentUserLogin = SecurityUtils.getCurrentUserLoginOrThrow();
//            assignments.forEach(flowRun -> {
//                assignmentHistoryRepository.findLastEntryByUidAndUser(flowRun.getUid(), currentUserLogin)
//                    .flatMap(history -> history.getEntries()
//                        .stream().max(Comparator
//                            .comparing(AssignmentSubmissionHistory.HistoryEntry::getEntryDate)))
//                    .ifPresentOrElse(lastEntry -> {
//                        flowRun.setStatus(lastEntry.getSubmissionStatus());
//                        flowRun.setLastEntryDate(lastEntry.getEntryDate());
//                        flowRun.setLastEntryBy(lastEntry.getSubmissionUser());
//                    }, () -> flowRun.setStatus(AssignmentStatus.PLANNED));
//            });
//        }
//        return assignments;
//    }


    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentWithAccessDto> getAllUserAccessibleDto(QueryRequest queryRequest, String jsonQueryBody) {
        Page<FlowRun> assignedPage = findAllByUser(queryRequest, jsonQueryBody);
        return assignedPage.map(assignmentMapper::toDto);
    }

    List<FlowRun> getAssignmentsWithChildren(Collection<String> uids) {
        List<FlowRun> flowRuns = new ArrayList<>();
        for (String uid : uids) {
            flowRuns.addAll(repository.findAllByPathContaining(uid));
        }
        return flowRuns;
    }

    List<FlowRun> getManagedTeamsAssignmentsWithChildren() {
        Specification<Team> spec = TeamSpecifications
            .getManagedTeamsByUserTeams(SecurityUtils.getCurrentUserLoginOrThrow())
            .and(TeamSpecifications.isEnabled());

        List<String> managedAssignmentsUids = teamRepository
            .findAll(spec).stream()
            .flatMap(team -> team.getFlowRuns().stream())
            .map(FlowRun::getUid).toList();
        return getAssignmentsWithChildren(managedAssignmentsUids);
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
