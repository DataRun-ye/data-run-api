package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.drun.postgres.common.UserGroupSpecifications;
import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Service Interface for managing {@link UserGroup}.
 */
public interface UserGroupService
    extends IdentifiableRelationalService<UserGroup> {

    @Override
    default Specification<UserGroup> canRead() {
        return UserGroupSpecifications.canRead();
    }

    Page<UserGroup> findAllManagedByUser(Pageable pageable);

    List<UserGroup> findAllManagedByUser();
}
