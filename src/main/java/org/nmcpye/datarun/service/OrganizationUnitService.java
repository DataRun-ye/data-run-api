package org.nmcpye.datarun.service;

import java.util.Optional;
import org.nmcpye.datarun.domain.OrganizationUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link org.nmcpye.datarun.domain.OrganizationUnit}.
 */
public interface OrganizationUnitService {
    /**
     * Save a organizationUnit.
     *
     * @param organizationUnit the entity to save.
     * @return the persisted entity.
     */
    OrganizationUnit save(OrganizationUnit organizationUnit);

    /**
     * Updates a organizationUnit.
     *
     * @param organizationUnit the entity to update.
     * @return the persisted entity.
     */
    OrganizationUnit update(OrganizationUnit organizationUnit);

    /**
     * Partially updates a organizationUnit.
     *
     * @param organizationUnit the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OrganizationUnit> partialUpdate(OrganizationUnit organizationUnit);

    /**
     * Get all the organizationUnits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OrganizationUnit> findAll(Pageable pageable);

    /**
     * Get all the organizationUnits with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OrganizationUnit> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" organizationUnit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OrganizationUnit> findOne(Long id);

    /**
     * Delete the "id" organizationUnit.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
