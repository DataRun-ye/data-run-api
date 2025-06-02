package org.nmcpye.datarun.common;

import java.time.Instant;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface SoftDeleteObject<ID> extends AuditableObject<ID> {
    Boolean getDeleted();

    Instant getDeletedAt();

    void setDeletedAt(Instant deletedAt);

    void setDeleted(Boolean deleted);
}
