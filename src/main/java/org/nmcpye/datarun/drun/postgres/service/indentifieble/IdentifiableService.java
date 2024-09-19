package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Optional;

public interface IdentifiableService<T extends IdentifiableObject<ID>,
    ID extends Serializable> {

    T saveWithRelations(T object);

    boolean existsById(ID id);
    boolean existsByUid(String uid);

    Optional<T> findByUid(String uid);

    Optional<T> findOneByCode(String code);

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
     * Get all the chvSessions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<T> findAllByUser(Pageable pageable);

    /**
     * Get the "id" object.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<T> findOne(ID id);

    /**
     * Delete the "id" object.
     *
     * @param id the id of the entity.
     */
    void delete(ID id);
}
