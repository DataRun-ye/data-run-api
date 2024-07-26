package org.nmcpye.datarun.service;

import java.util.List;
import java.util.Optional;
import org.nmcpye.datarun.domain.ChvSupply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link org.nmcpye.datarun.domain.ChvSupply}.
 */
public interface ChvSupplyService {
    /**
     * Save a chvSupply.
     *
     * @param chvSupply the entity to save.
     * @return the persisted entity.
     */
    ChvSupply save(ChvSupply chvSupply);

    /**
     * Updates a chvSupply.
     *
     * @param chvSupply the entity to update.
     * @return the persisted entity.
     */
    ChvSupply update(ChvSupply chvSupply);

    /**
     * Partially updates a chvSupply.
     *
     * @param chvSupply the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ChvSupply> partialUpdate(ChvSupply chvSupply);

    /**
     * Get all the chvSupplies.
     *
     * @return the list of entities.
     */
    List<ChvSupply> findAll();

    /**
     * Get all the chvSupplies with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ChvSupply> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" chvSupply.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ChvSupply> findOne(Long id);

    /**
     * Delete the "id" chvSupply.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
