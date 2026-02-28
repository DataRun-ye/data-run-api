package org.nmcpye.datarun.jpa.common;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.hibernate.proxy.HibernateProxy;
import org.nmcpye.datarun.common.CollectionUtils;
import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.common.IdentifiableObjectUtils;
import org.nmcpye.datarun.common.IdentifiableProperty;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Hamza Assada
 * @since 04/06/2025
 */
@Service
@Slf4j
public class DefaultIdentifiableObjectManager implements IdentifiableObjectManager {

    private final Set<JpaIdentifiableRepository<? extends JpaIdentifiableObject>> identifiableObjectStores;
    private final Map<Class<? extends JpaIdentifiableObject>,
        JpaIdentifiableRepository<? extends JpaIdentifiableObject>>
        identifiableObjectStoreCache = new ConcurrentHashMap<>();

    public DefaultIdentifiableObjectManager(Set<JpaIdentifiableRepository<? extends JpaIdentifiableObject>> identifiableObjectStores) {
        this.identifiableObjectStores = identifiableObjectStores;
    }


    @Transactional
    @Override
    @SuppressWarnings("unchecked")
    public void save(JpaIdentifiableObject object) {
        JpaIdentifiableRepository<JpaIdentifiableObject> store = getIdentifiableObjectStore(
            HibernateProxyUtils.getRealClass(object));
        if (store != null) {
            store.save(object);
        }
    }

//    @Override
//    public void save(JpaIdentifiableObject object, boolean clearSharing) {
//
//    }

    @Transactional
    @Override
    public void save(List<JpaIdentifiableObject> objects) {
        objects.forEach(this::save);
    }

    @Transactional
    @Override
    public void update(JpaIdentifiableObject object) {
        update(object, SecurityUtils.getCurrentUserDetailsOrNull());
    }

    @Transactional
    @Override
    public void update(JpaIdentifiableObject object, CurrentUserDetails user) {
        JpaIdentifiableRepository<? super JpaIdentifiableObject> store = getIdentifiableObjectStore(object);

        if (store != null) {
            store.save(object, user);
        }
    }

    @Transactional
    @Override
    public void update(List<JpaIdentifiableObject> objects) {
        update(objects, SecurityUtils.getCurrentUserDetailsOrNull());
    }

    @Transactional
    @Override
    public void update(List<JpaIdentifiableObject> objects, CurrentUserDetails user) {
        if (objects.isEmpty()) {
            return;
        }

        for (JpaIdentifiableObject object : objects) {
            update(object, user);
        }
    }

    @Transactional
    @Override
    public <T extends JpaIdentifiableObject> void updateNoAcl(T object) {
        JpaIdentifiableRepository<? super T> store = getIdentifiableObjectStore(object);

        if (store != null) {
            store.updateNoAcl(object);
        }
    }

    @Transactional
    @Override
    public void delete(JpaIdentifiableObject object) {
        delete(object, SecurityUtils.getCurrentUserDetailsOrNull());
    }

    @Transactional
    @Override
    public void delete(JpaIdentifiableObject object, CurrentUserDetails user) {

    }

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public Optional<? extends JpaIdentifiableObject> find(@Nonnull String uid) {
        return Optional.empty();
    }

    @Transactional(readOnly = true)
//    //    @CheckForNull
    @Override
    public <T extends JpaIdentifiableObject> T get(@Nonnull Class<T> type, @Nonnull String uid) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return null;
        }

        return store.findByIdOrUid(uid, uid).orElse(null);
    }

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public <T extends JpaIdentifiableObject> T load(@Nonnull Class<T> type, @Nonnull String uid) throws IllegalQueryException {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            throw new IllegalQueryException("No store registered for objects of type: " + type);
        }

        return store.findByUid(uid).orElseThrow();
    }

    @Transactional(readOnly = true)
    @Nonnull
    @Override
    public <T extends JpaIdentifiableObject> T load(@Nonnull Class<T> type, @Nonnull ErrorCode errorCode, @Nonnull String uid)
        throws IllegalQueryException {
        T object = get(type, uid);

        if (object == null) {
            throw new IllegalQueryException(new ErrorMessage(errorCode, uid));
        }

        return object;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> boolean exists(@Nonnull Class<T> type, @Nonnull String uid) {
        return get(type, uid) != null;
    }

//    //    @CheckForNull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> T get(@Nonnull Collection<Class<? extends T>> types, @Nonnull String uid) {
        return types.stream().map(type -> get(type, uid))
            .filter(Objects::nonNull).findFirst().orElse(null);
    }

//    //    @CheckForNull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> T get(
        @Nonnull Collection<Class<? extends T>> types,
        @Nonnull IdScheme idScheme,
        @Nonnull String identifier
    ) {
        return types.stream().map(type -> getObject(type, idScheme, identifier)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> getNoAcl(@Nonnull Class<T> type, @Nonnull Collection<String> uids) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return List.of();
        }

        return store.findAllByUidIn(uids);
    }

//    //    @CheckForNull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> T getByCode(@Nonnull Class<T> type, @Nonnull String code) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return null;
        }

        return store.findFirstByName(code).orElse(null);
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> T loadByCode(@Nonnull Class<T> type, @Nonnull String code) throws IllegalQueryException {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            throw new IllegalQueryException(ErrorCode.E1113, type.getSimpleName(), code);
        }

        return store.findFirstByCode(code).orElseThrow(() -> new IllegalQueryException(ErrorCode.E1113, type.getSimpleName(), code));
    }

//    //    @CheckForNull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> T getByName(@Nonnull Class<T> type, @Nonnull String name) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return null;
        }

        return store.findFirstByName(name).orElseThrow();
    }

//    //    @CheckForNull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> T search(@Nonnull Class<T> type, @Nonnull String query) {
        T object = get(type, query);

        if (object == null) {
            object = getByCode(type, query);
        }

        if (object == null) {
            object = getByName(type, query);
        }

        return object;
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> filter(@Nonnull Class<T> type, @Nonnull String query) {
        Set<T> uniqueObjects = new HashSet<>();

        T uidObject = get(type, query);

        if (uidObject != null) {
            uniqueObjects.add(uidObject);
        }

        T codeObject = getByCode(type, query);

        if (codeObject != null) {
            uniqueObjects.add(codeObject);
        }

        uniqueObjects.addAll(getLikeName(type, query, false));

        List<T> objects = new ArrayList<>(uniqueObjects);

//        Collections.sort(objects);

        return objects;
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> getAll(@Nonnull Class<T> type) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return List.of();
        }

        return Lists.newArrayList(store.findAll().iterator());
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> getByUid(@Nonnull Class<T> type, @Nonnull Collection<String> uids) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return List.of();
        }

        return store.findAllByUidIn(uids);
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> getByUid(Collection<Class<? extends T>> types, @Nonnull Collection<String> uids) {
        List<T> list = new ArrayList<>();

        for (Class<? extends T> type : types) {
            list.addAll(getByUid(type, uids));
        }
        return list;
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> loadByUid(@Nonnull Class<T> type, Collection<String> uids)
        throws IllegalQueryException {
        if (uids == null || uids.isEmpty()) {
            return List.of();
        }

        List<T> objects = getByUid(type, uids);

        List<String> identifiers = IdentifiableObjectUtils.getUids(objects);
        List<String> difference = CollectionUtils.difference(uids, identifiers);

        if (!difference.isEmpty()) {
            throw new IllegalQueryException(new ErrorMessage(ErrorCode.E1112, type.getSimpleName(), difference));
        }

        return objects;
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> getById(@Nonnull Class<T> type, @Nonnull Collection<String> ids) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return List.of();
        }

        return store.findById(ids);
    }

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends JpaIdentifiableObject> List<T> getByCode(@Nonnull Class<T> type, @Nonnull Collection<String> codes) {
        JpaIdentifiableRepository<T> store = getIdentifiableObjectStore(type);

        if (store == null) {
            return List.of();
        }

        return store.findAllByCodeIn(codes);
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> List<T> getLikeName(Class<T> clazz, String name) {
        return List.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> List<T> getLikeName(Class<T> clazz, String name, boolean caseSensitive) {
        return List.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> Map<String, T> getIdMap(Class<T> clazz, IdentifiableProperty property) {
        return Map.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> Map<String, T> getIdMap(Class<T> clazz, IdScheme idScheme) {
        return Map.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> Map<String, T> getIdMapNoAcl(Class<T> clazz, IdentifiableProperty property) {
        return Map.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> Map<String, T> getIdMapNoAcl(Class<T> clazz, IdScheme idScheme) {
        return Map.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> List<T> getObjects(Class<T> clazz, IdentifiableProperty property, Collection<String> identifiers) {
        return List.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> List<T> getObjects(Class<T> clazz, Collection<String> identifiers) {
        return List.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> T getObject(Class<T> clazz, IdentifiableProperty property, String value) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> T getObject(Class<T> clazz, IdScheme idScheme, String value) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public JpaIdentifiableObject getObject(String uid, String simpleClassName) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> int getCount(Class<T> clazz) {
        return 0;
    }


    @Transactional(readOnly = true)
    @Override
    public List<String> getUidsCreatedBefore(Class<? extends JpaIdentifiableObject> klass, Date date) {
        return List.of();
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> T getNoAcl(Class<T> clazz, String uid) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public <T extends JpaIdentifiableObject> List<T> getAllNoAcl(Class<T> clazz) {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private <T extends JpaIdentifiableObject> JpaIdentifiableRepository<T> getIdentifiableObjectStore(@Nonnull T object) {
        return getIdentifiableObjectStore((Class<T>) HibernateProxyUtils.getRealClass(object));
    }

    @SuppressWarnings("unchecked")
    private <T extends JpaIdentifiableObject> JpaIdentifiableRepository<T> getIdentifiableObjectStore(@Nonnull Class<T> type) {
        return (JpaIdentifiableRepository<T>) getObjectStore(type, identifiableObjectStoreCache, identifiableObjectStores);
    }

    private <T extends E, E extends JpaIdentifiableObject, S extends JpaIdentifiableRepository<? extends E>> S getObjectStore(
        Class<T> type,
        Map<Class<? extends E>, S> cache,
        Set<S> stores
    ) {
        @SuppressWarnings("unchecked")
        Class<T> realType = HibernateProxy.class.isAssignableFrom(type) ? (Class<T>) type.getSuperclass() : type;
        return cache.computeIfAbsent(
            realType,
            key -> {
                S store = stores.stream().filter(s -> s.getEntityClass() == key).findFirst().orElse(null);
                if (store == null) {
                    // as this is within the "loader" function this will only get
                    // logged once
                    log.warn("No IdentifiableObjectStore found for class: '{}'", realType);
                }
                return store;
            }
        );
    }
}
