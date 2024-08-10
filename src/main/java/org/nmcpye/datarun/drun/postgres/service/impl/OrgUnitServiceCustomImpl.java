package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
        return repositoryCustom.findAssignedByStatus(false, pageable);
    }

    @Override
    public Optional<OrgUnit> findAssignedByUid(String uid) {
        return repositoryCustom.findAssignedByUid(uid);
    }
}
