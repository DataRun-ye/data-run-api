package org.nmcpye.datarun.common;

import java.time.Instant;

/**
 * @author Hamza Assada 20/03/2025 <7amza.it@gmail.com>
 */
public interface AuditableObject<ID> extends WithIdentifierEntity<ID> {

    String getCreatedBy();

    void setCreatedBy(String user);

    Instant getCreatedDate();

    String getLastModifiedBy();

    void setLastModifiedBy(String user);

    Instant getLastModifiedDate();
}
