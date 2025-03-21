package org.nmcpye.datarun.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * <p>
 * Common interface for objects that have a unique ID used in RESTful APIs but
 * that might not have use for a name and other fundamentals that come with
 * {@link IdentifiableObject}s.
 */
public interface PrimaryKeyObject<T> extends Serializable {

    @JsonIgnore
    T getId();

    /**
     * @return external unique ID of the object as used in the RESTful API
     */
    @JsonProperty(value = "id")
    String getUid();
}
