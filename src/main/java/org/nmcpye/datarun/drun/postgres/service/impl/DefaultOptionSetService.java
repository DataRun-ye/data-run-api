package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.common.jpa.DefaultJpaAuditableService;
import org.nmcpye.datarun.drun.postgres.service.OptionSetService;
import org.nmcpye.datarun.optionset.OptionSet;
import org.nmcpye.datarun.optionset.repository.OptionSetRepository;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultOptionSetService extends DefaultJpaAuditableService<OptionSet> implements OptionSetService {
    public DefaultOptionSetService(OptionSetRepository repository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
