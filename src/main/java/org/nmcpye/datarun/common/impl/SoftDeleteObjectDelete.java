package org.nmcpye.datarun.common.impl;

import org.nmcpye.datarun.common.AuditableObjectService;
import org.nmcpye.datarun.common.SoftDeleteObject;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Hamza Assada, 20/03/2025
 */
@Transactional
public interface SoftDeleteObjectDelete<T extends SoftDeleteObject<ID>, ID>
    extends AuditableObjectService<T, ID> {

    @Override
    default void deleteByUid(String uid) {
        final var toMarkAsDeleted = findByUid(uid).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(ErrorCode.E1106, "Id", uid)));
        toMarkAsDeleted.setDeleted(Boolean.TRUE);
        save(toMarkAsDeleted);
    }

    @Override
    default void delete(T object) {
        final var toMarkAsDeleted = findByIdentifyingProperties(object).orElseThrow(() ->
            new IllegalQueryException(new ErrorMessage(ErrorCode.E1109,
                object.getClass().getSimpleName(), object.getUid())));
        toMarkAsDeleted.setDeleted(Boolean.TRUE);
        save(toMarkAsDeleted);
    }
}
