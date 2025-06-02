package org.nmcpye.datarun.jpa.entityattribute.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaAuditableService;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeType;
import org.nmcpye.datarun.jpa.entityattribute.repository.EntityAttributeTypeRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultEntityAttributeTypeService
    extends DefaultJpaAuditableService<EntityAttributeType>
    implements EntityAttributeTypeService {

    private final EntityAttributeTypeRepository repository;

    public DefaultEntityAttributeTypeService(EntityAttributeTypeRepository repository,
                                             CacheManager cacheManager,
                                             UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
