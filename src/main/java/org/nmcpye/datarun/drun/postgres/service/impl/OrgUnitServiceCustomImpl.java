package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.postgres.common.OrgUnitSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class OrgUnitServiceCustomImpl
    extends OrgUnitSpecifications
    implements OrgUnitServiceCustom {

    final OrgUnitRelationalRepositoryCustom repositoryCustom;
    final AssignmentRelationalRepositoryCustom assignmentRepository;

    public OrgUnitServiceCustomImpl(OrgUnitRelationalRepositoryCustom orgUnitRepositoryCustom, AssignmentRelationalRepositoryCustom assignmentRepository) {
        super(orgUnitRepositoryCustom);
        this.repositoryCustom = orgUnitRepositoryCustom;
        this.assignmentRepository = assignmentRepository;
    }


    @Override
    public OrgUnit saveWithRelations(OrgUnit object) {
        OrgUnit parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            object.setParent(parent);
        }

        return repositoryCustom.save(object);
    }

    private OrgUnit findParent(OrgUnit parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repositoryCustom::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repositoryCustom::findByUid))
            .or(() -> Optional.ofNullable(parent.getCode())
                .flatMap(repositoryCustom::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    @Override
    public Page<OrgUnit> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
        final List<OrgUnit> userOrgUnits = repositoryCustom
            .findAllWithRelation();

        final Set<String> uids = userOrgUnits
            .stream()
            .flatMap(orgUnit -> orgUnit.getAncestorUids(null).stream())
            .collect(Collectors.toSet());
        userOrgUnits.addAll(repositoryCustom.findAllByUidIn(uids));

        if (pageable.isUnpaged()) {
            return new PageImpl<>(userOrgUnits);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userOrgUnits.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<OrgUnit> sublist = userOrgUnits.subList(start, end);
        return new PageImpl<>(sublist, pageable, userOrgUnits.size());
    }

    @Override
    public Set<OrgUnit> getUserTeamsOrganisationUnits() {

        return Set.of();
    }

    @Override
    public Set<OrgUnit> getUserManagedTeamsOrganisationUnits() {
        return Set.of();
    }

    @Override
    public Set<OrgUnit> getAllUserAccessibleOrganisationUnits() {
        return Set.of();
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void updatePaths() {
        repositoryCustom.updatePaths();
    }

    /**
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Override
    @Transactional
    public void forceUpdatePaths() {
        repositoryCustom.forceUpdatePaths();
    }

}
