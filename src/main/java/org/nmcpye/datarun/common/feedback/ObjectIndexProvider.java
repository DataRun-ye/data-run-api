package org.nmcpye.datarun.common.feedback;


import org.nmcpye.datarun.common.AuditableObject;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface ObjectIndexProvider {
    /**
     * Returns the object index for the specified object. If the object has not
     * yet an index, an index will be created.
     *
     * @param object the object for which an index should be returned.
     * @return the index of the specified object.
     */
    @Nonnull
    Integer mergeObjectIndex(@Nonnull AuditableObject<?> object);
}
