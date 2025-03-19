package org.nmcpye.datarun.mongo.common;

import org.nmcpye.datarun.common.PrimaryKeyObject;
import org.nmcpye.datarun.drun.postgres.common.Identifiable;

/**
 * <p>
 * Common interface for objects that have a unique ID used in RESTful APIs but
 * that might not have use for a name and other fundamentals that come with
 * {@link Identifiable}s.
 */
public interface PrimaryKeyStringObject extends PrimaryKeyObject<String> {
    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    String getUid();
}
