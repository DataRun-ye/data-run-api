package org.nmcpye.datarun.common;

import java.time.Instant;

/**
 * @author Hamza, 20/03/2025
 */
public interface AuditableObject<ID> extends PrimaryKeyObject<ID> {
    void setId(ID id);

    String getCreatedBy();

    void setCreatedBy(String user);

    Instant getCreatedDate();

    String getLastModifiedBy();

    void setLastModifiedBy(String user);

    Instant getLastModifiedDate();

}
