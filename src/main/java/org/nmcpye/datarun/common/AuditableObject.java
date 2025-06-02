package org.nmcpye.datarun.common;

import java.time.Instant;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface AuditableObject<ID> extends PrimaryKeyObject<ID> {

    String getCreatedBy();

    void setCreatedBy(String user);

    Instant getCreatedDate();

    String getLastModifiedBy();

    void setLastModifiedBy(String user);

    Instant getLastModifiedDate();

    @Override
    default String getPropertyValue(IdScheme idScheme) {
        if (idScheme.isNull() || idScheme.is(IdentifiableProperty.UID)) {
            return getUid();
        }

        return null;
    }
}
