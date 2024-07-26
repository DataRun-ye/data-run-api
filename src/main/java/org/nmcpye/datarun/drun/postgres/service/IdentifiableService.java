package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.ChvRegister;
import org.nmcpye.datarun.domain.common.IdentifiableObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IdentifiableService<T extends IdentifiableObject> {

    default T saveWithRelations(T object) {
        return save(object);
    }

    boolean existsByUid(String uid);

    Optional<T> findByUid(String uid);

    void deleteByUid(String uid);

    /**
     * Save an object.
     *
     * @param object the entity to save.
     * @return the persisted entity.
     */
    T save(T object);

    /**
     * Updates a object.
     *
     * @param object the entity to update.
     * @return the persisted entity.
     */
    T update(T object);

    /**
     * Partially updates a object.
     *
     * @param object the entity to update partially.
     * @return the persisted entity.
     */
    Optional<T> partialUpdate(T object);

    /**
     * Get all the chvSessions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Get all the chvSessions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<T> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" object.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<T> findOne(Long id);

    /**
     * Delete the "id" object.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
