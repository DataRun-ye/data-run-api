package org.nmcpye.datarun.drun.postgres.service.indentifieble;

import org.nmcpye.datarun.domain.common.IdentifiableObject;

public interface IdentifiableRelationalService
    <T extends IdentifiableObject<Long>>
    extends IdentifiableService<T, Long> {
}
