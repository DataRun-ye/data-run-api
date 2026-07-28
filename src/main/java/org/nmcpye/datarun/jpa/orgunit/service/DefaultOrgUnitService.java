package org.nmcpye.datarun.jpa.orgunit.service;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureAuthorityUnavailableException;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Hamza Assada 18/01/2022
 */
@Service
@Primary
@Transactional
public class DefaultOrgUnitService extends DefaultJpaIdentifiableService<OrgUnit> implements OrgUnitService {

    private final OrgUnitRepository repository;
    private final OrgUnitMaintenanceService maintenanceService;
    private final ReleasedWorkReadAuthority releasedWorkAuthority;

    public DefaultOrgUnitService(
        OrgUnitRepository repository,
        UserAccessService userAccessService,
        CacheManager cacheManager,
        OrgUnitMaintenanceService maintenanceService,
        ReleasedWorkReadAuthority releasedWorkAuthority
    ) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
        this.maintenanceService = maintenanceService;
        this.releasedWorkAuthority = releasedWorkAuthority;
    }

    @Override
    public OrgUnit save(OrgUnit object) {
        OrgUnit parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);

            // persistent instance references an unsaved transient instance
            parent.setPersisted(true);

            object.setParent(parent);
        }
        return repository.save(object);
    }

    @Override
    public OrgUnit saveWithRelations(OrgUnit object) {
        OrgUnit parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            parent.setPersisted(true);
            object.setParent(parent);
        }
        return save(object);
    }

    private OrgUnit findParent(OrgUnit parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repository::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repository::findByUid))
            .or(() -> Optional.ofNullable(parent.getCode())
                .flatMap(repository::findByCode))
//            .map(OrgUnit::setIsPersisted)
//            .map(OrgUnit.class::cast)
            .orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    /**
     * Updates the paths of organization units in the system.
     * This method is scheduled to run automatically at 3:00 AM every day.
     * It ensures that the hierarchical paths of organization units are kept up-to-date.
     * The method is transactional to ensure data consistency during the update process.
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 4 * * ?")
    public void updatePaths() {
        maintenanceService.updateMissingPaths();
    }

    @Override
    @Transactional
    public void forceUpdatePaths() {
        maintenanceService.forceRecomputePaths();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrgUnit> findAllReleasedWork(
        QueryRequest queryRequest,
        String jsonQueryBody
    ) {
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        ReleasedWorkReadScope scope = releasedWorkAuthority.readAll(user);
        if (scope.administrator()) {
            return findAllByUser(queryRequest, jsonQueryBody);
        }

        Set<String> directUids = scope.directOrgUnitUids();
        Set<OrgUnit> directOrgUnits = loadProjectionUids(directUids);
        requireProjectionUids(directUids, directOrgUnits);
        Set<String> ancestorUids = directOrgUnits.stream()
            .flatMap(orgUnit -> orgUnit.getAncestorUids(null).stream())
            .collect(Collectors.toSet());
        requireProjectionUids(
            ancestorUids,
            loadProjectionUids(ancestorUids)
        );
        Set<String> authorizedUids = Stream.concat(
            directUids.stream(),
            ancestorUids.stream()
        ).collect(Collectors.toSet());

        Specification<OrgUnit> authorized = (root, query, cb) ->
            authorizedUids.isEmpty()
                ? cb.disjunction()
                : root.get("uid").in(authorizedUids);
        return repository.findAll(
            querySpecification(queryRequest, jsonQueryBody).and(authorized),
            queryRequest.getPageable()
        );
    }

    private Set<OrgUnit> loadProjectionUids(Set<String> uids) {
        return uids.isEmpty()
            ? Set.of()
            : Set.copyOf(repository.findAllByUidIn(uids));
    }

    private void requireProjectionUids(
        Set<String> expectedUids,
        java.util.Collection<OrgUnit> projections
    ) {
        Set<String> actualUids = projections.stream()
            .map(OrgUnit::getUid)
            .collect(Collectors.toSet());
        if (!actualUids.equals(expectedUids)) {
            throw new AssignmentCaptureAuthorityUnavailableException();
        }
    }

}
