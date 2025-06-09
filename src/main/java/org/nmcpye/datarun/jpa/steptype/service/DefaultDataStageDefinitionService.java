package org.nmcpye.datarun.jpa.steptype.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaSoftDeleteService;
import org.nmcpye.datarun.jpa.steptype.StepType;
import org.nmcpye.datarun.jpa.steptype.repository.DataStageDefinitionRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link StepType}.
 */
@Service
@Transactional
public class DefaultDataStageDefinitionService
    extends DefaultJpaSoftDeleteService<StepType>
    implements DataStageDefinitionService {

    public DefaultDataStageDefinitionService(DataStageDefinitionRepository repository,
                                             CacheManager cacheManager,
                                             UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
