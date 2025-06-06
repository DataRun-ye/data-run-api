package org.nmcpye.datarun.jpa.entityType.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityType.repository.EntityTypeRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultEntityTypeService
        extends DefaultJpaIdentifiableService<EntityType>
        implements EntityTypeService {

    private final EntityTypeRepository repository;

    public DefaultEntityTypeService(EntityTypeRepository repository,
                                    UserAccessService userAccessService,
                                    CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
