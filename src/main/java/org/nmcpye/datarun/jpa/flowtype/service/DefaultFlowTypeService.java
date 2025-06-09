package org.nmcpye.datarun.jpa.flowtype.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.nmcpye.datarun.jpa.flowtype.repository.FlowTypeRepository;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultFlowTypeService
        extends DefaultJpaIdentifiableService<FlowType>
        implements FlowTypeService {

    private final FlowTypeRepository repository;

    public DefaultFlowTypeService(FlowTypeRepository repository,
                                  UserAccessService userAccessService,
                                  CacheManager cacheManager) {
        super(repository, cacheManager, userAccessService);
        this.repository = repository;
    }
}
