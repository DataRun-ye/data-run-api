package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface IdentifiableService<T extends Identifiable<ID>,
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
    <Q extends QueryRequest> Page<T> findAllByUser(Pageable pageable, Q queryRequest);


    <Q extends QueryRequest> List<T> findAllByUser(Q queryRequest);
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
    <Q extends QueryRequest> Page<T> findAll(Pageable pageable, Q queryRequest);

    <Q extends QueryRequest> List<T> findAll(Q queryRequest);

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
