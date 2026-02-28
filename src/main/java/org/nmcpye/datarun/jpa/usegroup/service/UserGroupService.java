package org.nmcpye.datarun.jpa.usegroup.service;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableObjectService;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing {@link UserGroup}.
 */
public interface UserGroupService
    extends JpaIdentifiableObjectService<UserGroup> {

    Page<UserGroup> findAllManagedByUser(Pageable pageable);
}
