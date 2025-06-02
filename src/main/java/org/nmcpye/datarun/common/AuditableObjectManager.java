package org.nmcpye.datarun.common;

import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.translation.Translation;
import org.nmcpye.datarun.security.CurrentUserDetails;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.*;

public interface AuditableObjectManager {
    void save(AuditableObject<?> object);

    void save(AuditableObject<?> object, boolean clearSharing);

    void save(List<AuditableObject<?>> objects);

    void update(AuditableObject<?> object);

    void update(AuditableObject<?> object, CurrentUserDetails user);

    void update(List<AuditableObject<?>> objects);

    void update(List<AuditableObject<?>> objects, CurrentUserDetails user);

    void delete(AuditableObject<?> object);

    void delete(AuditableObject<?> object, CurrentUserDetails user);

    /**
     * Lookup objects of unknown type.
     * <p>
     * If the type is known at compile time this method should not be used.
     * Instead, use
     * {@link AuditableObjectManager#get(Class, String)}.
     *
     * @param uid a UID of an object of unknown type
     * @return The {@link AuditableObject<?>} with the given UID
     */
    @Nonnull
    Optional<? extends AuditableObject<?>> find(@Nonnull String uid);

    /**
     * Lookup objects of a specific type by database ID.
     *
     * @param type the object class type.
     * @param id   object's database ID
     * @return the found object
     */
    @CheckForNull
    <T extends AuditableObject<?>> T get(@Nonnull Class<T> type, Long id);

    /**
     * Retrieves the object of the given type and UID, or null if no object
     * exists.
     *
     * @param type the object class type.
     * @param uid  the UID.
     * @return the object with the given UID.
     */
    @CheckForNull
    <T extends AuditableObject<?>> T get(@Nonnull Class<T> type, @Nonnull String uid);

    /**
     * Retrieves the object of the given type and UID, throws exception if no
     * object exists.
     *
     * @param type the object class type.
     * @param uid  the UID.
     * @return the object with the given UID.
     * @throws IllegalQueryException if no object exists.
     */
    @Nonnull
    <T extends AuditableObject<?>> T load(@Nonnull Class<T> type, @Nonnull String uid) throws IllegalQueryException;

    /**
     * Retrieves the object of the given type and UID, throws exception using
     * the given error code if no object exists.
     *
     * @param type      the object class type.
     * @param errorCode the {@link ErrorCode} to use for the exception.
     * @param uid       the UID.
     * @return the object with the given UID.
     * @throws IllegalQueryException if no object exists.
     */
    @Nonnull
    <T extends AuditableObject<?>> T load(@Nonnull Class<T> type, @Nonnull ErrorCode errorCode, @Nonnull String uid)
        throws IllegalQueryException;

    <T extends AuditableObject<?>> boolean exists(@Nonnull Class<T> type, @Nonnull String uid);

    @CheckForNull
    <T extends AuditableObject<?>> T get(@Nonnull Collection<Class<? extends T>> types, @Nonnull String uid);

    @CheckForNull
    <T extends AuditableObject<?>> T get(@Nonnull Collection<Class<? extends T>> types, @Nonnull IdScheme idScheme, @Nonnull String value);

    /**
     * Retrieves the object of the given type and code, or null if no object
     * exists.
     *
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     */
    @CheckForNull
    <T extends AuditableObject<?>> T getByCode(@Nonnull Class<T> type, @Nonnull String code);

    /**
     * Retrieves the object of the given type and code, throws exception if no
     * object exists.
     *
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     * @throws IllegalQueryException if no object exists.
     */
    @Nonnull
    <T extends AuditableObject<?>> T loadByCode(@Nonnull Class<T> type, @Nonnull String code) throws IllegalQueryException;

    @Nonnull
    <T extends AuditableObject<?>> List<T> getByCode(@Nonnull Class<T> type, @Nonnull Collection<String> codes);

    @CheckForNull
    <T extends AuditableObject<?>> T getByName(@Nonnull Class<T> type, @Nonnull String name);

    <T extends AuditableObject<?>> T search(Class<T> clazz, String query);

    <T extends AuditableObject<?>> List<T> filter(Class<T> clazz, String query);

    <T extends AuditableObject<?>> List<T> getAll(Class<T> clazz);

    <T extends AuditableObject<?>> List<T> getDataWriteAll(Class<T> clazz);

    <T extends AuditableObject<?>> List<T> getDataReadAll(Class<T> clazz);

    <T extends AuditableObject<?>> List<T> getAllSorted(Class<T> clazz);

    <T extends AuditableObject<?>> List<T> getByUid(Class<T> clazz, Collection<String> uids);

    /**
     * Retrieves the objects of the given type and collection of UIDs, throws
     * exception is any object does not exist.
     *
     * @param types the object class type.
     * @param uids  the collection of UIDs.
     * @return a list of objects.
     * @throws IllegalQueryException if any object does not exist.
     */

    @Nonnull
    <T extends AuditableObject<?>> List<T> getByUid(@Nonnull Collection<Class<? extends T>> types, @Nonnull Collection<String> uids);

    @Nonnull
    <T extends AuditableObject<?>> List<T> loadByUid(@Nonnull Class<T> type, @CheckForNull Collection<String> uids)
        throws IllegalQueryException;

    <T extends AuditableObject<?>> List<T> getById(Class<T> clazz, Collection<Long> ids);

    <T extends AuditableObject<?>> List<T> getOrdered(Class<T> clazz, IdScheme idScheme, Collection<String> values);

    <T extends AuditableObject<?>> List<T> getByUidOrdered(Class<T> clazz, List<String> uids);

    <T extends AuditableObject<?>> List<T> getLikeName(Class<T> clazz, String name);

    <T extends AuditableObject<?>> List<T> getLikeName(Class<T> clazz, String name, boolean caseSensitive);

    <T extends AuditableObject<?>> List<T> getBetweenSorted(Class<T> clazz, int first, int max);

    <T extends AuditableObject<?>> List<T> getBetweenLikeName(Class<T> clazz, Set<String> words, int first, int max);

    <T extends AuditableObject<?>> Instant getLastUpdated(Class<T> clazz);

    <T extends AuditableObject<?>> Map<String, T> getIdMap(Class<T> clazz, IdentifiableProperty property);

    <T extends AuditableObject<?>> Map<String, T> getIdMap(Class<T> clazz, IdScheme idScheme);

    <T extends AuditableObject<?>> Map<String, T> getIdMapNoAcl(Class<T> clazz, IdentifiableProperty property);

    <T extends AuditableObject<?>> Map<String, T> getIdMapNoAcl(Class<T> clazz, IdScheme idScheme);

    <T extends AuditableObject<?>> List<T> getObjects(Class<T> clazz, IdentifiableProperty property, Collection<String> identifiers);

    <T extends AuditableObject<?>> List<T> getObjects(Class<T> clazz, Collection<Long> identifiers);

    <T extends AuditableObject<?>> T getObject(Class<T> clazz, IdentifiableProperty property, String value);

    <T extends AuditableObject<?>> T getObject(Class<T> clazz, IdScheme idScheme, String value);

    AuditableObject<?> getObject(String uid, String simpleClassName);

    AuditableObject<?> getObject(Long id, String simpleClassName);

    <T extends AuditableObject<?>> int getCount(Class<T> clazz);

    /**
     * Resets all properties that are not owned by the object type.
     *
     * @param object object to reset
     */
    void resetNonOwnerProperties(Object object);

    void flush();

    void clear();

    void evict(Object object);

    void updateTranslations(AuditableObject<?> persistedObject, Set<Translation> translations);

    <T extends AuditableObject<?>> List<T> getNoAcl(Class<T> clazz, Collection<String> uids);

    //    boolean isDefault(AuditableObject<?> object);

    List<String> getUidsCreatedBefore(Class<? extends AuditableObject<?>> klass, Date date);

    // -------------------------------------------------------------------------
    // NO ACL
    // -------------------------------------------------------------------------

    <T extends AuditableObject<?>> T getNoAcl(Class<T> clazz, String uid);

    <T extends AuditableObject<?>> void updateNoAcl(T object);

    <T extends AuditableObject<?>> List<T> getAllNoAcl(Class<T> clazz);
}
