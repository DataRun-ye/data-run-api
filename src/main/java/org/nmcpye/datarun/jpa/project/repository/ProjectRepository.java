package org.nmcpye.datarun.jpa.project.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.project.Project;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Project entity.
 *
 * @author Hamza Assada 11/02/2022
 */
@SuppressWarnings("unused")
@Repository
public interface ProjectRepository
    extends JpaIdentifiableRepository<Project> {
}
