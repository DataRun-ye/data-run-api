package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.UserGroup;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepositoryWithBagRelationships {
    Optional<UserGroup> fetchBagRelationships(UserGroup groups);

    List<UserGroup> fetchBagRelationships(List<UserGroup> groups);

    Page<UserGroup> fetchBagRelationships(Page<UserGroup> groups);
}
