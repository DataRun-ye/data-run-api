package org.nmcpye.datarun.jpa.project.service;

import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.common.DefaultJpaIdentifiableService;
import org.nmcpye.datarun.jpa.project.Project;
import org.nmcpye.datarun.jpa.project.repository.ProjectRepository;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class DefaultProjectService extends DefaultJpaIdentifiableService<Project> implements ProjectService {

    public DefaultProjectService(ProjectRepository repository, CacheManager cacheManager, UserAccessService userAccessService) {
        super(repository, cacheManager, userAccessService);
    }
}
