//package org.nmcpye.datarun.service;
//
//import java.util.Optional;
//import org.nmcpye.datarun.domain.Warehouse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//
///**
// * Service Interface for managing {@link org.nmcpye.datarun.domain.Warehouse}.
// */
//public interface WarehouseService {
//    /**
//     * Save a warehouse.
//     *
//     * @param warehouse the entity to save.
//     * @return the persisted entity.
//     */
//    Warehouse save(Warehouse warehouse);
//
//    /**
//     * Updates a warehouse.
//     *
//     * @param warehouse the entity to update.
//     * @return the persisted entity.
//     */
//    Warehouse update(Warehouse warehouse);
//
//    /**
//     * Partially updates a warehouse.
//     *
//     * @param warehouse the entity to update partially.
//     * @return the persisted entity.
//     */
//    Optional<Warehouse> partialUpdate(Warehouse warehouse);
//
//    /**
//     * Get all the warehouses.
//     *
//     * @param pageable the pagination information.
//     * @return the list of entities.
//     */
//    Page<Warehouse> findAll(Pageable pageable);
//
//    /**
//     * Get all the warehouses with eager load of many-to-many relationships.
//     *
//     * @param pageable the pagination information.
//     * @return the list of entities.
//     */
//    Page<Warehouse> findAllWithEagerRelationships(Pageable pageable);
//
//    /**
//     * Get the "id" warehouse.
//     *
//     * @param id the id of the entity.
//     * @return the entity.
//     */
//    Optional<Warehouse> findOne(Long id);
//
//    /**
//     * Delete the "id" warehouse.
//     *
//     * @param id the id of the entity.
//     */
//    void delete(Long id);
//}
