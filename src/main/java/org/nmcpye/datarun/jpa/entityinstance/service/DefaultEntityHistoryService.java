package org.nmcpye.datarun.jpa.entityinstance.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.entityinstance.EntityHistory;
import org.nmcpye.datarun.jpa.entityinstance.repository.EntityHistoryRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class DefaultEntityHistoryService
    extends DefaultJpaIdentifiableService<EntityHistory>
    implements EntityHistoryService {
    private final EntityHistoryRepository entityInstanceRepository;

    public DefaultEntityHistoryService(EntityHistoryRepository repository,
                                       CacheManager cacheManager,
                                       UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
        this.entityInstanceRepository = repository;
    }

    @Transactional
    @Override
    public void recordHistory(String entityId, String flowInstanceId, String stageSubmissionId, String stage, Map<String, Object> data) {

    }
}
