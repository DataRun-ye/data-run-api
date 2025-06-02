package org.nmcpye.datarun.common;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Transactional
public interface SoftDeleteService<T extends SoftDeleteObject<ID>, ID>
    extends AuditableObjectService<T, ID> {
}
