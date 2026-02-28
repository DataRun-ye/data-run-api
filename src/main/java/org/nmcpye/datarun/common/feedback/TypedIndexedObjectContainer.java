package org.nmcpye.datarun.common.feedback;


import jakarta.annotation.Nonnull;
import org.nmcpye.datarun.common.AuditableObject;
import org.nmcpye.datarun.jpa.common.HibernateProxyUtils;

import java.util.HashMap;
import java.util.Map;

public class TypedIndexedObjectContainer implements ObjectIndexProvider {
    private final Map<Class<? extends AuditableObject<?>>, IndexedObjectContainer> typedIndexedObjectContainers = new HashMap<>();

    /**
     * Get the typed container for the specified object class.
     *
     * @param c the object class for which the container should be returned.
     * @return the container (if it does not exists, the method creates a
     * container).
     */
    @Nonnull
    public IndexedObjectContainer getTypedContainer(@Nonnull Class<? extends AuditableObject<?>> c) {
        return typedIndexedObjectContainers.computeIfAbsent(c, key -> new IndexedObjectContainer());
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Integer mergeObjectIndex(@Nonnull AuditableObject<?> object) {
        return getTypedContainer(HibernateProxyUtils.getRealClass(object)).mergeObjectIndex(object);
    }

    /**
     * @param object the identifiable object that should be checked.
     * @return <code>true</code> if the object is included in the container,
     * <code>false</code> otherwise.
     */
    public boolean containsObject(@Nonnull AuditableObject<?> object) {
        final IndexedObjectContainer indexedObjectContainer = typedIndexedObjectContainers
            .get(HibernateProxyUtils.getRealClass(object));
        return indexedObjectContainer != null && indexedObjectContainer.containsObject(object);
    }

    /**
     * Adds an object to the corresponding indexed object container.
     *
     * @param identifiableObject the object that should be added to the
     *                           container.
     */
    @SuppressWarnings("unchecked")
    public void add(@Nonnull AuditableObject<?> identifiableObject) {
        getTypedContainer(HibernateProxyUtils.getRealClass(identifiableObject)).add(identifiableObject);
    }
}
