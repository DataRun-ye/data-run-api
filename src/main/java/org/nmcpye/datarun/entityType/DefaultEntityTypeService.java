package org.nmcpye.datarun.entityType;

import org.nmcpye.datarun.common.jpa.DefaultJpaAuditableService;
import org.nmcpye.datarun.entityType.repository.EntityTypeRepository;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultEntityTypeService
        extends DefaultJpaAuditableService<EntityType>
        implements EntityTypeService {

    private final EntityTypeRepository repository;

    public DefaultEntityTypeService(EntityTypeRepository repository,
                                    UserAccessService userAccessService,
                                    CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
