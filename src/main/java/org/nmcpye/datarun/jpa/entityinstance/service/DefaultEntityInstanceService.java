package org.nmcpye.datarun.jpa.entityinstance.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaAuditableService;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultEntityInstanceService
    extends DefaultJpaAuditableService<EntityInstance>
    implements EntityInstanceService {

    private final EntityInstanceRepository repository;

    public DefaultEntityInstanceService(EntityInstanceRepository repository,
                                        CacheManager cacheManager,
                                        UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
