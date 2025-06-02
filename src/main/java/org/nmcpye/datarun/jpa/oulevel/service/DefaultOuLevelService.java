package org.nmcpye.datarun.jpa.oulevel.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaAuditableService;
import org.nmcpye.datarun.jpa.oulevel.OuLevel;
import org.nmcpye.datarun.jpa.oulevel.repository.OuLevelRepository;
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
