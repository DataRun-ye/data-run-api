package org.nmcpye.datarun.jpa.usegroup.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the UserGroup entity.
 */
@Repository
public interface UserGroupRepository
    extends UserGroupRepositoryWithBagRelationships,
        JpaIdentifiableRepository<UserGroup> {

    Optional<UserGroup> findByCode(String code);

    @Query(
        "select ug from UserGroup ug " +
            "left join ug.users user " +
            "where user.login =:login and (:includeDisabled = true OR ug.disabled = false)"
    )
    List<UserGroup> findAllByUserLogin(@Param("login") String userLogin, boolean includeDisabled);
}
