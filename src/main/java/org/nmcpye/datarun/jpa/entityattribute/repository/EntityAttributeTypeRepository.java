package org.nmcpye.datarun.jpa.entityattribute.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EntityAttributeTypeRepository
    extends JpaIdentifiableRepository<EntityAttributeType> {
    Optional<EntityAttributeType> findByNameIgnoreCase(String name);
}
