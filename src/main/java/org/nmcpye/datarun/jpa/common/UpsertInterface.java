package org.nmcpye.datarun.jpa.common;

import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.security.CurrentUserDetails;

import java.util.Collection;
import java.util.List;

public interface UpsertInterface<T extends JpaIdentifiableObject> {

    /**
     * Performs an "upsert" operation for a single entity.
     * Delegates entirely to {@link #upsertAll(Collection, CurrentUserDetails, EntitySaveSummaryVM)} for consistent logic and zero duplication.
     * All input validation (null entity, null UID) is handled by {@code upsertAll}.
     *
     * @param entity The entity data to upsert.
     * @return The managed entity after the operation.
     * @throws IllegalArgumentException if the entity is null or its UID is null (thrown by upsertAll).
     * @throws IllegalStateException    if the underlying bulk operation fails to return the expected single result.
     */
    T upsert(T entity, CurrentUserDetails user, EntitySaveSummaryVM summary);

    /**
     * Performs a bulk "upsert" operation for a list of entities.
     * Optimizes by fetching all potentially existing entities by UID in a single query.
     * This method orchestrates the application of lifecycle hooks and event publishing.
     *
     * @param entities The list of entities to upsert. Can be null or empty, in which case an empty list is returned.
     * @return A list of managed entities after the bulk operation.
     * The order of entities in the returned list attempts to match the input list's order.
     * @throws IllegalArgumentException if any entity in the list is null or has a null UID.
     */
    List<T> upsertAll(Collection<T> entities, CurrentUserDetails user, EntitySaveSummaryVM summary);

    // --- Protected Lifecycle Hooks (Default Implementations) ---
    // These remain the same, providing default no-ops or event publishing

    void beforeUpsertChecks(T entity, boolean isNew);

    void beforePersist(T entity);

    void afterPersist(T entity);

    void beforeUpdate(T existingEntity, T incomingEntity);

    void afterUpdate(T entity);

    void updateEntityFields(T existingEntity, T incomingEntity);

    void delete(T entity);

    void beforeDelete(T entity);
}
