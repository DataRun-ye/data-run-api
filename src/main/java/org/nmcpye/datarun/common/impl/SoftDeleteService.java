package org.nmcpye.datarun.common.impl;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.SoftDeleteObject;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Transactional
public interface SoftDeleteService<T extends SoftDeleteObject<ID>, ID>
    extends AuditableObjectService<T, ID> {
}
