package org.nmcpye.datarun.repository;

import org.nmcpye.datarun.domain.OuLevel;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OuLevel entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OuLevelRepository extends JpaRepository<OuLevel, Long> {}
