package org.nmcpye.datarun.entityType.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.entityType.EntityType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EntityTypeRepository
    extends JpaAuditableRepository<EntityType> {
    Optional<EntityType> findByNameIgnoreCase(String name);
}
