package org.nmcpye.datarun.entityinstance.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.entityinstance.EntityInstance;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EntityInstanceRepository
    extends JpaAuditableRepository<EntityInstance> {
    Optional<EntityInstance> findByNameIgnoreCase(String name);
}
