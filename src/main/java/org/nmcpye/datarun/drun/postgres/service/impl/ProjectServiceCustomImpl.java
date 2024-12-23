package org.nmcpye.datarun.drun.postgres.service.impl;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.drun.postgres.common.DefaultIdentifiableSpecifications;
import org.nmcpye.datarun.drun.postgres.repository.ProjectRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.ProjectServiceCustom;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@Transactional
public class ProjectServiceCustomImpl
    extends DefaultIdentifiableSpecifications<Project>
    implements ProjectServiceCustom {
    final private ProjectRelationalRepositoryCustom repositoryCustom;

    public ProjectServiceCustomImpl(ProjectRelationalRepositoryCustom repositoryCustom) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
    }

    @Override
    public <Q extends QueryRequest> Page<Project> findAllByUser(Pageable pageable, Q queryRequest) {
        return repositoryCustom.findAll(pageable);
    }
}
