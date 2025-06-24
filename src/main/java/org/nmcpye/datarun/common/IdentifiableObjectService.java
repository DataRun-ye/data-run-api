package org.nmcpye.datarun.common;

import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * @author Hamza Assada 20/03/2025 <7amza.it@gmail.com>
 */
public interface IdentifiableObjectService<T extends IdentifiableObject<ID>, ID> {
    Class<T> getClazz();

    T saveWithRelations(T object);

    @Deprecated(since = "V7")
    boolean existsByUid(String uid);


    @Deprecated(since = "V7")
    Optional<T> findByUid(String uid);

    @Deprecated(since = "V7")
    void deleteByUid(String uid);

    boolean existsById(ID id);

    Optional<T> findById(ID id);

    void deleteById(ID id);

    Page<T> findAllByUser(QueryRequest queryRequest, String jsonQueryBody);

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
     * Delete an object.
     *
     * @param object the entity to delete.
     */
    void delete(T object);

    Optional<T> findByIdOrUid(T entity);
}
