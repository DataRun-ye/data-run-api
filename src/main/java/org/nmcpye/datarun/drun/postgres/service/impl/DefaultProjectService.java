package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.common.jpa.impl.DefaultJpaAuditableService;
import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRepository;
import org.nmcpye.datarun.drun.postgres.service.ProjectService;
import org.nmcpye.datarun.security.useraccess.UserAccessService;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultProjectService extends DefaultJpaAuditableService<Project> implements ProjectService {

    public DefaultProjectService(ProjectRepository repository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
