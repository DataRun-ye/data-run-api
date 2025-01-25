package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.OuLevel;
import org.nmcpye.datarun.drun.postgres.repository.OuLevelRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.OuLevelServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class OuLevelServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<OuLevel> implements OuLevelServiceCustom {

    public OuLevelServiceCustomImpl(OuLevelRelationalRepositoryCustom repository,
                                    CacheManager cacheManager) {
        super(repository, cacheManager);
    }
}
