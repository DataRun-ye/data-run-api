package org.nmcpye.datarun.common;

import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface AuditableObjectService<T extends AuditableObject<ID>, ID> {
    Class<T> getClazz();

    Optional<T> findByIdentifyingProperties(T identifiableObject);

    T saveWithRelations(T object);

    boolean existsByUid(String uid);

    Optional<T> findByUid(String uid);

    void deleteByUid(String uid);

    Page<T> findAllByUser(QueryRequest queryRequest);

//    List<T> findAllByUser(QueryRequest queryRequest);

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
}
