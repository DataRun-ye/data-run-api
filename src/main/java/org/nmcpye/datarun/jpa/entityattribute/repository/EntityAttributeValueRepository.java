package org.nmcpye.datarun.jpa.entityattribute.repository;

import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link EntityAttributeValue}.
 */
@Repository
public interface EntityAttributeValueRepository
        extends JpaRepository<EntityAttributeValue, Long> {
}
