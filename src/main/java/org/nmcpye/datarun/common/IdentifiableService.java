package org.nmcpye.datarun.common;

import org.nmcpye.datarun.drun.postgres.common.IdentifiableEntity;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface IdentifiableService<T extends IdentifiableEntity<ID>,
    ID extends Serializable> {

    T saveWithRelations(T object);

    boolean existsById(ID id);

    boolean existsByUid(String uid);

    Optional<T> findByUid(String uid);

    void deleteByUid(String uid);

    /**
     * Get all the chvSessions with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<T> findAllByUser(Pageable pageable, QueryRequest queryRequest);


    List<T> findAllByUser(QueryRequest queryRequest);

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
     * Delete the "id" object.
     *
     * @param id the id of the entity.
     */
    void delete(ID id);
}
