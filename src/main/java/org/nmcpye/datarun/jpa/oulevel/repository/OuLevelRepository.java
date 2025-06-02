package org.nmcpye.datarun.jpa.oulevel.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.oulevel.OuLevel;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OuLevelRepository
    extends JpaAuditableRepository<OuLevel> {
}
