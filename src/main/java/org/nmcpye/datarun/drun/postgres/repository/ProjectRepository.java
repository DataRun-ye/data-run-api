package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaIdentifiableRepository;
import org.nmcpye.datarun.domain.Project;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProjectRepository
    extends JpaIdentifiableRepository<Project> {
}
