package org.nmcpye.datarun.service;

import java.util.List;
import java.util.Optional;
import org.nmcpye.datarun.domain.WarehouseItem;

/**
 * Service Interface for managing {@link org.nmcpye.datarun.domain.WarehouseItem}.
 */
public interface WarehouseItemService {
    /**
     * Save a warehouseItem.
     *
     * @param warehouseItem the entity to save.
     * @return the persisted entity.
     */
    WarehouseItem save(WarehouseItem warehouseItem);

    /**
     * Updates a warehouseItem.
     *
     * @param warehouseItem the entity to update.
     * @return the persisted entity.
     */
    WarehouseItem update(WarehouseItem warehouseItem);

    /**
     * Partially updates a warehouseItem.
     *
     * @param warehouseItem the entity to update partially.
     * @return the persisted entity.
     */
    Optional<WarehouseItem> partialUpdate(WarehouseItem warehouseItem);

    /**
     * Get all the warehouseItems.
     *
     * @return the list of entities.
     */
    List<WarehouseItem> findAll();

    /**
     * Get the "id" warehouseItem.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<WarehouseItem> findOne(Long id);

    /**
     * Delete the "id" warehouseItem.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
