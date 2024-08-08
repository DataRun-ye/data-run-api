package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OrgUnitRelationalServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class OrgUnitRelationalServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<OrgUnit>
    implements OrgUnitRelationalServiceCustom {

    OrgUnitRelationalRepositoryCustom repositoryCustom;

    public OrgUnitRelationalServiceCustomImpl(OrgUnitRelationalRepositoryCustom orgUnitRepositoryCustom) {
        super(orgUnitRepositoryCustom);
        this.repositoryCustom = orgUnitRepositoryCustom;
    }
}
