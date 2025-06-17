package org.nmcpye.datarun.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * <p>
 * Common interface for objects that have a unique Key used in RESTful APIs but
 * that might not have use for a name and other fundamentals that come with
 * {@link IdentifiableObject}s.
 */
public interface WithIdentifierEntity<ID> extends Serializable {

    ID getId();

    /**
     * Returns the value of the property referred to by the given IdScheme.
     *
     * @param idScheme the IdScheme.
     * @return the value of the property referred to by the IdScheme.
     */
    @JsonIgnore
    default ID getPropertyValue(IdScheme idScheme) {
        if (idScheme.isNull() || idScheme.is(IdentifiableProperty.ID)) {
            return getId();
        }

        return null;
    }
}
