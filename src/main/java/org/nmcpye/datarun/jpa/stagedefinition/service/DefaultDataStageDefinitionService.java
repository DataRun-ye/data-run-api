package org.nmcpye.datarun.jpa.stagedefinition.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.stagedefinition.StageDefinition;
import org.nmcpye.datarun.jpa.stagedefinition.repository.StageDefinitionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link StageDefinition}.
 */
@Service
@Transactional
public class DefaultDataStageDefinitionService
    extends DefaultJpaSoftDeleteService<StageDefinition>
    implements DataStageDefinitionService {

    public DefaultDataStageDefinitionService(StageDefinitionRepository repository,
                                             CacheManager cacheManager,
                                             UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
