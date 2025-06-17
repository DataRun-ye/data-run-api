package org.nmcpye.datarun.jpa.entityinstance.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.entityinstance.EntityInstance;

import java.util.Map;

public interface EntityInstanceService
    extends JpaIdentifiableObjectService<EntityInstance> {
    /**
     * <pre>
     * 1. Determines: if the entity already exists (based on identifying attributes in `data`).
     * 2. Updates: the existing `EntityInstance` if it exists.
     * 3. Inserts: a new `EntityInstance` otherwise.
     * </pre>
     *
     * @param entityTypeId entityTypeId of the entity
     * @param data         entityInstance identifying data
     * @return id of the upserted entity (new or existing).
     */
    String upsertEntity(String entityTypeId, Map<String, Object> data);
}
