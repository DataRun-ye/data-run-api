package org.nmcpye.datarun.jpa.assignment.service;

import org.nmcpye.datarun.assignmentshadow.AssignmentAuthorityCommandService;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceAssignmentFormGate;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.mapper.AssignmentWithAccessMapper;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
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
    private final AssignmentAuthorityCommandService authorityCommands;
    private final AssignmentMaintenanceService maintenanceService;
    private final AssignmentWithAccessMapper assignmentMapper;
    private final ReferenceAssignmentFormGate referenceAssignmentFormGate;
    private final ReleasedWorkReadAuthority releasedWorkAuthority;

    public DefaultAssignmentService(AssignmentRepository repository,
                                    UserAccessService userAccessService,
                                    CacheManager cacheManager,
                                    AssignmentMaintenanceService maintenanceService,
                                    AssignmentWithAccessMapper assignmentMapper,
                                    ReferenceAssignmentFormGate referenceAssignmentFormGate,
                                    AssignmentAuthorityCommandService authorityCommands,
                                    ReleasedWorkReadAuthority releasedWorkAuthority) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.maintenanceService = maintenanceService;
        this.assignmentMapper = assignmentMapper;
        this.referenceAssignmentFormGate = referenceAssignmentFormGate;
        this.authorityCommands = authorityCommands;
        this.releasedWorkAuthority = releasedWorkAuthority;
    }

    @Override
    public Assignment saveWithRelations(Assignment object) {
        return authorityCommands.saveAssignment(object);
    }

    @Override
    public Assignment save(Assignment object) {
        return authorityCommands.saveAssignment(object);
    }

    @Override
    public Assignment update(Assignment object) {
        return authorityCommands.updateAssignment(object);
    }

    @Override
    public void delete(Assignment object) {
        authorityCommands.deleteAssignment(object);
    }

    @Override
    public void deleteByUid(String uid) {
        authorityCommands.deleteAssignment(findByUid(uid).orElseThrow());
    }

    @Override
    public void softDelete(Assignment object) {
        authorityCommands.deleteAssignment(object);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentWithAccessDto> getAllUserAccessibleDto(
        QueryRequest queryRequest,
        String jsonQueryBody,
        int referenceVersion) {
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        Page<Assignment> assignedPage = findAllByUser(
            queryRequest,
            jsonQueryBody
        );
        Page<AssignmentWithAccessDto> response = assignedPage.map(
            assignment -> assignmentMapper.toDto(assignment, user)
        );
        referenceAssignmentFormGate.filterUnsupportedForms(
            response.getContent(),
            referenceVersion
        );
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssignmentWithAccessDto> getAllReleasedWorkDto(
        QueryRequest queryRequest,
        String jsonQueryBody,
        int referenceVersion
    ) {
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        ReleasedWorkReadScope scope = releasedWorkAuthority.readAll(user);
        Page<Assignment> assignedPage = findReleasedWork(
            scope,
            queryRequest,
            jsonQueryBody
        );
        Page<AssignmentWithAccessDto> response = assignedPage.map(
            assignment -> scope.administrator()
                ? assignmentMapper.toDto(assignment, user)
                : assignmentMapper.toDto(
                    assignment,
                    user,
                    scope.formUids(assignment.getUid())
                )
        );
        referenceAssignmentFormGate.filterUnsupportedForms(
            response.getContent(),
            referenceVersion
        );
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Assignment> findAllReleasedWork(
        QueryRequest queryRequest,
        String jsonQueryBody
    ) {
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        ReleasedWorkReadScope scope = releasedWorkAuthority.readAll(user);
        return findReleasedWork(scope, queryRequest, jsonQueryBody);
    }

    private Page<Assignment> findReleasedWork(
        ReleasedWorkReadScope scope,
        QueryRequest queryRequest,
        String jsonQueryBody
    ) {
        if (scope.administrator()) {
            return findAllByUser(queryRequest, jsonQueryBody);
        }
        Specification<Assignment> authorizedAssignments =
            (root, query, cb) -> scope.assignmentUids().isEmpty()
                ? cb.disjunction()
                : root.get("uid").in(scope.assignmentUids());
        Specification<Assignment> query = querySpecification(
            queryRequest,
            jsonQueryBody
        ).and(authorizedAssignments);
        return repository.findAll(query, queryRequest.getPageable());
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
