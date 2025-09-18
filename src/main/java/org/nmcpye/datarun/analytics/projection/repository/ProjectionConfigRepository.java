package org.nmcpye.datarun.analytics.projection.repository;

import org.nmcpye.datarun.analytics.projection.dto.ProjectionConfig;
import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectionConfigRepository
        extends BaseJpaIdentifiableRepository<ProjectionConfig, String> {

    ProjectionConfig findByUid(String uid);
}
