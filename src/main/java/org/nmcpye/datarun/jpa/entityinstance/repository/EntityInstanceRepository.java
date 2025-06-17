package org.nmcpye.datarun.jpa.entityinstance.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityType.EntityType;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing EntityInstance entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@SuppressWarnings("unused")
@Repository
public interface EntityInstanceRepository
    extends JpaIdentifiableRepository<EntityInstance> {
    // Find entity instances by their type
    List<EntityInstance> findByEntityType(EntityType entityType);
//
//    @Query(value = "SELECT * FROM entity_instance " +
//        "WHERE entity_type_id = :entityTypeId " +
//        "AND identity_attributes ->> :key = :value " +
//        "LIMIT 1", nativeQuery = true)
//    Optional<EntityInstance> findByEntityTypeIdAndAttributesJsonField(
//        @Param("entityTypeId") String entityTypeId,
//        @Param("key") String key,
//        @Param("value") String value);

    Boolean existsByEntityTypeIdAndId(String entityType, String uid);

    /**
     * Lookup an EntityInstance by one of its identity attributes.
     * Example: find the batch whose identity_attributes->>'batchNumber' = 'B100'
     */
    @Query(value =
        "SELECT * FROM entity_instance ei " +
            " WHERE ei.identity_attributes->>:key = :value",
        nativeQuery = true)
    Optional<EntityInstance> findByIdentityAttribute(
        @Param("key") String attributeKey,
        @Param("value") String attributeValue);

    /**
     * Find all instances of a given EntityType.
     */
    List<EntityInstance> findByEntityTypeId(String entityTypeId);

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
