package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.TeamFormAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Hamza Assada, 16/04/2025
 */
@SuppressWarnings("unused")
@Repository
public interface TeamFormAccessRepository
    extends JpaRepository<TeamFormAccess, Long> {
}
