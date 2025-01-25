package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ProjectServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ProjectServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<Project>
    implements ProjectServiceCustom {

    public ProjectServiceCustomImpl(ProjectRelationalRepositoryCustom repository,
                                    CacheManager cacheManager) {
        super(repository, cacheManager);
    }
}
