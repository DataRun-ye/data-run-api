package org.nmcpye.datarun.drun.postgres.service;

import org.nmcpye.datarun.common.jpa.JpaAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing {@link UserGroup}.
 */
public interface UserGroupService
    extends JpaAuditableObjectService<UserGroup> {

    Page<UserGroup> findAllManagedByUser(Pageable pageable);

    List<UserGroup> findAllManagedByUser();
}
