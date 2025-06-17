package org.nmcpye.datarun.jpa.entityattribute.repository;

import org.nmcpye.datarun.jpa.common.BaseJpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.entityattribute.EntityAttributeValue;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for managing EntityAttributeValue entities.
 *
 * @author Hamza Assada 15/06/2025 <7amza.it@gmail.com>
 */
@Repository
public interface EntityAttributeValueRepository
    extends BaseJpaIdentifiableRepository<EntityAttributeValue, Long> {

    /**
     * Find attribute values for a specific entity instance
     *
     * @param entityInstance entity Instance id to look for
     * @return list of found Entity Attribute values
     */
    List<EntityAttributeValue> findByEntityInstance(EntityInstance entityInstance);

    /**
     * Find attribute values by attribute type and value
     *
     * @param attributeTypeId attribute type id to look for
     * @param value           value to look for
     * @return list of found Entity Attribute values
     */
    List<EntityAttributeValue> findByEntityAttributeIdAndValue(String attributeTypeId, String value);
}
