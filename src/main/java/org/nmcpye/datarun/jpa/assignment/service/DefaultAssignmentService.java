package org.nmcpye.datarun.jpa.assignment.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceAssignmentFormGate;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceAssignmentScopeGuard;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.activity.Activity;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.mapper.AssignmentWithAccessMapper;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Primary
@Transactional
public class DefaultAssignmentService
    extends DefaultJpaSoftDeleteService<Assignment>
    implements AssignmentService {

    private final AssignmentRepository repository;
    private final TeamRepository teamRepository;
    private final ActivityRepository activityRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final AssignmentMaintenanceService maintenanceService;
    private final AssignmentWithAccessMapper assignmentMapper;
    private final ReferenceAssignmentFormGate referenceAssignmentFormGate;
    private final ReferenceAssignmentScopeGuard referenceAssignmentScopeGuard;

    public DefaultAssignmentService(AssignmentRepository repository,
                                    TeamRepository teamRepository,
                                    OrgUnitRepository orgUnitRepository,
                                    UserAccessService userAccessService,
                                    CacheManager cacheManager,
                                    AssignmentMaintenanceService maintenanceService,
                                    AssignmentWithAccessMapper assignmentMapper,
                                    ActivityRepository activityRepository,
                                    ReferenceAssignmentFormGate referenceAssignmentFormGate,
                                    ReferenceAssignmentScopeGuard referenceAssignmentScopeGuard) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.maintenanceService = maintenanceService;
        this.assignmentMapper = assignmentMapper;
        this.activityRepository = activityRepository;
        this.referenceAssignmentFormGate = referenceAssignmentFormGate;
        this.referenceAssignmentScopeGuard = referenceAssignmentScopeGuard;
    }

    @Override
    public Assignment saveWithRelations(Assignment object) {
        Assignment existing = findExisting(object).orElse(null);

        Team team = null;
        Activity activity = null;
        OrgUnit orgUnit = null;

        if (object.getTeam() != null) {
            team = findTeam(object.getTeam());
        }

        if (object.getActivity() != null) {
            activity = findActivity(object.getActivity());
        }

        if (object.getOrgUnit() != null) {
            orgUnit = findOrgUnit(object.getOrgUnit());
        }

        referenceAssignmentScopeGuard.validateScopeUpdate(
            existing,
            activity,
            orgUnit,
            object.getForms());

        Assignment parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            object.setParent(parent);
        }

        object.setActivity(activity);
        object.setTeam(team);
        object.setOrgUnit(orgUnit);

        return save(object);
    }

    private Assignment findParent(Assignment parent) {
        return Optional.ofNullable(parent.getId()).flatMap(repository::findById).or(() -> Optional.ofNullable(parent.getUid()).flatMap(repository::findByUid)).orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    private Team findTeam(Team team) {
        return Optional.ofNullable(team.getId()).flatMap(teamRepository::findById)
            .or(() -> Optional.ofNullable(team.getUid())
                .flatMap(teamRepository::findByUid))
            .or(() -> Optional.ofNullable(team.getCode())
                .flatMap((code) -> teamRepository.findByCodeAndActivityUid(code, team.getActivity().getUid()))).orElseThrow(() -> new PropertyNotFoundException("Team not found: " + team));
    }

    private Activity findActivity(Activity activity) {
        return Optional.ofNullable(activity.getId())
            .flatMap(activityRepository::findById)
            .or(() -> Optional
                .ofNullable(activity.getUid()).flatMap(activityRepository::findByUid))
            .orElseThrow(() -> new PropertyNotFoundException("Activity not found: " + activity));
    }
    private OrgUnit findOrgUnit(OrgUnit orgUnit) {
        return Optional.ofNullable(orgUnit.getId()).flatMap(orgUnitRepository::findById).or(() -> Optional.ofNullable(orgUnit.getUid()).flatMap(orgUnitRepository::findByUid)).or(() -> Optional.ofNullable(orgUnit.getCode()).flatMap(orgUnitRepository::findByCode)).orElseThrow(() -> new PropertyNotFoundException("OrgUniy not found: " + orgUnit));
    }

    private Optional<Assignment> findExisting(Assignment assignment) {
        return Optional.ofNullable(assignment.getId())
            .flatMap(repository::findById)
            .or(() -> Optional.ofNullable(assignment.getUid())
                .flatMap(repository::findByUid));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentWithAccessDto> getAllUserAccessibleDto(
        QueryRequest queryRequest,
        String jsonQueryBody,
        int referenceVersion) {
        Page<Assignment> assignedPage = findAllByUser(queryRequest, jsonQueryBody);
        Page<AssignmentWithAccessDto> response = assignedPage.map(assignmentMapper::toDto);
        referenceAssignmentFormGate.filterUnsupportedForms(
            assignedPage.getContent(),
            response.getContent(),
            referenceVersion);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Assignment> findAccessibleByIdOrUid(String idOrUid) {
        QueryRequest queryRequest = new QueryRequest();
        Specification<Assignment> identity = (root, query, cb) -> cb.or(
            cb.equal(root.get("id"), idOrUid),
            cb.equal(root.get("uid"), idOrUid));
        Specification<Assignment> access = baseAccessSpecification(
            SecurityUtils.getCurrentUserDetailsOrThrow(),
            queryRequest,
            null);
        return repository.findOne(access.and(identity));
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
