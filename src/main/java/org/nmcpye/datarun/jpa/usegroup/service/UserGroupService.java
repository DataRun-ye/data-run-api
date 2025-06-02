package org.nmcpye.datarun.jpa.usegroup.service;

import org.nmcpye.datarun.jpa.common.JpaAuditableObjectService;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
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
