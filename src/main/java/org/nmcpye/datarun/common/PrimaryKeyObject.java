package org.nmcpye.datarun.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * <p>
 * Common interface for objects that have a unique ID used in RESTful APIs but
 * that might not have use for a name and other fundamentals that come with
 * {@link IdentifiableObject}s.
 */
public interface PrimaryKeyObject<ID> extends Serializable {

    ID getId();

    void setId(ID id);

    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    String getUid();

    void setUid(String uid);

    /**
     * Returns the value of the property referred to by the given IdScheme.
     *
     * @param idScheme the IdScheme.
     * @return the value of the property referred to by the IdScheme.
     */
    @JsonIgnore
    String getPropertyValue(IdScheme idScheme);
}
