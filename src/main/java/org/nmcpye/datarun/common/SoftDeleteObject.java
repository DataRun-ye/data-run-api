package org.nmcpye.datarun.common;

import java.time.Instant;

/**
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
public interface SoftDeleteObject<ID> extends IdentifiableObject<ID> {
    Boolean getDeleted();

    Instant getDeletedAt();

    void setDeletedAt(Instant deletedAt);

    void setDeleted(Boolean deleted);
}
