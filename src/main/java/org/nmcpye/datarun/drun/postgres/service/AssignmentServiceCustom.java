package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssignmentServiceCustom
    extends IdentifiableRelationalService<Assignment> {
    Page<Assignment> findAllByUser(Pageable pageable);
}
