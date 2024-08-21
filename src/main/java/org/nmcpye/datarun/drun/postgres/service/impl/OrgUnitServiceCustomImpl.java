package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
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
    extends IdentifiableRelationalServiceImpl<OrgUnit>
    implements OrgUnitServiceCustom {

    final OrgUnitRelationalRepositoryCustom repositoryCustom;
    final AssignmentRelationalRepositoryCustom assignmentRepository;

    public OrgUnitServiceCustomImpl(OrgUnitRelationalRepositoryCustom orgUnitRepositoryCustom, AssignmentRelationalRepositoryCustom assignmentRepository) {
        super(orgUnitRepositoryCustom);
        this.repositoryCustom = orgUnitRepositoryCustom;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public Page<OrgUnit> findAllByUser(Pageable pageable) {
        final List<OrgUnit> userOrgUnits = repositoryCustom
            .findAssignedWithEagerRelation();
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
    public Optional<OrgUnit> findAssignedByUid(String uid) {
        return repositoryCustom.findAssignedByUidWithEagerRelation(uid);
    }

    @Override
    @Transactional
    public void updatePaths() {
        repositoryCustom.updatePaths();
    }

    /**
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Override
    @Transactional
    public void forceUpdatePaths() {
        repositoryCustom.forceUpdatePaths();
    }

}
