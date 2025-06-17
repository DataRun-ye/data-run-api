package org.nmcpye.datarun.jpa.entityType.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing EntityType entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
@SuppressWarnings("unused")
public interface EntityTypeRepository
    extends JpaIdentifiableRepository<EntityType> {
    Optional<EntityType> findByName(String name);

    Optional<EntityType> findByNameIgnoreCase(String name);
}
