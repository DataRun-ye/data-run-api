package org.nmcpye.datarun.service;

import org.nmcpye.datarun.drun.postgres.domain.OuLevel;

import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link OuLevel}.
 */
public interface OuLevelService {
    /**
     * Save a ouLevel.
     *
     * @param ouLevel the entity to save.
     * @return the persisted entity.
     */
    OuLevel save(OuLevel ouLevel);

    /**
     * Updates a ouLevel.
     *
     * @param ouLevel the entity to update.
     * @return the persisted entity.
     */
    OuLevel update(OuLevel ouLevel);

    /**
     * Partially updates a ouLevel.
     *
     * @param ouLevel the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OuLevel> partialUpdate(OuLevel ouLevel);

    /**
     * Get all the ouLevels.
     *
     * @return the list of entities.
     */
    List<OuLevel> findAll();

    /**
     * Get the "id" ouLevel.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OuLevel> findOne(Long id);

    /**
     * Delete the "id" ouLevel.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
