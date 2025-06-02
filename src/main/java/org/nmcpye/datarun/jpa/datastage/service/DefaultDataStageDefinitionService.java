package org.nmcpye.datarun.jpa.datastage.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.datastage.DataStageDefinition;
import org.nmcpye.datarun.jpa.datastage.repository.DataStageDefinitionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link DataStageDefinition}.
 */
@Service
@Transactional
public class DefaultDataStageDefinitionService
    extends DefaultJpaSoftDeleteService<DataStageDefinition>
    implements DataStageDefinitionService {

    public DefaultDataStageDefinitionService(DataStageDefinitionRepository repository,
                                             CacheManager cacheManager,
                                             UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
