package org.nmcpye.datarun.team.repository;

import org.nmcpye.datarun.team.TeamFormAccess;
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
