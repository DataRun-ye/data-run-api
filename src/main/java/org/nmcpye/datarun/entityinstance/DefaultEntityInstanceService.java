package org.nmcpye.datarun.entityinstance;

import org.nmcpye.datarun.common.impl.DefaultAuditableObjectService;
import org.nmcpye.datarun.entityinstance.repository.EntityInstanceRepository;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultEntityInstanceService
        extends DefaultAuditableObjectService<EntityInstance, Long>
        implements EntityInstanceService {

    private final EntityInstanceRepository repository;

    public DefaultEntityInstanceService(EntityInstanceRepository repository,
                                        UserAccessService userAccessService,
                                        CacheManager cacheManager) {
        super(repository, cacheManager);
        this.repository = repository;
    }

    @Override
    public Page<EntityInstance> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        return null;
    }
}
