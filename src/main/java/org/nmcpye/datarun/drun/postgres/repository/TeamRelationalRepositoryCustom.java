package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Team entity.
 */
@Repository
public interface TeamRelationalRepositoryCustom
    extends TeamRepositoryWithBagRelationships,
    IdentifiableRelationalRepository<Team> {

    Optional<Team> findByCodeAndActivityUid(String code, String activityUid);

//    default Page<Team> findAllByUser(Pageable pageable) {
//        if (!SecurityUtils.isAuthenticated()) {
//            return Page.empty(pageable);
//        }
//
//        String userLogin = SecurityUtils.getCurrentUserLogin().get();
//
//        Specification<Team> specification = Specification.where(TeamSpecifications.canRead(userLogin))
//            .and(TeamSpecifications.isNotDisabled());
//
//        return this.fetchBagRelationships(this.findAll(specification, pageable));
//    }

//    default List<Team> findAllByUser() {
//        if (!SecurityUtils.isAuthenticated()) {
//            return Collections.emptyList();
//        }
//
//        String userLogin = SecurityUtils.getCurrentUserLogin().get();
//
//        Specification<Team> specification = Specification.where(TeamSpecifications.canRead(userLogin))
//            .and(TeamSpecifications.isNotDisabled());
//
//        return this.fetchBagRelationships(this.findAll(specification));
//    }

    default Optional<Team> findByUidWithEagerRelation(String uid) {
        return this.fetchBagRelationships(this.findByUid(uid));
    }

    default Optional<Team> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<Team> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }


    @Query(
        value = "select team from Team team " +
            "left join team.activity " +
            "left join team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    List<Team> findAllWithEagerRelation();

    @Query(
        value = "select team from Team team " +
            "left join team.activity ac " +
            "left join team.users u " +
            "where u.login = ?#{authentication.name} and team.disabled = false and ac.disabled = false",
        countQuery = "select count(team) from Team team " +
            "left join team.activity ac " +
            "left join team.users u " +
            "where u.login = ?#{authentication.name} and team.disabled = false and ac.disabled = false"
    )
    Page<Team> findAllWithEagerRelation(Pageable pageable);

    @Query(
        value = "select team from Team team " +
            "left join fetch team.activity " +
            "left join team.users u " +
            "where u.login = ?#{authentication.name}",
        countQuery = "select count(team) from Team team " +
            "left join team.users u " +
            "where u.login = ?#{authentication.name}"
    )
    Page<Team> findAllWithToOneRelationshipsByUser(Pageable pageable);

    @Query(
        "select team from Team team " +
            "left join fetch team.activity " +
            "left join team.users user " +
            "where team.id =:id and user.login = ?#{authentication.name}"
    )
    Optional<Team> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
