package org.nmcpye.datarun.jpa.usegroup.repository;

import org.nmcpye.datarun.jpa.common.repository.JpaAuditableRepository;
import org.nmcpye.datarun.jpa.usegroup.UserGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    JpaAuditableRepository<UserGroup> {

    Optional<UserGroup> findByCode(String code);

    default Optional<UserGroup> findByUidWithEagerRelation(String uid) {
        return this.findByUid(uid).flatMap(this::fetchBagRelationships);
    }

    default Optional<UserGroup> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<UserGroup> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select ug from UserGroup ug " +
            "left join ug.users user " +
            "where user.login = ?#{authentication.name}"
    )
    List<UserGroup> findAllWithEagerRelation();

    @Query(
        "select ug from UserGroup ug " +
            "left join ug.users user " +
            "where user.login =:login and (:includeDisabled = true OR ug.disabled = false)"
    )
    List<UserGroup> findAllByUserLogin(@Param("login") String userLogin, boolean includeDisabled);

    @Query(
        value = "select ug from UserGroup ug " +
            "left join ug.users u " +
            "where u.login = ?#{authentication.name} and ug.disabled = false",
        countQuery = "select count(ug) from UserGroup ug " +
            "left join ug.users u " +
            "where u.login = ?#{authentication.name} and ug.disabled = false"
    )
    Page<UserGroup> findAllWithEagerRelation(Pageable pageable);

    @Query(
        value = "select ug from UserGroup ug " +
            "left join ug.users u " +
            "where u.login = ?#{authentication.name}",
        countQuery = "select count(ug) from UserGroup ug " +
            "left join ug.users u " +
            "where u.login = ?#{authentication.name}"
    )
    Page<UserGroup> findAllWithToOneRelationshipsByUser(Pageable pageable);

    @Query(
        "select ug from UserGroup ug " +
            "left join ug.users user " +
            "where ug.id =:id and user.login = ?#{authentication.name}"
    )
    Optional<UserGroup> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
