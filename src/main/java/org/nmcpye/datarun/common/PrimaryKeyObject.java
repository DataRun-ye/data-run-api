package org.nmcpye.datarun.common;

import org.nmcpye.datarun.drun.postgres.common.Identifiable;

import java.io.Serializable;

/**
 * <p>
 * Common interface for objects that have a unique ID used in RESTful APIs but
 * that might not have use for a name and other fundamentals that come with
 * {@link Identifiable}s.
 */
public interface PrimaryKeyObject<T> extends Serializable {
    /**
     * @return internal unique ID of the object as used in the database
     */
    T getId();

    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    String getUid();
}
