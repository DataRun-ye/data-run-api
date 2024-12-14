package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.common.DefaultIdentifiableSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.OuLevel;
import org.nmcpye.datarun.drun.postgres.repository.OuLevelRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OuLevelServiceCustom;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class OuLevelServiceCustomImpl
    extends DefaultIdentifiableSpecifications<OuLevel> implements OuLevelServiceCustom {

    final private OuLevelRelationalRepositoryCustom repositoryCustom;

    public OuLevelServiceCustomImpl(OuLevelRelationalRepositoryCustom repositoryCustom) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
    }
}
