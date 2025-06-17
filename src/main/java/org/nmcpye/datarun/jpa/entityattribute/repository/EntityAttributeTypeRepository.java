package org.nmcpye.datarun.jpa.entityattribute.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing EntityAttributeType entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@SuppressWarnings("unused")
@Repository
public interface EntityAttributeTypeRepository
    extends JpaIdentifiableRepository<EntityAttributeType> {
    Optional<EntityAttributeType> findByEntityTypeNameAndEntityType(String name, EntityType entityType);

    List<EntityAttributeType> findByEntityType(EntityType entityType);

    Optional<EntityAttributeType> findByEntityTypeNameIgnoreCase(String name);

    Optional<EntityAttributeType> findByNameIgnoreCase(String name);
}
