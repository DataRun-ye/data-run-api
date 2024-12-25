package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRelationalRepositoryCustom
    extends IdentifiableRelationalRepository<Assignment>, AssignmentRepositoryWithBagRelationships {

    default Optional<Assignment> findOneByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Optional<Assignment> findOneByUser(String uid) {
        return this.findOneWithToOneRelationshipsByUser(uid);
    }

    default Page<Assignment> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select assignment from Assignment assignment " +
            "join fetch assignment.parent p " +
            "join assignment.activity " +
            "join assignment.orgUnit " +
            "join assignment.team " +
            "where p.uid IN :parents"
    )
    List<Assignment> findAllByParentIn(@Param("parents") List<String> parents);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.activity.uid =:activityUid and user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) " +
            "from Assignment assignment " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByActivityAndUser(String activityUid, Pageable pageable);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByUser(Pageable pageable);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.activity.disabled =:disabled and " +
            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}"
    )
    List<Assignment> findAllByStatusUser(@Param("disabled") boolean disabled);


    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.activity.disabled =:disabled and " +
            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "join assignment.team.users user " +
            "where assignment.activity.disabled =:disabled and " +
            "assignment.team.disabled =:disabled and user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllByStatusAndUser(@Param("disabled") boolean disabled, Pageable pageable);

    @Query(
        value = "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}",
        countQuery = "select count(assignment) from Assignment assignment " +
            "join assignment.team.users user " +
            "where user.login = ?#{authentication.name}"
    )
    Page<Assignment> findAllWithToOneRelationshipsByUser(Pageable pageable);

    @Query(
        "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.id =:id and user.login = ?#{authentication.name}"
    )
    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("id") Long id);

    @Query(
        "select assignment from Assignment assignment " +
            "left join assignment.activity " +
            "left join assignment.orgUnit " +
            "left join assignment.team " +
            "join assignment.team.users user " +
            "where assignment.uid =:uid and user.login = ?#{authentication.name}"
    )
    Optional<Assignment> findOneWithToOneRelationshipsByUser(@Param("uid") String uid);
}
