package org.nmcpye.datarun.jpa.common;

import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.common.IdentifiableProperty;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.security.CurrentUserDetails;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.*;

public interface IdentifiableObjectManager {
    void save(JpaIdentifiableObject object);

//    void save(JpaIdentifiableObject object, boolean clearSharing);

    void save(List<JpaIdentifiableObject> objects);

    void update(JpaIdentifiableObject object);

    void update(JpaIdentifiableObject object, CurrentUserDetails user);

    void update(List<JpaIdentifiableObject> objects);

    void update(List<JpaIdentifiableObject> objects, CurrentUserDetails user);

    void delete(JpaIdentifiableObject object);

    void delete(JpaIdentifiableObject object, CurrentUserDetails user);

    /**
     * Lookup objects of unknown type.
     * <p>
     * If the type is known at compile time this method should not be used.
     * Instead, use
     * {@link IdentifiableObjectManager#get(Class, String)}.
     *
     * @param uid a UID of an object of unknown type
     * @return The {@link JpaIdentifiableObject} with the given UID
     */
    @Nonnull
    Optional<? extends JpaIdentifiableObject> find(@Nonnull String uid);

    /**
     * Lookup objects of a specific type by database ID.
     *
     * @param type the object class type.
     * @param id   object's database ID
     * @return the found object
     */
    @CheckForNull
    <T extends JpaIdentifiableObject> T get(@Nonnull Class<T> type, Long id);

    /**
     * Retrieves the object of the given type and UID, or null if no object
     * exists.
     *
     * @param type the object class type.
     * @param uid  the UID.
     * @return the object with the given UID.
     */
    @CheckForNull
    <T extends JpaIdentifiableObject> T get(@Nonnull Class<T> type, @Nonnull String uid);

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
    <T extends JpaIdentifiableObject> T load(@Nonnull Class<T> type, @Nonnull String uid) throws IllegalQueryException;

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
    <T extends JpaIdentifiableObject> T load(@Nonnull Class<T> type, @Nonnull ErrorCode errorCode, @Nonnull String uid)
        throws IllegalQueryException;

    <T extends JpaIdentifiableObject> boolean exists(@Nonnull Class<T> type, @Nonnull String uid);

    @CheckForNull
    <T extends JpaIdentifiableObject> T get(@Nonnull Collection<Class<? extends T>> types, @Nonnull String uid);

    @CheckForNull
    <T extends JpaIdentifiableObject> T get(@Nonnull Collection<Class<? extends T>> types, @Nonnull IdScheme idScheme, @Nonnull String value);

    /**
     * Retrieves the object of the given type and code, or null if no object
     * exists.
     *
     * @param type the object class type.
     * @param code the code.
     * @return the object with the given code.
     */
    @CheckForNull
    <T extends JpaIdentifiableObject> T getByCode(@Nonnull Class<T> type, @Nonnull String code);

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
    <T extends JpaIdentifiableObject> T loadByCode(@Nonnull Class<T> type, @Nonnull String code) throws IllegalQueryException;

    @Nonnull
    <T extends JpaIdentifiableObject> List<T> getByCode(@Nonnull Class<T> type, @Nonnull Collection<String> codes);

    @CheckForNull
    <T extends JpaIdentifiableObject> T getByName(@Nonnull Class<T> type, @Nonnull String name);

    <T extends JpaIdentifiableObject> T search(Class<T> clazz, String query);

    <T extends JpaIdentifiableObject> List<T> filter(Class<T> clazz, String query);

    <T extends JpaIdentifiableObject> List<T> getAll(Class<T> clazz);

//    <T extends JpaIdentifiableObject> List<T> getDataWriteAll(Class<T> clazz);
//
//    <T extends JpaIdentifiableObject> List<T> getDataReadAll(Class<T> clazz);
//
//    <T extends JpaIdentifiableObject> List<T> getAllSorted(Class<T> clazz);

    <T extends JpaIdentifiableObject> List<T> getByUid(Class<T> clazz, Collection<String> uids);

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
    <T extends JpaIdentifiableObject> List<T> getByUid(@Nonnull Collection<Class<? extends T>> types, @Nonnull Collection<String> uids);

    @Nonnull
    <T extends JpaIdentifiableObject> List<T> loadByUid(@Nonnull Class<T> type, @CheckForNull Collection<String> uids)
        throws IllegalQueryException;

    <T extends JpaIdentifiableObject> List<T> getById(Class<T> clazz, Collection<Long> ids);

//    <T extends JpaIdentifiableObject> List<T> getOrdered(Class<T> clazz, IdScheme idScheme, Collection<String> values);

//    <T extends JpaIdentifiableObject> List<T> getByUidOrdered(Class<T> clazz, List<String> uids);

    <T extends JpaIdentifiableObject> List<T> getLikeName(Class<T> clazz, String name);

    <T extends JpaIdentifiableObject> List<T> getLikeName(Class<T> clazz, String name, boolean caseSensitive);

//    <T extends JpaIdentifiableObject> List<T> getBetweenSorted(Class<T> clazz, int first, int max);
//
//    <T extends JpaIdentifiableObject> List<T> getBetweenLikeName(Class<T> clazz, Set<String> words, int first, int max);
//
//    <T extends JpaIdentifiableObject> Instant getLastUpdated(Class<T> clazz);

    <T extends JpaIdentifiableObject> Map<String, T> getIdMap(Class<T> clazz, IdentifiableProperty property);

    <T extends JpaIdentifiableObject> Map<String, T> getIdMap(Class<T> clazz, IdScheme idScheme);

    <T extends JpaIdentifiableObject> Map<String, T> getIdMapNoAcl(Class<T> clazz, IdentifiableProperty property);

    <T extends JpaIdentifiableObject> Map<String, T> getIdMapNoAcl(Class<T> clazz, IdScheme idScheme);

    <T extends JpaIdentifiableObject> List<T> getObjects(Class<T> clazz, IdentifiableProperty property, Collection<String> identifiers);

    <T extends JpaIdentifiableObject> List<T> getObjects(Class<T> clazz, Collection<Long> identifiers);

    <T extends JpaIdentifiableObject> T getObject(Class<T> clazz, IdentifiableProperty property, String value);

    <T extends JpaIdentifiableObject> T getObject(Class<T> clazz, IdScheme idScheme, String value);

    JpaIdentifiableObject getObject(String uid, String simpleClassName);

    JpaIdentifiableObject getObject(Long id, String simpleClassName);

    <T extends JpaIdentifiableObject> int getCount(Class<T> clazz);
//
//    /**
//     * Resets all properties that are not owned by the object type.
//     *
//     * @param object object to reset
//     */
//    void resetNonOwnerProperties(Object object);

//    void flush();

//    void clear();

//    void evict(Object object);

//    void updateTranslations(JpaIdentifiableObject persistedObject, Set<Translation> translations);

    <T extends JpaIdentifiableObject> List<T> getNoAcl(Class<T> clazz, Collection<String> uids);

    //    boolean isDefault(JpaIdentifiableObject object);

    List<String> getUidsCreatedBefore(Class<? extends JpaIdentifiableObject> klass, Date date);

    // -------------------------------------------------------------------------
    // NO ACL
    // -------------------------------------------------------------------------

    <T extends JpaIdentifiableObject> T getNoAcl(Class<T> clazz, String uid);

    <T extends JpaIdentifiableObject> void updateNoAcl(T object);

    <T extends JpaIdentifiableObject> List<T> getAllNoAcl(Class<T> clazz);
}
