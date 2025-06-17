package org.nmcpye.datarun.jpa.entityinstance.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityinstance.EntityHistory;
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
public interface EntityHistoryRepository
    extends JpaIdentifiableRepository<EntityHistory> {
    @Override
    default List<EntityHistory> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }

    @Override
    default Optional<EntityHistory> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default Optional<EntityHistory> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<EntityHistory> findByNameLike(String name) {
        return Collections.emptyList();
    }
}
