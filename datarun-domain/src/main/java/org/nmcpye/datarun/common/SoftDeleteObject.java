package org.nmcpye.datarun.common;

/**
 * @author Hamza Assada, 20/03/2025
 */
public interface SoftDeleteObject<ID> extends AuditableObject<ID> {
    Boolean getDeleted();

    void setDeleted(Boolean deleted);
}
