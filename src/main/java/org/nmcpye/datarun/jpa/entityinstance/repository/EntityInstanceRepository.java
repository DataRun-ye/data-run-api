package org.nmcpye.datarun.jpa.entityinstance.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EntityInstanceRepository
    extends JpaIdentifiableRepository<EntityInstance> {
    Boolean existsByEntityTypeUidAndUid(String entityType, String uid);

    @Override
    default List<EntityInstance> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }

    @Override
    default Optional<EntityInstance> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default Optional<EntityInstance> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<EntityInstance> findByNameLike(String name) {
        return Collections.emptyList();
    }
}
