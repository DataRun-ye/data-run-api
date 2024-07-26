package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.Project;
import org.nmcpye.datarun.repository.ProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProjectRepositoryCustom
    extends ProjectRepository {
    Optional<Project> findByUid(String uid);

    void deleteByUid(String uid);
}
