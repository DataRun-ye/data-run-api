package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.common.jpa.DefaultJpaAuditableService;
import org.nmcpye.datarun.drun.postgres.service.OuLevelService;
import org.nmcpye.datarun.oulevel.OuLevel;
import org.nmcpye.datarun.oulevel.repository.OuLevelRepository;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultOuLevelService extends DefaultJpaAuditableService<OuLevel> implements OuLevelService {

    public DefaultOuLevelService(OuLevelRepository repository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
