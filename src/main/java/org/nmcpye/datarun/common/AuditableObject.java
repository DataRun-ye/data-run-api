package org.nmcpye.datarun.common;

import java.time.Instant;

/**
 * @author Hamza, 20/03/2025
 */
public interface AuditableObject<ID> extends PrimaryKeyObject<ID> {
    String getCreatedBy();

    Instant getCreatedDate();

    String getLastModifiedBy();

    Instant getLastModifiedDate();
}
