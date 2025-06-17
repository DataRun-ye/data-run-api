package org.nmcpye.datarun.jpa.entityinstance.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityInstanceRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class DefaultEntityInstanceService
    extends DefaultJpaIdentifiableService<EntityInstance>
    implements EntityInstanceService {
    private final EntityInstanceRepository entityInstanceRepository;

    public DefaultEntityInstanceService(EntityInstanceRepository repository,
                                        CacheManager cacheManager,
                                        UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.entityInstanceRepository = repository;
    }

    @Transactional
    @Override
    public String upsertEntity(String entityTypeId, Map<String, Object> data) {
        String externalId = (String) data.get("externalId");
        if (externalId == null) {
            throw new IllegalArgumentException("Missing externalId in entity data");
        }

        Optional<EntityInstance> existing = entityInstanceRepository
            .findByEntityTypeIdAndAttributesJsonField(entityTypeId, "externalId", externalId);

        EntityInstance entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setIdentityAttributes(data); // Overwrite all attributes; could be smarter (e.g., merge only non-null)
        } else {
            entity = new EntityInstance();
            // entityType with this Id should be valid and exist before this
            entity.setEntityType(new EntityType(entityTypeId));
            entity.setIdentityAttributes(data);
        }

        entityInstanceRepository.save(entity);
        return entity.getId();
    }
}
