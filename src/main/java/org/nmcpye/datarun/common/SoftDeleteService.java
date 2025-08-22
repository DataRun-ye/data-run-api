package org.nmcpye.datarun.common;

import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada 20/03/2025 (7amza.it@gmail.com)
 */
@Transactional
public interface SoftDeleteService<T extends SoftDeleteObject<ID>, ID>
    extends IdentifiableObjectService<T, ID> {
    void softDelete(T object);
}
